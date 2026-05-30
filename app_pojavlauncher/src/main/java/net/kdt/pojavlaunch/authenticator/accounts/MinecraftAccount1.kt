package net.kdt.pojavlaunch.authenticator.accounts

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.annotation.Keep
import com.google.gson.JsonParseException
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.authenticator.AuthType
import net.kdt.pojavlaunch.utils.FileUtils
import net.kdt.pojavlaunch.utils.JSONUtils
import net.kdt.pojavlaunch.utils.JSONUtils.writeToFile
import net.kdt.pojavlaunch.utils.SkinUtils.isSlimModel
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL


@Keep
class MinecraftAccount {
    @Transient
    var mSaveLocation: File? = null
    var accessToken: String = "0" // access token
    var profileId: String =
        "00000000-0000-0000-0000-000000000000" // profile UUID, for obtaining skin
    var username: String = "Steve"
    var authType: AuthType = AuthType.LOCAL
    
    @get:JvmName("isMicrosoftProperty")
    var isMicrosoft: Boolean = false
    var refreshToken: String = "0"
    var xuid: String? = null
    var expiresAt: Long = 0
    var localSkinPath: String? = null // Path to a local skin file
    var skinModel: String = "classic" // "classic" or "slim"

    @Transient
    private var mFaceCache: Bitmap? = null

    @Transient
    private var mFacePlainCache: Bitmap? = null

    fun updateSkinFace() {
        if (localSkinPath != null) {
            updateSkinFaceFromLocal()
            return
        }
        updateSkinFace(false)
        updateSkinFace(true)
    }

    private fun updateSkinFaceFromLocal() {
        try {
            Log.i("SkinLoader", "Updating skin face from local path: " + localSkinPath)
            val localFile = File(localSkinPath)
            if (!localFile.exists()) return

            // Detect skin model
            if (isSlimModel(localFile)) {
                skinModel = "slim"
            } else {
                skinModel = "classic"
            }

            val skinBitmap = BitmapFactory.decodeFile(localSkinPath)
            if (skinBitmap == null) return


            // Render and save both 3D and Plain versions
            renderAndSave(skinBitmap, false)
            renderAndSave(skinBitmap, true)

            skinBitmap.recycle()
            Log.i("SkinLoader", "Update local skin face success. Model: " + skinModel)
        } catch (e: IOException) {
            Log.w("SkinLoader", "Could not update local skin face", e)
        }
    }

    @Throws(IOException::class)
    private fun renderAndSave(skinBitmap: Bitmap, plain: Boolean) {
        val skinFile = getSkinFaceFile(plain)
        val skinFace: Bitmap?
        if (plain) {
            skinFace = SkinHeadRenderer().renderPlain(512, skinBitmap)
        } else {
            skinFace = SkinHeadRenderer().render(512, skinBitmap)
        }
        if (skinFace == null) return
        FileOutputStream(skinFile).use { fileOutputStream ->
            skinFace.compress(Bitmap.CompressFormat.WEBP, 90, fileOutputStream)
        }
        if (plain) mFacePlainCache = skinFace
        else mFaceCache = skinFace
    }

    private fun updateSkinFace(plain: Boolean) {
        val skinFaceUrlTemplate = authType.skinUrl
        if (skinFaceUrlTemplate == null) return
        val skinFaceUrl = String.format(skinFaceUrlTemplate, username)
        try {
            Log.i("SkinLoader", "Updating skin face (plain=" + plain + ")...")
            val skinFile = getSkinFaceFile(plain)
            // Streaming it directly breaks on some devices
            val skinBytes = IOUtils.toByteArray(URL(skinFaceUrl))
            val skinBitmap = BitmapFactory.decodeByteArray(skinBytes, 0, skinBytes.size)
            if (skinBitmap == null) return

            val skinFace: Bitmap?
            if (plain) {
                skinFace = SkinHeadRenderer().renderPlain(512, skinBitmap)
            } else {
                skinFace = SkinHeadRenderer().render(512, skinBitmap)
            }

            skinBitmap.recycle()
            if (skinFace == null) return
            FileOutputStream(skinFile).use { fileOutputStream ->
                skinFace.compress(Bitmap.CompressFormat.WEBP, 90, fileOutputStream)
            }
            Log.i("SkinLoader", "Update skin face success (plain=" + plain + ")")
        } catch (e: IOException) {
            // Skin refresh limit, no internet connection, etc...
            // Simply ignore updating skin face
            Log.w("SkinLoader", "Could not update skin face", e)
        }
    }

    val isLocal: Boolean
        get() = authType == AuthType.LOCAL || accessToken == "0"

    @Throws(IOException::class)
    fun save() {
        FileUtils.ensureParentDirectory(mSaveLocation!!)
        writeToFile(mSaveLocation, this)
    }

    fun reload(): MinecraftAccount? {
        try {
            val minecraftAccount = JSONUtils.readFromFile(
                mSaveLocation,
                MinecraftAccount::class.java
            )
            if (minecraftAccount == null) return null
            minecraftAccount.mSaveLocation = mSaveLocation
            return minecraftAccount
        } catch (e: IOException) {
            return null
        } catch (e: JsonParseException) {
            return null
        }
    }

    val skinFace: Bitmap?
        get() = getSkinFace(false)

    val skinFacePlain: Bitmap?
        get() = getSkinFace(true)

    private fun getSkinFace(plain: Boolean): Bitmap? {
        if (this.isLocal && localSkinPath == null) return null
        val skinFaceFile = getSkinFaceFile(plain)
        if (!skinFaceFile.exists()) {
            // Try to download/render it if it doesn't exist
            Thread(Runnable { updateSkinFace() }).start()
            return null
        }
        if (plain) {
            if (mFacePlainCache == null) {
                mFacePlainCache = BitmapFactory.decodeFile(skinFaceFile.getAbsolutePath())
            }
            return mFacePlainCache
        } else {
            if (mFaceCache == null) {
                mFaceCache = BitmapFactory.decodeFile(skinFaceFile.getAbsolutePath())
            }
            return mFaceCache
        }
    }

    private fun getSkinFaceFile(plain: Boolean): File {
        val suffix = if (plain) "-plain" else "-3d"
        return File(
            Tools.DIR_CACHE,
            "skin-face" + suffix + "-" + profileId + "-" + authType.name + ".webp"
        )
    }
}
