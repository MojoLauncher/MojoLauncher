package net.kdt.pojavlaunch.multirt

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.OnShowListener
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.ashmeet.hyperlauncher.R

class MultiRTConfigDialog {
    private var mDialog: AlertDialog? = null
    private var mDialogView: RecyclerView? = null

    /** Show the dialog, refreshes the adapter data before showing it  */
    fun show() {
        refresh()
        mDialog!!.show()
    }

    @SuppressLint("NotifyDataSetChanged") //only used to completely refresh the list, it is necessary
    fun refresh() {
        val adapter = mDialogView!!.getAdapter()
        if (adapter != null) adapter.notifyDataSetChanged()
    }

    /** Build the dialog behavior and style  */
    fun prepare(activity: Context, installJvmLauncher: ActivityResultLauncher<Any?>) {
        mDialogView = RecyclerView(activity)
        mDialogView!!.setLayoutManager(
            LinearLayoutManager(
                activity,
                LinearLayoutManager.VERTICAL,
                false
            )
        )
        val adapter = RTRecyclerViewAdapter()
        mDialogView!!.setAdapter(adapter)

        mDialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.multirt_config_title)
            .setView(mDialogView)
            .setPositiveButton(
                R.string.multirt_config_add,
                DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                    installJvmLauncher.launch(null)
                })
            .setNeutralButton(R.string.multirt_delete_runtime, null)
            .create()

        // Custom button behavior without dismiss
        mDialog!!.setOnShowListener(OnShowListener { dialog: DialogInterface? ->
            val button = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_NEUTRAL)
            button.setOnClickListener(View.OnClickListener { view: View? ->
                val isEditing = !adapter.isEditing
                adapter.isEditing = isEditing
                button.setText(if (isEditing) R.string.multirt_config_setdefault else R.string.multirt_delete_runtime)
            })
        })
    }
}
