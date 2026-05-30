package net.kdt.pojavlaunch.utils.interfaces

import android.widget.AdapterView

/**
 * Most interfaces implementations of [AdapterView.OnItemSelectedListener]
 * only implement the [AdapterView.OnItemSelectedListener.onItemSelected] onItemClick method.
 * This class provides a default for other methods.
 */
interface SimpleItemSelectedListener : AdapterView.OnItemSelectedListener {
    override fun onNothingSelected(parent: AdapterView<*>?) {
    }
}
