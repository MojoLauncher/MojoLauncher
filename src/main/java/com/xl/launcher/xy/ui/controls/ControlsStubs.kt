package com.xl.launcher.xy.ui.controls

data class ControlConfig(var x: Float, var y: Float, var scale: Float, var alpha: Float)

object TouchLayoutManager {
    private val layouts = mutableMapOf<String, List<ControlConfig>>()
    fun save(name: String, layout: List<ControlConfig>) { layouts[name] = layout }
    fun load(name: String): List<ControlConfig>? = layouts[name]
}

object VirtualKeyboard {
    fun mapTouch(x: Float, y: Float): String = "W" // mock
}
