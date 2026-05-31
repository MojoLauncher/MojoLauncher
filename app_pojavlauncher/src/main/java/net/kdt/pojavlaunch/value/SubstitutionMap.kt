package net.kdt.pojavlaunch.value

import com.google.gson.annotations.SerializedName

class SubstitutionMap {
    @SerializedName("substitutions")
    var substitutions: Map<String, LibrarySubstitution>? = null

    fun findSubstitution(name: String): LibrarySubstitution? {
        val subs = substitutions ?: return null
        // Try exact match first
        subs[name]?.let { return it }
        // Try base name match (group:artifact)
        val baseName = name.substringBeforeLast(':')
        return subs[baseName]
    }
}

class LibrarySubstitution : DependentLibrary() {
    @SerializedName("skip")
    var skip: Boolean = false
}
