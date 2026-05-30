package net.kdt.pojavlaunch.tasks

import net.kdt.pojavlaunch.JMinecraftVersionList
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.instances.Instance

object AsyncMinecraftDownloader {
    @JvmStatic
    fun normalizeVersionId(versionString: String?): String? {
        var versionString = versionString
        val versionList = getVersionList()
        if (versionList == null || versionList.versions == null) return versionString
        if (Instance.VERSION_LATEST_RELEASE == versionString) versionString =
            versionList.latest?.get("release")
        if (Instance.VERSION_LATEST_SNAPSHOT == versionString) versionString =
            versionList.latest?.get("snapshot")
        return versionString
    }

    @JvmStatic
    fun getListedVersion(normalizedVersionString: String?): JMinecraftVersionList.Version? {
        val versionList = getVersionList()
        if (versionList == null || versionList.versions == null) return null // can't have listed versions if there's no list

        for (version in versionList.versions!!) {
            if (version?.id == normalizedVersionString) return version
        }
        return null
    }

    private fun getVersionList(): JMinecraftVersionList? {
        var versionList = ExtraCore.getValue(ExtraConstants.RELEASE_TABLE) as JMinecraftVersionList?
        if (versionList == null || versionList.versions == null) {
            versionList = AsyncVersionList().getVersionListSync()
            if (versionList != null) {
                ExtraCore.setValue(ExtraConstants.RELEASE_TABLE, versionList)
            }
        }
        return versionList
    }

    interface DoneListener {
        fun onDownloadDone()
        fun onDownloadFailed(throwable: Throwable?)
    }
}
