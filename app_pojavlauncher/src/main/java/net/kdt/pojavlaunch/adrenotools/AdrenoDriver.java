package net.kdt.pojavlaunch.adrenotools;

import android.util.Log;

import net.kdt.pojavlaunch.utils.JSONUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import kotlin.jvm.Transient;

public class AdrenoDriver implements BaseDriver {
    private final String name;
    private final String description;
    private final String author;
    private final String driverVersion;
    private final String libraryName;

    private AdrenoDriver(String name, String description, String author, String driverVersion, String libraryName, int schemaVersion, int minApi) {
        this.name = name;
        this.description = description;
        this.author = author;
        this.driverVersion = driverVersion;
        this.libraryName = libraryName;
        this.schemaVersion = schemaVersion;
        this.minApi = minApi;
    }

    private final int schemaVersion;
    private final int minApi;


    public static AdrenoDriver fromJson(File metadata){
        AdrenoDriver adrenoDriver;
        try {
            adrenoDriver = JSONUtils.readFromFile(metadata, AdrenoDriver.class);
        } catch (IOException e) {
            Log.i(AdrenoManager.TAG, "Failed to read metadata from " + metadata.getAbsolutePath() + "!");
            return null;
        }
        return adrenoDriver;
    }

    public static AdrenoDriver fromJson(InputStream is) throws IOException {
        return JSONUtils.readFromStream(is, AdrenoDriver.class);
    }

    public String getMainLibrary() {
        return libraryName;
    }

    @Override
    public boolean isDefault() {
        return false;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getAuthor() {
        return author;
    }

    public String getDriverVersion() {
        return driverVersion;
    }

    public String getLibraryName() {
        return libraryName;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public int getMinApi() {
        return minApi;
    }

    public String toHash(){
        String to = name + author;
        return UUID.nameUUIDFromBytes(to.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
