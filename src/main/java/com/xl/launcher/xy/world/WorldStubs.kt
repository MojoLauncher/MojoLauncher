package com.xl.launcher.xy.world

import android.util.Log
import java.io.File

object WorldManager {
    fun listWorlds(root: File): List<File> = root.listFiles()?.filter { it.isDirectory } ?: emptyList()
}

object WorldBackupCenter {
    fun backupWorld(world: File, dest: File): Boolean { Log.i("WorldBackupCenter","Backing ${'$'}{world.name}"); return true }
}

object WorldRepairCenter {
    fun repair(world: File): Boolean { Log.i("WorldRepairCenter","Repairing ${'$'}{world.name}"); return true }
}

object WorldAnalytics {
    fun analyze(world: File): Map<String, Any> = mapOf("sizeMb" to (world.totalSpace / 1024 / 1024))
}

object WorldOptimizer {
    fun trimCaches(world: File) { Log.i("WorldOptimizer","Trimming caches for ${'$'}{world.name}") }
}
