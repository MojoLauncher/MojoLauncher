package net.kdt.pojavlaunch.modloaders

import android.graphics.BitmapFactory
import android.util.Log
import com.kdt.mcgui.ProgressLayout
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.instances.Instance
import net.kdt.pojavlaunch.instances.InstanceSetter
import net.kdt.pojavlaunch.instances.Instances.Companion.createInstance
import net.kdt.pojavlaunch.modloaders.BTAUtils.BTAVersion
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper
import net.kdt.pojavlaunch.utils.FileUtils
import java.io.File
import java.io.IOException
import java.net.URL

class BTADownloadTask(
    private val mListener: ModloaderDownloadListener,
    private val mBtaVersion: BTAVersion
) : Runnable {
    override fun run() {
        ProgressKeeper.submitProgress(
            ProgressLayout.INSTALL_MODPACK,
            0,
            R.string.fabric_dl_progress,
            "BTA"
        )
        try {
            runCatching()
            mListener.onDownloadFinished(null)
        } catch (e: IOException) {
            mListener.onDownloadError(e)
        }
        ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK)
    }

    private fun tryDownloadIcon(targetInstance: Instance) {
        try {
            val iconBitmap = BitmapFactory.decodeStream(URL(mBtaVersion.iconUrl).openStream())
            targetInstance.encodeNewIcon(iconBitmap)
        } catch (e: IOException) {
            Log.w("BTADownloadTask", "Failed to download bta icon", e)
        }
    }

    @Throws(IOException::class)
    private fun createJson(btaVersionId: String) {
        val btaJson: String =
            String.format(BASE_JSON, mBtaVersion.versionName, mBtaVersion.downloadUrl, btaVersionId)
        val jsonDir = File(Tools.DIR_HOME_VERSION, btaVersionId)
        val jsonFile = File(jsonDir, btaVersionId + ".json")
        FileUtils.ensureDirectory(jsonDir)
        Tools.write(jsonFile, btaJson)
    }

    // BTA doesn't have SHA1 checksums in its repositories, so the user may try to reinstall it
    // if it didn't work due to a broken download. So, for reinstalls like that to work,
    // we need to delete the old client jar to force the download of a new one.
    @Throws(IOException::class)
    private fun removeOldClient() {
        val btaClientPath = File(
            Tools.DIR_HOME_LIBRARY,
            String.format("bta-client/bta-client-%1\$s.jar", mBtaVersion.versionName)
        )
        if (btaClientPath.exists() && !btaClientPath.delete()) throw IOException("Failed to delete old client jar")
    }

    @Throws(IOException::class)
    private fun createProfile(btaVersionId: String?) {
        val instance = createInstance(InstanceSetter { i: Instance? ->
            i!!.versionId = btaVersionId
            i.name = "Better than Adventure!"
        }, "BTA-" + btaVersionId)
        tryDownloadIcon(instance)
    }

    @Throws(IOException::class)
    fun runCatching() {
        removeOldClient()
        val btaVersionId = "bta-" + mBtaVersion.versionName
        createJson(btaVersionId)
        createProfile(btaVersionId)
    }

    companion object {
        private const val BASE_JSON =
            "{\"inheritsFrom\":\"b1.7.3\",\"mainClass\":\"net.minecraft.client.Minecraft\",\"libraries\":[{\"name\":\"bta-client:bta-client:%1\$s\",\"downloads\":{\"artifact\":{\"path\":\"bta-client/bta-client-%1\$s.jar\",\"url\":\"%2\$s\"}}}],\"id\":\"%3\$s\"}"
    }
}
