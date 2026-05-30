package com.kdt.mcgui

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.transition.Slide
import android.transition.Transition
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.GridView
import android.widget.ListView
import android.widget.PopupWindow
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.FragmentActivity
import fr.spse.extended_view.ExtendedTextView
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.PojavApplication
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.Tools.runOnUiThread
import net.kdt.pojavlaunch.Tools.showError
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.extra.ExtraListener
import net.kdt.pojavlaunch.fragments.ProfileTypeSelectFragment
import net.kdt.pojavlaunch.instances.DisplayInstance
import net.kdt.pojavlaunch.instances.InstanceAdapter
import net.kdt.pojavlaunch.instances.InstanceAdapterExtra
import net.kdt.pojavlaunch.instances.Instances
import net.kdt.pojavlaunch.instances.Instances.Companion.loadDisplay
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import java.io.IOException

/**
 * A class implementing custom spinner like behavior, notably:
 * dropdown popup view with a custom direction.
 */
class mcVersionSpinner : ExtendedTextView {
    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    /* The class is in charge of displaying its own list with adapter content being known in advance */
    private var mListView: ListView? = null
    private var mSelectorView: AdapterView<*>? = null
    private var mPopupWindow: PopupWindow? = null
    private var mPopupAnimation: Any? = null
    private var mSelectedIndex = 0

    private var mProfileAdapter: InstanceAdapter? = null


    /** Set the selection AND saves it as a shared preference  */
    fun setProfileSelection(position: Int) {
        setSelection(position)
        Instances.setSelectedInstance((mProfileAdapter!!.getItem(position) as DisplayInstance?)!!)
    }

    fun setSelection(position: Int) {
        if (mSelectorView != null) mSelectorView!!.setSelection(position)
        mProfileAdapter!!.setView(this, position, false)
        mSelectedIndex = position
        mProfileAdapter!!.applySelectionIndex(mSelectedIndex)
    }

    private fun applyInstances(instances: Instances) {
        mProfileAdapter!!.applyInstances(instances)
        setSelection(instances.selectedIndex)
    }

    /** Reload profiles from the file, forcing the spinner to consider the new data  */
    fun reloadProfiles() {
        PojavApplication.sExecutorService.execute(Runnable {
            try {
                val instances = loadDisplay()
                runOnUiThread(Runnable { applyInstances(instances) })
            } catch (e: IOException) {
                runOnUiThread(Runnable { showError(getContext(), e) })
            }
        })
    }

    /** Initialize various behaviors  */
    private fun init() {
        // Setup various attributes
        setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            getResources().getDimensionPixelSize(R.dimen._12ssp).toFloat()
        )
        setGravity(Gravity.CENTER_VERTICAL)


        // Use a more modern padding if not set in XML
        if (getPaddingStart() == 0) {
            val startPadding = getContext().getResources().getDimensionPixelOffset(R.dimen._12sdp)
            val endPadding = getContext().getResources().getDimensionPixelOffset(R.dimen._8sdp)
            setPaddingRelative(startPadding, 0, endPadding, 0)
            setCompoundDrawablePadding(startPadding)
        }

        var addIcon = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_add, null)
        if (addIcon != null) {
            addIcon = DrawableCompat.wrap(addIcon).mutate()
            DrawableCompat.setTint(addIcon, LauncherPreferences.PREF_GLOBAL_ICON_COLOR)
        }
        mProfileAdapter = InstanceAdapter(
            arrayOf<InstanceAdapterExtra?>(
                InstanceAdapterExtra(
                    VERSION_SPINNER_PROFILE_CREATE,
                    R.string.create_instance,
                    addIcon
                ),
            )
        )

        addOnAttachStateChangeListener(ExtraAttachListener())
        setSelection(0)

        // Popup window behavior
        setOnClickListener(object : OnClickListener {
            override fun onClick(v: View?) {
                if (mPopupWindow == null) createPopupWindow()

                if (mPopupWindow!!.isShowing()) {
                    mPopupWindow!!.dismiss()
                    return
                }


                // Show above the bottom bar with a small offset
                val offset = -getHeight() - getContext().getResources()
                    .getDimensionPixelOffset(R.dimen._184sdp) - getContext().getResources()
                    .getDimensionPixelOffset(R.dimen._8sdp)
                mPopupWindow!!.showAsDropDown(this@mcVersionSpinner, 0, offset)


                // Post() is required for the layout inflation phase
                post(Runnable {
                    if (mSelectorView != null) mSelectorView!!.setSelection(mSelectedIndex)
                })
            }
        })
    }

    private fun performExtraAction(extra: InstanceAdapterExtra) {
        //Replace with switch-case if you want to add more extra actions
        if (extra.id == VERSION_SPINNER_PROFILE_CREATE) {
            Tools.swapFragment(
                (getContext() as FragmentActivity?)!!, ProfileTypeSelectFragment::class.java,
                ProfileTypeSelectFragment.TAG, null
            )
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun createPopupWindow() {
        /** Create the listView and popup window for the interface, and set up the click behavior  */
        val isLandscape =
            getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE
        if (isLandscape) {
            val gridView = inflate(
                getContext(),
                R.layout.spinner_mc_version_grid,
                null
            ) as GridView
            gridView.setAdapter(mProfileAdapter)
            mSelectorView = gridView
            mListView = null
        } else {
            mListView = inflate(
                getContext(),
                R.layout.spinner_mc_version,
                null
            ) as ListView?
            mListView!!.setAdapter(mProfileAdapter)
            mSelectorView = mListView
        }

        mSelectorView!!.setOnItemClickListener(OnItemClickListener { parent: AdapterView<*>?, view: View?, position: Int, id: Long ->
            val item = mProfileAdapter!!.getItem(position)
            if (item is DisplayInstance) {
                hidePopup(true)
                setProfileSelection(position)
            } else if (item is InstanceAdapterExtra) {
                hidePopup(false)
                performExtraAction(item)
            }
        })

        var width = ViewGroup.LayoutParams.MATCH_PARENT
        if (isLandscape) {
            // In landscape, don't take full width to look more floating and modern
            width =
                getResources().getDisplayMetrics().widthPixels - getResources().getDimensionPixelOffset(
                    R.dimen._64sdp
                )
        }

        mPopupWindow = PopupWindow(
            mSelectorView,
            width,
            getContext().getResources().getDimensionPixelOffset(R.dimen._184sdp)
        )
        mPopupWindow!!.setElevation(24f) // More pronounced shadow for modern look
        mPopupWindow!!.setClippingEnabled(false)
        mPopupWindow!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Block clicking outside of the popup window
        mPopupWindow!!.setOutsideTouchable(true)
        mPopupWindow!!.setFocusable(true)
        mPopupWindow!!.setTouchInterceptor(OnTouchListener { v: View?, event: MotionEvent? ->
            if (event!!.getAction() == MotionEvent.ACTION_OUTSIDE) {
                mPopupWindow!!.dismiss()
                return@OnTouchListener true
            }
            false
        })


        // Custom animation, nice slide in
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mPopupAnimation = Slide(Gravity.BOTTOM)
            mPopupWindow!!.setEnterTransition(mPopupAnimation as Transition)
            mPopupWindow!!.setExitTransition(mPopupAnimation as Transition?)
        }
    }

    private fun hidePopup(animate: Boolean) {
        if (mPopupWindow == null) return
        if (!animate && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mPopupWindow!!.setEnterTransition(null)
            mPopupWindow!!.setExitTransition(null)
            mPopupWindow!!.dismiss()
            mPopupWindow!!.setEnterTransition(mPopupAnimation as Transition?)
            mPopupWindow!!.setExitTransition(mPopupAnimation as Transition?)
        } else {
            mPopupWindow!!.dismiss()
        }
    }

    internal inner class ExtraAttachListener : OnAttachStateChangeListener, ExtraListener<Void?> {
        override fun onViewAttachedToWindow(view: View) {
            reloadProfiles()
            ExtraCore.addExtraListener(ExtraConstants.REFRESH_VERSION_SPINNER, this)
        }

        override fun onViewDetachedFromWindow(view: View) {
            ExtraCore.removeExtraListenerFromValue(ExtraConstants.REFRESH_VERSION_SPINNER, this)
        }

        override fun onValueSet(key: String?, value: Void?): Boolean {
            post(Runnable { this@mcVersionSpinner.reloadProfiles() })
            ExtraCore.consumeValue(key)
            return false
        }
    }

    companion object {
        private const val VERSION_SPINNER_PROFILE_CREATE = 0
    }
}
