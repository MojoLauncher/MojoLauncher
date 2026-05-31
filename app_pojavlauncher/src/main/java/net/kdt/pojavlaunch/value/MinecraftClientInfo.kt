package net.kdt.pojavlaunch.value

import androidx.annotation.Keep

@Keep
open class MinecraftClientInfo {
    @JvmField
    var sha1: String? = null
    @JvmField
    var size: Long = -1
    @JvmField
    var url: String? = null
}
