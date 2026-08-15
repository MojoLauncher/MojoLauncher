package net.kdt.pojavlaunch.customcontrols;

import android.graphics.Point;
import android.view.KeyEvent;

import com.google.gson.JsonSyntaxException;

import net.kdt.pojavlaunch.LwjglGlfwKeycode;
import net.kdt.pojavlaunch.Tools;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class LayoutConverter {

    private static final int TARGET_VERSION = 9;

    public static CustomControls loadAndConvertIfNecessary(Point size, String jsonPath) throws IOException, JsonSyntaxException{
        File jsonFile = new File(jsonPath);
        LayoutBitmaps.ControlsContainer container = LayoutBitmaps.load(jsonFile);
        LayoutBitmaps layoutBitmaps = container.mLayoutZip;
        CustomControls controls = internalLoad(size, container.mControlsJson);
        if(controls == null) throw new IOException("Unsupported control layout version");
        controls.mLayoutBitmaps = layoutBitmaps;
        return controls;
    }

    public static CustomControls internalLoad(Point size, String jsonLayoutData) throws JsonSyntaxException {
        try {
            JSONObject layoutJobj = new JSONObject(jsonLayoutData);

            if (!layoutJobj.has("version")) {
                // Fixed conversion, layout object is completely rebuilt
                return LayoutConverter.convertV1Layout(size, layoutJobj);
            }

            int version = layoutJobj.getInt("version");
            if(version == 2) {
                // Almost-fixed conversion due to data structure changes
                return LayoutConverter.convertV2Layout(size, layoutJobj);
            }

            // On version 3 and above, the data structure is pretty much fixed. Changes were
            // only made to fix bugs or improve scalability.

            CustomControls layout = Tools.GLOBAL_GSON.fromJson(jsonLayoutData, CustomControls.class);

            if(layout.version > TARGET_VERSION)
                throw new JsonSyntaxException("Layout version " +layout.version+ " is too new, only up to "+TARGET_VERSION +" is supported");

            if(layout.version == 3 || layout.version == 4 || layout.version == 5)
                LayoutConverter.convertV3_4Layout(layout);

            if(layout.version == 6 || layout.version == 7)
                LayoutConverter.convertV6_7Layout(layout);

            if(layout.version == 8)
                convertV8Layout(layout);

            return layout;


        } catch (JSONException e) {
            throw new JsonSyntaxException("Failed to load the layout. Maybe it's corrupted?", e);
        }
    }


    /**
     * Normalize the layout to v8 from v6/7. An issue from the joystick height and position has to be fixed.
     * @param layout The layout object to upgrade
     */
    public static void convertV6_7Layout(CustomControls layout) {
        for (ControlJoystickData data : layout.mJoystickDataList) {
            if (data.getHeight() > data.getWidth()) {
                // Make the size square, adjust the dynamic position related to height
                float ratio = data.getHeight() / data.getWidth();

                data.dynamicX = data.dynamicX.replace("${height}", "(" + ratio + " * ${height})");
                data.dynamicY = data.dynamicY.replace("${height}", "(" + ratio + " * ${height})") +  " + (" + (ratio-1) + " * ${height})";

                data.setHeight(data.getWidth());
            }
        }
        layout.version = 8;
    }

    /**
     * Normalize the layout to v6 from v3/4: The stroke width is no longer dependant on the button size
     */
    private static void convertV3_4Layout(CustomControls layout) {
        for (ControlData data : layout.mControlDataList) {
            convertStrokeWidth(data);
        }

        for (ControlDrawerData data : layout.mDrawerDataList) {
            convertStrokeWidth(data.properties);
            for (ControlData subButtonData : data.buttonProperties) {
                convertStrokeWidth(subButtonData);
            }
        }
        layout.version = 6;
    }

    private static CustomControls convertV2Layout(Point size, JSONObject oldLayoutJson) throws JSONException {
        CustomControls layout = Tools.GLOBAL_GSON.fromJson(oldLayoutJson.toString(), CustomControls.class);
        assert layout.mJoystickDataList == null || layout.mJoystickDataList.isEmpty(); // Joysticks shouldn't be in v2 layouts
        JSONArray layoutMainArray = oldLayoutJson.getJSONArray("mControlDataList");
        layout.mControlDataList = new ArrayList<>(layoutMainArray.length());
        for (int i = 0; i < layoutMainArray.length(); i++) {
            JSONObject button = layoutMainArray.getJSONObject(i);
            ControlData n_button = Tools.GLOBAL_GSON.fromJson(button.toString(), ControlData.class);
            if (!Tools.isValidString(n_button.dynamicX) && button.has("x")) {
                double buttonC = button.getDouble("x");
                double ratio = buttonC / size.x;
                n_button.dynamicX = ratio + " * ${screen_width}";
            }
            if (!Tools.isValidString(n_button.dynamicY) && button.has("y")) {
                double buttonC = button.getDouble("y");
                double ratio = buttonC / size.y;
                n_button.dynamicY = ratio + " * ${screen_height}";
            }
            convertKeycodes(n_button.keycodes);
            convertStrokeWidth(n_button);
            layout.mControlDataList.add(n_button);
        }
        JSONArray layoutDrawerArray = oldLayoutJson.getJSONArray("mDrawerDataList");
        layout.mDrawerDataList = new ArrayList<>();
        for (int i = 0; i < layoutDrawerArray.length(); i++) {
            JSONObject button = layoutDrawerArray.getJSONObject(i);
            JSONObject buttonProperties = button.getJSONObject("properties");
            ControlDrawerData n_button = Tools.GLOBAL_GSON.fromJson(button.toString(), ControlDrawerData.class);
            if (!Tools.isValidString(n_button.properties.dynamicX) && buttonProperties.has("x")) {
                double buttonC = buttonProperties.getDouble("x");
                double ratio = buttonC / size.x;
                n_button.properties.dynamicX = ratio + " * ${screen_width}";
            }
            if (!Tools.isValidString(n_button.properties.dynamicY) && buttonProperties.has("y")) {
                double buttonC = buttonProperties.getDouble("y");
                double ratio = buttonC / size.y;
                n_button.properties.dynamicY = ratio + " * ${screen_height}";
            }

            convertKeycodes(n_button.properties.keycodes);
            convertStrokeWidth(n_button.properties);
            for(ControlData subButton : n_button.buttonProperties) {
                convertKeycodes(subButton.keycodes);
                convertStrokeWidth(subButton);
            }
            layout.mDrawerDataList.add(n_button);
        }

        layout.version = 9;
        return layout;
    }

    private static CustomControls convertV1Layout(Point size, JSONObject oldLayoutJson) throws JSONException {
        CustomControls empty = new CustomControls();
        JSONArray layoutMainArray = oldLayoutJson.getJSONArray("mControlDataList");
        for (int i = 0; i < layoutMainArray.length(); i++) {
            JSONObject button = layoutMainArray.getJSONObject(i);
            ControlData n_button = new ControlData();
            int[] keycodes = new int[]{
                    KeyEvent.KEYCODE_UNKNOWN, KeyEvent.KEYCODE_UNKNOWN,
                    KeyEvent.KEYCODE_UNKNOWN, KeyEvent.KEYCODE_UNKNOWN
            };
            n_button.dynamicX = button.getString("dynamicX");
            n_button.dynamicY = button.getString("dynamicY");
            if (!Tools.isValidString(n_button.dynamicX) && button.has("x")) {
                double buttonC = button.getDouble("x");
                double ratio = buttonC / size.x;
                n_button.dynamicX = ratio + " * ${screen_width}";
            }
            if (!Tools.isValidString(n_button.dynamicY) && button.has("y")) {
                double buttonC = button.getDouble("y");
                double ratio = buttonC / size.y;
                n_button.dynamicY = ratio + " * ${screen_height}";
            }
            n_button.name = button.getString("name");
            n_button.opacity = ((float) ((button.getInt("transparency") - 100) * -1)) / 100f;
            n_button.passThruEnabled = button.getBoolean("passThruEnabled");
            n_button.isToggle = button.getBoolean("isToggle");
            n_button.setHeight(button.getInt("height"));
            n_button.setWidth(button.getInt("width"));
            n_button.bgColor = 0x4d000000;
            n_button.strokeWidth = 0;
            if (button.getBoolean("isRound")) {
                n_button.cornerRadius = 35f;
            }
            int next_idx = 0;
            if (button.getBoolean("holdShift")) {
                keycodes[next_idx] = KeyEvent.KEYCODE_SHIFT_LEFT;
                next_idx++;
            }
            if (button.getBoolean("holdCtrl")) {
                keycodes[next_idx] = KeyEvent.KEYCODE_CTRL_LEFT;
                next_idx++;
            }
            if (button.getBoolean("holdAlt")) {
                keycodes[next_idx] = KeyEvent.KEYCODE_ALT_LEFT;
                next_idx++;
            }
            keycodes[next_idx] = keyCodeFromGLFW(button.getInt("keycode"));
            n_button.keycodes = keycodes;
            empty.mControlDataList.add(n_button);
        }
        empty.scaledAt = (float) oldLayoutJson.getDouble("scaledAt");
        empty.version = 9;
        return empty;
    }

    /**
     * Upgrade v8 layout to v9. Switched button keycodes from GLFW to Android
     */
    private static void convertV8Layout(CustomControls layout) {
        if(layout.mControlDataList != null){
            for(ControlData data : layout.mControlDataList){
                convertKeycodes(data.keycodes);
            }
        }
        if(layout.mDrawerDataList != null){
            for(ControlDrawerData drawerData : layout.mDrawerDataList){
                convertKeycodes(drawerData.properties.keycodes);
                if(drawerData.buttonProperties != null){
                    for(ControlData data : drawerData.buttonProperties){
                        convertKeycodes(data.keycodes);
                    }
                }
            }
        }
        if(layout.mJoystickDataList != null){
            for(ControlJoystickData data : layout.mJoystickDataList){
                convertKeycodes(data.keycodes);
            }
        }
        layout.version = 9;
    }

    private static void convertKeycodes(int[] keycodes){
        for(int i = 0; i < keycodes.length; i++){
            if(keycodes[i] > 0)
                keycodes[i] = keyCodeFromGLFW(keycodes[i]);
        }
    }


    private static void convertStrokeWidth(ControlData data) {
        data.strokeWidth = Tools.pxToDp(computeStrokeWidth(data.strokeWidth, data.getWidth(), data.getHeight()));
    }

    /**
     * Convert a size percentage into a px size, used by older layout versions
     */
    static int computeStrokeWidth(float widthInPercent, float width, float height) {
        float maxSize = Math.max(width, height);
        return (int) ((maxSize / 2) * (widthInPercent / 100));
    }

    private static int keyCodeFromGLFW(int keycode){
        switch (keycode) {
            case LwjglGlfwKeycode.GLFW_KEY_SPACE: return KeyEvent.KEYCODE_SPACE;
            case LwjglGlfwKeycode.GLFW_KEY_APOSTROPHE: return KeyEvent.KEYCODE_APOSTROPHE;
            case LwjglGlfwKeycode.GLFW_KEY_COMMA: return KeyEvent.KEYCODE_COMMA;
            case LwjglGlfwKeycode.GLFW_KEY_MINUS: return KeyEvent.KEYCODE_MINUS;
            case LwjglGlfwKeycode.GLFW_KEY_PERIOD: return KeyEvent.KEYCODE_PERIOD;
            case LwjglGlfwKeycode.GLFW_KEY_SLASH: return KeyEvent.KEYCODE_SLASH;
            case LwjglGlfwKeycode.GLFW_KEY_0: return KeyEvent.KEYCODE_0;
            case LwjglGlfwKeycode.GLFW_KEY_1: return KeyEvent.KEYCODE_1;
            case LwjglGlfwKeycode.GLFW_KEY_2: return KeyEvent.KEYCODE_2;
            case LwjglGlfwKeycode.GLFW_KEY_3: return KeyEvent.KEYCODE_3;
            case LwjglGlfwKeycode.GLFW_KEY_4: return KeyEvent.KEYCODE_4;
            case LwjglGlfwKeycode.GLFW_KEY_5: return KeyEvent.KEYCODE_5;
            case LwjglGlfwKeycode.GLFW_KEY_6: return KeyEvent.KEYCODE_6;
            case LwjglGlfwKeycode.GLFW_KEY_7: return KeyEvent.KEYCODE_7;
            case LwjglGlfwKeycode.GLFW_KEY_8: return KeyEvent.KEYCODE_8;
            case LwjglGlfwKeycode.GLFW_KEY_9: return KeyEvent.KEYCODE_9;
            case LwjglGlfwKeycode.GLFW_KEY_SEMICOLON: return KeyEvent.KEYCODE_SEMICOLON;
            case LwjglGlfwKeycode.GLFW_KEY_EQUAL: return KeyEvent.KEYCODE_EQUALS;
            case LwjglGlfwKeycode.GLFW_KEY_A: return KeyEvent.KEYCODE_A;
            case LwjglGlfwKeycode.GLFW_KEY_B: return KeyEvent.KEYCODE_B;
            case LwjglGlfwKeycode.GLFW_KEY_C: return KeyEvent.KEYCODE_C;
            case LwjglGlfwKeycode.GLFW_KEY_D: return KeyEvent.KEYCODE_D;
            case LwjglGlfwKeycode.GLFW_KEY_E: return KeyEvent.KEYCODE_E;
            case LwjglGlfwKeycode.GLFW_KEY_F: return KeyEvent.KEYCODE_F;
            case LwjglGlfwKeycode.GLFW_KEY_G: return KeyEvent.KEYCODE_G;
            case LwjglGlfwKeycode.GLFW_KEY_H: return KeyEvent.KEYCODE_H;
            case LwjglGlfwKeycode.GLFW_KEY_I: return KeyEvent.KEYCODE_I;
            case LwjglGlfwKeycode.GLFW_KEY_J: return KeyEvent.KEYCODE_J;
            case LwjglGlfwKeycode.GLFW_KEY_K: return KeyEvent.KEYCODE_K;
            case LwjglGlfwKeycode.GLFW_KEY_L: return KeyEvent.KEYCODE_L;
            case LwjglGlfwKeycode.GLFW_KEY_M: return KeyEvent.KEYCODE_M;
            case LwjglGlfwKeycode.GLFW_KEY_N: return KeyEvent.KEYCODE_N;
            case LwjglGlfwKeycode.GLFW_KEY_O: return KeyEvent.KEYCODE_O;
            case LwjglGlfwKeycode.GLFW_KEY_P: return KeyEvent.KEYCODE_P;
            case LwjglGlfwKeycode.GLFW_KEY_Q: return KeyEvent.KEYCODE_Q;
            case LwjglGlfwKeycode.GLFW_KEY_R: return KeyEvent.KEYCODE_R;
            case LwjglGlfwKeycode.GLFW_KEY_S: return KeyEvent.KEYCODE_S;
            case LwjglGlfwKeycode.GLFW_KEY_T: return KeyEvent.KEYCODE_T;
            case LwjglGlfwKeycode.GLFW_KEY_U: return KeyEvent.KEYCODE_U;
            case LwjglGlfwKeycode.GLFW_KEY_V: return KeyEvent.KEYCODE_V;
            case LwjglGlfwKeycode.GLFW_KEY_W: return KeyEvent.KEYCODE_W;
            case LwjglGlfwKeycode.GLFW_KEY_X: return KeyEvent.KEYCODE_X;
            case LwjglGlfwKeycode.GLFW_KEY_Y: return KeyEvent.KEYCODE_Y;
            case LwjglGlfwKeycode.GLFW_KEY_Z: return KeyEvent.KEYCODE_Z;
            case LwjglGlfwKeycode.GLFW_KEY_LEFT_BRACKET: return KeyEvent.KEYCODE_LEFT_BRACKET;
            case LwjglGlfwKeycode.GLFW_KEY_BACKSLASH: return KeyEvent.KEYCODE_BACKSLASH;
            case LwjglGlfwKeycode.GLFW_KEY_RIGHT_BRACKET: return KeyEvent.KEYCODE_RIGHT_BRACKET;
            case LwjglGlfwKeycode.GLFW_KEY_GRAVE_ACCENT: return KeyEvent.KEYCODE_GRAVE;
            case LwjglGlfwKeycode.GLFW_KEY_ESCAPE: return KeyEvent.KEYCODE_ESCAPE;
            case LwjglGlfwKeycode.GLFW_KEY_ENTER: return KeyEvent.KEYCODE_ENTER;
            case LwjglGlfwKeycode.GLFW_KEY_TAB: return KeyEvent.KEYCODE_TAB;
            case LwjglGlfwKeycode.GLFW_KEY_BACKSPACE: return KeyEvent.KEYCODE_DEL;
            case LwjglGlfwKeycode.GLFW_KEY_INSERT: return KeyEvent.KEYCODE_INSERT;
            case LwjglGlfwKeycode.GLFW_KEY_DELETE: return KeyEvent.KEYCODE_FORWARD_DEL;
            case LwjglGlfwKeycode.GLFW_KEY_RIGHT: return KeyEvent.KEYCODE_DPAD_RIGHT;
            case LwjglGlfwKeycode.GLFW_KEY_LEFT: return KeyEvent.KEYCODE_DPAD_LEFT;
            case LwjglGlfwKeycode.GLFW_KEY_UP: return KeyEvent.KEYCODE_DPAD_UP;
            case LwjglGlfwKeycode.GLFW_KEY_DOWN: return KeyEvent.KEYCODE_DPAD_DOWN;
            case LwjglGlfwKeycode.GLFW_KEY_PAGE_UP: return KeyEvent.KEYCODE_PAGE_UP;
            case LwjglGlfwKeycode.GLFW_KEY_PAGE_DOWN: return KeyEvent.KEYCODE_PAGE_DOWN;
            case LwjglGlfwKeycode.GLFW_KEY_HOME: return KeyEvent.KEYCODE_MOVE_HOME;
            case LwjglGlfwKeycode.GLFW_KEY_END: return KeyEvent.KEYCODE_MOVE_END;
            case LwjglGlfwKeycode.GLFW_KEY_CAPS_LOCK: return KeyEvent.KEYCODE_CAPS_LOCK;
            case LwjglGlfwKeycode.GLFW_KEY_SCROLL_LOCK: return KeyEvent.KEYCODE_SCROLL_LOCK;
            case LwjglGlfwKeycode.GLFW_KEY_NUM_LOCK: return KeyEvent.KEYCODE_NUM_LOCK;
            case LwjglGlfwKeycode.GLFW_KEY_PRINT_SCREEN: return KeyEvent.KEYCODE_SYSRQ;
            case LwjglGlfwKeycode.GLFW_KEY_PAUSE: return KeyEvent.KEYCODE_BREAK;
            case LwjglGlfwKeycode.GLFW_KEY_F1: return KeyEvent.KEYCODE_F1;
            case LwjglGlfwKeycode.GLFW_KEY_F2: return KeyEvent.KEYCODE_F2;
            case LwjglGlfwKeycode.GLFW_KEY_F3: return KeyEvent.KEYCODE_F3;
            case LwjglGlfwKeycode.GLFW_KEY_F4: return KeyEvent.KEYCODE_F4;
            case LwjglGlfwKeycode.GLFW_KEY_F5: return KeyEvent.KEYCODE_F5;
            case LwjglGlfwKeycode.GLFW_KEY_F6: return KeyEvent.KEYCODE_F6;
            case LwjglGlfwKeycode.GLFW_KEY_F7: return KeyEvent.KEYCODE_F7;
            case LwjglGlfwKeycode.GLFW_KEY_F8: return KeyEvent.KEYCODE_F8;
            case LwjglGlfwKeycode.GLFW_KEY_F9: return KeyEvent.KEYCODE_F9;
            case LwjglGlfwKeycode.GLFW_KEY_F10: return KeyEvent.KEYCODE_F10;
            case LwjglGlfwKeycode.GLFW_KEY_F11: return KeyEvent.KEYCODE_F11;
            case LwjglGlfwKeycode.GLFW_KEY_F12: return KeyEvent.KEYCODE_F12;
            case LwjglGlfwKeycode.GLFW_KEY_KP_0: return KeyEvent.KEYCODE_NUMPAD_0;
            case LwjglGlfwKeycode.GLFW_KEY_KP_1: return KeyEvent.KEYCODE_NUMPAD_1;
            case LwjglGlfwKeycode.GLFW_KEY_KP_2: return KeyEvent.KEYCODE_NUMPAD_2;
            case LwjglGlfwKeycode.GLFW_KEY_KP_3: return KeyEvent.KEYCODE_NUMPAD_3;
            case LwjglGlfwKeycode.GLFW_KEY_KP_4: return KeyEvent.KEYCODE_NUMPAD_4;
            case LwjglGlfwKeycode.GLFW_KEY_KP_5: return KeyEvent.KEYCODE_NUMPAD_5;
            case LwjglGlfwKeycode.GLFW_KEY_KP_6: return KeyEvent.KEYCODE_NUMPAD_6;
            case LwjglGlfwKeycode.GLFW_KEY_KP_7: return KeyEvent.KEYCODE_NUMPAD_7;
            case LwjglGlfwKeycode.GLFW_KEY_KP_8: return KeyEvent.KEYCODE_NUMPAD_8;
            case LwjglGlfwKeycode.GLFW_KEY_KP_9: return KeyEvent.KEYCODE_NUMPAD_9;
            case LwjglGlfwKeycode.GLFW_KEY_KP_DECIMAL: return KeyEvent.KEYCODE_NUMPAD_DOT;
            case LwjglGlfwKeycode.GLFW_KEY_KP_DIVIDE: return KeyEvent.KEYCODE_NUMPAD_DIVIDE;
            case LwjglGlfwKeycode.GLFW_KEY_KP_SUBTRACT: return KeyEvent.KEYCODE_NUMPAD_SUBTRACT;
            case LwjglGlfwKeycode.GLFW_KEY_KP_ADD: return KeyEvent.KEYCODE_NUMPAD_ADD;
            case LwjglGlfwKeycode.GLFW_KEY_KP_ENTER: return KeyEvent.KEYCODE_NUMPAD_ENTER;
            case LwjglGlfwKeycode.GLFW_KEY_KP_EQUAL: return KeyEvent.KEYCODE_NUMPAD_EQUALS;
            case LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT: return KeyEvent.KEYCODE_SHIFT_LEFT;
            case LwjglGlfwKeycode.GLFW_KEY_LEFT_CONTROL: return KeyEvent.KEYCODE_CTRL_LEFT;
            case LwjglGlfwKeycode.GLFW_KEY_LEFT_ALT: return KeyEvent.KEYCODE_ALT_LEFT;
            case LwjglGlfwKeycode.GLFW_KEY_RIGHT_SHIFT: return KeyEvent.KEYCODE_SHIFT_RIGHT;
            case LwjglGlfwKeycode.GLFW_KEY_RIGHT_CONTROL: return KeyEvent.KEYCODE_CTRL_RIGHT;
            case LwjglGlfwKeycode.GLFW_KEY_RIGHT_ALT: return KeyEvent.KEYCODE_ALT_RIGHT;
            default:
                return KeyEvent.KEYCODE_UNKNOWN;
        }
    }
}
