package net.kdt.pojavlaunch.authenticator.model

import com.google.gson.annotations.SerializedName

class OAuthTokenResponse {
    @SerializedName("access_token")
    var accessToken: String? = null

    @SerializedName("refresh_token")
    var refreshToken: String? = null

    @SerializedName("expires_in")
    var expiresIn: Long = 0
}
