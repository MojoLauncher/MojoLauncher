package net.kdt.pojavlaunch.authenticator.model;

import com.google.gson.annotations.SerializedName;

public class OAuthTokenResponse {
    @SerializedName("access_token") public String accessToken;
    @SerializedName("refresh_token") public String refreshToken;
    @SerializedName("expires_in") public long expiresIn;
}
