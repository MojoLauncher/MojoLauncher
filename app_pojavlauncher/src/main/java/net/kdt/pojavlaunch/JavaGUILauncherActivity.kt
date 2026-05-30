package net.kdt.pojavlaunch

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.core.math.MathUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kdt.LoggerView
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.customcontrols.keyboard.AwtCharSender
import net.kdt.pojavlaunch.customcontrols.keyboard.TouchCharInput
import net.kdt.pojavlaunch.lifecycle.ContextExecutor.execute
import net.kdt.pojavlaunch.multirt.MultiRTUtils.forceReread
import net.kdt.pojavlaunch.multirt.MultiRTUtils.getNearestJreName
import net.kdt.pojavlaunch.multirt.Runtime
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.utils.JREUtils.parseJavaArguments
import net.kdt.pojavlaunch.utils.JREUtils.redirectAndPrintJRELog
import net.kdt.pojavlaunch.utils.MathUtils.map
import net.kdt.pojavlaunch.utils.jre.JavaRunner
import net.kdt.pojavlaunch.utils.jre.JavaRunner.nativeSetupExit
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.Collections
import java.util.jar.JarFile
import kotlin.math.max
import kotlin.math.min

class JavaGUILauncherActivity : BaseActivity(), OnTouchListener {
    private var mTextureView: AWTCanvasView? = null
    private var mLoggerView: LoggerView? = null
    private var mTouchCharInput: TouchCharInput? = null

    private var mTouchPad: LinearLayout? = null
    private var mMousePointerImageView: ImageView? = null
    private var mGestureDetector: GestureDetector? = null

    private var mIsVirtualMouseEnabled = false
    private var mIsTrusted = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_java_gui_launcher)

        try {
            val latestLogFile = File(Tools.DIR_GAME_HOME, "latestlog.txt")
            if (!latestLogFile.exists() && !latestLogFile.createNewFile()) throw IOException("Failed to create a new log file")
            Logger.begin(latestLogFile.absolutePath)
        } catch (e: IOException) {
            Tools.showError(this, e, true)
        }
        MainActivity.GLOBAL_CLIPBOARD =
            getSystemService(CLIPBOARD_SERVICE) as ClipboardManager?
        mTouchCharInput = findViewById<TouchCharInput>(R.id.awt_touch_char)
        mTouchCharInput!!.setCharacterSender(AwtCharSender())

        mTouchPad = findViewById<LinearLayout>(R.id.main_touchpad)
        mLoggerView = findViewById<LoggerView>(R.id.launcherLoggerView)
        mMousePointerImageView = findViewById<ImageView>(R.id.main_mouse_pointer)
        mTextureView = findViewById<AWTCanvasView>(R.id.installmod_surfaceview)
        mGestureDetector = GestureDetector(this, SingleTapConfirm())
        mTouchPad!!.isFocusable = false
        mTouchPad!!.visibility = View.GONE

        findViewById<View?>(R.id.installmod_mouse_pri).setOnTouchListener(this)
        findViewById<View?>(R.id.installmod_mouse_sec).setOnTouchListener(this)
        findViewById<View?>(R.id.installmod_window_moveup).setOnTouchListener(this)
        findViewById<View?>(R.id.installmod_window_movedown).setOnTouchListener(this)
        findViewById<View?>(R.id.installmod_window_moveleft).setOnTouchListener(this)
        findViewById<View?>(R.id.installmod_window_moveright).setOnTouchListener(this)

        mMousePointerImageView!!.post {
            val params = mMousePointerImageView!!.layoutParams
            params.width = (36 * LauncherPreferences.PREF_MOUSESCALE).toInt()
            params.height = (54 * LauncherPreferences.PREF_MOUSESCALE).toInt()
        }

        mTouchPad!!.setOnTouchListener(object : OnTouchListener {
            var prevX: Float = 0f
            var prevY: Float = 0f
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val action = event.actionMasked

                val x = event.x
                val y = event.y
                var mouseX: Float
                var mouseY: Float

                mouseX = mMousePointerImageView!!.x
                mouseY = mMousePointerImageView!!.y

                if (mGestureDetector!!.onTouchEvent(event)) {
                    sendScaledMousePosition(mouseX, mouseY)
                    AWTInputBridge.sendMousePress(AWTInputEvent.BUTTON1_DOWN_MASK)
                } else {
                    if (action == MotionEvent.ACTION_MOVE) { // 2
                        mouseX = max(0f, min(v.width.toFloat(), mouseX + x - prevX))
                        mouseY = max(0f, min(v.height.toFloat(), mouseY + y - prevY))
                        placeMouseAt(mouseX, mouseY)
                        sendScaledMousePosition(mouseX, mouseY)
                    }
                }

                prevY = y
                prevX = x
                return true
            }
        })

        mTextureView!!.setOnTouchListener { _, event ->
            val x = event!!.x
            val y = event.y
            if (mGestureDetector!!.onTouchEvent(event)) {
                sendScaledMousePosition(x + mTextureView!!.x, y)
                AWTInputBridge.sendMousePress(AWTInputEvent.BUTTON1_DOWN_MASK)
                return@setOnTouchListener true
            }

            when (event.actionMasked) {
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_UP -> {}
                MotionEvent.ACTION_MOVE -> sendScaledMousePosition(x + mTextureView!!.x, y)
            }
            true
        }

        try {
            val extras = intent.extras
            if (extras == null) {
                finish()
                return
            }
            mIsTrusted = extras.getBoolean("trusted", false)
            val javaArgs: MutableList<String>? = extras.getStringArrayList("javaArgs")
            val resourceUri = extras.getParcelable<Uri?>("modUri")
            val jarPath = extras.getString("modPath")
            if (jarPath != null) {
                val jarFile = File(jarPath)
                startModInstaller(jarFile, javaArgs)
            } else {
                PojavApplication.sExecutorService.execute {
                    startModInstallerWithUri(
                        resourceUri,
                        javaArgs
                    )
                }
            }
            if (extras.getBoolean("openLogOutput", false)) openLogOutput(null)
        } catch (th: Throwable) {
            Tools.showError(this, th, true)
        }


        onBackPressedDispatcher.addCallback(this) {
            Tools.dialogForceClose(this@JavaGUILauncherActivity)
        }
    }

    private fun startModInstallerWithUri(uri: Uri?, javaArgs: MutableList<String>?) {
        if (uri == null) {
            startModInstaller(null, javaArgs)
            return
        }
        try {
            val cacheFile = File(cacheDir, "mod-installer-temp")
            val contentStream = contentResolver.openInputStream(uri)
            if (contentStream == null) throw IOException("Failed to open content stream")
            FileOutputStream(cacheFile).use { fileOutputStream ->
                IOUtils.copy(contentStream, fileOutputStream)
            }
            contentStream.close()
            startModInstaller(cacheFile, javaArgs)
        } catch (e: IOException) {
            Tools.showError(this, e, true)
        }
    }

    fun selectRuntime(javaVersion: Int): Runtime? {
        if (javaVersion == -1) {
            finalErrorDialog(getString(R.string.execute_jar_failed_to_read_file))
            return null
        }
        val nearestRuntime = getNearestJreName(javaVersion)
        if (nearestRuntime == null) {
            finalErrorDialog(getString(R.string.multirt_nocompatiblert, javaVersion))
            return null
        }
        return forceReread(nearestRuntime)
    }

    private class JarFileProperties(val mainClass: String?, val minJavaVersion: Int) {
        companion object {
            @Throws(IOException::class)
            fun read(file: File?): JarFileProperties? {
                if (file == null) return null
                JarFile(file).use { jarFile ->
                    val manifest = jarFile.manifest
                    if (manifest == null) return null
                    val mainAttrs = manifest.mainAttributes
                    if (mainAttrs == null) return null
                    val mainClass = mainAttrs.getValue("Main-Class")?.trim()
                    if (mainClass.isNullOrEmpty()) return null
                    val javaVersion: Int = getJavaVersion(jarFile, mainClass)
                    return JarFileProperties(mainClass, javaVersion)
                }
            }
        }
    }

    private fun runModInstaller(modFile: File?, javaArgs: MutableList<String>?) {
        if (modFile == null) {
             finalErrorDialog(getString(R.string.execute_jar_failed_to_read_file))
             return
        }
        var jarFileProperties: JarFileProperties? = null
        try {
            jarFileProperties = JarFileProperties.read(modFile)
        } catch (e: IOException) {
            Log.i("JavaGUILauncherActivity", "Failed to read JarFileProperties", e)
        }
        if (jarFileProperties == null) {
            finalErrorDialog(getString(R.string.execute_jar_failed_to_read_file))
            return
        }
        val selectedRuntime = selectRuntime(jarFileProperties.minJavaVersion)
        if (selectedRuntime == null) return
        launchJavaRuntime(selectedRuntime, javaArgs, modFile, jarFileProperties.mainClass)
    }

    private fun startModInstaller(modFile: File?, javaArgs: MutableList<String>?) {
        Thread({ runModInstaller(modFile, javaArgs) }, "JREMainThread").start()
    }

    private fun finalErrorDialog(msg: CharSequence?) {
        runOnUiThread {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.global_error)
                .setMessage(msg)
                .setPositiveButton(
                    android.R.string.ok
                ) { _, _ -> this.finish() }
                .setCancelable(false)
                .show()
        }
    }

    public override fun onResume() {
        super.onResume()
        val uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        val decorView = window.decorView
        decorView.systemUiVisibility = uiOptions
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, e: MotionEvent): Boolean {
        val isDown: Boolean
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> isDown = true
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_UP -> isDown =
                false

            else -> return false
        }

        when (v.id) {
            R.id.installmod_mouse_pri -> AWTInputBridge.sendMousePress(
                AWTInputEvent.BUTTON1_DOWN_MASK,
                isDown
            )

            R.id.installmod_mouse_sec -> AWTInputBridge.sendMousePress(
                AWTInputEvent.BUTTON3_DOWN_MASK,
                isDown
            )
        }
        if (isDown) when (v.id) {
            R.id.installmod_window_moveup -> AWTInputBridge.nativeMoveWindow(0, -10)
            R.id.installmod_window_movedown -> AWTInputBridge.nativeMoveWindow(0, 10)
            R.id.installmod_window_moveleft -> AWTInputBridge.nativeMoveWindow(-10, 0)
            R.id.installmod_window_moveright -> AWTInputBridge.nativeMoveWindow(10, 0)
        }
        return true
    }

    fun placeMouseAt(x: Float, y: Float) {
        mMousePointerImageView!!.x = x
        mMousePointerImageView!!.y = y
    }

    fun sendScaledMousePosition(x: Float, y: Float) {
        // Clamp positions to the borders of the usable view, then scale them
        var newX = x
        var newY = y
        newX = MathUtils.clamp(
            newX,
            mTextureView!!.x,
            mTextureView!!.x + mTextureView!!.width
        )
        newY = MathUtils.clamp(
            newY,
            mTextureView!!.y,
            mTextureView!!.y + mTextureView!!.height
        )

        AWTInputBridge.sendMousePos(
            map(
                newX,
                mTextureView!!.x,
                mTextureView!!.x + mTextureView!!.width,
                0f,
                AWTCanvasView.AWT_CANVAS_WIDTH.toFloat()
            ).toInt(),
            map(
                newY,
                mTextureView!!.y,
                mTextureView!!.y + mTextureView!!.height,
                0f,
                AWTCanvasView.AWT_CANVAS_HEIGHT.toFloat()
            ).toInt()
        )
    }

    fun forceClose(v: View?) {
        Tools.dialogForceClose(this)
    }

    fun openLogOutput(v: View?) {
        mLoggerView!!.visibility = View.VISIBLE
    }

    fun toggleVirtualMouse(v: View?) {
        mIsVirtualMouseEnabled = !mIsVirtualMouseEnabled
        mTouchPad!!.visibility = if (mIsVirtualMouseEnabled) View.VISIBLE else View.GONE
        if (mIsVirtualMouseEnabled && mMousePointerImageView!!.x == 0f && mMousePointerImageView!!.y == 0f) {
            mTouchPad!!.post {
                placeMouseAt(
                    mTouchPad!!.width / 2f,
                    mTouchPad!!.height / 2f
                )
            }
        }
        Toast.makeText(
            this,
            if (mIsVirtualMouseEnabled) R.string.control_mouseon else R.string.control_mouseoff,
            Toast.LENGTH_SHORT
        ).show()
    }

    fun launchJavaRuntime(
        runtime: Runtime,
        javaArgs: MutableList<String>?,
        modFile: File,
        mainClass: String?
    ) {
        redirectAndPrintJRELog()
        try {
            val javaArgList: MutableList<String> = ArrayList<String>()

            if (javaArgs != null) {
                javaArgList.addAll(javaArgs)
            }

            javaArgList.addAll(
                parseJavaArguments(LauncherPreferences.PREF_CUSTOM_JAVA_ARGS).filterNotNull()
            )

            if (LauncherPreferences.PREF_JAVA_SANDBOX && !mIsTrusted) {
                javaArgList.add(0, "-Djava.security.policy=" + Tools.DIR_DATA + "/security/java_sandbox.policy")
                javaArgList.add(0, "-Djava.security.manager=net.sourceforge.prograde.sm.ProGradeJSM")
                javaArgList.add(0, "-Xbootclasspath/a:" + Tools.DIR_DATA + "/security/pro-grade.jar")
            }

            Logger.appendToLog("Info: Java arguments: " + javaArgList)

            nativeSetupExit(this.applicationContext)
            JavaRunner.startJvm(
                runtime,
                javaArgList,
                mutableListOf(modFile.absolutePath),
                mainClass,
                mutableListOf()
            )
        } catch (th: Throwable) {
            Tools.showError(this, th, true)
        }
    }

    fun toggleKeyboard(view: View?) {
        mTouchCharInput!!.switchKeyboardState()
    }

    fun performCopy(view: View?) {
        AWTInputBridge.sendKey(' ', AWTInputEvent.VK_CONTROL, 1)
        AWTInputBridge.sendKey(' ', AWTInputEvent.VK_C)
        AWTInputBridge.sendKey(' ', AWTInputEvent.VK_CONTROL, 0)
    }

    fun performPaste(view: View?) {
        AWTInputBridge.sendKey(' ', AWTInputEvent.VK_CONTROL, 1)
        AWTInputBridge.sendKey(' ', AWTInputEvent.VK_V)
        AWTInputBridge.sendKey(' ', AWTInputEvent.VK_CONTROL, 0)
    }

    companion object {
        @Throws(IOException::class)
        private fun getJavaVersion(jarFile: JarFile, mainClass: String): Int {
            var mc = mainClass
            mc = mc.trim { it <= ' ' }.replace('.', '/') + ".class"
            val mainClassFile = jarFile.getEntry(mc)
            if (mainClassFile == null) return -1

            val bytesWeNeed = ByteArray(8)
            jarFile.getInputStream(mainClassFile).use { classStream ->
                val readCount = classStream.read(bytesWeNeed)
                if (readCount < bytesWeNeed.size) return -1
            }
            val byteBuffer = ByteBuffer.wrap(bytesWeNeed)
            if (byteBuffer.getInt() != -0x35014542) return -1
            val minorVersion = byteBuffer.short
            val majorVersion = byteBuffer.short
            Log.i("JavaGUILauncher", majorVersion.toString() + "," + minorVersion)
            return classVersionToJavaVersion(majorVersion.toInt())
        }

        fun classVersionToJavaVersion(majorVersion: Int): Int {
            if (majorVersion < 46) return 2 // there isn't even an arm64 port of jre 1.1 (or anything before 1.8 in fact)

            return majorVersion - 44
        }
    }
}
