package net.kdt.pojavlaunch.modloaders;

import android.util.Log;

import androidx.annotation.Keep;

import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.utils.DownloadUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class BTAUtils {
    private static final String BASE_DOWNLOADS_URL = "https://downloads.betterthanadventure.net/bta-client/";
    private static final String CLIENT_JAR_URL = BASE_DOWNLOADS_URL + "%s/%s/client.jar";
    private static final String ICON_URL = BASE_DOWNLOADS_URL + "%s/%s/auto/%s.png";
    private static final String MANIFEST_URL = BASE_DOWNLOADS_URL + "%s/versions.json";
    private static final String BUILD_TYPE_RELEASE = "release";
    private static final String BUILD_TYPE_NIGHTLY = "nightly";
    private static final List<String> BTA_TESTED_VERSIONS = new ArrayList<>();
    static {
        BTA_TESTED_VERSIONS.add("v7.3");
        BTA_TESTED_VERSIONS.add("v7.2_01");
        BTA_TESTED_VERSIONS.add("v7.2");
        BTA_TESTED_VERSIONS.add("v7.1_01");
        BTA_TESTED_VERSIONS.add("v7.1");
    }

    private static String getIconUrl(String version, String buildType) {
        String iconName = version.replace('.','_');
        if(buildType.equals("nightly")) iconName = "v"+iconName;
        return String.format(ICON_URL, buildType, version, iconName);
    }

    private static String getClientJarUrl(String version, String buildType) {
        return String.format(CLIENT_JAR_URL, buildType, version);
    }

    private static String getManifestUrl(String buildType) {
        return String.format(MANIFEST_URL, buildType);
    }

    private static <T> T getManifest(String buildType, DownloadUtils.ParseCallback<T> parser)
            throws DownloadUtils.ParseException, IOException {
        String manifestUrl = getManifestUrl(buildType);
        return DownloadUtils.downloadStringCached(manifestUrl,"bta_"+manifestUrl, parser);
    }

    private static List<BTAVersion> createVersionList(List<String> versionStrings, String buildType) {
        ListIterator<String> iterator = versionStrings.listIterator(versionStrings.size());
        ArrayList<BTAVersion> btaVersions = new ArrayList<>(versionStrings.size());
        // The original list is guaranteed to be in ascending order - the earliest versions
        // are at the top, but for user convenience we need to put the newest versions at the top,
        // so the BTAVersion list is made from the reverse of the string list.
        while(iterator.hasPrevious()) {
            String version = iterator.previous();
            if(version == null) continue;
            btaVersions.add(new BTAVersion(
                    version,
                    getClientJarUrl(version, buildType),
                    getIconUrl(version, buildType)
            ));
        }
        btaVersions.trimToSize();
        return btaVersions;
    }

    private static List<BTAVersion> processNightliesJson(String nightliesInfo) throws JsonParseException {
        BTAVersionsManifest manifest = Tools.GLOBAL_GSON.fromJson(nightliesInfo, BTAVersionsManifest.class);
        return createVersionList(manifest.versions, BUILD_TYPE_NIGHTLY);
    }

    private static BTAVersionList processReleasesJson(String releasesInfo) throws JsonParseException {
        BTAVersionsManifest manifest = Tools.GLOBAL_GSON.fromJson(releasesInfo, BTAVersionsManifest.class);
        List<String> stringVersions = manifest.versions;
        List<String> testedVersions = new ArrayList<>();
        List<String> untestedVersions = new ArrayList<>();
        for(String version : stringVersions) {
            if(version == null) break;
            // Checking for presence in testing array here to avoid accidentally adding nonexistent
            // versions if some of them end up getting removed.
            if(BTA_TESTED_VERSIONS.contains(version)) {
                testedVersions.add(version);
            }else {
                untestedVersions.add(version);
            }
        }

        return new BTAVersionList(
                createVersionList(testedVersions, BUILD_TYPE_RELEASE),
                createVersionList(untestedVersions, BUILD_TYPE_RELEASE),
                null
        );
    }

    public static BTAVersionList downloadVersionList() throws IOException {
        try {
            BTAVersionList releases = getManifest(BUILD_TYPE_RELEASE, BTAUtils::processReleasesJson);
            List<BTAVersion> nightlies = getManifest(BUILD_TYPE_NIGHTLY, BTAUtils::processNightliesJson);
            return new BTAVersionList(releases.testedVersions, releases.untestedVersions, nightlies);
        }catch (DownloadUtils.ParseException e) {
            Log.e("BTAUtils", "Failed to process json", e);
            return null;
        }
    }

    private static class BTAVersionsManifest {
        @Keep
        public List<String> versions;
        @Keep
        @SerializedName("default")
        public String defaultVersion;
    }

    public static class BTAVersion {
        public final String versionName;
        public final String downloadUrl;
        public final String iconUrl;

        public BTAVersion(String versionName, String downloadUrl, String iconUrl) {
            this.versionName = versionName;
            this.downloadUrl = downloadUrl;
            this.iconUrl = iconUrl;
        }
    }
    public static class BTAVersionList {
        public final List<BTAVersion> testedVersions;
        public final List<BTAVersion> untestedVersions;
        public final List<BTAVersion> nightlyVersions;

        public BTAVersionList(List<BTAVersion> mTestedVersions, List<BTAVersion> mUntestedVersions, List<BTAVersion> nightlyVersions) {
            this.testedVersions = mTestedVersions;
            this.untestedVersions = mUntestedVersions;
            this.nightlyVersions = nightlyVersions;
        }
    }
}
