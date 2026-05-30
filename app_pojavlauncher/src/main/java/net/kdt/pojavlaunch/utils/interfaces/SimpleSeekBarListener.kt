package net.kdt.pojavlaunch.utils.interfaces

import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener

/**
 * Most interfaces implementations of [SeekBar.OnSeekBarChangeListener]
 * only implement the onProgressChanged method. This class provides a default for other methods.
 */
interface SimpleSeekBarListener : OnSeekBarChangeListener {
    override fun onStartTrackingTouch(seekBar: SeekBar?) {
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
    }
}
