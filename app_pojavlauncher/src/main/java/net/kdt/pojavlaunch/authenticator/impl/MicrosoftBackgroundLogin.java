package net.kdt.pojavlaunch.authenticator.impl;

import static net.kdt.pojavlaunch.PojavApplication.sExecutorService;

import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.NonNull;

import com.kdt.mcgui.ProgressLayout;

import git.artdeell.mojo.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.authenticator.AuthType;
import net.kdt.pojavlaunch.authenticator.BackgroundLogin;
import net.kdt.pojavlaunch.authenticator.accounts.PojavProfile;
import net.kdt.pojavlaunch.authenticator.listener.LoginListener;
import net.kdt.pojavlaunch.authenticator.model.OAuthTokenResponse;
import net.kdt.pojavlaunch.authenticator.accounts.MinecraftAccount;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;

/** Allow to perform a background login on a given account */
public class MicrosoftBackgroundLogin implements BackgroundLogin{
    public static final BackgroundLogin.Creator CREATOR = MicrosoftBackgroundLogin::new;

    private static final String authTokenUrl = "https://login.live.com/oauth20_token.srf";
    private static final String xblAuthUrl = "https://user.auth.xboxlive.com/user/authenticate";
    private static final String xstsAuthUrl = "https://xsts.auth.xboxlive.com/xsts/authorize";
    private static final String mcLoginUrl = "https://api.minecraftservices.com/authentication/login_with_xbox";
    private static final String mcProfileUrl = "https://api.minecraftservices.com/minecraft/profile";
    private static final String mcStoreUrl = "https://api.minecraftservices.com/entitlements/mcstore";

    private static final Map<Long, Integer> XSTS_ERRORS;
    static {
        XSTS_ERRORS = new ArrayMap<>();
        XSTS_ERRORS.put(2148916233L, R.string.xerr_no_account);
        XSTS_ERRORS.put(2148916235L, R.string.xerr_not_available);
        XSTS_ERRORS.put(2148916236L ,R.string.xerr_adult_verification);
        XSTS_ERRORS.put(2148916237L ,R.string.xerr_adult_verification);
        XSTS_ERRORS.put(2148916238L ,R.string.xerr_child);
    }

    /* Fields used to fill the account  */
    public String msRefreshToken;
    public String mcName;
    public String mcToken;
    public String mcUuid;
    public String msXsts;
    public boolean doesOwnGame;
    public long expiresAt;

    private MicrosoftBackgroundLogin() {}

    private void acquireAccountDetails(
            @NonNull LoginListener loginListener, Callable<Void> continuation,
            String code, boolean isRefresh
    ) {
        ProgressLayout.setProgress(ProgressLayout.AUTHENTICATE, 0);
        sExecutorService.execute(() -> {
            loginListener.setMaxLoginProgress(5);
            try {
                notifyProgress(loginListener, 1);
                String accessToken = acquireAccessToken(isRefresh, code);
                notifyProgress(loginListener, 2);
                String xboxLiveToken = acquireXBLToken(accessToken);
                notifyProgress(loginListener, 3);
                String[] xsts = acquireXsts(xboxLiveToken);
                notifyProgress(loginListener, 4);
                String mcToken = acquireMinecraftToken(xsts[0], xsts[1]);
                notifyProgress(loginListener, 5);
                fetchOwnedItems(mcToken);
                checkMcProfile(mcToken);
                msXsts  = xsts[0];
                continuation.call();
            }catch (Exception e){
                Log.e("MicroAuth", "Exception thrown during authentication", e);
                loginListener.onLoginError(e);
            } finally {
                ProgressLayout.clearProgress(ProgressLayout.AUTHENTICATE);
            }
        });
    }

    private void fillAccount(MinecraftAccount acc) {
        acc.xuid = msXsts;
        acc.accessToken = mcToken;
        acc.username = mcName;
        acc.profileId = mcUuid;
        acc.authType = AuthType.MICROSOFT;
        acc.refreshToken = msRefreshToken;
        acc.expiresAt = expiresAt;
        acc.updateSkinFace();
    }

    @Override
    public void createAccount(@NonNull LoginListener loginListener, String code) {
        acquireAccountDetails(loginListener, ()->{
            MinecraftAccount account = PojavProfile.createAccount(this::fillAccount);
            Tools.runOnUiThread(() -> loginListener.onLoginDone(account));
            return null;
        }, code, false);
    }

    @Override
    public void refreshAccount(@NonNull LoginListener loginListener, MinecraftAccount account) {
        acquireAccountDetails(loginListener, ()->{
            if(doesOwnGame) fillAccount(account);
            account.save();
            Tools.runOnUiThread(() -> loginListener.onLoginDone(account));
            return null;
        }, account.refreshToken, true);
    }

    private String acquireAccessToken(boolean isRefresh, String code) throws IOException {
        URL url = new URL(authTokenUrl);
        Log.i("MicrosoftLogin", "isRefresh=" + isRefresh + ", authCode= "+code);

        String formData = CommonLoginUtils.convertToFormData(
                "client_id", "00000000402b5328",
                isRefresh ? "refresh_token" : "code", code,
                "grant_type", isRefresh ? "refresh_token" : "authorization_code",
                "redirect_url", "https://login.live.com/oauth20_desktop.srf",
                "scope", "service::user.auth.xboxlive.com::MBI_SSL"
        );

        OAuthTokenResponse response = CommonLoginUtils.exchangeAuthCode(url, formData);
        msRefreshToken = response.refreshToken;
        return response.accessToken;
    }

    private String acquireXBLToken(String accessToken) throws IOException, JSONException {
        URL url = new URL(xblAuthUrl);

        JSONObject data = new JSONObject();
        JSONObject properties = new JSONObject();
        properties.put("AuthMethod", "RPS");
        properties.put("SiteName", "user.auth.xboxlive.com");
        properties.put("RpsTicket", accessToken);
        data.put("Properties",properties);
        data.put("RelyingParty", "http://auth.xboxlive.com");
        data.put("TokenType", "JWT");

        String req = data.toString();
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        setCommonProperties(conn, req);
        conn.connect();

        try(OutputStream wr = conn.getOutputStream()) {
            wr.write(req.getBytes(StandardCharsets.UTF_8));
        }
        if(conn.getResponseCode() >= 200 && conn.getResponseCode() < 300) {
            JSONObject jo = new JSONObject(Tools.read(conn.getInputStream()));
            conn.disconnect();
            Log.i("MicrosoftLogin","Xbl Token = "+jo.getString("Token"));
            return jo.getString("Token");
            //acquireXsts(jo.getString("Token"));
        }else{
            throw CommonLoginUtils.getResponseThrowable(conn);
        }
    }

    /** @return [uhs, token]*/
    private @NonNull String[] acquireXsts(String xblToken) throws IOException, JSONException {
        URL url = new URL(xstsAuthUrl);

        JSONObject data = new JSONObject();
        JSONObject properties = new JSONObject();
        properties.put("SandboxId", "RETAIL");
        properties.put("UserTokens", new JSONArray(Collections.singleton(xblToken)));
        data.put("Properties", properties);
        data.put("RelyingParty", "rp://api.minecraftservices.com/");
        data.put("TokenType", "JWT");

        String req = data.toString();
        Log.i("MicroAuth", req);
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        setCommonProperties(conn, req);
        Log.i("MicroAuth", conn.getRequestMethod());
        conn.connect();

        try(OutputStream wr = conn.getOutputStream()) {
            wr.write(req.getBytes(StandardCharsets.UTF_8));
        }

        if(conn.getResponseCode() >= 200 && conn.getResponseCode() < 300) {
            JSONObject jo = new JSONObject(Tools.read(conn.getInputStream()));
            String uhs = jo.getJSONObject("DisplayClaims").getJSONArray("xui").getJSONObject(0).getString("uhs");
            String token = jo.getString("Token");
            conn.disconnect();
            Log.i("MicrosoftLogin","Xbl Xsts = " + token + "; Uhs = " + uhs);
            return new String[]{uhs, token};
            //acquireMinecraftToken(uhs,jo.getString("Token"));
        }else if(conn.getResponseCode() == 401) {
            String responseContents = Tools.read(conn.getErrorStream());
            JSONObject jo = new JSONObject(responseContents);
            long xerr = jo.optLong("XErr", -1);
            Integer locale_id = XSTS_ERRORS.get(xerr);
            if(locale_id != null) {
                throw new PresentedException(new RuntimeException(responseContents), locale_id);
            }
            throw new PresentedException(new RuntimeException(responseContents), R.string.xerr_unknown, xerr);
        }else{
            throw CommonLoginUtils.getResponseThrowable(conn);
        }
    }

    private String acquireMinecraftToken(String xblUhs, String xblXsts) throws IOException, JSONException {
        URL url = new URL(mcLoginUrl);

        JSONObject data = new JSONObject();
        data.put("identityToken", "XBL3.0 x=" + xblUhs + ";" + xblXsts);

        String req = data.toString();
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        setCommonProperties(conn, req);
        conn.connect();

        try(OutputStream wr = conn.getOutputStream()) {
            wr.write(req.getBytes(StandardCharsets.UTF_8));
        }

        if(conn.getResponseCode() >= 200 && conn.getResponseCode() < 300) {
            expiresAt = System.currentTimeMillis() + 86400000;
            JSONObject jo = new JSONObject(Tools.read(conn.getInputStream()));
            conn.disconnect();
            Log.i("MicrosoftLogin","MC token: "+jo.getString("access_token"));
            mcToken = jo.getString("access_token");
            //checkMcProfile(jo.getString("access_token"));
            return jo.getString("access_token");
        }else{
            throw CommonLoginUtils.getResponseThrowable(conn);
        }
    }

    private void fetchOwnedItems(String mcAccessToken) throws IOException {
        URL url = new URL(mcStoreUrl);

        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setRequestProperty("Authorization", "Bearer " + mcAccessToken);
        conn.setUseCaches(false);
        conn.connect();
        if(conn.getResponseCode() < 200 || conn.getResponseCode() >= 300) {
            throw CommonLoginUtils.getResponseThrowable(conn);
        }
        // We don't need any data from this request, it just needs to happen in order for
        // the MS servers to work properly. The data from this is practically useless
        // as it does not indicate whether the user owns the game through Game Pass.
    }

    private void checkMcProfile(String mcAccessToken) throws IOException, JSONException {
        URL url = new URL(mcProfileUrl);

        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setRequestProperty("Authorization", "Bearer " + mcAccessToken);
        conn.setUseCaches(false);
        conn.connect();

        if(conn.getResponseCode() >= 200 && conn.getResponseCode() < 300) {
            String s= Tools.read(conn.getInputStream());
            conn.disconnect();
            Log.i("MicrosoftLogin","profile:" + s);
            JSONObject jsonObject = new JSONObject(s);
            String name = (String) jsonObject.get("name");
            String uuid = (String) jsonObject.get("id");
            String uuidDashes = uuid.replaceFirst(
                    "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5"
            );
            doesOwnGame = true;
            Log.i("MicrosoftLogin","UserName = " + name);
            Log.i("MicrosoftLogin","Uuid Minecraft = " + uuidDashes);
            mcName=name;
            mcUuid=uuidDashes;
        }else{
            Log.i("MicrosoftLogin","It seems that this Microsoft Account does not own the game.");
            doesOwnGame = false;
            throw new PresentedException(new RuntimeException(conn.getResponseMessage()), R.string.minecraft_not_owned);
            //throwResponseError(conn);
        }
    }

    /** Wrapper to ease notifying the listener */
    private void notifyProgress(LoginListener listener, int step){
        Tools.runOnUiThread(() -> listener.onLoginProgress(step));
        ProgressLayout.setProgress(ProgressLayout.AUTHENTICATE, step*20);
    }


    /** Set common properties for the connection. Given that all requests are POST, interactivity is always enabled */
    private static void setCommonProperties(HttpURLConnection conn, String formData) {
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("charset", "utf-8");
        try {
            conn.setRequestProperty("Content-Length", Integer.toString(formData.getBytes(StandardCharsets.UTF_8).length));
            conn.setRequestMethod("POST");
        }catch (ProtocolException e) {
            Log.e("MicrosoftAuth", e.toString());
        }
        conn.setUseCaches(false);
        conn.setDoInput(true);
        conn.setDoOutput(true);
    }
}
