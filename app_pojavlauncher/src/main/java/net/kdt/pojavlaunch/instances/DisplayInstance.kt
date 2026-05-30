package net.kdt.pojavlaunch.instances

import java.io.File

open class DisplayInstance protected constructor() {
    @JvmField
    @Transient
    var mInstanceRoot: File? = null
    @JvmField
    var name: String? = null
    @JvmField
    var versionId: String? = null
    @JvmField
    var icon: String? = null

    open fun sanitize() {
        sanitizeIcon()
    }

    val instanceIconLocation: File
        get() = File(mInstanceRoot, "icon.webp")

    private fun sanitizeIcon() {
        if (!InstanceIconProvider.hasStaticIcon(icon)) {
            icon = InstanceIconProvider.FALLBACK_ICON_NAME
        }
    }
}
