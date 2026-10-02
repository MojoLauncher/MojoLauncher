package net.kdt.pojavlaunch.fragments;

import net.kdt.pojavlaunch.extra.ExtraConstants;

public class MicrosoftLoginFragment extends OAuthFragment {
    public static final String TAG = "MICROSOFT_LOGIN_FRAGMENT";
    public MicrosoftLoginFragment() {
        super("ms-xal-00000000402b5328",
                "https://login.live.com/oauth20_authorize.srf" +
                        "?client_id=00000000402b5328" +
                        "&response_type=code" +
                        "&scope=service%3A%3Auser.auth.xboxlive.com%3A%3AMBI_SSL" +
                        "&redirect_url=https%3A%2F%2Flogin.live.com%2Foauth20_desktop.srf",
                ExtraConstants.MICROSOFT_LOGIN_TODO);
    }
}
