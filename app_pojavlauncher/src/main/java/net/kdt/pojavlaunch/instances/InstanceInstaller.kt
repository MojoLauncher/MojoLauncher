package net.kdt.pojavlaunch.instances

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.os.Bundle
import com.kdt.mcgui.ProgressLayout
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.JavaGUILauncherActivity
import net.kdt.pojavlaunch.LauncherActivity
import net.kdt.pojavlaunch.PojavApplication
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.instances.profcompat.ProfileWatcher
import net.kdt.pojavlaunch.lifecycle.ContextExecutor
import net.kdt.pojavlaunch.lifecycle.ContextExecutorTask
import net.kdt.pojavlaunch.modloaders.OFDownloadPageScraper
import net.kdt.pojavlaunch.progresskeeper.DownloaderProgressWrapper
import net.kdt.pojavlaunch.utils.DownloadUtils
import net.kdt.pojavlaunch.utils.JSONUtils
import net.kdt.pojavlaunch.utils.NotificationUtils
import java.io.File
import java.io.IOException
import java.util.Objects
import java.util.concurrent.Callable

class InstanceInstaller : ContextExecutorTask {
    @JvmField
    var installerJar: String? = null

    @Transient
    private var installerJarFile: File? = null

    @Transient
    private var mTransformedUrl: String? = null
    @JvmField
    var commandLineArgs: MutableList<String?>? = null
    @JvmField
    var installerUrlTransformer: String? = null
    @JvmField
    var installerDownloadUrl: String? = null
    @JvmField
    var installerSha1: String? = null

    private fun installerJar(): File {
        if (installerJarFile == null) return File(installerJar ?: "").also { installerJarFile = it }
        return installerJarFile!!
    }

    @Throws(IOException::class)
    private fun installerDownloadUrl(): String? {
        if (mTransformedUrl != null) return mTransformedUrl
        val newUrl: String?
        if ("optifine" == installerUrlTransformer) {
            newUrl = OFDownloadPageScraper.run(installerDownloadUrl)
        } else {
            newUrl = installerDownloadUrl
        }
        mTransformedUrl = newUrl
        return newUrl
    }

    @Throws(IOException::class)
    private fun writeLastInstaller() {
        JSONUtils.writeToFile(sLastInstallInfo, this)
    }

    @Throws(IOException::class)
    fun threadedStart() {
        try {
            val buffer = ByteArray(8192)
            val wrapper = DownloaderProgressWrapper(
                R.string.mcl_launch_downloading_progress, ProgressLayout.INSTANCE_INSTALL
            )
            wrapper.extraString = installerJar().name
            DownloadUtils.ensureSha1<Any?>(installerJar(), installerSha1, Callable {
                DownloadUtils.downloadFileMonitored(
                    installerDownloadUrl(),
                    installerJar(),
                    buffer,
                    wrapper
                )
                null
            })
            ContextExecutor.execute(this)
        } finally {
            ProgressLayout.clearProgress(ProgressLayout.INSTANCE_INSTALL)
        }
    }

    fun start() {
        ProgressLayout.setProgress(ProgressLayout.INSTANCE_INSTALL, 0)
        PojavApplication.sExecutorService.execute {
            try {
                threadedStart()
            } catch (e: Exception) {
                Tools.showErrorRemote(e)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InstanceInstaller) return false
        return installerJar == other.installerJar &&
                commandLineArgs == other.commandLineArgs &&
                installerDownloadUrl == other.installerDownloadUrl &&
                installerUrlTransformer == other.installerUrlTransformer &&
                installerSha1 == other.installerSha1
    }

    override fun hashCode(): Int {
        return Objects.hash(
            installerJar,
            commandLineArgs,
            installerDownloadUrl,
            installerUrlTransformer,
            installerSha1
        )
    }

    private val isTrustedInstaller: Boolean
        get() {
            val url = installerDownloadUrl ?: return false
            for (frontTrusted in TRUSTED_URLS) {
                if (frontTrusted != null && url.startsWith(frontTrusted)) return true
            }
            return false
        }

    override fun executeWithActivity(activity: Activity?) {
        val act = activity ?: return
        try {
            ProfileWatcher.installDefaultProfiles(act.assets)
            writeLastInstaller()
        } catch (e: Exception) {
            Tools.showError(act, e)
            return
        }
        val intent = Intent(act, JavaGUILauncherActivity::class.java)
        val extras = Bundle()
        extras.putStringArrayList("javaArgs", if (commandLineArgs == null) null else ArrayList(commandLineArgs!!))
        extras.putString("modPath", installerJar)
        extras.putBoolean("trusted", this.isTrustedInstaller)
        intent.putExtras(extras)
        act.startActivity(intent)
    }

    override fun executeWithApplication(context: Context?) {
        if (context == null) return
        Tools.runOnUiThread {
            NotificationUtils.sendBasicNotification(
                context,
                R.string.modpack_install_notification_title,
                R.string.modpack_install_notification_success,
                Intent(context, LauncherActivity::class.java),
                NotificationUtils.PENDINGINTENT_CODE_DOWNLOAD_SERVICE,
                NotificationUtils.NOTIFICATION_ID_DOWNLOAD_LISTENER
            )
        }
    }

    companion object {
        private val sLastInstallInfo = File(Tools.DIR_CACHE, "last_installer.json")

        private val TRUSTED_URLS: Array<String?> = arrayOf(
            "https://maven.neoforged.net/releases/net/neoforged/neoforge/",
            "https://maven.minecraftforge.net/net/minecraftforge/forge/",
            "https://optifine.net/adloadx"
        )

        @Throws(IOException::class)
        fun postInstallCheck(assetManager: AssetManager?) {
            if (!sLastInstallInfo.exists() || !sLastInstallInfo.isFile) return
            val lastInstaller = JSONUtils.readFromFile(
                sLastInstallInfo,
                InstanceInstaller::class.java
            ) ?: return
            lastInstaller.installerJar().delete()
            if (!sLastInstallInfo.delete()) throw IOException("Failed to delete mod installer info")
            val targetVersionId = ProfileWatcher.consumePendingVersion(assetManager!!)
            if (targetVersionId == null) return
            for (instance in Instances.loadAllInstances()) {
                if (lastInstaller != instance.installer) continue
                instance.installer = null
                instance.versionId = targetVersionId
                instance.write()
            }
            ExtraCore.setValue(ExtraConstants.REFRESH_VERSION_SPINNER, null)
        }

        @JvmStatic
        fun postInstallCheck(context: Context) {
            try {
                postInstallCheck(context.assets)
            } catch (e: Exception) {
                Tools.showError(context, e)
                if (sLastInstallInfo.isFile) {
                    sLastInstallInfo.delete()
                }
            }
        }
    }
}
