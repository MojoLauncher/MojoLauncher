package net.kdt.pojavlaunch

import android.os.Build
import java.util.Locale

/**
 * This class aims at providing a simple and easy way to deal with the device architecture.
 */
object Architecture {
    val UNSUPPORTED_ARCH: Int = -1
    const val ARCH_ARM64: Int = 0x1
    const val ARCH_ARM: Int = 0x2
    const val ARCH_X86: Int = 0x4
    const val ARCH_X86_64: Int = 0x8

    /* On both 32-bit ARM and x86, the top 1GB is reserved for kernel use. */
    const val ADDRESS_SPACE_LIMIT_32_BIT: Long = 0xbfffffffL

    /*
	 * Technically, this is supposed to be 48 bits on x86_64, but nobody's allocating
	 * 524288 terabytes of RAM on Pojav any time soon.
	 */
    const val ADDRESS_SPACE_LIMIT_64_BIT: Long = 0x7fffffffffL

    val addressSpaceLimit: Long
        /**
         * Get the highest byte accessible within the process's address space.
         * @return the highest byte accessible within the process's address space.
         */
        get() = if (is64BitsDevice()) ADDRESS_SPACE_LIMIT_64_BIT else ADDRESS_SPACE_LIMIT_32_BIT

    /**
     * Tell us if the device supports 64 bits architecture
     * @return If the device supports 64 bits architecture
     */
    fun is64BitsDevice(): Boolean {
        return Build.SUPPORTED_64_BIT_ABIS.size != 0
    }

    /**
     * Tell us if the device supports 32 bits architecture
     * Note, that a 64 bits device won't be reported as supporting 32 bits.
     * @return If the device supports 32 bits architecture
     */
    fun is32BitsDevice(): Boolean {
        return !is64BitsDevice()
    }

    val deviceArchitecture: Int
        /**
         * Tells the device supported architecture.
         * Since mips(/64) has been phased out long ago, is isn't checked here.
         * 
         * @return ARCH_ARM || ARCH_ARM64 || ARCH_X86 || ARCH_86_64
         */
        get() {
            if (isx86Device()) {
                return if (is64BitsDevice()) ARCH_X86_64 else ARCH_X86
            }
            return if (is64BitsDevice()) ARCH_ARM64 else ARCH_ARM
        }

    /**
     * Tell is the device is based on an x86 processor.
     * It doesn't tell if the device is 64 or 32 bits.
     * @return Whether or not the device is x86 based.
     */
    fun isx86Device(): Boolean {
        //We check the whole range of supported ABIs,
        //Since asus zenfones can place arm before their native instruction set.
        val ABI = if (is64BitsDevice()) Build.SUPPORTED_64_BIT_ABIS else Build.SUPPORTED_32_BIT_ABIS
        val comparedArch = if (is64BitsDevice()) ARCH_X86_64 else ARCH_X86
        for (str in ABI) {
            if (archAsInt(str) == comparedArch) return true
        }
        return false
    }


    /**
     * Convert an architecture from a String to an int.
     * @param arch The architecture as a String
     * @return The architecture as an int, can be UNSUPPORTED_ARCH if unknown.
     */
    fun archAsInt(arch: String): Int {
        var arch = arch
        arch = arch.lowercase(Locale.getDefault()).trim { it <= ' ' }.replace(" ", "")
        if (arch.contains("arm64") || arch == "aarch64") return ARCH_ARM64
        if (arch.contains("arm") || arch == "aarch32") return ARCH_ARM
        if (arch.contains("x86_64") || arch.contains("amd64")) return ARCH_X86_64
        if (arch.contains("x86") || (arch.startsWith("i") && arch.endsWith("86"))) return ARCH_X86
        //Shouldn't happen
        return UNSUPPORTED_ARCH
    }

    /**
     * Convert to a string an architecture.
     * @param arch The architecture as an int.
     * @return "arm64" || "arm" || "x86_64" || "x86" || "UNSUPPORTED_ARCH"
     */
    fun archAsString(arch: Int): String {
        if (arch == ARCH_ARM64) return "arm64"
        if (arch == ARCH_ARM) return "arm"
        if (arch == ARCH_X86_64) return "x86_64"
        if (arch == ARCH_X86) return "x86"
        return "UNSUPPORTED_ARCH"
    }
}
