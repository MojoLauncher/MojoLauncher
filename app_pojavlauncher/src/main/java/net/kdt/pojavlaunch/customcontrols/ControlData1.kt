package net.kdt.pojavlaunch.customcontrols

import android.content.Context
import android.util.ArrayMap
import androidx.annotation.Keep
import net.kdt.pojavlaunch.LwjglGlfwKeycode.GLFW_KEY_UNKNOWN
import net.kdt.pojavlaunch.Tools.dpToPx
import net.kdt.pojavlaunch.Tools.pxToDp
import net.kdt.pojavlaunch.customcontrols.buttons.ControlInterface
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.utils.JSONUtils.insertSingleJSONValue
import net.objecthunter.exp4j.ExpressionBuilder
import net.objecthunter.exp4j.function.Function
import java.lang.ref.WeakReference

@Keep
open class ControlData {
    // Internal usage only
    @Transient
    var isHideable: Boolean = false

    /**
     * Both fields below are dynamic position data, auto updates
     * X and Y position, unlike the original one which uses fixed
     * position, so it does not provide auto-location when a control
     * is made on a small device, then import the control to a
     * bigger device or vice versa.
     */
    var dynamicX: String? = null
    var dynamicY: String? = null
    var isToggle: Boolean = false
    var passThruEnabled: Boolean = false
    var name: String? = null
    var keycodes: IntArray = intArrayOf() //Should store up to 4 keys
    var opacity: Float = 1f //Alpha value from 0 to 1;
    var bgColor: Int = 0x4D000000
    var strokeColor: Int = -0x1
    var strokeWidth: Float = 0f // Dp instead of % now
    var cornerRadius: Float = 0f //0-100%
    var isSwipeable: Boolean = false
    var displayInGame: Boolean = true
    var displayInMenu: Boolean = true
    var bitmapTag: String? = null
    private var _width = 50f //Dp instead of Px now
    private var _height = 50f //Dp instead of Px now

    @get:JvmName("getWidthProp")
    @set:JvmName("setWidthProp")
    var width: Float
        get() = dpToPx(_width)
        set(value) {
            _width = pxToDp(value)
        }

    @get:JvmName("getHeightProp")
    @set:JvmName("setHeightProp")
    var height: Float
        get() = dpToPx(_height)
        set(value) {
            _height = pxToDp(value)
        }

    @JvmOverloads
    constructor(
        name: String? = "button",
        keycodes: IntArray = intArrayOf(),
        dynamicX: String? = "100",
        dynamicY: String? = "100",
        width: Float = 50f,
        height: Float = 50f,
        isToggle: Boolean = false,
        opacity: Float = 1f,
        bgColor: Int = 0x4D000000,
        strokeColor: Int = -0x1,
        strokeWidth: Float = 0f,
        cornerRadius: Float = 0f,
        displayInGame: Boolean = true,
        displayInMenu: Boolean = true,
        isSwipable: Boolean = false,
        mousePassthrough: Boolean = false,
        bitmapTag: String? = null
    ) {
        this.name = name
        this.keycodes = inflateKeycodeArray(keycodes)
        this.dynamicX = dynamicX
        this.dynamicY = dynamicY
        this._width = width
        this._height = height
        this.isToggle = isToggle
        this.opacity = opacity
        this.bgColor = bgColor
        this.strokeColor = strokeColor
        this.strokeWidth = strokeWidth
        this.cornerRadius = cornerRadius
        this.displayInGame = displayInGame
        this.displayInMenu = displayInMenu
        this.isSwipeable = isSwipable
        this.passThruEnabled = mousePassthrough
        this.bitmapTag = bitmapTag
    }

    constructor(
        ctx: Context,
        resId: Int,
        keycodes: IntArray,
        x: Float,
        y: Float,
        isSquare: Boolean
    ) : this(ctx.resources.getString(resId), keycodes, x.toString(), y.toString(), (if (isSquare) 50 else 80).toFloat(), (if (isSquare) 50 else 30).toFloat())

    constructor(name: String?, keycodes: IntArray, x: Float, y: Float, isSquare: Boolean) : this(
        name,
        keycodes,
        x.toString(),
        y.toString(),
        (if (isSquare) 50 else 80).toFloat(),
        (if (isSquare) 50 else 30).toFloat()
    )

    constructor(
        ctx: Context,
        resId: Int,
        keycodes: IntArray,
        dynamicX: String?,
        dynamicY: String?,
        isSquare: Boolean
    ) : this(ctx.resources.getString(resId), keycodes, dynamicX, dynamicY, (if (isSquare) 50 else 80).toFloat(), (if (isSquare) 50 else 30).toFloat())

    //Deep copy constructor
    constructor(controlData: ControlData) : this(
        controlData.name,
        controlData.keycodes,
        controlData.dynamicX,
        controlData.dynamicY,
        controlData._width,
        controlData._height,
        controlData.isToggle,
        controlData.opacity,
        controlData.bgColor,
        controlData.strokeColor,
        controlData.strokeWidth,
        controlData.cornerRadius,
        controlData.displayInGame,
        controlData.displayInMenu,
        controlData.isSwipeable,
        controlData.passThruEnabled,
        controlData.bitmapTag
    )

    fun insertDynamicPos(dynamicPos: String, w: Int, h: Int): Float {
        // Insert value to ${variable}
        val insertedPos = insertSingleJSONValue(dynamicPos, fillConversionMap(w, h))

        // Calculate, because the dynamic position contains some math equations
        return calculate(insertedPos)
    }

    fun containsKeycode(keycodeToCheck: Int): Boolean {
        for (keycode in keycodes) if (keycodeToCheck == keycode) return true

        return false
    }

    // Legacy getters for compatibility during migration
    fun getWidth(): Float = width
    fun setWidth(widthInPx: Float) { width = widthInPx }
    fun getHeight(): Float = height
    fun setHeight(heightInPx: Float) { height = heightInPx }

    /**
     * Fill the conversionMap with controlData dependent values.
     * The returned valueMap should NOT be kept in memory.
     * 
     * @return the valueMap to use.
     */
    private fun fillConversionMap(w: Int, h: Int): MutableMap<String?, String?> {
        val valueMap: ArrayMap<String?, String?> = conversionMap.get() ?: run {
            buildConversionMap()
            conversionMap.get()!!
        }

        valueMap["right"] = (w - width).toString()
        valueMap["bottom"] = (h - height).toString()
        valueMap["width"] = width.toString()
        valueMap["height"] = height.toString()
        valueMap["screen_width"] = w.toString()
        valueMap["screen_height"] = h.toString()
        valueMap["preferred_scale"] = LauncherPreferences.PREF_BUTTONSIZE.toString()

        return valueMap
    }

    companion object {
        const val SPECIALBTN_KEYBOARD: Int = -1
        const val SPECIALBTN_TOGGLECTRL: Int = -2
        const val SPECIALBTN_MOUSEPRI: Int = -3
        const val SPECIALBTN_MOUSESEC: Int = -4
        const val SPECIALBTN_VIRTUALMOUSE: Int = -5
        const val SPECIALBTN_MOUSEMID: Int = -6
        const val SPECIALBTN_SCROLLUP: Int = -7
        const val SPECIALBTN_SCROLLDOWN: Int = -8
        const val SPECIALBTN_MENU: Int = -9

        private var SPECIAL_BUTTONS: Array<ControlData>? = null
        private var SPECIAL_BUTTON_NAME_ARRAY: MutableList<String?>? = null
        private var builder = WeakReference<ExpressionBuilder?>(null)
        private var conversionMap = WeakReference<ArrayMap<String?, String?>?>(null)

        init {
            buildExpressionBuilder()
            buildConversionMap()
        }

        val specialButtons: Array<ControlData>
            get() {
                if (SPECIAL_BUTTONS == null) {
                    SPECIAL_BUTTONS = arrayOf(
                        ControlData(
                            "Keyboard",
                            intArrayOf(SPECIALBTN_KEYBOARD),
                            "${'$'}{margin} * 3 + ${'$'}{width} * 2",
                            "${'$'}{margin}",
                            50f, 50f, false
                        ),
                        ControlData(
                            "GUI",
                            intArrayOf(SPECIALBTN_TOGGLECTRL),
                            "${'$'}{margin}",
                            "${'$'}{bottom} - ${'$'}{margin}"
                        ),
                        ControlData(
                            "PRI",
                            intArrayOf(SPECIALBTN_MOUSEPRI),
                            "${'$'}{margin}",
                            "${'$'}{screen_height} - ${'$'}{margin} * 3 - ${'$'}{height} * 3"
                        ),
                        ControlData(
                            "SEC",
                            intArrayOf(SPECIALBTN_MOUSESEC),
                            "${'$'}{margin} * 3 + ${'$'}{width} * 2",
                            "${'$'}{screen_height} - ${'$'}{margin} * 3 - ${'$'}{height} * 3"
                        ),
                        ControlData(
                            "Mouse",
                            intArrayOf(SPECIALBTN_VIRTUALMOUSE),
                            "${'$'}{right}",
                            "${'$'}{margin}",
                            50f, 50f, false
                        ),

                        ControlData(
                            "MID",
                            intArrayOf(SPECIALBTN_MOUSEMID),
                            "${'$'}{margin}",
                            "${'$'}{margin}"
                        ),
                        ControlData(
                            "SCROLLUP",
                            intArrayOf(SPECIALBTN_SCROLLUP),
                            "${'$'}{margin}",
                            "${'$'}{margin}"
                        ),
                        ControlData(
                            "SCROLLDOWN",
                            intArrayOf(SPECIALBTN_SCROLLDOWN),
                            "${'$'}{margin}",
                            "${'$'}{margin}"
                        ),
                        ControlData(
                            "MENU",
                            intArrayOf(SPECIALBTN_MENU),
                            "${'$'}{margin}",
                            "${'$'}{margin}"
                        )
                    )
                }

                return SPECIAL_BUTTONS!!
            }

        fun buildSpecialButtonArray(): MutableList<String?>? {
            if (SPECIAL_BUTTON_NAME_ARRAY == null) {
                val nameList = ArrayList<String?>()
                for (btn in specialButtons) {
                    nameList.add("SPECIAL_" + btn.name)
                }
                SPECIAL_BUTTON_NAME_ARRAY = nameList
                SPECIAL_BUTTON_NAME_ARRAY?.reverse()
            }

            return SPECIAL_BUTTON_NAME_ARRAY
        }

        private fun calculate(math: String?): Float {
            setExpression(math)
            return builder.get()!!.build().evaluate().toFloat()
        }

        private fun inflateKeycodeArray(keycodes: IntArray): IntArray {
            val inflatedArray = intArrayOf(
                GLFW_KEY_UNKNOWN.toInt(),
                GLFW_KEY_UNKNOWN.toInt(),
                GLFW_KEY_UNKNOWN.toInt(),
                GLFW_KEY_UNKNOWN.toInt()
            )
            System.arraycopy(keycodes, 0, inflatedArray, 0, keycodes.size.coerceAtMost(4))
            return inflatedArray
        }

        /**
         * Create a builder, keep a weak reference to it to use it with all views on first inflation
         */
        private fun buildExpressionBuilder() {
            val expressionBuilder = ExpressionBuilder("1 + 1")
                .function(object : Function("dp", 1) {
                    override fun apply(vararg args: Double): Double {
                        return pxToDp(args[0].toFloat()).toDouble()
                    }
                })
                .function(object : Function("px", 1) {
                    override fun apply(vararg args: Double): Double {
                        return dpToPx(args[0].toFloat()).toDouble()
                    }
                })
            builder = WeakReference(expressionBuilder)
        }

        /**
         * wrapper for the WeakReference to the expressionField.
         * 
         * @param stringExpression the expression to set.
         */
        private fun setExpression(stringExpression: String?) {
            if (builder.get() == null) buildExpressionBuilder()
            builder.get()!!.expression(stringExpression)
        }

        /**
         * Build a shared conversion map without the ControlData dependent values
         * You need to set the view dependent values before using it.
         */
        private fun buildConversionMap() {
            // Values in the map below may be always changed
            val keyValueMap = ArrayMap<String?, String?>(10)
            keyValueMap["top"] = "0"
            keyValueMap["left"] = "0"
            keyValueMap["right"] = "DUMMY_RIGHT"
            keyValueMap["bottom"] = "DUMMY_BOTTOM"
            keyValueMap["width"] = "DUMMY_WIDTH"
            keyValueMap["height"] = "DUMMY_HEIGHT"
            keyValueMap["screen_width"] = "DUMMY_DATA"
            keyValueMap["screen_height"] = "DUMMY_DATA"
            keyValueMap["margin"] = ControlInterface.marginDistance.toInt().toString()
            keyValueMap["preferred_scale"] = "DUMMY_DATA"

            conversionMap = WeakReference(keyValueMap)
        }
    }
}
