package com.xl.launcher.xy.ai

import android.util.Log

/** Parses crash logs and extracts stack traces. */
object CrashAnalyzer {
    fun analyzeLog(raw: String): List<String> {
        // Very simple parser: split by 'at ' lines
        val lines = raw.lines().filter { it.trim().startsWith("at ") }
        Log.i("CrashAnalyzer", "Found ${'$'}{lines.size} stack lines")
        return lines
    }
}

/** Categorizes crash reasons. */
object CrashClassifier {
    fun classify(lines: List<String>): String {
        if (lines.any { it.contains("OutOfMemoryError") }) return "OutOfMemory"
        if (lines.any { it.contains("NoSuchMethodError") }) return "ModConflict"
        if (lines.isEmpty()) return "Unknown"
        return "Other"
    }
}

/** Small knowledge base shipped in assets (mock). */
object CrashKnowledgeBase {
    val entries = mapOf(
        "OutOfMemory" to "Increase the heap size in settings or enable low-memory mode.",
        "ModConflict" to "A mod uses incompatible core classes; try disabling recently added mods.",
        "MissingAsset" to "Some assets are missing. Re-download or verify game files."
    )

    fun lookup(code: String): String? = entries[code]
}

/** Generates fix recommendations. */
object FixRecommendationEngine {
    fun recommend(classifier: String): List<String> = when (classifier) {
        "OutOfMemory" -> listOf("Increase heap to 2G", "Enable low-end optimizations")
        "ModConflict" -> listOf("Run ModDoctor", "Disable suspected mods")
        else -> listOf("Inspect logs", "Ask Assistant")
    }
}

/** Simple mod advisor mock. */
object ModAdvisor {
    fun recommendForPlaystyle(playstyle: String): List<String> = listOf("Optifine-lite", "PerformanceMod")
}

/** Modpack builder simulator (natural-language driven mock). */
object ModpackBuilder {
    fun buildFromDescription(desc: String): String {
        return "modpack-sim-${'$'}{desc.hashCode()}" // returns a fake modpack id
    }
}
