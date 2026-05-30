package net.kdt.pojavlaunch.modloaders

class ComparableVersionString : Comparable<ComparableVersionString> {
    private var major = 0
    private var minor = 0
    private var patch = 0
    val original: String
    val isValid: Boolean

    private constructor(str: String) {
        this.original = str
        this.isValid = false
    }

    constructor(original: String, major: Int, minor: Int, patch: Int) {
        this.major = major
        this.minor = minor
        this.patch = patch
        this.original = original
        this.isValid = true
    }

    override fun compareTo(other: ComparableVersionString): Int {
        if (!this.isValid) return other.proper.compareTo(this.original)

        if (this.major != other.major) return this.major.compareTo(other.major)
        if (this.minor != other.minor) return this.minor.compareTo(other.minor)
        if (this.patch != other.patch) return this.patch.compareTo(other.patch)
        return 0
    }

    val proper: String
        /**
         * @return the original but if the patch was .0 it will not include it, e.g.
         * "1.20.0" -> "1.20"
         */
        get() {
            if (!this.isValid) return original

            val sb = StringBuilder()
            sb.append(major)
            sb.append('.')
            sb.append(minor)
            if (patch != 0) {
                sb.append('.')
                sb.append(patch)
            }
            return sb.toString()
        }

    companion object {
        fun parse(str: String): ComparableVersionString {
            val split: Array<String?> =
                str.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (split.size < 2) return ComparableVersionString(str)

            try {
                val major = split[0]!!.toInt()
                val minor = split[1]!!.toInt()
                val patch = if (split.size >= 3) split[2]!!.toInt() else 0
                return ComparableVersionString(str, major, minor, patch)
            } catch (_: NumberFormatException) {
                return ComparableVersionString(str)
            }
        }
    }
}
