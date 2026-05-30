package net.kdt.pojavlaunch.fragments

import android.content.ContentResolver
import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.OnShowListener
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.core.math.MathUtils
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kdt.mcgui.ProgressLayout
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.PojavApplication
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.Tools.getFileName
import net.kdt.pojavlaunch.Tools.showErrorRemote
import net.kdt.pojavlaunch.modloaders.modpacks.ModItemAdapter
import net.kdt.pojavlaunch.modloaders.modpacks.ModItemAdapter.SearchResultCallback
import net.kdt.pojavlaunch.modloaders.modpacks.api.CommonApi
import net.kdt.pojavlaunch.modloaders.modpacks.api.ModpackApi
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchFilters
import net.kdt.pojavlaunch.profiles.VersionSelectorDialog.open
import net.kdt.pojavlaunch.profiles.VersionSelectorListener
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper.removeTaskCountListener
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class SearchModFragment : Fragment(R.layout.fragment_mod_search), SearchResultCallback {
    private var mOverlay: View? = null
    private var mOverlayTopCache = 0f // Padding cache reduce resource lookup

    private val mOverlayPositionListener: RecyclerView.OnScrollListener =
        object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                mOverlay?.let {
                    it.y = MathUtils.clamp(
                        it.y - dy,
                        -it.height.toFloat(),
                        mOverlayTopCache
                    )
                }
            }
        }

    private var mSearchEditText: EditText? = null
    private var mFilterButton: ImageButton? = null
    private var mRecyclerview: RecyclerView? = null
    private var mModItemAdapter: ModItemAdapter? = null
    private var mSearchProgressBar: ProgressBar? = null
    private var mStatusTextView: TextView? = null
    private var mDefaultTextColor: ColorStateList? = null
    private var modpackApi: ModpackApi? = null

    private val mSearchFilters: SearchFilters = SearchFilters().apply {
        isModpack = true
    }

    private var mImportButton: Button? = null

    var mImportLauncher: ActivityResultLauncher<String?> = registerForActivityResult(
        GetContent(),
        object : ActivityResultCallback<Uri?> {
            override fun onActivityResult(uri: Uri?) {
                if (uri == null) return
                val context = context
                val contentResolver = context?.contentResolver
                if (context != null && contentResolver != null) {
                    PojavApplication.sExecutorService.execute {
                        performLocalInstall(uri, context, contentResolver)
                    }
                }
            }
        })

    fun performLocalInstall(uri: Uri, context: Context, contentResolver: ContentResolver) {
        val fileName = getFileName(context, uri)
        if (fileName == null) return
        val outFile = File(Tools.DIR_CACHE, fileName + ".cf")
        ProgressLayout.setProgress(
            ProgressLayout.INSTALL_MODPACK,
            0,
            R.string.multirt_progress_caching
        )
        try {
            contentResolver.openInputStream(uri).use { inputStream ->
                FileOutputStream(outFile).use { outputStream ->
                    if (inputStream == null) return
                    IOUtils.copy(inputStream, outputStream)
                    outputStream.flush()
                }
            }
        } catch (e: IOException) {
            showErrorRemote("Error", e)
            ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK)
            return
        }
        try {
            modpackApi?.installLocalModpack(fileName, outFile, null)
        } catch (e: IOException) {
            showErrorRemote("Error", e)
        } finally {
            outFile.delete()
            ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        modpackApi = CommonApi(context.getString(R.string.curseforge_api_key))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val api = modpackApi ?: return
        // You can only access resources after attaching to current context
        mModItemAdapter = ModItemAdapter(resources, api, this)
        mModItemAdapter?.let { ProgressKeeper.addTaskCountListener(it) }
        mOverlayTopCache = resources.getDimension(R.dimen.fragment_padding_medium)

        mOverlay = view.findViewById(R.id.search_mod_overlay)
        mSearchEditText = view.findViewById(R.id.search_mod_edittext)
        mSearchProgressBar = view.findViewById(R.id.search_mod_progressbar)
        mRecyclerview = view.findViewById(R.id.search_mod_list)
        mStatusTextView = view.findViewById(R.id.search_mod_status_text)
        mFilterButton = view.findViewById(R.id.search_mod_filter)

        mDefaultTextColor = mStatusTextView?.textColors

        mRecyclerview!!.layoutManager = LinearLayoutManager(context)
        mRecyclerview!!.adapter = mModItemAdapter

        mRecyclerview!!.addOnScrollListener(mOverlayPositionListener)

        mSearchEditText!!.setOnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent? ->
            searchMods(mSearchEditText!!.text.toString())
            mSearchEditText!!.clearFocus()
            false
        }

        mOverlay!!.post {
            val overlayHeight = mOverlay!!.height
            mRecyclerview!!.setPadding(
                mRecyclerview!!.paddingLeft,
                mRecyclerview!!.paddingTop + overlayHeight,
                mRecyclerview!!.paddingRight,
                mRecyclerview!!.paddingBottom
            )
        }
        mFilterButton!!.setOnClickListener { v: View? -> displayFilterDialog() }
        mImportButton = view.findViewById(R.id.mineButton_import_local_modpack)
        mImportButton!!.setOnClickListener { v: View? ->
            mImportLauncher.launch("*/*")
        }

        searchMods(null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        removeTaskCountListener(mModItemAdapter)
        mRecyclerview?.removeOnScrollListener(mOverlayPositionListener)
    }

    override fun onSearchFinished() {
        mSearchProgressBar!!.visibility = View.GONE
        mStatusTextView!!.visibility = View.GONE
    }

    override fun onSearchError(error: Int) {
        mSearchProgressBar!!.visibility = View.GONE
        mStatusTextView!!.visibility = View.VISIBLE
        when (error) {
            SearchResultCallback.ERROR_INTERNAL -> {
                mStatusTextView!!.setTextColor(Color.RED)
                mStatusTextView!!.setText(R.string.search_modpack_error)
            }

            SearchResultCallback.ERROR_NO_RESULTS -> {
                mStatusTextView!!.setTextColor(mDefaultTextColor)
                mStatusTextView!!.setText(R.string.search_modpack_no_result)
            }
        }
    }

    private fun searchMods(name: String?) {
        mSearchProgressBar!!.visibility = View.VISIBLE
        mSearchFilters.name = name ?: ""
        mModItemAdapter?.performSearchQuery(mSearchFilters)
    }

    private fun displayFilterDialog() {
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(R.layout.dialog_mod_filters)
            .create()

        // setup the view behavior
        dialog.setOnShowListener { dialogInterface: DialogInterface? ->
            val mSelectedVersion =
                dialog.findViewById<TextView>(R.id.search_mod_selected_mc_version_textview)
            val mSelectVersionButton =
                dialog.findViewById<Button>(R.id.search_mod_mc_version_button)
            val mApplyButton = dialog.findViewById<Button>(R.id.search_mod_apply_filters)

            if (mSelectVersionButton == null || mSelectedVersion == null || mApplyButton == null) return@setOnShowListener

            // Setup the expendable list behavior
            mSelectVersionButton.setOnClickListener { v: View? ->
                open(
                    v!!.context,
                    true,
                    object : VersionSelectorListener {
                        override fun onVersionSelected(versionId: String?, isSnapshot: Boolean) {
                            mSelectedVersion.text = versionId
                        }
                    })
            }

            // Apply visually all the current settings
            mSelectedVersion.text = mSearchFilters.mcVersion

            // Apply the new settings
            mApplyButton.setOnClickListener { v: View? ->
                mSearchFilters.mcVersion = mSelectedVersion.text.toString()
                searchMods(mSearchEditText?.text?.toString())
                dialogInterface?.dismiss()
            }
        }

        dialog.show()
    }

    companion object {
        const val TAG: String = "SearchModFragment"
    }
}
