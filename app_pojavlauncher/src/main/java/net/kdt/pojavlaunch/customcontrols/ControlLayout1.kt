package net.kdt.pojavlaunch.customcontrols

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Point
import android.os.Build
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.JsonSyntaxException
import com.kdt.pickafile.FileListView
import com.kdt.pickafile.FileSelectedListener
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.MinecraftGLSurface
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.customcontrols.buttons.ControlButton
import net.kdt.pojavlaunch.customcontrols.buttons.ControlDrawer
import net.kdt.pojavlaunch.customcontrols.buttons.ControlInterface
import net.kdt.pojavlaunch.customcontrols.buttons.ControlJoystick
import net.kdt.pojavlaunch.customcontrols.buttons.ControlSubButton
import net.kdt.pojavlaunch.customcontrols.handleview.ActionRow
import net.kdt.pojavlaunch.customcontrols.handleview.ControlHandleView
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import org.lwjgl.glfw.CallbackBridge.isGrabbing
import java.io.File
import java.io.IOException

class ControlLayout : FrameLayout {
    var layout: CustomControls? = null
        protected set

    /* Accessible when inside the game by ControlInterface implementations, cached for perf. */
    private var mGameSurface: MinecraftGLSurface? = null

    /* Cache to buttons for performance purposes */
    private var mButtons: MutableList<ControlInterface>? = null
    private var mModifiable = false
    private var mIsModified = false
    private var mControlVisible = false

    private var mHandleView: ControlHandleView? = null
    private var mMenuListener: ControlButtonMenuListener? = null
    var mActionRow: ActionRow? = null
    var mLayoutFileName: String? = null

    constructor(ctx: Context) : super(ctx)

    constructor(ctx: Context, attrs: AttributeSet?) : super(ctx, attrs)

    private fun getActivity(context: Context): Activity? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) {
                return ctx
            }
            ctx = ctx.baseContext
        }
        return null
    }

    fun loadLayout(jsonPath: String) {
        mLayoutFileName = File(jsonPath).nameWithoutExtension
        try {
            val activity = getActivity(context)
            if (activity != null) {
                val metrics = Tools.getDisplayMetrics(activity)
                loadLayout(LayoutConverter.loadAndConvertIfNecessary(Point(metrics.widthPixels, metrics.heightPixels), jsonPath))
            } else {
                // Fallback to simpler metrics if activity not found
                val metrics = context.resources.displayMetrics
                loadLayout(LayoutConverter.loadAndConvertIfNecessary(Point(metrics.widthPixels, metrics.heightPixels), jsonPath))
            }
        } catch (e: Exception) {
            Tools.showError(context, e)
        }
    }

    fun loadLayout(controlLayout: CustomControls?) {
        removeAllButtons()
        this.layout = controlLayout
        if (layout == null) return
        layout?.mControlDataList?.forEach { it?.let { addControlButton(it) } }
        layout?.mDrawerDataList?.forEach { it?.let { addDrawer(it) } }
        layout?.mJoystickDataList?.forEach { it?.let { addJoystickButton(it) } }
        setModified(false)
    }

    fun addControlButton(data: ControlData) {
        if (layout == null) layout = CustomControls()
        if (layout?.mControlDataList == null) layout?.mControlDataList = ArrayList()
        if (layout?.mControlDataList?.contains(data) == false) layout?.mControlDataList?.add(data)
        val view = ControlButton(this, data)

        if (!mModifiable) {
            view.alpha = view.properties.opacity
            view.isFocusable = false
            view.isFocusableInTouchMode = false
        }
        addView(view)
        setModified(true)
    }

    fun addSubButton(drawer: ControlDrawer, data: ControlData) {
        if (drawer.drawerData.buttonProperties.contains(data) == false) drawer.drawerData.buttonProperties.add(
            data
        )
        val view = ControlSubButton(this, data, drawer)

        if (!mModifiable) {
            view.alpha = view.properties.opacity
            view.isFocusable = false
            view.isFocusableInTouchMode = false
        }
        addView(view)
        drawer.addButton(view)
        setModified(true)
    }

    fun addDrawer(data: ControlDrawerData) {
        if (layout == null) layout = CustomControls()
        if (layout?.mDrawerDataList == null) layout?.mDrawerDataList = ArrayList()
        if (layout?.mDrawerDataList?.contains(data) == false) layout?.mDrawerDataList?.add(data)
        val view = ControlDrawer(this, data)

        if (!mModifiable) {
            view.alpha = view.properties.opacity
            view.isFocusable = false
            view.isFocusableInTouchMode = false
        }
        addView(view)

        for (buttonProperties in data.buttonProperties) {
            if (buttonProperties != null) addSubButton(view, buttonProperties)
        }
        setModified(true)
    }

    fun addJoystickButton(data: ControlJoystickData) {
        if (layout == null) layout = CustomControls()
        if (layout?.mJoystickDataList == null) layout?.mJoystickDataList = ArrayList()
        if (layout?.mJoystickDataList?.contains(data) == false) layout?.mJoystickDataList?.add(data)
        val view = ControlJoystick(this, data)

        if (!mModifiable) {
            view.alpha = view.properties.opacity
            view.isFocusable = false
            view.isFocusableInTouchMode = false
        }
        addView(view)
    }


    private fun removeAllButtons() {
        for (button in this.buttonChildren!!) {
            removeView(button.controlView)
        }

        System.gc()
    }

    @Throws(Exception::class)
    fun saveLayout(path: String?) {
        layout?.save(path)
        setModified(false)
        if (path != null) mLayoutFileName = File(path).nameWithoutExtension
    }

    fun toggleControlVisible() {
        setControlVisible(!mControlVisible)
    }

    fun setControlVisible(visible: Boolean) {
        mControlVisible = visible
        for (button in this.buttonChildren!!) {
            button.setVisible(visible)
        }
    }

    val buttonChildren: List<ControlInterface>?
        get() {
            if (mButtons == null) mButtons = ArrayList()
            mButtons!!.clear()
            val childCount = childCount
            for (i in 0..<childCount) {
                val view = getChildAt(i)
                if (view is ControlInterface) {
                    mButtons!!.add(view as ControlInterface)
                }
            }

            return mButtons
        }

    var modifiable: Boolean
        get() = mModifiable
        set(modifiable) {
            mModifiable = modifiable
            if (mHandleView != null) mHandleView!!.visibility = if (modifiable) VISIBLE else GONE
            if (mActionRow != null) mActionRow!!.visibility = if (modifiable) VISIBLE else GONE
            for (button in this.buttonChildren!!) {
                button.controlView!!.isFocusable = modifiable
                button.controlView!!.isFocusableInTouchMode = modifiable
            }
            if (modifiable) setControlVisible(true)
            else removeEditWindow()
        }

    fun setModified(modified: Boolean) {
        mIsModified = modified
    }

    /**
     * Notify the listener that a button is being edited.
     */
    fun editControlButton(button: ControlInterface) {
        mMenuListener?.editControlButton(button)
        
        if (mHandleView == null) {
            mHandleView = ControlHandleView(context)
            addView(mHandleView)
        }
        mHandleView!!.setControlButton(button)

        if (mActionRow == null) {
            mActionRow = ActionRow(context)
            addView(mActionRow)
        }
        mActionRow!!.setFollowedButton(button)
    }

    fun removeEditWindow() {
        mHandleView?.hide()
        mActionRow?.hide()
    }

    fun onClickedMenu() {
        mMenuListener?.onClickedMenu()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (mModifiable || isGrabbing) return false

        if (ev.action == MotionEvent.ACTION_DOWN) {
            for (button in this.buttonChildren!!) {
                if (button.controlView!!.visibility == VISIBLE && eventInViewBounds(
                        ev,
                        button.controlView!!
                    )
                ) {
                    return false
                }
            }

            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(windowToken, 0)
        }

        return false
    }

    val layoutScale: Float
        get() = LauncherPreferences.PREF_BUTTONSIZE

    @RequiresApi(api = Build.VERSION_CODES.R)
    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        if (mModifiable) {
            val imeHeight = insets.getInsets(WindowInsets.Type.ime()).bottom
            val navigationHeight = insets.getInsets(WindowInsets.Type.navigationBars()).bottom
            setPadding(0, 0, 0, Math.max(0, imeHeight - navigationHeight))
        } else {
            setPadding(0, 0, 0, 0)
        }
        return super.onApplyWindowInsets(insets)
    }

    fun saveToDirectory(name: String?): String {
        return save(Tools.CTRLMAP_PATH + "/" + name + ".json")
    }

    fun save(path: String?): String {
        try {
            layout?.save(path)
            if (path != null) mLayoutFileName = File(path).nameWithoutExtension
        } catch (e: IOException) {
            Log.e("ControlLayout", "Failed to save the layout at:" + path)
        }
        return path ?: ""
    }

    internal inner class OnClickExitListener(
        private val mDialog: AlertDialog,
        private val mEditText: EditText,
        private val mListener: EditorExitable?
    ) : OnClickListener {
        override fun onClick(v: View) {
            val context = v.context
            if (mEditText.text.toString().isEmpty()) {
                mEditText.error = context.getString(R.string.global_error_field_empty)
                return
            }
            try {
                val jsonPath = saveToDirectory(mEditText.text.toString())
                Toast.makeText(
                    context,
                    context.getString(R.string.global_save) + ": " + jsonPath,
                    Toast.LENGTH_SHORT
                ).show()
                mDialog.dismiss()
                if (mListener != null) mListener.exitEditor()
            } catch (th: Throwable) {
                Tools.showError(context, th, mListener != null)
            }
        }
    }

    fun openSaveDialog(editorExitable: EditorExitable?) {
        val context = context
        val edit = EditText(context)
        edit.isSingleLine = true
        edit.setText(mLayoutFileName)

        val builder: AlertDialog.Builder = MaterialAlertDialogBuilder(context)
        builder.setTitle(R.string.global_save)
        builder.setView(edit)
        builder.setPositiveButton(android.R.string.ok, null)
        builder.setNegativeButton(android.R.string.cancel, null)
        if (editorExitable != null) builder.setNeutralButton(R.string.global_save_and_exit, null)
        val dialog = builder.create()
        dialog.setOnShowListener { _ ->
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(OnClickExitListener(dialog, edit, null))
            if (editorExitable != null) dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                .setOnClickListener(OnClickExitListener(dialog, edit, editorExitable))
        }
        dialog.show()
    }

    fun openLoadDialog() {
        val builder: AlertDialog.Builder = MaterialAlertDialogBuilder(context)
        builder.setTitle(R.string.global_load)
        builder.setPositiveButton(android.R.string.cancel, null)

        val dialog = builder.create()
        val flv = FileListView(dialog, "json")
        if (Build.VERSION.SDK_INT < 29) flv.listFileAt(File(Tools.CTRLMAP_PATH!!))
        else flv.lockPathAt(File(Tools.CTRLMAP_PATH!!))
        flv.setFileSelectedListener(object : FileSelectedListener() {
            override fun onFileSelected(file: File?, path: String?) {
                try {
                    loadLayout(path!!)
                } catch (e: IOException) {
                    Tools.showError(context, e)
                }
                dialog.dismiss()
            }
        })
        dialog.setView(flv)
        dialog.show()
    }

    fun openSetDefaultDialog() {
        val builder: AlertDialog.Builder = MaterialAlertDialogBuilder(context)
        builder.setTitle(R.string.customctrl_selectdefault)
        builder.setPositiveButton(android.R.string.cancel, null)

        val dialog = builder.create()
        val flv = FileListView(dialog, "json")
        flv.lockPathAt(File(Tools.CTRLMAP_PATH!!))
        flv.setFileSelectedListener(object : FileSelectedListener() {
            override fun onFileSelected(file: File?, path: String?) {
                try {
                    LauncherPreferences.DEFAULT_PREF!!.edit().putString("defaultCtrl", path).apply()
                    LauncherPreferences.PREF_DEFAULTCTRL_PATH = path
                    loadLayout(path!!)
                } catch (e: IOException) {
                    Tools.showError(context, e)
                } catch (e: JsonSyntaxException) {
                    Tools.showError(context, e)
                }
                dialog.dismiss()
            }
        })
        dialog.setView(flv)
        dialog.show()
    }

    fun openExitDialog(exitListener: EditorExitable) {
        val builder: AlertDialog.Builder = MaterialAlertDialogBuilder(context)
        builder.setTitle(R.string.customctrl_editor_exit_title)
        builder.setMessage(R.string.customctrl_editor_exit_msg)
        builder.setPositiveButton(
            R.string.global_yes
        ) { _, _ -> exitListener.exitEditor() }
        builder.setNegativeButton(
            R.string.global_no
        ) { _, _ -> }
        builder.show()
    }

    // Copied from https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/widget/FrameLayout.java
    // (and edited to avoid laying out control buttons)
    // Handled explicitly via getAbsoluteGravity()
    private fun layoutNonButtonChildren(left: Int, top: Int, right: Int, bottom: Int) {
        val count = childCount
        val parentLeft = paddingLeft
        val parentRight = right - left - paddingRight
        val parentTop = paddingTop
        val parentBottom = bottom - top - paddingBottom
        val layoutDirection = layoutDirection
        for (i in 0..<count) {
            val child = getChildAt(i)
            if (child is ControlInterface || child.visibility == GONE) continue
            val lp = child.layoutParams as LayoutParams
            val width = child.measuredWidth
            val height = child.measuredHeight
            val childLeft: Int
            val childTop: Int
            var gravity = lp.gravity
            if (gravity == -1) {
                gravity = Gravity.START or Gravity.TOP
            }
            val absoluteGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection)
            when (absoluteGravity and Gravity.HORIZONTAL_GRAVITY_MASK) {
                Gravity.CENTER_HORIZONTAL -> childLeft =
                    parentLeft + (parentRight - parentLeft - width) / 2 +
                            lp.leftMargin - lp.rightMargin

                Gravity.RIGHT -> childLeft = parentRight - width - lp.rightMargin
                Gravity.LEFT -> childLeft = parentLeft + lp.leftMargin
                else -> childLeft = parentLeft + lp.leftMargin
            }
            when (gravity and Gravity.VERTICAL_GRAVITY_MASK) {
                Gravity.TOP -> childTop = parentTop + lp.topMargin
                Gravity.CENTER_VERTICAL -> childTop =
                    parentTop + (parentBottom - parentTop - height) / 2 +
                            lp.topMargin - lp.bottomMargin

                Gravity.BOTTOM -> childTop = parentBottom - height - lp.bottomMargin
                else -> childTop = parentTop + lp.topMargin
            }
            child.layout(childLeft, childTop, childLeft + width, childTop + height)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        layoutNonButtonChildren(left, top, right, bottom)
        val w = right - left
        val h = bottom - top

        for (controlInterface in this.buttonChildren!!) {
            val props = controlInterface.properties ?: continue
            val interfaceView = controlInterface.controlView ?: continue

            val wView = props.width.toInt()
            val hView = props.height.toInt()

            interfaceView.measure(
                MeasureSpec.makeMeasureSpec(wView, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(hView, MeasureSpec.EXACTLY)
            )

            if (!changed && !interfaceView.isLayoutRequested) {
                interfaceView.layout(
                    interfaceView.left, interfaceView.top,
                    interfaceView.right, interfaceView.bottom
                )
            } else {
                val l = (props.insertDynamicPos(props.dynamicX!!, w, h) + left).toInt()
                val t = (props.insertDynamicPos(props.dynamicY!!, w, h) + top).toInt()

                val r = l + wView
                val b = t + hView
                interfaceView.layout(l, t, r, b)
            }
        }
    }

    fun askToExit(editorExitable: EditorExitable) {
        if (mIsModified) {
            openExitDialog(editorExitable)
        } else {
            editorExitable.exitEditor()
        }
    }

    fun setMenuListener(menuListener: ControlButtonMenuListener?) {
        this.mMenuListener = menuListener
    }

    fun areControlVisible(): Boolean {
        return mControlVisible
    }

    fun hasMenuButton(): Boolean {
        for (controlInterface in this.buttonChildren!!) {
            val props = controlInterface.properties ?: continue
            for (keycode in props.keycodes) {
                if (keycode == ControlData.SPECIALBTN_MENU) return true
            }
        }
        return false
    }

    fun refreshControlButtonPositions() {
        for (controlInterface in this.buttonChildren!!) {
            controlInterface.updateProperties()
        }
    }

    val bitmaps: LayoutBitmaps?
        get() = layout?.mLayoutBitmaps

    companion object {
        private fun eventInViewBounds(event: MotionEvent, view: View): Boolean {
            val x = event.x
            val y = event.y
            return x > view.left && x < view.right && y > view.top && y < view.bottom
        }
    }
}
