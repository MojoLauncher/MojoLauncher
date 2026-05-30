package net.kdt.pojavlaunch.customcontrols

object LayoutSanitizer {
    // Maybe add more conditions here later?
    private fun isValidFormula(formula: String?): Boolean {
        if (formula == null) return false
        return !formula.contains("Infinity") && !formula.contains("NaN")
    }

    private fun isSaneData(controlData: ControlData): Boolean {
        if (controlData.getWidth() == 0f || controlData.getHeight() == 0f) return false
        return isValidFormula(controlData.dynamicX) && isValidFormula(controlData.dynamicY)
    }

    private fun checkEntry(entry: Any): Boolean {
        if (entry is ControlData) {
            return isSaneData(entry)
        } else if (entry is ControlDrawerData) {
            val drawerData = entry
            if (!isSaneData(drawerData.properties)) return false
            sanitizeList(drawerData.buttonProperties)
            return true
        } else throw RuntimeException("Unknown data entry " + entry.javaClass.getName())
    }

    private fun sanitizeList(controlDataList: MutableList<*>?): Boolean {
        if (controlDataList == null) return false
        var madeChanges = false
        val iterator: MutableIterator<*> = controlDataList.iterator()
        while (iterator.hasNext()) {
            if (!LayoutSanitizer.checkEntry(iterator.next()!!)) {
                madeChanges = true
                iterator.remove()
            }
        }
        return madeChanges
    }

    /**
     * Check all buttons in a control layout and ensure they're sane (contain values valid enough
     * to be displayed properly). Removes any buttons deemed not sane.
     * @param controls the original control layout.
     * @return whether the sanitization process made any changes to the layout
     */
    fun sanitizeLayout(controls: CustomControls): Boolean {
        var madeChanges = sanitizeList(controls.mControlDataList)
        if (sanitizeList(controls.mDrawerDataList)) madeChanges = true
        if (sanitizeList(controls.mJoystickDataList)) madeChanges = true
        return madeChanges
    }
}
