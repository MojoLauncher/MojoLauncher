package xy.runtime

import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.File

/** JNI Bridge stub - declare native methods here and provide safe fallbacks. */
object JniBridge {
    init {
        // Attempt to load native library if available. Safe fallback otherwise.
        try {
            System.loadLibrary("xl_native")
            Log.i("JniBridge", "Native library loaded")
        } catch (e: Throwable) {
            Log.w("JniBridge", "Native library not available: ${'$'}e")
        }
    }

    // Example native method signature
    external fun hookMinecraftLoop(): Int

    fun hookSafe(): Int {
        // Fallback behavior for environments without native support
        Log.i("JniBridge", "hookSafe called (no-op fallback)")
        return -1
    }
}

/** Simple OpenGL/Vulkan conversion wrapper facade. Mock implementation. */
class LwjglWrapper {
    fun initialize(): Boolean {
        Log.i("LwjglWrapper", "initialize called (mock)")
        return true
    }

    fun shutdown() {
        Log.i("LwjglWrapper", "shutdown (mock)")
    }
}

/** Manages local JDK runtime choices (mock). */
class OpenJdkManager {
    private val installedRuntimes = mutableListOf("openjdk-17", "openjdk-8")

    fun listRuntimes(): List<String> = installedRuntimes.toList()

    fun selectRuntime(name: String): Boolean {
        Log.i("OpenJdkManager", "Selecting runtime: ${'$'}name")
        return installedRuntimes.contains(name)
    }
}

/** Virtual File System Tracker for Minecraft directories (mock). */
object VfsTracker {
    private val tracked = mutableListOf<File>()

    fun track(dir: File) {
        if (!tracked.contains(dir)) tracked.add(dir)
        Log.i("VfsTracker", "Tracking: ${'$'}{dir.absolutePath}")
    }

    fun listTracked(): List<String> = tracked.map { it.absolutePath }
}

/** LaunchEngine orchestrates launch arguments, environment, and classpath for the game (simulated). */
class LaunchEngine(private val jni: JniBridge? = null) {
    var isRunning = false

    fun simulateLaunch(onLog: (String) -> Unit = {}) {
        // Simulate realistic startup steps
        isRunning = true
        runBlocking {
            val steps = listOf(
                "Validating profile",
                "Preparing classpath",
                "Verifying assets",
                "Initializing rendering bridge",
                "Starting VM",
                "Handing off to game loop"
            )
            for (s in steps) {
                onLog(s)
                Log.i("LaunchEngine", s)
                delay(500)
            }
            onLog("Game started (simulation)")
            isRunning = false
        }
    }

    fun stop() {
        Log.i("LaunchEngine", "stop called (simulation)")
        isRunning = false
    }
}
