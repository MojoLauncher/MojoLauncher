package net.kdt.pojavlaunch.modloaders.modpacks

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.kdt.SimpleArrayAdapter
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.PojavApplication
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.modloaders.modpacks.SelfReferencingFuture.FutureInterface
import net.kdt.pojavlaunch.modloaders.modpacks.api.ModpackApi
import net.kdt.pojavlaunch.modloaders.modpacks.imagecache.ImageReceiver
import net.kdt.pojavlaunch.modloaders.modpacks.imagecache.ModIconCache
import net.kdt.pojavlaunch.modloaders.modpacks.models.Constants
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModDetail
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModItem
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchFilters
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchResult
import net.kdt.pojavlaunch.progresskeeper.TaskCountListener
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.Future

class ModItemAdapter(
    resources: Resources,
    private val mModpackApi: ModpackApi,
    private val mSearchResultCallback: SearchResultCallback
) : RecyclerView.Adapter<RecyclerView.ViewHolder?>(), TaskCountListener {
    /* Used when versions haven't loaded yet, default text to reduce layout shifting */
    private val mLoadingAdapter = SimpleArrayAdapter<String?>(mutableListOf<String?>("Loading"))

    /* This my seem horribly inefficient but it is in fact the most efficient way without effectively writing a weak collection from scratch */
    private val mViewHolderSet: MutableSet<ViewHolder> = Collections.newSetFromMap(
        WeakHashMap<ViewHolder, Boolean>()
    )
    private val mIconCache = ModIconCache()
    private var mModItems: Array<ModItem?>?

    /* Cache for ever so slightly rounding the image for the corner not to stick out of the layout */
    private val mCornerDimensionCache: Float = resources.getDimension(R.dimen._1sdp) / 250

    private var mTaskInProgress: Future<*>? = null
    private var mSearchFilters: SearchFilters? = null
    private var mCurrentResult: SearchResult? = null
    private var mLastPage = false
    private var mTasksRunning = false


    init {
        mModItems = emptyArray()
    }

    fun performSearchQuery(searchFilters: SearchFilters?) {
        if (mTaskInProgress != null) {
            mTaskInProgress!!.cancel(true)
            mTaskInProgress = null
        }
        this.mSearchFilters = searchFilters
        this.mLastPage = false
        mTaskInProgress = SelfReferencingFuture(SearchApiTask(mSearchFilters, null))
            .startOnExecutor(PojavApplication.sExecutorService)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(viewGroup.context)
        val view: View
        when (viewType) {
            VIEW_TYPE_MOD_ITEM -> {
                // Create a new view, which defines the UI of the list item
                view = layoutInflater.inflate(R.layout.view_mod, viewGroup, false)
                return ViewHolder(view)
            }

            VIEW_TYPE_LOADING -> {
                // Create a new view, which is actually just the progress bar
                view = layoutInflater.inflate(R.layout.view_loading, viewGroup, false)
                return LoadingViewHolder(view)
            }

            else -> throw RuntimeException("Unimplemented view type!")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            VIEW_TYPE_MOD_ITEM -> (holder as ViewHolder).setStateLimited(mModItems!![position])
            VIEW_TYPE_LOADING -> loadMoreResults()
            else -> throw RuntimeException("Unimplemented view type!")
        }
    }

    override fun getItemCount(): Int {
        if (mLastPage || mModItems!!.isEmpty()) return mModItems!!.size
        return mModItems!!.size + 1
    }

    private fun loadMoreResults() {
        if (mTaskInProgress != null) return
        mTaskInProgress = SelfReferencingFuture(SearchApiTask(mSearchFilters, mCurrentResult))
            .startOnExecutor(PojavApplication.sExecutorService)
    }

    override fun getItemViewType(position: Int): Int {
        if (position < mModItems!!.size) return VIEW_TYPE_MOD_ITEM
        return VIEW_TYPE_LOADING
    }

    override fun onUpdateTaskCount(taskCount: Int): Boolean {
        Tools.runOnUiThread {
            mTasksRunning = taskCount != 0
            for (viewHolder in mViewHolderSet) {
                viewHolder.updateInstallButtonState()
            }
        }
        return false
    }


    /**
     * Basic viewholder with expension capabilities
     */
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private var mModDetail: ModDetail? = null
        private var mModItem: ModItem? = null
        private val mTitle: TextView = view.findViewById(R.id.mod_title_textview)
        private val mDescription: TextView = view.findViewById(R.id.mod_body_textview)
        private val mIconView: ImageView = view.findViewById(R.id.mod_thumbnail_imageview)
        private val mSourceView: ImageView = view.findViewById(R.id.mod_source_imageview)
        private var mExtendedLayout: View? = null
        private var mExtendedSpinner: Spinner? = null
        private var mExtendedButton: Button? = null
        private var mExtendedErrorTextView: TextView? = null
        private var mExtensionFuture: Future<*>? = null
        private var mThumbnailBitmap: Bitmap? = null
        private var mImageReceiver: ImageReceiver? = null
        private var mInstallEnabled = false

        /* Used to display available versions of the mod(pack) */
        private val mVersionAdapter = SimpleArrayAdapter<String?>(null)

        init {
            mViewHolderSet.add(this)
            view.setOnClickListener { v: View? ->
                if (!hasExtended()) {
                    // Inflate the ViewStub
                    mExtendedLayout =
                        (v!!.findViewById<View?>(R.id.mod_limited_state_stub) as ViewStub).inflate()
                    mExtendedButton =
                        mExtendedLayout!!.findViewById(R.id.mod_extended_select_version_button)
                    mExtendedSpinner =
                        mExtendedLayout!!.findViewById(R.id.mod_extended_version_spinner)
                    mExtendedErrorTextView =
                        mExtendedLayout!!.findViewById(R.id.mod_extended_error_textview)

                    mExtendedButton!!.setOnClickListener {
                        mModpackApi.handleModpackInstallation(
                            mExtendedButton!!.context.applicationContext,
                            mModDetail,
                            mExtendedSpinner!!.selectedItemPosition
                        )
                    }
                    mExtendedSpinner!!.adapter = mLoadingAdapter
                } else {
                    if (this.isExtended) closeDetailedView()
                    else openDetailedView()
                }
                if (this.isExtended && mModDetail == null && mExtensionFuture == null) { // only reload if no reloads are in progress
                    setDetailedStateDefault()
                    /*
                     * Why do we do this?
                     * The reason is simple: multithreading is difficult as hell to manage
                     * Let me explain:
                     */
                    mExtensionFuture =
                        SelfReferencingFuture(object : FutureInterface {
                            override fun run(myFuture: Future<*>?) {
                                /*
                             * While we are sitting in the function below doing networking, the view might have already gotten recycled.
                             * If we didn't use a Future, we would have extended a ViewHolder with completely unrelated content
                             * or with an error that has never actually happened
                             */
                                mModDetail = mModpackApi.getModDetails(mModItem)
                                println(mModDetail)
                                Tools.runOnUiThread {
                                    /*
                                                    * Once we enter here, the state we're in is already defined - no view shuffling can happen on the UI
                                                    * thread while we are on the UI thread ourselves. If we were cancelled, this means that the future
                                                    * we were supposed to have no longer makes sense, so we return and do not alter the state (since we might
                                                    * alter the state of an unrelated item otherwise)
                                                    */
                                    if (myFuture!!.isCancelled) return@runOnUiThread
                                    /*
                                 * We do not null the future before returning since this field might already belong to a different item with its
                                 * own Future, which we don't want to interfere with.
                                 * But if the future is not cancelled, it is the right one for this ViewHolder, and we don't need it anymore, so
                                 * let's help GC clean it up once we exit!
                                 */
                                    mExtensionFuture = null
                                    setStateDetailed(mModDetail)
                                }
                            }
                        }).startOnExecutor(PojavApplication.sExecutorService)
                }
            }
        }

        /** Display basic info about the moditem  */
        fun setStateLimited(item: ModItem?) {
            mModDetail = null
            if (mThumbnailBitmap != null) {
                mIconView.setImageBitmap(null)
                mThumbnailBitmap!!.recycle()
            }
            if (mImageReceiver != null) {
                mIconCache.cancelImage(mImageReceiver)
            }
            if (mExtensionFuture != null) {
                /*
                 * Since this method reinitializes the ViewHolder for a new mod, this Future stops being ours, so we cancel it
                 * and null it. The rest is handled above
                 */
                mExtensionFuture!!.cancel(true)
                mExtensionFuture = null
            }

            mModItem = item
            // here the previous reference to the image receiver will disappear
            mImageReceiver = object : ImageReceiver {
                override fun onImageAvailable(image: Bitmap?) {
                    mImageReceiver = null
                    mThumbnailBitmap = image
                    val drawable = RoundedBitmapDrawableFactory.create(mIconView.resources, image)
                    drawable.cornerRadius = mCornerDimensionCache * image!!.height
                    mIconView.setImageDrawable(drawable)
                }
            }
            mIconCache.getImage(mImageReceiver, mModItem!!.iconCacheTag, mModItem!!.imageUrl)
            mSourceView.setImageResource(getSourceDrawable(item!!.apiSource))
            mTitle.text = item.title
            mDescription.text = item.description

            if (hasExtended()) {
                closeDetailedView()
            }
        }

        /** Display extended info/interaction about a modpack  */
        private fun setStateDetailed(detailedItem: ModDetail?) {
            if (detailedItem != null) {
                setInstallEnabled(true)
                mExtendedErrorTextView!!.visibility = View.GONE
                mVersionAdapter.setObjects(detailedItem.versionNames?.toMutableList())
                mExtendedSpinner!!.adapter = mVersionAdapter
            } else {
                closeDetailedView()
                setInstallEnabled(false)
                mExtendedErrorTextView!!.visibility = View.VISIBLE
                mExtendedSpinner!!.adapter = null
                mVersionAdapter.setObjects(null)
            }
        }

        private fun openDetailedView() {
            mExtendedLayout!!.visibility = View.VISIBLE
            mDescription.maxLines = 99

            // We need to align to the longer section
            val futureBottom =
                mDescription.bottom + Tools.mesureTextviewHeight(mDescription) - mDescription.height
            val params = mExtendedLayout!!.layoutParams as ConstraintLayout.LayoutParams
            params.topToBottom =
                if (futureBottom > mIconView.bottom) R.id.mod_body_textview else R.id.mod_thumbnail_imageview
            mExtendedLayout!!.layoutParams = params
        }

        private fun closeDetailedView() {
            mExtendedLayout!!.visibility = View.GONE
            mDescription.maxLines = 3
        }

        private fun setDetailedStateDefault() {
            setInstallEnabled(false)
            mExtendedSpinner!!.adapter = mLoadingAdapter
            mExtendedErrorTextView!!.visibility = View.GONE
            openDetailedView()
        }

        private fun hasExtended(): Boolean {
            return mExtendedLayout != null
        }

        private val isExtended: Boolean
            get() = hasExtended() && mExtendedLayout!!.isVisible

        private fun getSourceDrawable(apiSource: Int): Int {
            return when (apiSource) {
                Constants.SOURCE_CURSEFORGE -> R.drawable.ic_curseforge
                Constants.SOURCE_MODRINTH -> R.drawable.ic_modrinth
                else -> throw RuntimeException("Unknown API source")
            }
        }

        private fun setInstallEnabled(enabled: Boolean) {
            mInstallEnabled = enabled
            updateInstallButtonState()
        }

        internal fun updateInstallButtonState() {
            if (mExtendedButton != null) mExtendedButton!!.isEnabled = mInstallEnabled && !mTasksRunning
        }
    }

    /**
     * The view holder used to hold the progress bar at the end of the list
     */
    private class LoadingViewHolder(view: View) : RecyclerView.ViewHolder(view)

    private inner class SearchApiTask(
        private val mSearchFilters: SearchFilters?,
        private val mPreviousResult: SearchResult?
    ) : FutureInterface {
        @SuppressLint("NotifyDataSetChanged")
        override fun run(myFuture: Future<*>?) {
            val result = mModpackApi.searchMod(mSearchFilters, mPreviousResult)
            var resultModItems = result?.results
            if (!resultModItems.isNullOrEmpty() && mPreviousResult != null) {
                val newModItems = arrayOfNulls<ModItem>(resultModItems.size + mModItems!!.size)
                System.arraycopy(mModItems!!, 0, newModItems, 0, mModItems!!.size)
                System.arraycopy(
                    resultModItems,
                    0,
                    newModItems,
                    mModItems!!.size,
                    resultModItems.size
                )
                resultModItems = newModItems
            }
            val finalModItems = resultModItems
            Tools.runOnUiThread {
                if (myFuture!!.isCancelled) return@runOnUiThread
                mTaskInProgress = null
                if (finalModItems == null) {
                    mSearchResultCallback.onSearchError(SearchResultCallback.ERROR_INTERNAL)
                } else if (finalModItems.isEmpty()) {
                    if (mPreviousResult != null) {
                        mLastPage = true
                        notifyItemChanged(mModItems!!.size)
                        mSearchResultCallback.onSearchFinished()
                        return@runOnUiThread
                    }
                    mSearchResultCallback.onSearchError(SearchResultCallback.ERROR_NO_RESULTS)
                } else {
                    mSearchResultCallback.onSearchFinished()
                }
                mCurrentResult = result
                if (finalModItems == null) {
                    mModItems = MOD_ITEMS_EMPTY
                    notifyDataSetChanged()
                    return@runOnUiThread
                }
                if (mPreviousResult != null) {
                    val prevLength = mModItems!!.size
                    mModItems = finalModItems
                    notifyItemChanged(prevLength)
                    notifyItemRangeInserted(prevLength + 1, mModItems!!.size)
                } else {
                    mModItems = finalModItems
                    notifyDataSetChanged()
                }
            }
        }
    }

    interface SearchResultCallback {
        fun onSearchFinished()
        fun onSearchError(error: Int)

        companion object {
            const val ERROR_INTERNAL: Int = 0
            const val ERROR_NO_RESULTS: Int = 1
        }
    }

    companion object {
        private val MOD_ITEMS_EMPTY = arrayOfNulls<ModItem>(0)
        private const val VIEW_TYPE_MOD_ITEM = 0
        private const val VIEW_TYPE_LOADING = 1
    }
}
