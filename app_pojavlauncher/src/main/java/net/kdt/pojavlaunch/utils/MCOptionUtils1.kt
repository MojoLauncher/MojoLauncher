package net.kdt.pojavlaunch.utils

import android.os.Build
import android.os.Build.VERSION
import android.os.FileObserver
import android.util.Log
import net.kdt.pojavlaunch.Tools
import org.lwjgl.glfw.CallbackBridge
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.lang.ref.WeakReference
import kotlin.math.max
import kotlin.math.min

object MCOptionUtils {
    private val sParameterMap = HashMap<String?, String?>()
    private val sOptionListeners = ArrayList<WeakReference<MCOptionListener?>>()
    private var sFileObserver: FileObserver? = null
    private var sOptionFolderPath: String? = null

    @JvmStatic
    @JvmOverloads
    fun load(
        folderPath: String? = sOptionFolderPath ?: Tools.DIR_GAME_NEW
    ) {
        if (folderPath == null) return
        val optionFile = File("$folderPath/options.txt")
        if (!optionFile.exists()) {
            try { // Needed for new instances I guess  :think:
                optionFile.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        if (sFileObserver == null || sOptionFolderPath != folderPath) {
            sOptionFolderPath = folderPath
            setupFileObserver()
        }
        sOptionFolderPath = folderPath // Yeah I know, it may be redundant

        sParameterMap.clear()

        try {
            BufferedReader(FileReader(optionFile)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val currentLine = line ?: continue
                    val firstColonIndex = currentLine.indexOf(':')
                    if (firstColonIndex < 0) {
                        Log.w(Tools.APP_NAME, "No colon on line \"$currentLine\", skipping")
                        continue
                    }
                    sParameterMap[currentLine.substring(0, firstColonIndex)] =
                        currentLine.substring(firstColonIndex + 1)
                }
            }
        } catch (e: IOException) {
            Log.w(Tools.APP_NAME, "Could not load options.txt", e)
        }
    }

    @JvmStatic
    fun set(key: String?, value: String?) {
        sParameterMap[key] = value
    }

    /** Set an array of String, instead of a simple value. Not supported on all options  */
    fun set(key: String?, values: MutableList<String?>) {
        sParameterMap[key] = values.toString()
    }

    fun get(key: String?): String? {
        return sParameterMap[key]
    }

    @JvmStatic
    fun save() {
        val result = StringBuilder()
        for (key in sParameterMap.keys) {
            result.append(key)
                .append(':')
                .append(sParameterMap[key])
                .append('\n')
        }

        try {
            sFileObserver?.stopWatching()
            Tools.write("$sOptionFolderPath/options.txt", result.toString())
            sFileObserver?.startWatching()
        } catch (e: IOException) {
            Log.w(Tools.APP_NAME, "Could not save options.txt", e)
        }
    }

    @JvmStatic
    fun mcScale(): Int {
        /** @return The stored Minecraft GUI scale, also auto-computed if on auto-mode or improper setting
         */
        val str = get("guiScale")
        var guiScale = str?.toInt() ?: 0

        val scale = max(
            min(
                CallbackBridge.windowWidth / 320,
                CallbackBridge.windowHeight / 240
            ), 1
        )
        if (scale < guiScale || guiScale == 0) {
            guiScale = scale
        }

        return guiScale
    }

    /** Add a file observer to reload options on file change
     * Listeners get notified of the change  */
    private fun setupFileObserver() {
        val folderPath = sOptionFolderPath ?: return
        val file = File(folderPath, "options.txt")

        sFileObserver = if (VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            object : FileObserver(file, MODIFY) {
                override fun onEvent(i: Int, s: String?) {
                    load()
                    notifyListeners()
                }
            }
        } else {
            @Suppress("DEPRECATION")
            object : FileObserver(file.absolutePath, MODIFY) {
                override fun onEvent(i: Int, s: String?) {
                    load()
                    notifyListeners()
                }
            }
        }

        sFileObserver?.startWatching()
    }

    /** Notify the option listeners  */
    fun notifyListeners() {
        for (weakReference in sOptionListeners) {
            val optionListener = weakReference.get() ?: continue
            optionListener.onOptionChanged()
        }
    }

    /** Add an option listener, notice how we don't have a reference to it  */
    @JvmStatic
    fun addMCOptionListener(listener: MCOptionListener?) {
        sOptionListeners.add(WeakReference(listener))
    }

    fun interface MCOptionListener {
        /** Called when an option is changed. Don't know which one though  */
        fun onOptionChanged()
    }
}
