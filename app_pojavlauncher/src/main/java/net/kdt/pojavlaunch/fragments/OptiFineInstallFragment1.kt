package net.kdt.pojavlaunch.fragments

import android.content.Context
import android.view.LayoutInflater
import android.widget.ExpandableListAdapter
import com.kdt.mcgui.ProgressLayout
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.instances.Instance
import net.kdt.pojavlaunch.instances.InstanceSetter
import net.kdt.pojavlaunch.instances.Instances.Companion.createInstance
import net.kdt.pojavlaunch.modloaders.ModloaderListenerProxy
import net.kdt.pojavlaunch.modloaders.OptiFineDownloadTask
import net.kdt.pojavlaunch.modloaders.OptiFineUtils.OptiFineVersion
import net.kdt.pojavlaunch.modloaders.OptiFineUtils.OptiFineVersions
import net.kdt.pojavlaunch.modloaders.OptiFineUtils.createInstaller
import net.kdt.pojavlaunch.modloaders.OptiFineUtils.downloadOptiFineVersions
import net.kdt.pojavlaunch.modloaders.OptiFineVersionListAdapter
import java.io.File
import java.io.IOException

class OptiFineInstallFragment : ModVersionListFragment<OptiFineVersions>(TAG) {
    override val titleText: Int
        get() = R.string.of_dl_select_version

    override val noDataMsg: Int
        get() = R.string.of_dl_failed_to_scrape

    @Throws(IOException::class)
    override fun loadVersionList(): OptiFineVersions? {
        return downloadOptiFineVersions()
    }

    override fun createAdapter(
        versionList: OptiFineVersions?,
        layoutInflater: LayoutInflater?
    ): ExpandableListAdapter? {
        return if (versionList != null && layoutInflater != null) {
            OptiFineVersionListAdapter(versionList, layoutInflater)
        } else {
            null
        }
    }

    private fun createInstance(version: OptiFineVersion, listenerProxy: ModloaderListenerProxy) {
        try {
            ProgressLayout.setProgress(ProgressLayout.INSTALL_MODPACK, 0)
            OptiFineDownloadTask(version).prepareForInstall()
            val instanceInstaller = createInstaller(version)
            createInstance(InstanceSetter { instance: Instance? ->
                instance!!.name = "OptiFine"
                instance.installer = instanceInstaller
                instance.sharedData = true
            }, "OptiFine")
            ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK)
            instanceInstaller.start()
            listenerProxy.onDownloadFinished(null)
        } catch (e: Exception) {
            listenerProxy.onDownloadError(e)
        }
    }

    override fun createDownloadTask(
        selectedVersion: Any?,
        listenerProxy: ModloaderListenerProxy?
    ): Runnable? {
        val version = selectedVersion as? OptiFineVersion
        return if (listenerProxy != null && version != null) {
            Runnable { createInstance(version, listenerProxy) }
        } else {
            null
        }
    }

    override fun onDownloadFinished(context: Context?, downloadedFile: File?) {
    }

    companion object {
        const val TAG: String = "OptiFineInstallFragment"
    }
}
