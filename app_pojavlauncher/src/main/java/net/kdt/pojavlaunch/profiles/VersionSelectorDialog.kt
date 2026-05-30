package net.kdt.pojavlaunch.profiles

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ExpandableListView
import android.widget.ExpandableListView.OnChildClickListener
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.JMinecraftVersionList
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore

object VersionSelectorDialog {
    @JvmStatic
    fun open(context: Context, hideCustomVersions: Boolean, listener: VersionSelectorListener) {
        val builder: AlertDialog.Builder = MaterialAlertDialogBuilder(context)
        val expandableListView = LayoutInflater.from(context)
            .inflate(R.layout.dialog_expendable_list_view, null) as ExpandableListView
        val jMinecraftVersionList =
            ExtraCore.getValue(ExtraConstants.RELEASE_TABLE) as JMinecraftVersionList?
        val versionArray: Array<JMinecraftVersionList.Version?>?
        if (jMinecraftVersionList == null || jMinecraftVersionList.versions == null) versionArray =
            arrayOfNulls<JMinecraftVersionList.Version>(0)
        else versionArray = jMinecraftVersionList.versions
        val adapter = VersionListAdapter(versionArray, hideCustomVersions, context)

        expandableListView.setAdapter(adapter)
        builder.setView(expandableListView)
        val dialog = builder.show()

        expandableListView.setOnChildClickListener(OnChildClickListener { parent: ExpandableListView?, v1: View?, groupPosition: Int, childPosition: Int, id: Long ->
            val version = adapter.getChild(groupPosition, childPosition)
            listener.onVersionSelected(version, adapter.isSnapshotSelected(groupPosition))
            dialog.dismiss()
            true
        })
    }
}
