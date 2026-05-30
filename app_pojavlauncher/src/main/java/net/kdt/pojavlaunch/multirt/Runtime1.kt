package net.kdt.pojavlaunch.multirt

import java.util.Objects

class Runtime {
    @JvmField
    val name: String
    @JvmField
    val versionString: String?
    @JvmField
    val arch: String?
    @JvmField
    val javaVersion: Int

    constructor(name: String) {
        this.name = name
        this.versionString = null
        this.arch = null
        this.javaVersion = 0
    }

    internal constructor(name: String, versionString: String?, arch: String?, javaVersion: Int) {
        this.name = name
        this.versionString = versionString
        this.arch = arch
        this.javaVersion = javaVersion
    }


    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val runtime = o as Runtime
        return name == runtime.name
    }

    override fun hashCode(): Int {
        return Objects.hash(name)
    }
}