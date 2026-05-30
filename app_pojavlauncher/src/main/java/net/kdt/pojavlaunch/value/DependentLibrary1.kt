package net.kdt.pojavlaunch.value

import androidx.annotation.Keep
import net.kdt.pojavlaunch.JMinecraftVersionList.Arguments.ArgValue.ArgRules

@Keep
class DependentLibrary {
    @JvmField
    var rules: Array<ArgRules?>? = null
    @JvmField
    var name: String? = null
    @JvmField
    var downloads: LibraryDownloads? = null
    @JvmField
    var url: String? = null

    @JvmField
    @Transient
    var replaced: Boolean = false

    @Keep
    class LibraryDownloads(@JvmField val artifact: MinecraftLibraryArtifact?)
}
