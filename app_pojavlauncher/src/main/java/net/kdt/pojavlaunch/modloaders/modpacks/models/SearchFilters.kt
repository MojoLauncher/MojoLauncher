package net.kdt.pojavlaunch.modloaders.modpacks.models

/**
 * Search filters, passed to APIs
 */
class SearchFilters {
    @JvmField
    var isModpack: Boolean = false
    @JvmField
    var name: String? = null
    @JvmField
    var mcVersion: String? = null
}
