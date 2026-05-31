package net.kdt.pojavlaunch.utils

import java.io.File

object MavenNameUtils {
    @JvmStatic
    fun mavenNameToPath(name: String): String {
        val parts = name.split(":")
        if (parts.size < 3) return name
        val group = parts[0].replace('.', '/')
        val artifact = parts[1]
        val version = parts[2]
        return "$group/$artifact/$version/$artifact-$version.jar"
    }

    @JvmStatic
    fun mavenNameToAarPath(name: String): String {
        return mavenNameToPath(name).replace(".jar", ".aar")
    }

    @JvmStatic
    fun mavenBaseName(name: String): String {
        val parts = name.split(":")
        if (parts.size < 2) return name
        return "${parts[0]}:${parts[1]}"
    }
}
