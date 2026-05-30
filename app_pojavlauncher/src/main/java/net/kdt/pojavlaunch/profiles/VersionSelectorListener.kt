package net.kdt.pojavlaunch.profiles

interface VersionSelectorListener {
    fun onVersionSelected(versionId: String?, isSnapshot: Boolean)
}
