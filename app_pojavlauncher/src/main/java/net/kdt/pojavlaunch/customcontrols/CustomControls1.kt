package net.kdt.pojavlaunch.customcontrols

import android.content.Context
import androidx.annotation.Keep
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.LwjglGlfwKeycode
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.customcontrols.LayoutBitmaps.ControlsContainer
import java.io.FileOutputStream
import java.io.IOException

@Keep
class CustomControls @JvmOverloads constructor(
    mControlDataList: MutableList<ControlData?> = ArrayList<ControlData?>(),
    mDrawerDataList: MutableList<ControlDrawerData?>? = ArrayList<ControlDrawerData?>(),
    mJoystickDataList: MutableList<ControlJoystickData?>? = ArrayList<ControlJoystickData?>()
) {
    var version: Int = -1
    var scaledAt: Float = 0f
    var mControlDataList: MutableList<ControlData?>? = null
    var mDrawerDataList: MutableList<ControlDrawerData?>? = null
    var mJoystickDataList: MutableList<ControlJoystickData?>? = null

    @Transient
    var mLayoutBitmaps: LayoutBitmaps? = null

    init {
        this.mControlDataList = mControlDataList
        this.mDrawerDataList = mDrawerDataList
        this.mJoystickDataList = mJoystickDataList
        this.scaledAt = 100f
    }


    // Generate default control
    // Here for historical reasons
    // Just admire it idk
    @Suppress("unused")
    constructor(ctx: Context) : this() {
        this.mControlDataList!!.add(ControlData(ControlData.specialButtons[0])) // Keyboard
        this.mControlDataList!!.add(ControlData(ControlData.specialButtons[1])) // GUI
        this.mControlDataList!!.add(ControlData(ControlData.specialButtons[2])) // Primary Mouse mControlDataList
        this.mControlDataList!!.add(ControlData(ControlData.specialButtons[3])) // Secondary Mouse mControlDataList
        this.mControlDataList!!.add(ControlData(ControlData.specialButtons[4])) // Virtual mouse toggle

        this.mControlDataList!!.add(
            ControlData(
                ctx,
                R.string.control_debug,
                intArrayOf(LwjglGlfwKeycode.GLFW_KEY_F3.toInt()),
                "\${margin}",
                "\${margin}",
                false
            )
        )
        this.mControlDataList!!.add(
            ControlData(
                ctx,
                R.string.control_chat,
                intArrayOf(LwjglGlfwKeycode.GLFW_KEY_T.toInt()),
                "\${margin} * 2 + \${width}",
                "\${margin}",
                false
            )
        )
        this.mControlDataList!!.add(
            ControlData(
                ctx,
                R.string.control_listplayers,
                intArrayOf(LwjglGlfwKeycode.GLFW_KEY_TAB.toInt()),
                "\${margin} * 4 + \${width} * 3",
                "\${margin}",
                false
            )
        )
        this.mControlDataList!!.add(
            ControlData(
                ctx,
                R.string.control_thirdperson,
                intArrayOf(LwjglGlfwKeycode.GLFW_KEY_F5.toInt()),
                "\${margin}",
                "\${height} + \${margin}",
                false
            )
        )

        this.mControlDataList!!.add(
            ControlData(
                ctx,
                R.string.control_up,
                intArrayOf(LwjglGlfwKeycode.GLFW_KEY_W.toInt()),
                "\${margin} * 2 + \${width}",
                "\${bottom} - \${margin} * 3 - \${height} * 2",
                true
            )
        )
        this.mControlDataList!!.add(
            ControlData(
                ctx,
                R.string.control_left,
                intArrayOf(LwjglGlfwKeycode.GLFW_KEY_A.toInt()),
                "\${margin}",
                "\${bottom} - \${margin} * 2 - \${height}",
                true
            )
        )
        this.mControlDataList!!.add(
            ControlData(
                ctx,
                R.string.control_down,
                intArrayOf(LwjglGlfwKeycode.GLFW_KEY_S.toInt()),
                "\${margin} * 2 + \${width}",
                "\${bottom} - \${margin}",
                true
            )
        )
        this.mControlDataList!!.add(
            ControlData(
                ctx,
                R.string.control_right,
                intArrayOf(LwjglGlfwKeycode.GLFW_KEY_D.toInt()),
                "\${margin} * 3 + \${width} * 2",
                "\${bottom} - \${margin} * 2 - \${height}",
                true
            )
        )

        this.mControlDataList!!.add(
            ControlData(
                ctx,
                R.string.control_inventory,
                intArrayOf(LwjglGlfwKeycode.GLFW_KEY_E.toInt()),
                "\${margin} * 3 + \${width} * 2",
                "\${bottom} - \${margin}",
                true
            )
        )

        val shiftData = ControlData(
            ctx,
            R.string.control_shift,
            intArrayOf(LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT.toInt()),
            "\${margin} * 2 + \${width}",
            "\${screen_height} - \${margin} * 2 - \${height} * 2",
            true
        )
        shiftData.isToggle = true
        this.mControlDataList!!.add(shiftData)
        this.mControlDataList!!.add(
            ControlData(
                ctx,
                R.string.control_jump,
                intArrayOf(LwjglGlfwKeycode.GLFW_KEY_SPACE.toInt()),
                "\${right} - \${margin} * 2 - \${width}",
                "\${bottom} - \${margin} * 2 - \${height}",
                true
            )
        )

        //The default controls are conform to the V3
        version = 8
    }

    @Throws(IOException::class)
    fun save(path: String?) {
        //Current version is the V3.2 so the version as to be marked as 8 !
        version = 8
        val jsonControls = Tools.GLOBAL_GSON.toJson(this)
        FileOutputStream(path).use { fileOutputStream ->
            LayoutBitmaps.store(
                fileOutputStream, ControlsContainer(
                    jsonControls,
                    mLayoutBitmaps ?: LayoutBitmaps.createEmpty()
                )
            )
        }
    }
}
