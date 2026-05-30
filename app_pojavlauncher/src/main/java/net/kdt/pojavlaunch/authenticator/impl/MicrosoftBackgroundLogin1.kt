package net.kdt.pojavlaunch.authenticator.impl

import android.util.ArrayMap
import android.util.Log
import com.kdt.mcgui.ProgressLayout
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.PojavApplication
import net.kdt.pojavlaunch.Tools.read
import net.kdt.pojavlaunch.Tools.runOnUiThread
import net.kdt.pojavlaunch.authenticator.AuthType
import net.kdt.pojavlaunch.authenticator.BackgroundLogin
import net.kdt.pojavlaunch.authenticator.accounts.Accounts
import net.kdt.pojavlaunch.authenticator.accounts.MinecraftAccount
import net.kdt.pojavlaunch.authenticator.listener.LoginListener
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.ProtocolException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.Callable

/** Allow to perform a background login on a given account  */
class MicrosoftBackgroundLogin private constructor() : BackgroundLogin {
    /* Fields used to fill the account  */
    var msRefreshToken: String? = null
    var mcName: String? = null
    var mcToken: String? = null
    var mcUuid: String? = null
    var msXsts: String? = null
    var doesOwnGame: Boolean = false
    var expiresAt: Long = 0

    private fun acquireAccountDetails(
        loginListener: LoginListener, continuation: Callable<Void?>,
        code: String?, isRefresh: Boolean
    ) {
        ProgressLayout.setProgress(ProgressLayout.AUTHENTICATE, 0)
        PojavApplication.sExecutorService.execute(Runnable {
            loginListener.setMaxLoginProgress(5)
            try {
                notifyProgress(loginListener, 1)
                val accessToken = acquireAccessToken(isRefresh, code)
                notifyProgress(loginListener, 2)
                val xboxLiveToken = acquireXBLToken(accessToken)
                notifyProgress(loginListener, 3)
                val xsts = acquireXsts(xboxLiveToken)
                notifyProgress(loginListener, 4)
                val mcToken = acquireMinecraftToken(xsts[0], xsts[1])
                notifyProgress(loginListener, 5)
                fetchOwnedItems(mcToken)
                checkMcProfile(mcToken)
                msXsts = xsts[0]
                continuation.call()
            } catch (e: Exception) {
                Log.e("MicroAuth", "Exception thrown during authentication", e)
                runOnUiThread(Runnable { loginListener.onLoginError(e) })
            } finally {
                ProgressLayout.clearProgress(ProgressLayout.AUTHENTICATE)
            }
        })
    }

    private fun fillAccount(acc: MinecraftAccount) {
        acc.xuid = msXsts
        acc.accessToken = mcToken!!
        acc.username = mcName!!
        acc.profileId = mcUuid!!
        acc.authType = AuthType.MICROSOFT
        acc.refreshToken = msRefreshToken!!
        acc.expiresAt = expiresAt
        acc.updateSkinFace()
    }

    override fun createAccount(loginListener: LoginListener, code: String?) {
        acquireAccountDetails(loginListener, Callable {
            val account: MinecraftAccount =
                Accounts.upsertByProfileId(Accounts.Setter { acc: MinecraftAccount ->
                    this.fillAccount(acc)
                })
            runOnUiThread(Runnable { loginListener.onLoginDone(account) })
            null
        }, code, false)
    }

    override fun refreshAccount(loginListener: LoginListener, account: MinecraftAccount?) {
        acquireAccountDetails(loginListener, Callable {
            if (doesOwnGame) fillAccount(account!!)
            account?.save()
            runOnUiThread(Runnable { loginListener.onLoginDone(account) })
            null
        }, account?.refreshToken, true)
    }

    @Throws(IOException::class)
    private fun acquireAccessToken(isRefresh: Boolean, code: String?): String? {
        val url = URL(authTokenUrl)
        Log.i("MicrosoftLogin", "isRefresh=" + isRefresh + ", authCode= " + code)

        val formData = CommonLoginUtils.convertToFormData(
            "client_id", "00000000402b5328",
            if (isRefresh) "refresh_token" else "code", code,
            "grant_type", if (isRefresh) "refresh_token" else "authorization_code",
            "redirect_uri", "https://login.live.com/oauth20_desktop.srf",
            "scope", "service::user.auth.xboxlive.com::MBI_SSL"
        )

        val response = CommonLoginUtils.exchangeAuthCode(url, formData)
        msRefreshToken = response?.refreshToken
        return response?.accessToken
    }

    @Throws(IOException::class, JSONException::class)
    private fun acquireXBLToken(accessToken: String?): String {
        val url = URL(xblAuthUrl)

        val data = JSONObject()
        val properties = JSONObject()
        properties.put("AuthMethod", "RPS")
        properties.put("SiteName", "user.auth.xboxlive.com")
        properties.put("RpsTicket", accessToken)
        data.put("Properties", properties)
        data.put("RelyingParty", "http://auth.xboxlive.com")
        data.put("TokenType", "JWT")

        val req = data.toString()
        val conn = url.openConnection() as HttpURLConnection
        setCommonProperties(conn, req)
        conn.connect()

        conn.getOutputStream().use { wr ->
            wr.write(req.toByteArray(StandardCharsets.UTF_8))
        }
        if (conn.getResponseCode() in 200..299) {
            val jo = JSONObject(read(conn.getInputStream()))
            conn.disconnect()
            Log.i("MicrosoftLogin", "Xbl Token = " + jo.getString("Token"))
            return jo.getString("Token")
            //acquireXsts(jo.getString("Token"));
        } else {
            throw CommonLoginUtils.getResponseThrowable(conn)
        }
    }

    /** @return [uhs, token]
     */
    @Throws(IOException::class, JSONException::class)
    private fun acquireXsts(xblToken: String?): Array<String?> {
        val url = URL(xstsAuthUrl)

        val data = JSONObject()
        val properties = JSONObject()
        properties.put("SandboxId", "RETAIL")
        properties.put("UserTokens", JSONArray(mutableSetOf<String?>(xblToken)))
        data.put("Properties", properties)
        data.put("RelyingParty", "rp://api.minecraftservices.com/")
        data.put("TokenType", "JWT")

        val req = data.toString()
        Log.i("MicroAuth", req)
        val conn = url.openConnection() as HttpURLConnection
        setCommonProperties(conn, req)
        Log.i("MicroAuth", conn.getRequestMethod())
        conn.connect()

        conn.getOutputStream().use { wr ->
            wr.write(req.toByteArray(StandardCharsets.UTF_8))
        }
        if (conn.getResponseCode() in 200..299) {
            val jo = JSONObject(read(conn.getInputStream()))
            val uhs = jo.getJSONObject("DisplayClaims").getJSONArray("xui").getJSONObject(0)
                .getString("uhs")
            val token = jo.getString("Token")
            conn.disconnect()
            Log.i("MicrosoftLogin", "Xbl Xsts = " + token + "; Uhs = " + uhs)
            return arrayOf<String?>(uhs, token)
            //acquireMinecraftToken(uhs,jo.getString("Token"));
        } else if (conn.getResponseCode() == 401) {
            val responseContents = read(conn.getErrorStream())
            val jo = JSONObject(responseContents)
            val xerr = jo.optLong("XErr", -1)
            val locale_id: Int? = XSTS_ERRORS.get(xerr)
            if (locale_id != null) {
                throw PresentedException(RuntimeException(responseContents), locale_id)
            }
            throw PresentedException(
                RuntimeException(responseContents),
                R.string.xerr_unknown,
                xerr
            )
        } else {
            throw CommonLoginUtils.getResponseThrowable(conn)
        }
    }

    @Throws(IOException::class, JSONException::class)
    private fun acquireMinecraftToken(xblUhs: String?, xblXsts: String?): String {
        val url = URL(mcLoginUrl)

        val data = JSONObject()
        data.put("identityToken", "XBL3.0 x=" + xblUhs + ";" + xblXsts)

        val req = data.toString()
        val conn = url.openConnection() as HttpURLConnection
        setCommonProperties(conn, req)
        conn.connect()

        conn.getOutputStream().use { wr ->
            wr.write(req.toByteArray(StandardCharsets.UTF_8))
        }
        if (conn.getResponseCode() in 200..299) {
            expiresAt = System.currentTimeMillis() + 86400000
            val jo = JSONObject(read(conn.getInputStream()))
            conn.disconnect()
            Log.i("MicrosoftLogin", "MC token: " + jo.getString("access_token"))
            mcToken = jo.getString("access_token")
            //checkMcProfile(jo.getString("access_token"));
            return jo.getString("access_token")
        } else {
            throw CommonLoginUtils.getResponseThrowable(conn)
        }
    }

    @Throws(IOException::class)
    private fun fetchOwnedItems(mcAccessToken: String?) {
        val url = URL(mcStoreUrl)

        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer " + mcAccessToken)
        conn.setUseCaches(false)
        conn.connect()
        if (conn.getResponseCode() < 200 || conn.getResponseCode() >= 300) {
            throw CommonLoginUtils.getResponseThrowable(conn)
        }
        // We don't need any data from this request, it just needs to happen in order for
        // the MS servers to work properly. The data from this is practically useless
        // as it does not indicate whether the user owns the game through Game Pass.
    }

    @Throws(IOException::class, JSONException::class)
    private fun checkMcProfile(mcAccessToken: String?) {
        val url = URL(mcProfileUrl)

        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer " + mcAccessToken)
        conn.setUseCaches(false)
        conn.connect()

        if (conn.getResponseCode() in 200..299) {
            val s = read(conn.getInputStream())
            conn.disconnect()
            Log.i("MicrosoftLogin", "profile:" + s)
            val jsonObject = JSONObject(s)
            val name = jsonObject.get("name") as String
            val uuid = jsonObject.get("id") as String
            val uuidDashes = uuid.replaceFirst(
                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)".toRegex(),
                "$1-$2-$3-$4-$5"
            )
            doesOwnGame = true
            Log.i("MicrosoftLogin", "UserName = " + name)
            Log.i("MicrosoftLogin", "Uuid Minecraft = " + uuidDashes)
            mcName = name
            mcUuid = uuidDashes
        } else {
            Log.i("MicrosoftLogin", "It seems that this Microsoft Account does not own the game.")
            doesOwnGame = false
            throw PresentedException(
                RuntimeException(conn.getResponseMessage()),
                R.string.minecraft_not_owned
            )
            //throwResponseError(conn);
        }
    }

    /** Wrapper to ease notifying the listener  */
    private fun notifyProgress(listener: LoginListener, step: Int) {
        runOnUiThread(Runnable { listener.onLoginProgress(step) })
        ProgressLayout.setProgress(ProgressLayout.AUTHENTICATE, step * 20)
    }


    companion object {
        val CREATOR: BackgroundLogin.Creator = object : BackgroundLogin.Creator {
            override fun create(): BackgroundLogin = MicrosoftBackgroundLogin()
        }

        private const val authTokenUrl = "https://login.live.com/oauth20_token.srf"
        private const val xblAuthUrl = "https://user.auth.xboxlive.com/user/authenticate"
        private const val xstsAuthUrl = "https://xsts.auth.xboxlive.com/xsts/authorize"
        private const val mcLoginUrl =
            "https://api.minecraftservices.com/authentication/login_with_xbox"
        private const val mcProfileUrl = "https://api.minecraftservices.com/minecraft/profile"
        private const val mcStoreUrl = "https://api.minecraftservices.com/entitlements/mcstore"

        private val XSTS_ERRORS: MutableMap<Long, Int>

        init {
            XSTS_ERRORS = ArrayMap<Long, Int>()
            XSTS_ERRORS.put(2148916233L, R.string.xerr_no_account)
            XSTS_ERRORS.put(2148916235L, R.string.xerr_not_available)
            XSTS_ERRORS.put(2148916236L, R.string.xerr_adult_verification)
            XSTS_ERRORS.put(2148916237L, R.string.xerr_adult_verification)
            XSTS_ERRORS.put(2148916238L, R.string.xerr_child)
        }

        /** Set common properties for the connection. Given that all requests are POST, interactivity is always enabled  */
        private fun setCommonProperties(conn: HttpURLConnection, formData: String) {
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("charset", "utf-8")
            try {
                conn.setRequestProperty(
                    "Content-Length",
                    formData.toByteArray(StandardCharsets.UTF_8).size.toString()
                )
                conn.setRequestMethod("POST")
            } catch (e: ProtocolException) {
                Log.e("MicrosoftAuth", e.toString())
            }
            conn.setUseCaches(false)
            conn.setDoInput(true)
            conn.setDoOutput(true)
        }
    }
}
