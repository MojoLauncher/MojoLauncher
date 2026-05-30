package net.kdt.pojavlaunch.customcontrols

class ControlJoystickData : ControlData {
    /* Whether the joystick can stay forward */
    var forwardLock: Boolean = false

    /*
     * Whether the finger tracking is absolute (joystick jumps to where you touched)
     * or relative (joystick stays in the center)
     */
    var absolute: Boolean = false

    constructor() : super()

    constructor(properties: ControlJoystickData) : super(properties) {
        forwardLock = properties.forwardLock
        absolute = properties.absolute
    }
}
