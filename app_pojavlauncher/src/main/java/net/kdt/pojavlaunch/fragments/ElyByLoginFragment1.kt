package net.kdt.pojavlaunch.fragments

import net.kdt.pojavlaunch.extra.ExtraConstants

class ElyByLoginFragment : OAuthFragment(
    mTrackedUrl = "internalredirect://complete",
    mAuthUrl = "https://account.ely.by/oauth2/v1/authorize?client_id=mojolauncher2&response_type=code&redirect_uri=internalredirect%3A%2F%2Fcomplete&scope=account_info%20offline_access",
    mExtraCoreConstant = ExtraConstants.ELYBY_LOGIN_TODO
) {
    companion object {
        const val TAG: String = "ELYBY_LOGIN_FRAGMENT"
    }
}
