package net.kdt.pojavlaunch.customcontrols.buttons;

import static net.kdt.pojavlaunch.customcontrols.gamepad.GamepadJoystick.DIRECTION_EAST;
import static net.kdt.pojavlaunch.customcontrols.gamepad.GamepadJoystick.DIRECTION_NONE;
import static net.kdt.pojavlaunch.customcontrols.gamepad.GamepadJoystick.DIRECTION_NORTH;
import static net.kdt.pojavlaunch.customcontrols.gamepad.GamepadJoystick.DIRECTION_NORTH_EAST;
import static net.kdt.pojavlaunch.customcontrols.gamepad.GamepadJoystick.DIRECTION_NORTH_WEST;
import static net.kdt.pojavlaunch.customcontrols.gamepad.GamepadJoystick.DIRECTION_SOUTH;
import static net.kdt.pojavlaunch.customcontrols.gamepad.GamepadJoystick.DIRECTION_SOUTH_EAST;
import static net.kdt.pojavlaunch.customcontrols.gamepad.GamepadJoystick.DIRECTION_SOUTH_WEST;
import static net.kdt.pojavlaunch.customcontrols.gamepad.GamepadJoystick.DIRECTION_WEST;
import static net.kdt.pojavlaunch.game.platform.Platform.PLATFORM;

import android.annotation.SuppressLint;
import android.view.KeyEvent;
import android.view.View;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.customcontrols.ControlData;
import net.kdt.pojavlaunch.customcontrols.ControlJoystickData;
import net.kdt.pojavlaunch.customcontrols.ControlLayout;
import net.kdt.pojavlaunch.customcontrols.gamepad.GamepadJoystick;
import net.kdt.pojavlaunch.customcontrols.handleview.EditControlSideDialog;

import net.kdt.pojavlaunch.CallbackBridge;

import io.github.controlwear.virtual.joystick.android.JoystickView;

@SuppressLint("ViewConstructor")
public class ControlJoystick extends JoystickView implements ControlInterface {
    public final static int DIRECTION_FORWARD_LOCK = 8;
    private final static int DEADZONE = 35;
    // Directions keycode
    private final int[] mDirectionForwardLock = new int[]{KeyEvent.KEYCODE_CTRL_LEFT};
    private final int[] mDirectionForward = new int[]{KeyEvent.KEYCODE_W};
    private final int[] mDirectionRight = new int[]{KeyEvent.KEYCODE_D};
    private final int[] mDirectionBackward = new int[]{KeyEvent.KEYCODE_S};
    private final int[] mDirectionLeft = new int[]{KeyEvent.KEYCODE_A};
    private ControlJoystickData mControlData;
    private int mLastDirectionInt = GamepadJoystick.DIRECTION_NONE;
    private int mCurrentDirectionInt = GamepadJoystick.DIRECTION_NONE;
    // mode of the previous onMove: true = analog to the mod, false = WASD
    private boolean mAnalogActive = false;
    public ControlJoystick(ControlLayout parent, ControlJoystickData data) {
        super(parent.getContext());
        init(data, parent);
    }

    private static void sendInput(int[] keys, boolean isDown) {
        for (int key : keys) {
            int modifiers = CallbackBridge.getCurrentMods();
            PLATFORM.sendKeyEvent(key, isDown, modifiers);
        }
    }

    private void init(ControlJoystickData data, ControlLayout layout) {
        mControlData = data;
        setProperties(preProcessProperties(data, layout));
        setDeadzone(DEADZONE);
        setFixedCenter(data.absolute);
        setAutoReCenterButton(true);

        injectBehaviors();

        setOnMoveListener(new OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                // enabled by a mod; without one always false -> legacy WASD
                boolean analog = CallbackBridge.isAnalogMovement();
                if (analog != mAnalogActive) switchMode(analog);
                if (analog) {
                    sendAnalog(angle, strength);
                    return;
                }

                mLastDirectionInt = mCurrentDirectionInt;
                mCurrentDirectionInt = getDirectionInt(angle, strength);

                if (mLastDirectionInt != mCurrentDirectionInt) {
                    sendDirectionalKeycode(mLastDirectionInt, false);
                    sendDirectionalKeycode(mCurrentDirectionInt, true);
                }
            }

            @Override
            public void onForwardLock(boolean isLocked) {
                sendInput(mDirectionForwardLock, isLocked);
            }
        });
    }

    @Override
    public View getControlView() {
        return this;
    }

    @Override
    public ControlData getProperties() {
        return mControlData;
    }

    @Override
    public void setProperties(ControlData properties, boolean changePos) {
        mControlData = (ControlJoystickData) properties;
        mControlData.isHideable = true;
        ControlInterface.super.setProperties(properties, changePos);
        postDelayed(() -> {
            setForwardLockDistance(mControlData.forwardLock ? (int) Tools.dpToPx(60) : 0);
            setFixedCenter(mControlData.absolute);
        }, 10);
    }

    @Override
    public void removeButton() {
        getControlLayoutParent().getLayout().mJoystickDataList.remove(getProperties());
        getControlLayoutParent().removeView(this);
    }

    @Override
    public void cloneButton() {
        ControlJoystickData data = new ControlJoystickData(mControlData);
        getControlLayoutParent().addJoystickButton(data);
    }

    @Override
    public void handlePressed() {/*STUB since non swipeable*/}

    @Override
    public void handleReleased() {/*STUB since non swipeable*/}


    @Override
    public void setBackground() {
        setBorderWidth((int) Tools.dpToPx(getProperties().strokeWidth * (getControlLayoutParent().getLayoutScale()/100f)));
        setBorderColor(getProperties().strokeColor);
        setBackgroundColor(getProperties().bgColor);
    }

    @Override
    public void loadEditValues(EditControlSideDialog editControlPopup) {
        editControlPopup.loadJoystickValues(mControlData);
    }

    // mod toggled mid-gesture: release whatever the previous mode was holding
    private void switchMode(boolean analog) {
        if (analog) {
            sendDirectionalKeycode(mCurrentDirectionInt, false);
            mLastDirectionInt = mCurrentDirectionInt = DIRECTION_NONE;
        } else {
            CallbackBridge.sendAnalogMovement(0, 0);
        }
        mAnalogActive = analog;
    }

    // angle 0 = right, 90 = forward; strength is 0 or DEADZONE..100 (the library sends 0 inside the deadzone)
    private static void sendAnalog(int angle, int strength) {
        if (strength <= 0) {
            CallbackBridge.sendAnalogMovement(0, 0);
            return;
        }
        double rad = Math.toRadians(angle);
        // DEADZONE..100 -> 0..1, so speed doesn't jump at the deadzone edge
        float power = (Math.min(strength, 100) - DEADZONE) / (100f - DEADZONE);
        // small floor: a press past the deadzone always moves the player a little
        power = Math.max(power, 0.05f);
        CallbackBridge.sendAnalogMovement((float) Math.cos(rad) * power, (float) Math.sin(rad) * power);
    }

    private int getDirectionInt(int angle, int intensity) {
        if (intensity == 0) return DIRECTION_NONE;
        return (int) (((angle + 22.5) / 45) % 8);
    }

    private void sendDirectionalKeycode(int direction, boolean isDown) {
        switch (direction) {
            case DIRECTION_NORTH:
                sendInput(mDirectionForward, isDown);
                break;
            case DIRECTION_NORTH_EAST:
                sendInput(mDirectionForward, isDown);
                sendInput(mDirectionRight, isDown);
                break;
            case DIRECTION_EAST:
                sendInput(mDirectionRight, isDown);
                break;
            case DIRECTION_SOUTH_EAST:
                sendInput(mDirectionRight, isDown);
                sendInput(mDirectionBackward, isDown);
                break;
            case DIRECTION_SOUTH:
                sendInput(mDirectionBackward, isDown);
                break;
            case DIRECTION_SOUTH_WEST:
                sendInput(mDirectionBackward, isDown);
                sendInput(mDirectionLeft, isDown);
                break;
            case DIRECTION_WEST:
                sendInput(mDirectionLeft, isDown);
                break;
            case DIRECTION_NORTH_WEST:
                sendInput(mDirectionForward, isDown);
                sendInput(mDirectionLeft, isDown);
                break;
            case DIRECTION_FORWARD_LOCK:
                sendInput(mDirectionForwardLock, isDown);
                break;
        }
    }

}
