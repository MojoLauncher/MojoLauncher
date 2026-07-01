package com.xl.launcher.xy.auth

object MicrosoftAuthService {
    fun startLogin() = "mock_token"
}

object OfflineSessionManager {
    fun createOfflineProfile(name: String) = "offline-${'$'}{name.hashCode()}"
}

object TokenValidator {
    fun validate(token: String) = token.isNotBlank()
}
