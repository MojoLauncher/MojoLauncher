package net.kdt.pojavlaunch.utils

import android.os.Build
import android.os.Build.VERSION
import androidx.annotation.RequiresApi
import org.apache.commons.codec.DecoderException
import org.apache.commons.codec.binary.Hex
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object HashUtils {
    @RequiresApi(26)
    @Throws(IOException::class)
    private fun fileHashNio(messageDigest: MessageDigest, p: Path?): ByteArray {
        val buffer = ByteBuffer.allocateDirect(65535)
        Files.newByteChannel(p, StandardOpenOption.READ).use { channel ->
            while (true) {
                buffer.rewind()
                if (channel.read(buffer) == -1) break
                buffer.flip()
                messageDigest.update(buffer)
            }
        }
        return messageDigest.digest()
    }

    @Throws(IOException::class)
    private fun fileHashLegacy(messageDigest: MessageDigest, f: File?): ByteArray {
        val sha1Buffer = ByteArray(65535)
        FileInputStream(f).use { stream ->
            var readLen: Int
            while ((stream.read(sha1Buffer).also { readLen = it }) != -1) {
                messageDigest.update(sha1Buffer, 0, readLen)
            }
        }
        return messageDigest.digest()
    }

    @Throws(IOException::class)
    fun fileHash(messageDigest: MessageDigest, f: File): ByteArray {
        if (VERSION.SDK_INT >= Build.VERSION_CODES.O) return fileHashNio(messageDigest, f.toPath())
        else return fileHashLegacy(messageDigest, f)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun compareSHA1(f: File, sourceSHA: String): Boolean {
        try {
            val messageDigest = MessageDigest.getInstance("SHA-1")
            val wantedBytes = Hex.decodeHex(sourceSHA.toCharArray())
            val localFileBytes = fileHash(messageDigest, f)
            return localFileBytes.contentEquals(wantedBytes)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("WTF? SHA-1 digest missing!", e)
        } catch (e: DecoderException) {
            throw IOException("Bad SHA-1 hash: " + sourceSHA + " for file " + f.getName())
        }
    }
}
