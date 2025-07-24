package net.kdt.pojavlaunch.modloaders;

import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.instances.InstanceInstaller;
import net.kdt.pojavlaunch.utils.DownloadUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NeoforgeUtils {
    private static final String VERSIONS_ENDPOINT = "https://maven.neoforged.net/api/maven/versions/releases/";
    private static final String FALLBACK_VERSIONS_ENDPOINT = "https://maven.creeperhost.net/api/maven/versions/releases/";
    private static final String NEOFORGE_GAV = "net/neoforged/neoforge";
    private static final String NEOFORGE_INSTALLER_URL = "https://maven.neoforged.net/releases/net/neoforged/neoforge/%1$s/neoforge-%1$s-installer.jar";

    public static List<String> fetchNeoForgeVersions() throws IOException {
        String mainUrl = VERSIONS_ENDPOINT + NEOFORGE_GAV;
        String fallbackUrl = FALLBACK_VERSIONS_ENDPOINT + NEOFORGE_GAV;
        DownloadUtils.ParseCallback<List<String>> callback = (data) -> {
            try {
                return NeoforgeUtils.parseAndFilterNeoForgeVersions(data);
            } catch (NeoForgeJsonParseException e) {
                throw new DownloadUtils.ParseException(e);
            }
        };

        try {
            try {
                return DownloadUtils.downloadStringCached(mainUrl, "neoforge_versions", callback);
            } catch (IOException e) {
                Log.e("NeoforgeUtils", "Main NeoForge maven failed, trying fallback", e);
                return DownloadUtils.downloadStringCached(fallbackUrl, "neoforge_versions_fallback", callback);
            }
        } catch (DownloadUtils.ParseException e) {
            Log.e("NeoforgeUtils", "Failed to parse NeoForge versions!", e);
            return null;
        }
    }

    public static String getInstallerUrl(String version) {
        return String.format(NEOFORGE_INSTALLER_URL, version);
    }

    private static List<String> parseAndFilterNeoForgeVersions(String json) throws NeoForgeJsonParseException {
        List<String> versions = new ArrayList<>();
        JsonObject obj;
        try {
            JsonElement element = JsonParser.parseString(json);
            if (!element.isJsonObject()) {
                throw new NeoForgeJsonParseException("Parsed JSON is not an object: " + json);
            }
            obj = element.getAsJsonObject();
        } catch (JsonSyntaxException e) {
            throw new NeoForgeJsonParseException("Failed to parse JSON string: " + json, e);
        }

        JsonElement versionsArray = obj.get("versions");
        if (!obj.has("versions") || !versionsArray.isJsonArray()) {
            throw new NeoForgeJsonParseException("Field 'versions' is missing or not an array: " + json);
        }

        for (JsonElement versionElement : versionsArray.getAsJsonArray()) {
            if (!versionElement.isJsonPrimitive()) continue;
            JsonPrimitive primitive = versionElement.getAsJsonPrimitive();
            if (!primitive.isString()) continue;

            String version = primitive.getAsString();
            if (!version.startsWith("0")) {
                versions.add(version);
            }
        }
        Collections.sort(versions, Collections.reverseOrder());
        return versions;
    }

    public static String getMcVersionForNeoVersion(String neoVersion) {
        // I feel like it's necessary to explain the NeoForge versioning format
        // basically, what it does is it trims the major version from minecrafts version
        // e.g.: 1.20.1 -> 20.1, and then appends its own "patch" version to that
        // e.g.: 20.1 -> 20.1.8, which means the version string includes both, the minecraft
        // and the loader version at once
        try {
            int firstIndex = neoVersion.indexOf('.');
            int secondIndex = neoVersion.indexOf('.', firstIndex + 1);
            if(firstIndex == -1 || secondIndex == -1) {
                Log.e("NeoforgeUtils", "Failed to parse neoforge version: " + neoVersion + "; not enough '.' found");
            }
            return "1." + neoVersion.substring(0, secondIndex);
        } catch (StringIndexOutOfBoundsException e) {
            Log.e("NeoforgeUtils", "Failed to parse neoforge version: " + neoVersion, e);
            return neoVersion;
        }
    }

    public static InstanceInstaller createInstaller(String neoVersion) throws IOException {
        String downloadUrl = getInstallerUrl(neoVersion);
        String hash = DownloadUtils.downloadString(downloadUrl+".sha1");
        File installerLocation = new File(Tools.DIR_CACHE, "neoforge-installer-"+neoVersion+".jar");
        InstanceInstaller instanceInstaller = new InstanceInstaller();
        // if the language is not explicitly set to english, neoforge
        // may set it to something else, which causes the OK button to be renamed
        // breaking the installer agent
        instanceInstaller.commandLineArgs = "-Duser.language=en -Duser.country=US -javaagent:"+ Tools.DIR_DATA+"/forge_installer/forge_installer.jar";
        instanceInstaller.installerJar = installerLocation.getAbsolutePath();
        instanceInstaller.installerSha1 = hash;
        instanceInstaller.installerDownloadUrl = downloadUrl;
        return instanceInstaller;
    }

    public static class ComparableVersionString implements Comparable<ComparableVersionString> {
        private int major;
        private int minor;
        private int patch;
        private final String original;
        private final boolean isValid;

        // Private constructor

        private ComparableVersionString(String str) {
            this.original = str;
            this.isValid = false;
        }

        public ComparableVersionString(String original, int major, int minor, int patch) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
            this.original = original;
            this.isValid = true;
        }

        @Override
        public int compareTo(ComparableVersionString str) {
            if(!this.isValid) return str.getProper().compareTo(this.original);

            if(this.major != str.major) return Integer.compare(this.major, str.major);
            if(this.minor != str.minor) return Integer.compare(this.minor, str.minor);
            if(this.patch != str.patch) return Integer.compare(this.patch, str.patch);
            return 0;
        }

        public String getOriginal() {
            return original;
        }

        /**
         * @return the original but if the patch was .0 it will not include it, e.g.
         *         "1.20.0" -> "1.20"
         */
        public String getProper() {
            if(!this.isValid) return original;

            StringBuilder sb = new StringBuilder();
            sb.append(major);
            sb.append('.');
            sb.append(minor);
            if(patch != 0) {
                sb.append('.');
                sb.append(patch);
            }
            return sb.toString();
        }

        public boolean isValid() {
            return isValid;
        }

        public static ComparableVersionString parse(String str) {
            String[] split = str.split("\\.");
            if (split.length < 2) return new ComparableVersionString(str);

            try {
                int major = Integer.parseInt(split[0]);
                int minor = Integer.parseInt(split[1]);
                int patch = (split.length >= 3) ? Integer.parseInt(split[2]) : 0;
                return new ComparableVersionString(str, major, minor, patch);
            } catch (NumberFormatException e) {
                return new ComparableVersionString(str);
            }
        }
    }

    public static class NeoForgeJsonParseException extends Exception {
        public NeoForgeJsonParseException(String message) {
            super(message);
        }

        public NeoForgeJsonParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
