package net.kdt.pojavlaunch.customcontrols

import android.graphics.Point
import com.google.gson.JsonSyntaxException
import net.kdt.pojavlaunch.LwjglGlfwKeycode
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.Tools.isValidString
import net.kdt.pojavlaunch.Tools.pxToDp
import net.kdt.pojavlaunch.customcontrols.LayoutBitmaps.ControlsContainer
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import kotlin.math.max

object LayoutConverter {
    @Throws(IOException::class, JsonSyntaxException::class)
    fun loadAndConvertIfNecessary(size: Point, jsonPath: String): CustomControls {
        val jsonFile = File(jsonPath)
        val container: ControlsContainer = LayoutBitmaps.Companion.load(jsonFile)
        val layoutBitmaps = container.mLayoutZip
        val controls = internalLoad(size, container.mControlsJson ?: "")
        if (controls == null) throw IOException("Unsupported control layout version")
        controls.mLayoutBitmaps = layoutBitmaps
        return controls
    }

    @Throws(JsonSyntaxException::class)
    fun internalLoad(size: Point, jsonLayoutData: String): CustomControls? {
        try {
            val layoutJobj = JSONObject(jsonLayoutData)

            if (!layoutJobj.has("version")) { //v1 layout
                return convertV1Layout(size, layoutJobj)
            } else {
                val version = layoutJobj.getInt("version")
                if (version == 2) {
                    return convertV2Layout(size, layoutJobj)
                }
                if (version == 3 || version == 4 || version == 5) {
                    return convertV3_4Layout(layoutJobj)
                }
                if (version == 6 || version == 7) {
                    return convertV6_7Layout(layoutJobj)
                } else if (version == 8) {
                    return Tools.GLOBAL_GSON.fromJson<CustomControls>(
                        jsonLayoutData,
                        CustomControls::class.java
                    )
                }
            }
            return null
        } catch (e: JSONException) {
            throw JsonSyntaxException("Failed to load", e)
        }
    }


    /**
     * Normalize the layout to v8 from v6/7. An issue from the joystick height and position has to be fixed.
     * @param oldLayoutJson The old layout
     * @return The new layout with the fixed joystick height
     */
    fun convertV6_7Layout(oldLayoutJson: JSONObject): CustomControls {
        val layout = Tools.GLOBAL_GSON.fromJson<CustomControls>(
            oldLayoutJson.toString(),
            CustomControls::class.java
        )
        for (data in layout.mJoystickDataList ?: emptyList()) {
            if (data == null) continue
            if (data.getHeight() > data.getWidth()) {
                // Make the size square, adjust the dynamic position related to height
                val ratio = data.getHeight() / data.getWidth()

                data.dynamicX = (data.dynamicX ?: "").replace("\${height}", "(" + ratio + " * \${height})")
                data.dynamicY = (data.dynamicY ?: "").replace(
                    "\${height}",
                    "(" + ratio + " * \${height})"
                ) + " + (" + (ratio - 1) + " * \${height})"

                data.setHeight(data.getWidth())
            }
        }
        layout.version = 8
        return layout
    }

    /**
     * Normalize the layout to v6 from v3/4: The stroke width is no longer dependant on the button size
     */
    private fun convertV3_4Layout(oldLayoutJson: JSONObject): CustomControls {
        val layout = Tools.GLOBAL_GSON.fromJson<CustomControls>(
            oldLayoutJson.toString(),
            CustomControls::class.java
        )
        convertStrokeWidth(layout)
        layout.version = 6
        return layout
    }


    @Throws(JSONException::class)
    private fun convertV2Layout(size: Point, oldLayoutJson: JSONObject): CustomControls {
        val layout = Tools.GLOBAL_GSON.fromJson<CustomControls>(
            oldLayoutJson.toString(),
            CustomControls::class.java
        )
        val layoutMainArray = oldLayoutJson.getJSONArray("mControlDataList")
        layout.mControlDataList = ArrayList<ControlData?>(layoutMainArray.length())
        for (i in 0..<layoutMainArray.length()) {
            val button = layoutMainArray.getJSONObject(i)
            val n_button =
                Tools.GLOBAL_GSON.fromJson<ControlData>(button.toString(), ControlData::class.java)
            if (!isValidString(n_button.dynamicX) && button.has("x")) {
                val buttonC = button.getDouble("x")
                val ratio = buttonC / size.x
                n_button.dynamicX = ratio.toString() + " * \${screen_width}"
            }
            if (!isValidString(n_button.dynamicY) && button.has("y")) {
                val buttonC = button.getDouble("y")
                val ratio = buttonC / size.y
                n_button.dynamicY = ratio.toString() + " * \${screen_height}"
            }
            layout.mControlDataList?.add(n_button)
        }
        val layoutDrawerArray = oldLayoutJson.getJSONArray("mDrawerDataList")
        layout.mDrawerDataList = ArrayList<ControlDrawerData?>()
        for (i in 0..<layoutDrawerArray.length()) {
            val button = layoutDrawerArray.getJSONObject(i)
            val buttonProperties = button.getJSONObject("properties")
            val n_button = Tools.GLOBAL_GSON.fromJson<ControlDrawerData>(
                button.toString(),
                ControlDrawerData::class.java
            )
            if (!isValidString(n_button.properties.dynamicX) && buttonProperties.has("x")) {
                val buttonC = buttonProperties.getDouble("x")
                val ratio = buttonC / size.x
                n_button.properties.dynamicX = ratio.toString() + " * \${screen_width}"
            }
            if (!isValidString(n_button.properties.dynamicY) && buttonProperties.has("y")) {
                val buttonC = buttonProperties.getDouble("y")
                val ratio = buttonC / size.y
                n_button.properties.dynamicY = ratio.toString() + " * \${screen_height}"
            }
            layout.mDrawerDataList?.add(n_button)
        }
        convertStrokeWidth(layout)

        layout.version = 3
        return layout
    }

    @Throws(JSONException::class)
    private fun convertV1Layout(size: Point, oldLayoutJson: JSONObject): CustomControls {
        val empty = CustomControls()
        val layoutMainArray = oldLayoutJson.getJSONArray("mControlDataList")
        for (i in 0..<layoutMainArray.length()) {
            val button = layoutMainArray.getJSONObject(i)
            val n_button = ControlData()
            val keycodes = intArrayOf(
                LwjglGlfwKeycode.GLFW_KEY_UNKNOWN.toInt(),
                LwjglGlfwKeycode.GLFW_KEY_UNKNOWN.toInt(),
                LwjglGlfwKeycode.GLFW_KEY_UNKNOWN.toInt(),
                LwjglGlfwKeycode.GLFW_KEY_UNKNOWN.toInt()
            )
            n_button.dynamicX = button.getString("dynamicX")
            n_button.dynamicY = button.getString("dynamicY")
            if (!isValidString(n_button.dynamicX) && button.has("x")) {
                val buttonC = button.getDouble("x")
                val ratio = buttonC / size.x
                n_button.dynamicX = ratio.toString() + " * \${screen_width}"
            }
            if (!isValidString(n_button.dynamicY) && button.has("y")) {
                val buttonC = button.getDouble("y")
                val ratio = buttonC / size.y
                n_button.dynamicY = ratio.toString() + " * \${screen_height}"
            }
            n_button.name = button.getString("name")
            n_button.opacity = (((button.getInt("transparency") - 100) * -1).toFloat()) / 100f
            n_button.passThruEnabled = button.getBoolean("passThruEnabled")
            n_button.isToggle = button.getBoolean("isToggle")
            n_button.setHeight(button.getInt("height").toFloat())
            n_button.setWidth(button.getInt("width").toFloat())
            n_button.bgColor = 0x4d000000
            n_button.strokeWidth = 0f
            if (button.getBoolean("isRound")) {
                n_button.cornerRadius = 35f
            }
            var next_idx = 0
            if (button.getBoolean("holdShift")) {
                keycodes[next_idx] = LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT.toInt()
                next_idx++
            }
            if (button.getBoolean("holdCtrl")) {
                keycodes[next_idx] = LwjglGlfwKeycode.GLFW_KEY_LEFT_CONTROL.toInt()
                next_idx++
            }
            if (button.getBoolean("holdAlt")) {
                keycodes[next_idx] = LwjglGlfwKeycode.GLFW_KEY_LEFT_ALT.toInt()
                next_idx++
            }
            keycodes[next_idx] = button.getInt("keycode")
            n_button.keycodes = keycodes
            empty.mControlDataList?.add(n_button)
        }
        empty.scaledAt = oldLayoutJson.getDouble("scaledAt").toFloat()
        empty.version = 3
        return empty
    }


    /**
     * Convert the layout stroke width to the V5 form
     */
    private fun convertStrokeWidth(layout: CustomControls) {
        for (data in layout.mControlDataList ?: emptyList()) {
            if (data == null) continue
            data.strokeWidth = pxToDp(
                computeStrokeWidth(
                    data.strokeWidth,
                    data.getWidth(),
                    data.getHeight()
                ).toFloat()
            )
        }

        for (data in layout.mDrawerDataList ?: emptyList()) {
            if (data == null) continue
            data.properties.strokeWidth = pxToDp(
                computeStrokeWidth(
                    data.properties.strokeWidth,
                    data.properties.getWidth(),
                    data.properties.getHeight()
                ).toFloat()
            )
            for (subButtonData in data.buttonProperties) {
                subButtonData.strokeWidth = pxToDp(
                    computeStrokeWidth(
                        subButtonData.strokeWidth,
                        data.properties.getWidth(),
                        data.properties.getWidth()
                    ).toFloat()
                )
            }
        }
    }

    /**
     * Convert a size percentage into a px size, used by older layout versions
     */
    fun computeStrokeWidth(widthInPercent: Float, width: Float, height: Float): Int {
        val maxSize = max(width, height)
        return ((maxSize / 2) * (widthInPercent / 100)).toInt()
    }
}
