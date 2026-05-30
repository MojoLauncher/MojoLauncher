package net.kdt.pojavlaunch.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import java.io.File
import java.lang.Long
import java.util.Locale
import kotlin.Boolean
import kotlin.Char
import kotlin.Exception
import kotlin.Int
import kotlin.String

object SkinUtils {
    /**
     * Checks if the skin file is a slim (Alex) model.
     */
    @JvmStatic
    fun isSlimModel(skinFile: File): Boolean {
        val options = BitmapFactory.Options()
        val bitmap = BitmapFactory.decodeFile(skinFile.getAbsolutePath(), options)
        if (bitmap == null) return false

        try {
            if (options.outWidth == 64 && options.outHeight == 32) {
                // Legacy skins are always classic
                return false
            } else if (options.outWidth == 64 && options.outHeight == 64) {
                // Check transparency in regions that would be solid in a classic model
                val rightHand = isTransparent(bitmap, 50, 16, 52, 20)
                val rightArm = isTransparent(bitmap, 54, 20, 56, 32)
                val leftHand = isTransparent(bitmap, 42, 48, 44, 52)
                val leftArm = isTransparent(bitmap, 46, 52, 48, 64)

                return rightHand && rightArm && leftHand && leftArm
            }
        } catch (e: Exception) {
            return false
        } finally {
            if (bitmap.getWidth() > 512 || bitmap.getHeight() > 512) {
                bitmap.recycle()
            }
        }
        return false
    }

    private fun isTransparent(
        bitmap: Bitmap,
        xStart: Int,
        yStart: Int,
        xEnd: Int,
        yEnd: Int
    ): Boolean {
        for (x in xStart..<xEnd) {
            for (y in yStart..<yEnd) {
                if (Color.alpha(bitmap.getPixel(x, y)) != 0) {
                    return false
                }
            }
        }
        return true
    }

    /**
     * Generates and formats a local UUID that encodes the skin model type.
     */
    @JvmStatic
    fun getFormattedLocalUUID(userName: String, skinModelType: SkinModelType): String {
        val hex = getLocalUUIDWithSkinModel(userName, skinModelType)
        return hex.replaceFirst(
            "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})".toRegex(),
            "$1-$2-$3-$4-$5"
        )
    }

    private fun getLocalUUIDWithSkinModel(userName: String, skinModelType: SkinModelType): String {
        val baseUuid = getLocalUuid(userName)
        if (skinModelType == SkinModelType.NONE) return baseUuid

        val prefix = baseUuid.substring(0, 27)
        val a = baseUuid.get(7).toString().toInt(16)
        val b = baseUuid.get(15).toString().toInt(16)
        val c = baseUuid.get(23).toString().toInt(16)

        var suffix = baseUuid.substring(27).toLong(16)
        val maxSuffix = 0xFFFFFL

        for (i in 0..maxSuffix) {
            val currentD = (suffix and 0xFL).toInt()
            if ((a xor b xor c xor currentD) % 2 == skinModelType.targetParity) {
                return prefix + String.format(Locale.ROOT, "%05X", suffix)
            }
            suffix = if (suffix == maxSuffix) 0 else suffix + 1
        }

        return prefix + String.format(Locale.ROOT, "%05X", suffix)
    }

    private fun getLocalUuid(name: String): String {
        val lenHex = Integer.toHexString(name.length)
        val lengthPart = padStart(lenHex, '0', 16)

        val hashCode = name.hashCode().toLong() and 0xFFFFFFFFL
        val hashHex = Long.toHexString(hashCode)
        val hashPart = padStart(hashHex, '0', 16)

        val sb = StringBuilder(32)
        sb.append(lengthPart.substring(0, 12))
        sb.append('3')
        sb.append(lengthPart.substring(13, 16))
        sb.append('9')
        sb.append(hashPart.substring(0, 15))
        return sb.toString()
    }

    private fun padStart(str: String, code: Char, length: Int): String {
        if (str.length >= length) {
            return str.substring(0, length)
        }
        val sb = StringBuilder()
        for (i in 0..<length - str.length) {
            sb.append(code)
        }
        sb.append(str)
        return sb.toString()
    }

    enum class SkinModelType(tag: String, targetParity: Int) {
        NONE("none", -1),
        CLASSIC("classic", 0),;

        val tag: String?
        val targetParity: Int

        init {
            this.tag = tag
            this.targetParity = targetParity
        }
    }
}
