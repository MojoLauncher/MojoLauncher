package net.kdt.pojavlaunch

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import com.google.firebase.FirebaseApp
import net.ashmeet.hyperlauncher.BuildConfig
import net.kdt.pojavlaunch.lifecycle.ContextExecutor
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.tasks.AsyncAssetManager


class PojavApplication : Application() {
    private fun installFatalErrorHandler() {
        Thread.setDefaultUncaughtExceptionHandler { thread: Thread?, th: Throwable? ->
            val storagePermAllowed =
                (Build.VERSION.SDK_INT < 23 || Build.VERSION.SDK_INT >= 29 || ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED) && Tools.checkStorageRoot(this)
            val crashFile = java.io.File(
                if (storagePermAllowed) Tools.DIR_GAME_HOME else Tools.DIR_DATA!!,
                "latestcrash.txt"
            )
            try {
                // Write to file, since some devices may not able to show error
                net.kdt.pojavlaunch.utils.FileUtils.ensureParentDirectory(crashFile)
                java.io.PrintStream(crashFile).use { crashStream ->
                    crashStream.append("PojavLauncher crash report\n")
                    crashStream.append(" - Time: ")
                        .append(java.text.DateFormat.getDateTimeInstance().format(java.util.Date()))
                        .append("\n")
                    crashStream.append(" - Device: ").append(Build.PRODUCT).append(" ")
                        .append(Build.MODEL).append("\n")
                    crashStream.append(" - Android version: ").append(Build.VERSION.RELEASE)
                        .append("\n")
                    crashStream.append(" - Crash stack trace:\n")
                    crashStream.append(" - Launcher version: " + BuildConfig.VERSION_NAME + "\n")
                    crashStream.append(android.util.Log.getStackTraceString(th))
                }
            } catch (throwable: Throwable) {
                android.util.Log.e(
                    CRASH_REPORT_TAG,
                    " - Exception attempt saving crash stack trace:",
                    throwable
                )
                android.util.Log.e(
                    CRASH_REPORT_TAG,
                    " - The crash stack trace was:",
                    th
                )
            }

            FatalErrorActivity.showError(
                this,
                crashFile.absolutePath,
                storagePermAllowed,
                th
            )
            Tools.fullyExit()
        }
    }

    override fun onCreate() {
        super.onCreate()

        ContextExecutor.setApplication(this)
        // Disable fatal errors on gplay. This is necessary so that google can collect crash report data and send it to me
        // (where i can find the cause and fix it)

        if (BuildConfig.BUILD_TYPE != "gplay") installFatalErrorHandler()

        try {
            if (Tools.checkStorageRoot(this)) {
                // Implicitly initializes early constants and storage constants.
                // Required to run the main activity properly.
                LauncherPreferences.loadPreferences(this)
            } else {
                // In other cases, only initialize enough for the basicmost basics to work
                // and not explode.
                Tools.initEarlyConstants(this)
            }
            Tools.DEVICE_ARCHITECTURE = Architecture.deviceArchitecture
            //Force x86 lib directory for Asus x86 based zenfones
            if (Architecture.isx86Device() && Architecture.is32BitsDevice()) {
                val originalJNIDirectory = applicationInfo.nativeLibraryDir
                applicationInfo.nativeLibraryDir = originalJNIDirectory.substring(
                    0,
                    originalJNIDirectory.lastIndexOf("/")
                ) + "/x86"
            }
            AsyncAssetManager.unpackRuntime(assets)

            // Initialize Firebase explicitly for multi-process compatibility (needed for :game process)
            FirebaseApp.initializeApp(this)

            // Initialize Lightweight Player Analytics
            PlayerAnalytics.init(this)
        } catch (throwable: Throwable) {
            val ferrorIntent = Intent(this, FatalErrorActivity::class.java)
            ferrorIntent.putExtra("throwable", throwable)
            ferrorIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(ferrorIntent)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        ContextExecutor.clearApplication()
    }

    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(net.kdt.pojavlaunch.utils.LocaleUtils.setLocale(base))
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        net.kdt.pojavlaunch.utils.LocaleUtils.setLocale(this)
    }

    companion object {
        const val CRASH_REPORT_TAG: String = "PojavCrashReport"
        @JvmField
		val sExecutorService: java.util.concurrent.ExecutorService =
            java.util.concurrent.ThreadPoolExecutor(
                4,
                4,
                500,
                java.util.concurrent.TimeUnit.MILLISECONDS,
                java.util.concurrent.LinkedBlockingQueue<Runnable?>()
            )
    }
}
