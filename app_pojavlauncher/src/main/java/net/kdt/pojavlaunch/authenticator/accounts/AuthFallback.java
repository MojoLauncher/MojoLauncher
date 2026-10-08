package net.kdt.pojavlaunch.authenticator.accounts;

import android.util.Log;

import net.kdt.pojavlaunch.authenticator.AuthType;
import net.kdt.pojavlaunch.utils.DownloadUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class AuthFallback {
    public static final String TAG = "AuthFallback";
    private static final int PROBE_TIMEOUT_MS = 5000;
    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 16;
    private static final String DEFAULT_OFFLINE_USERNAME = "Player";
    private static final Pattern INVALID_USERNAME_CHARS = Pattern.compile("[^a-zA-Z0-9_]");

    private AuthFallback() {}

    public static boolean isAuthServiceReachable(AuthType authType) {
        String injectorUrl = authType.injectorUrl;
        if (injectorUrl == null) return true;

        String targetUrl = withScheme(injectorUrl);
        HttpURLConnection connection = null;

        try {
            connection = createConnection(targetUrl);
            int responseCode = connection.getResponseCode();

            if (responseCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
                Log.w(TAG, "Auth discovery for " + targetUrl + " failed with HTTP " + responseCode);
                return false;
            }

            String apiLocation = connection.getHeaderField("X-Authlib-Injector-API-Location");
            if (apiLocation != null && !apiLocation.trim().isEmpty()) {
                targetUrl = apiLocation.trim();
                connection.disconnect();
                connection = createConnection(targetUrl);
                responseCode = connection.getResponseCode();
            }

            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "Auth API endpoint " + targetUrl + " returned HTTP " + responseCode);
                return false;
            }

            String contentType = connection.getContentType();
            if (contentType == null || !contentType.toLowerCase().contains("application/json")) {
                Log.w(TAG, "Auth API returned unexpected Content-Type: " + contentType);//todo: remove log after tests
                return false;
            }
            String responseBody = readStream(connection.getInputStream());
            boolean isValidMetadata = responseBody.contains("\"signaturePublickey\"") 
                    && responseBody.contains("\"skinDomains\"");

            if (!isValidMetadata) {
                Log.w(TAG, "Auth API returned invalid injector metadata: " + responseBody);//todo: remove log after tests
                return false;
            }
            return true;

        } catch (IOException e) {
            Log.w(TAG, "Auth service " + injectorUrl + " is unreachable", e);//todo: remove log after tests
            return false;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static HttpURLConnection createConnection(String urlStr) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestProperty("User-Agent", DownloadUtils.USER_AGENT);
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(PROBE_TIMEOUT_MS);
        conn.setReadTimeout(PROBE_TIMEOUT_MS);
        conn.setInstanceFollowRedirects(true);
        return conn;
    }

    private static String readStream(InputStream is) throws IOException {
        if (is == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    public static String getServiceName(AuthType authType) {
        return authType.injectorUrl == null ? authType.name() : authType.injectorUrl;
    }

    public static Account switchToOfflineAccount(Account account) throws IOException {
        Account offlineAccount = findOfflineAccount();
        if (offlineAccount == null) {
            offlineAccount = Accounts.create(acc -> acc.username = toOfflineUsername(account.username));
        }
        Accounts.setCurrent(offlineAccount);
        Log.i(TAG, "Auth service " + getServiceName(account.authType) + " is unavailable for " + account.username + ", switched to the offline account " + offlineAccount.username);
        return offlineAccount;
    }

    private static Account findOfflineAccount() throws IOException {
        for (Account candidate : Accounts.load().accounts) {
            if (candidate.authType == AuthType.LOCAL) return candidate;
        }
        return null;
    }

    private static String toOfflineUsername(String username) {
        if (username == null) return DEFAULT_OFFLINE_USERNAME;
        String sanitized = INVALID_USERNAME_CHARS.matcher(username).replaceAll("");
        if (sanitized.length() > MAX_USERNAME_LENGTH) sanitized = sanitized.substring(0, MAX_USERNAME_LENGTH);
        return sanitized.length() < MIN_USERNAME_LENGTH ? DEFAULT_OFFLINE_USERNAME : sanitized;
    }

    private static String withScheme(String url) {
        return url.contains("://") ? url : "https://" + url;
    }
}