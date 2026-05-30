package net.kdt.pojavlaunch.value

import androidx.annotation.Keep

@Keep
class MinecraftLibraryArtifact : MinecraftClientInfo() {
    @JvmField
    var path: String? = null
}
