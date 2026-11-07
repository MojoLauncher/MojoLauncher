package git.artdeell.mojo.vulkan.driver;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import git.artdeell.mojo.utils.JSONUtils;
import git.artdeell.mojo.vulkan.DriverManager;

// AdrenoTools package driver
public class AdrenoDriver extends DisplayDriver {
    public final String description;
    public final String libraryName;

    private AdrenoDriver(String name, String description, String author, String driverVersion, String libraryName, int schemaVersion, int minApi) {
        this.name = name;
        this.description = description;
        this.author = author;
        this.driverVersion = driverVersion;
        this.libraryName = libraryName;
        this.schemaVersion = schemaVersion;
        this.minApi = minApi;
    }

    public final int schemaVersion;
    public final int minApi;


    public static AdrenoDriver fromJson(File metadata) {
        AdrenoDriver adrenoDriver;
        try {
            adrenoDriver = JSONUtils.readFromFile(metadata, AdrenoDriver.class);
        } catch (IOException e) {
            Log.i(DriverManager.TAG, "Failed to read metadata from " + metadata.getAbsolutePath() + "!");
            return null;
        }
        return adrenoDriver;
    }

    public static AdrenoDriver fromJson(InputStream is) throws IOException {
        return JSONUtils.readFromStream(is, AdrenoDriver.class);
    }

    @Override
    public String getMainLibrary() {
        return libraryName;
    }

}
