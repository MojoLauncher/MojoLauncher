package net.kdt.pojavlaunch.customcontrols

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.apache.commons.io.IOUtils
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.Random
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class LayoutBitmaps private constructor() {
    private val mBitmaps: MutableMap<String?, Bitmap?>

    init {
        mBitmaps = HashMap<String?, Bitmap?>()
    }

    private fun pickKey(): String {
        var key: String
        do {
            key = mKeyPicker.nextInt().toString()
        } while (mBitmaps.containsKey(key))
        return key
    }

    fun getBitmap(key: String?): Bitmap? {
        return mBitmaps.get(key)
    }

    fun putBitmap(bitmap: Bitmap?, oldKey: String?): String {
        val newKey = pickKey()
        mBitmaps.remove(oldKey)
        if (bitmap != null) mBitmaps.put(newKey, bitmap)
        return newKey
    }

    class ControlsContainer(val mControlsJson: String?, val mLayoutZip: LayoutBitmaps)
    companion object {
        private val mKeyPicker = Random(System.nanoTime())
        fun createEmpty(): LayoutBitmaps {
            return LayoutBitmaps()
        }

        private fun createEmpty(controlsJson: String?): ControlsContainer {
            return ControlsContainer(controlsJson, LayoutBitmaps())
        }

        @Throws(IOException::class)
        private fun loadFromZip(zipIn: ZipInputStream): ControlsContainer {
            val layoutBitmaps = LayoutBitmaps()
            var layoutContent: String? = null
            var entry = zipIn.getNextEntry()
            while (entry != null) {
                if (entry.isDirectory()) {
                    entry = zipIn.getNextEntry()
                    continue
                }
                val entryName = entry.getName()
                if (entryName == "layout.json") {
                    layoutContent = IOUtils.toString(zipIn, StandardCharsets.UTF_8)
                    entry = zipIn.getNextEntry()
                    continue
                }
                layoutBitmaps.mBitmaps.put(entryName, BitmapFactory.decodeStream(zipIn))
                zipIn.closeEntry()
                entry = zipIn.getNextEntry()
            }
            if (layoutContent == null) throw ZipException("Incorrect ZIP file structure")
            return ControlsContainer(layoutContent, layoutBitmaps)
        }

        @Throws(IOException::class)
        private fun load(fileInputStream: FileInputStream?, fileSize: Long): ControlsContainer {
            BufferedInputStream(fileInputStream).use { bufferedIn ->
                var isZip: Boolean
                bufferedIn.mark(4096)
                try {
                    val zipIn = ZipInputStream(bufferedIn)
                    isZip = zipIn.getNextEntry() != null
                } catch (e: ZipException) {
                    isZip = false
                } catch (e: IOException) {
                    throw e
                } catch (e: Exception) {
                    isZip = false
                }
                bufferedIn.reset()
                if (isZip) {
                    ZipInputStream(bufferedIn).use { zipIn ->
                        return loadFromZip(zipIn)
                    }
                } else {
                    val meg = 1024L * 1024L
                    if (fileSize > (25L * meg)) throw IOException("Raw JSON control data size too large")
                    return createEmpty(IOUtils.toString(bufferedIn, StandardCharsets.UTF_8))
                }
            }
        }

        @Throws(IOException::class)
        private fun storeZip(
            fileOutputStream: FileOutputStream?,
            controlsContainer: ControlsContainer
        ) {
            val bitmaps = controlsContainer.mLayoutZip
            ZipOutputStream(fileOutputStream).use { zipOutputStream ->
                zipOutputStream.putNextEntry(ZipEntry("layout.json"))
                IOUtils.write(
                    controlsContainer.mControlsJson,
                    zipOutputStream,
                    StandardCharsets.UTF_8
                )
                zipOutputStream.closeEntry()
                for (bitmapEntry in bitmaps.mBitmaps.entries) {
                    val outBitmap = bitmapEntry.value
                    if (outBitmap == null) continue
                    zipOutputStream.putNextEntry(ZipEntry(bitmapEntry.key))
                    outBitmap.compress(Bitmap.CompressFormat.WEBP, 100, zipOutputStream)
                    zipOutputStream.closeEntry()
                }
            }
        }

        @Throws(IOException::class)
        fun store(fileOutputStream: FileOutputStream?, controlsContainer: ControlsContainer) {
            val bitmaps = controlsContainer.mLayoutZip
            val controlsContent = controlsContainer.mControlsJson
            if (bitmaps.mBitmaps.isEmpty()) {
                IOUtils.write(controlsContent, fileOutputStream, StandardCharsets.UTF_8)
                return
            }
            storeZip(fileOutputStream, controlsContainer)
        }

        @Throws(IOException::class)
        fun load(jsonLocation: File): ControlsContainer {
            FileInputStream(jsonLocation).use { fileInputStream ->
                return load(fileInputStream, jsonLocation.length())
            }
        }
    }
}
