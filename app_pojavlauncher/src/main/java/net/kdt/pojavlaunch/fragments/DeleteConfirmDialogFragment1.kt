package net.kdt.pojavlaunch.fragments

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.Tools.removeCurrentFragment
import net.kdt.pojavlaunch.Tools.showErrorRemote
import net.kdt.pojavlaunch.instances.InstanceIconProvider.dropIcon
import net.kdt.pojavlaunch.instances.Instances.Companion.loadSelectedInstance
import net.kdt.pojavlaunch.instances.Instances.Companion.removeInstance
import java.io.IOException

class DeleteConfirmDialogFragment : DialogFragment() {
    private val mInstance = loadSelectedInstance()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (mInstance == null) dismiss()
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.instance_delete)
            .setMessage(R.string.instance_delete_confirmation)
            .setPositiveButton(
                R.string.global_delete
            ) { _: DialogInterface?, _: Int ->
                val instance = mInstance ?: return@setPositiveButton
                dropIcon(instance)
                removeCurrentFragment(requireActivity())
                try {
                    removeInstance(instance)
                } catch (e: IOException) {
                    showErrorRemote(e)
                }
            }
            .setNegativeButton(R.string.global_no, null)
            .create()
    }

    companion object {
        const val TAG: String = "delete_dialog_confirm"
    }
}
