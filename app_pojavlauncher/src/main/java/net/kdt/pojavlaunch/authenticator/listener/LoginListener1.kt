package net.kdt.pojavlaunch.authenticator.listener

import net.kdt.pojavlaunch.authenticator.accounts.MinecraftAccount

interface LoginListener {
    fun onLoginDone(account: MinecraftAccount?)
    fun onLoginError(errorMessage: Throwable?)
    fun onLoginProgress(step: Int)
    fun setMaxLoginProgress(max: Int)
}
