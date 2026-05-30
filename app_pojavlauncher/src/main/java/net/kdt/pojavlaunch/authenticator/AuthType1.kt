package net.kdt.pojavlaunch.authenticator

import com.google.gson.annotations.SerializedName
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.authenticator.impl.ElyByBackgroundLogin
import net.kdt.pojavlaunch.authenticator.impl.MicrosoftBackgroundLogin

enum class AuthType(
    val creator: BackgroundLogin.Creator?,
    val iconResource: Int,
    val injectorUrl: String?,
    val skinUrl: String?
) {
    @SerializedName("microsoft")
    MICROSOFT(
        MicrosoftBackgroundLogin.CREATOR,
        R.drawable.ic_auth_ms,
        null,
        "https://mineskin.eu/skin/%s" // Switched from mc-heads.net cause blocked in Russia
    ),

    @SerializedName("elyby")
    ELY_BY(
        ElyByBackgroundLogin.CREATOR,
        R.drawable.ic_auth_elyby,
        "ely.by",
        "http://skinsystem.ely.by/skins/%s.png"
    ),

    @SerializedName("local")
    LOCAL(null, 0, null, null);

    fun requiresLogin(): Boolean {
        return creator != null
    }

    fun createAuth(): BackgroundLogin? {
        if (creator == null) throw RuntimeException("This account does not support login")
        return creator.create()
    }
}
