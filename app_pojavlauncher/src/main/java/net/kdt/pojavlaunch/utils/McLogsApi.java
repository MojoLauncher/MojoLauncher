package net.kdt.pojavlaunch.utils;

import com.google.gson.Gson;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.modloaders.modpacks.api.ApiHandler;

import java.io.File;
import java.io.IOException;

/**
 * Minimal client for the mclo.gs log sharing API.
 * See <a href="https://api.mclo.gs/">https://api.mclo.gs/</a> for the API documentation.
 */
public class McLogsApi {
    private static final String UPLOAD_URL = "https://api.mclo.gs/1/log";
    private static final String SOURCE_NAME = "MJLauncher";

    /** JSON body sent to the mclo.gs upload endpoint. */
    private static class UploadRequest {
        final String content;
        final String source = SOURCE_NAME;
        UploadRequest(String content) { this.content = content; }
    }

    /** JSON response returned by the mclo.gs upload endpoint. */
    private static class UploadResponse {
        boolean success;
        String id;
        String url;
        String error;
    }

    /**
     * Uploads the given log file to mclo.gs.
     * @param logFile the log file to upload
     * @return the public mclo.gs URL pointing to the uploaded log
     * @throws IOException if the log file is empty/missing, the request fails, or the server
     *                      reports an error
     */
    public static String upload(File logFile) throws IOException {
        if (logFile == null || !logFile.exists() || logFile.length() == 0) {
            throw new IOException("Log file is empty or doesn't exist");
        }

        String content = Tools.read(logFile);
        String requestBody = new Gson().toJson(new UploadRequest(content));
        String rawResponse = ApiHandler.postRaw(UPLOAD_URL, requestBody);

        if (rawResponse == null) {
            throw new IOException("No response from mclo.gs");
        }

        UploadResponse response = new Gson().fromJson(rawResponse, UploadResponse.class);
        if (response == null || !response.success || response.url == null) {
            String reason = (response != null && response.error != null) ? response.error : "unknown error";
            throw new IOException("mclo.gs upload failed: " + reason);
        }

        return response.url;
    }
}
