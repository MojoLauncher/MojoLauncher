package com.xl.launcher.xy.mod

import android.util.Log
import java.io.File

object ModDoctorManager {
    fun scanDirectory(dir: File): List<String> {
        Log.i("ModDoctorManager", "Scanning: ${'$'}{dir.absolutePath}")
        // returns list of fake findings
        return listOf("Missing dependency: lib-example", "Overlapping resources: assets/textures")
    }
}

object DependencyScanner {
    fun scan(file: File): List<String> = listOf("lib-a", "lib-b")
}

object ConflictScanner {
    fun findConflicts(files: List<File>): List<String> = listOf()
}

object RepairPlanner {
    fun planFixes(failures: List<String>): List<String> = failures.map { "Fix: ${'$'}it" }
}

object ModSecurityScanner {
    fun quickCheck(file: File): Boolean {
        // naive: return true if file size < 100MB
        return file.length() < 100L * 1024L * 1024L
    }
}

object CompatibilityEngine {
    fun isCompatible(modVersion: String, profileVersion: String): Boolean = true
}

object DependencyResolver {
    fun resolve(names: List<String>): Map<String, Boolean> = names.associateWith { true }
}
