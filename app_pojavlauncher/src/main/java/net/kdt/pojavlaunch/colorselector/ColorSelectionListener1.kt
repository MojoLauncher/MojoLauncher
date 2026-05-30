package net.kdt.pojavlaunch.colorselector

interface ColorSelectionListener {
    /**
     * This method gets called by the ColorSelector when the color is selected
     * @param color the selected color
     */
    fun onColorSelected(color: Int)
}
