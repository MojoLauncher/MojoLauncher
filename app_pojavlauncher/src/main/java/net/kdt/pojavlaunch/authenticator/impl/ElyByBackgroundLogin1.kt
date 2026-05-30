package net.kdt.pojavlaunch.authenticator.impl

import android.util.Log
import com.kdt.mcgui.ProgressLayout
import net.kdt.pojavlaunch.PojavApplication
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.Tools.runOnUiThread
import net.kdt.pojavlaunch.authenticator.AuthType
import net.kdt.pojavlaunch.authenticator.BackgroundLogin
import net.kdt.pojavlaunch.authenticator.accounts.Accounts
import net.kdt.pojavlaunch.authenticator.accounts.MinecraftAccount
import net.kdt.pojavlaunch.authenticator.listener.LoginListener
import net.kdt.pojavlaunch.authenticator.model.OAuthTokenResponse
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Callable

class ElyByBackgroundLogin private constructor() : BackgroundLogin {
    private var mOAuthData: OAuthTokenResponse? = null
    private var mAccountInfo: ElyAccountInfo? = null
    private var mExpiresAt: Long = 0

    private fun acquireAccountDetails(
        loginListener: LoginListener, continuation: Callable<Void?>,
        code: String?, isRefresh: Boolean
    ) {
        ProgressLayout.setProgress(ProgressLayout.AUTHENTICATE, 0)
        PojavApplication.sExecutorService.execute(Runnable {
            loginListener.setMaxLoginProgress(2)
            try {
                notifyProgress(loginListener, 1)
                acquireTokens(isRefresh, code)
                notifyProgress(loginListener, 2)
                mAccountInfo = acquireAccountData(mOAuthData!!.accessToken)
                continuation.call()
            } catch (e: Exception) {
                Log.e("ElyByAuth", "Exception thrown during authentication", e)
                runOnUiThread(Runnable { loginListener.onLoginError(e) })
            }
            ProgressLayout.clearProgress(ProgressLayout.AUTHENTICATE)
        })
    }

    private fun fillAccount(acc: MinecraftAccount) {
        acc.expiresAt = mExpiresAt
        acc.authType = AuthType.ELY_BY
        acc.accessToken = mOAuthData!!.accessToken!!
        acc.refreshToken = mOAuthData!!.refreshToken!!
        acc.username = mAccountInfo!!.username!!
        acc.profileId = mAccountInfo!!.uuid!!
        acc.xuid = null
        acc.updateSkinFace()
    }

    override fun createAccount(loginListener: LoginListener, code: String?) {
        acquireAccountDetails(loginListener, Callable {
            val account: MinecraftAccount =
                Accounts.upsertByProfileId(Accounts.Setter { acc: MinecraftAccount? ->
                    this.fillAccount(acc!!)
                })
            runOnUiThread(Runnable { loginListener.onLoginDone(account) })
            null
        }, code, false)
    }

    override fun refreshAccount(loginListener: LoginListener, account: MinecraftAccount?) {
        acquireAccountDetails(loginListener, Callable {
            fillAccount(account!!)
            account.save()
            runOnUiThread(Runnable { loginListener.onLoginDone(account) })
            null
        }, account?.refreshToken, true)
    }

    @Throws(IOException::class)
    private fun acquireTokens(isRefresh: Boolean, code: String?) {
        val url = URL(authTokenUrl)
        Log.i("ElyByLogin", "isRefresh=" + isRefresh + ", authCode= " + code)

        val formData = CommonLoginUtils.convertToFormData(
            "client_id", "mojolauncher2",
            "client_secret", "o14Zb2Zzj0_k6o4kN0t1mIEhoQxeayn8hYi5VSX2q3NXrdQm5T2Q6wqsCfpv1vhu",
            "redirect_uri", "internalredirect://complete",
            if (isRefresh) "refresh_token" else "code", code,
            "grant_type", if (isRefresh) "refresh_token" else "authorization_code"
        )
        mOAuthData = CommonLoginUtils.exchangeAuthCode(url, formData)
        mExpiresAt = mOAuthData!!.expiresIn * 1000 + System.currentTimeMillis()
    }

    @Throws(IOException::class)
    private fun acquireAccountData(accessToken: String?): ElyAccountInfo {
        val url = URL(accountInfoUrl)
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer " + accessToken)
        conn.setUseCaches(false)
        conn.connect()
        if (conn.getResponseCode() in 200..299) {
            try {
                InputStreamReader(conn.getInputStream()).use { reader ->
                    return Tools.GLOBAL_GSON.fromJson<ElyAccountInfo>(
                        reader,
                        ElyAccountInfo::class.java
                    )
                }
            } finally {
                conn.disconnect()
            }
        } else {
            throw CommonLoginUtils.getResponseThrowable(conn)
        }
    }

    private fun notifyProgress(listener: LoginListener, step: Int) {
        runOnUiThread(Runnable { listener.onLoginProgress(step) })
        ProgressLayout.setProgress(ProgressLayout.AUTHENTICATE, step * 50)
    }

    private class ElyAccountInfo {
        var uuid: String? = null
        var username: String? = null
    }

    companion object {
        val CREATOR: BackgroundLogin.Creator = object : BackgroundLogin.Creator {
            override fun create(): BackgroundLogin = ElyByBackgroundLogin()
        }

        private const val authTokenUrl = "https://account.ely.by/api/oauth2/v1/token"
        private const val accountInfoUrl = "https://account.ely.by/api/account/v1/info"
    }
}
