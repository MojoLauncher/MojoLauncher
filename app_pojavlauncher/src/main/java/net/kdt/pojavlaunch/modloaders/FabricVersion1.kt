package net.kdt.pojavlaunch.modloaders

open class FabricVersion {
    @JvmField
    var version: String? = null
    @JvmField
    var stable: Boolean = false

    override fun toString(): String {
        return version!!
    }
}
