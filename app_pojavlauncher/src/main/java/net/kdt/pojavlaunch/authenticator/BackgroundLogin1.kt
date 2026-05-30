package net.kdt.pojavlaunch.authenticator

import net.kdt.pojavlaunch.authenticator.accounts.MinecraftAccount
import net.kdt.pojavlaunch.authenticator.listener.LoginListener

interface BackgroundLogin {
    fun createAccount(loginListener: LoginListener, code: String?)
    fun refreshAccount(loginListener: LoginListener, account: MinecraftAccount?)
    fun interface Creator {
        fun create(): BackgroundLogin?
    }
}
