package net.kdt.pojavlaunch.customcontrols

import androidx.annotation.Keep

@Keep
class ControlDrawerData {
    val buttonProperties: ArrayList<ControlData>
    val properties: ControlData
    var orientation: Orientation?

    @Keep
    enum class Orientation {
        DOWN,
        LEFT,
        UP,
        RIGHT,
        FREE
    }

    @JvmOverloads
    constructor(
        buttonProperties: ArrayList<ControlData> = ArrayList<ControlData>(),
        properties: ControlData = ControlData("Drawer", intArrayOf(), 100f, 100f, false),
        orientation: Orientation? = Orientation.LEFT
    ) {
        this.buttonProperties = buttonProperties
        this.properties = properties
        this.orientation = orientation
    }

    constructor(drawerData: ControlDrawerData) {
        buttonProperties = ArrayList<ControlData>(drawerData.buttonProperties.size)
        for (controlData in drawerData.buttonProperties) {
            buttonProperties.add(ControlData(controlData))
        }
        properties = ControlData(drawerData.properties)
        orientation = drawerData.orientation
    }

    companion object {
        val orientations: Array<Orientation>
            get() = arrayOf(
                Orientation.DOWN,
                Orientation.LEFT,
                Orientation.UP,
                Orientation.RIGHT,
                Orientation.FREE
            )

        fun orientationToInt(orientation: Orientation): Int {
            when (orientation) {
                Orientation.DOWN -> return 0
                Orientation.LEFT -> return 1
                Orientation.UP -> return 2
                Orientation.RIGHT -> return 3
                Orientation.FREE -> return 4
            }
            return -1
        }

        fun intToOrientation(by: Int): Orientation? {
            when (by) {
                0 -> return Orientation.DOWN
                1 -> return Orientation.LEFT
                2 -> return Orientation.UP
                3 -> return Orientation.RIGHT
                4 -> return Orientation.FREE
            }
            return null
        }
    }
}
