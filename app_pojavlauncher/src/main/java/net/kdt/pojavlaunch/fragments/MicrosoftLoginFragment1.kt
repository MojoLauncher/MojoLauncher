package net.kdt.pojavlaunch.fragments

import net.kdt.pojavlaunch.extra.ExtraConstants

class MicrosoftLoginFragment : OAuthFragment(
    mTrackedUrl = "https://login.live.com/oauth20_desktop.srf",
    mAuthUrl = "https://login.live.com/oauth20_authorize.srf?client_id=00000000402b5328&response_type=code&scope=service%3A%3Auser.auth.xboxlive.com%3A%3AMBI_SSL&redirect_uri=https%3A%2F%2Flogin.live.com%2Foauth20_desktop.srf",
    mExtraCoreConstant = ExtraConstants.MICROSOFT_LOGIN_TODO
) {
    companion object {
        const val TAG: String = "MICROSOFT_LOGIN_FRAGMENT"
    }
}
