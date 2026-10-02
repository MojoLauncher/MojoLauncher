package net.kdt.pojavlaunch.authenticator.impl;

import android.util.Log;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.authenticator.model.OAuthTokenResponse;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import git.artdeell.mojo.R;

public class CommonLoginUtils {

    public static OAuthTokenResponse exchangeAuthCode(URL url, String formData) throws IOException {
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("charset", "utf-8");
        conn.setRequestProperty("Content-Length", Integer.toString(formData.getBytes(StandardCharsets.UTF_8).length));
        conn.setRequestMethod("POST");
        conn.setUseCaches(false);
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.connect();
        try(OutputStream wr = conn.getOutputStream()) {
            wr.write(formData.getBytes(StandardCharsets.UTF_8));
        }
        if(conn.getResponseCode() >= 200 && conn.getResponseCode() < 300) {
            try (InputStreamReader reader = new InputStreamReader(conn.getInputStream())) {
                return Tools.GLOBAL_GSON.fromJson(reader, OAuthTokenResponse.class);
            } finally {
                conn.disconnect();
            }
        }else{
            Log.i("CommonLogin", "Auth fail: "+Tools.read(conn.getErrorStream()));
            throw getResponseThrowable(conn);
        }
    }

    /**
     * @param data A series a strings: key1, value1, key2, value2...
     * @return the data converted as a form string for a POST request
     */
    public static String convertToFormData(String... data) throws UnsupportedEncodingException {
        StringBuilder builder = new StringBuilder();
        for(int i=0; i<data.length; i+=2){
            if (builder.length() > 0) builder.append("&");
            builder.append(URLEncoder.encode(data[i], "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(data[i+1], "UTF-8"));
        }
        return builder.toString();
    }

    public static RuntimeException getResponseThrowable(HttpURLConnection conn) throws IOException {
        Log.i("MicrosoftLogin", "Error code: " + conn.getResponseCode() + ": " + conn.getResponseMessage());
        if(conn.getResponseCode() == 429) {
            return new PresentedException(R.string.microsoft_login_retry_later);
        }
        return new RuntimeException(conn.getResponseMessage());
    }
}
