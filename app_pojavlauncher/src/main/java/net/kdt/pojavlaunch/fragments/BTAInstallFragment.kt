package net.kdt.pojavlaunch.fragments

import android.content.Context
import android.view.LayoutInflater
import android.widget.ExpandableListAdapter
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.modloaders.BTADownloadTask
import net.kdt.pojavlaunch.modloaders.BTAUtils
import net.kdt.pojavlaunch.modloaders.BTAUtils.BTAVersion
import net.kdt.pojavlaunch.modloaders.BTAUtils.BTAVersionList
import net.kdt.pojavlaunch.modloaders.BTAVersionListAdapter
import net.kdt.pojavlaunch.modloaders.ModloaderListenerProxy
import java.io.File
import java.io.IOException

class BTAInstallFragment : ModVersionListFragment<BTAVersionList?>(TAG) {
    override val titleText: Int
        get() = R.string.select_bta_version

    override val noDataMsg: Int
        get() = R.string.modloader_dl_failed_to_load_list

    @Throws(IOException::class)
    override fun loadVersionList(): BTAVersionList? {
        return BTAUtils.downloadVersionList()
    }

    override fun createAdapter(
        versionList: BTAVersionList?,
        layoutInflater: LayoutInflater?
    ): ExpandableListAdapter? {
        return if (versionList != null && layoutInflater != null) {
            BTAVersionListAdapter(versionList, layoutInflater)
        } else {
            null
        }
    }

    override fun createDownloadTask(
        selectedVersion: Any?,
        listenerProxy: ModloaderListenerProxy?
    ): Runnable? {
        val version = selectedVersion as? BTAVersion
        return if (listenerProxy != null && version != null) {
            BTADownloadTask(listenerProxy, version)
        } else {
            null
        }
    }

    override fun onDownloadFinished(context: Context?, downloadedFile: File?) {
        // We don't have to do anything after the BTADownloadTask ends, so this is a stub
    }

    companion object {
        const val TAG: String = "BTAInstallFragment"
    }
}
