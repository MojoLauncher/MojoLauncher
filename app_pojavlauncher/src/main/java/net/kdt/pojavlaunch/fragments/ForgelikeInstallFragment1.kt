package net.kdt.pojavlaunch.fragments

import android.content.Context
import android.view.LayoutInflater
import android.widget.ExpandableListAdapter
import com.kdt.mcgui.ProgressLayout
import net.kdt.pojavlaunch.instances.Instance
import net.kdt.pojavlaunch.instances.InstanceSetter
import net.kdt.pojavlaunch.instances.Instances.Companion.createInstance
import net.kdt.pojavlaunch.modloaders.ForgelikeUtils
import net.kdt.pojavlaunch.modloaders.ForgelikeVersionListAdapter
import net.kdt.pojavlaunch.modloaders.ModloaderListenerProxy
import java.io.File
import java.io.IOException

abstract class ForgelikeInstallFragment(private val mUtils: ForgelikeUtils, mFragmentTag: String?) :
    ModVersionListFragment<MutableList<String?>?>(mFragmentTag) {
    @Throws(IOException::class)
    override fun loadVersionList(): MutableList<String?>? {
        return mUtils.downloadVersions()
    }

    override fun createDownloadTask(
        selectedVersion: Any?,
        listenerProxy: ModloaderListenerProxy?
    ): Runnable? {
        return if (listenerProxy != null) {
            Runnable { createInstance((selectedVersion as kotlin.String?)!!, listenerProxy) }
        } else {
            null
        }
    }

    override fun createAdapter(
        versionList: MutableList<String?>?,
        layoutInflater: LayoutInflater?
    ): ExpandableListAdapter? {
        return if (versionList != null && layoutInflater != null) {
            ForgelikeVersionListAdapter(versionList, layoutInflater, mUtils)
        } else {
            null
        }
    }

    override fun onDownloadFinished(context: Context?, downloadedFile: File?) {
    }

    private fun createInstance(selectedVersion: String, listenerProxy: ModloaderListenerProxy) {
        try {
            ProgressLayout.setProgress(ProgressLayout.INSTALL_MODPACK, 0)
            val instanceInstaller = mUtils.createInstaller(selectedVersion)
            createInstance(InstanceSetter { instance: Instance? ->
                instance!!.name = mUtils.name
                instance.icon = mUtils.iconName
                instance.installer = instanceInstaller
            }, selectedVersion)
            ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK)
            instanceInstaller.start()
            listenerProxy.onDownloadFinished(null)
        } catch (e: IOException) {
            listenerProxy.onDownloadError(e)
        }
    }
}
