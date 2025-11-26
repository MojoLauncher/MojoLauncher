package net.kdt.pojavlaunch.modloaders;

import android.util.Log;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.instances.InstanceInstaller;
import net.kdt.pojavlaunch.utils.DownloadUtils;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public abstract class ForgelikeUtils {
    public static final ForgelikeUtils FORGE_UTILS = new ForgeUtils();
    public static final ForgelikeUtils NEOFORGE_UTILS = new NeoforgeUtils();

    private final String mName;
    private final String mCachePrefix;
    private final String mVersionResolver;
    private final String mIconName;
    private final String mMetadataUrl;
    private final String mInstallerUrl;
    private final boolean mVersionOrderInversed;

    private ForgelikeUtils(String name, String cachePrefix, String iconName, String versionResolver, String metadataUrl, String installerUrl, boolean versionOrderInversed) {
        this.mName = name;
        this.mCachePrefix = cachePrefix;
        this.mIconName = iconName;
        this.mVersionResolver = versionResolver;
        this.mMetadataUrl = metadataUrl;
        this.mInstallerUrl = installerUrl;
        this.mVersionOrderInversed = versionOrderInversed;
    }

    public List<String> downloadVersions() throws IOException {
        SAXParser saxParser;
        try {
            SAXParserFactory parserFactory = SAXParserFactory.newInstance();
            saxParser = parserFactory.newSAXParser();
        } catch (SAXException | ParserConfigurationException e) {
            e.printStackTrace();
            // if we cant make a parser we might as well not even try to parse anything
            return null;
        }
        try {
            //of_test();
            return DownloadUtils.downloadStringCached(mMetadataUrl, mCachePrefix + "_versions", input -> {
                try {
                    ForgelikeVersionListHandler handler = new ForgelikeVersionListHandler();
                    saxParser.parse(new InputSource(new StringReader(input)), handler);
                    return handler.getVersions();
                    // IOException is present here StringReader throws it only if the parser called close()
                    // sooner than needed, which is a parser issue and not an I/O one
                } catch (SAXException | IOException e) {
                    throw new DownloadUtils.ParseException(e);
                }
            });
        } catch (DownloadUtils.ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getInstallerUrl(String version) {
        return String.format(mInstallerUrl, version);
    }

    public InstanceInstaller createInstaller(String gameVersion, String modLoaderVersion) throws IOException {
        List<String> versions = downloadVersions();
        if (versions == null) return null;
        String versionStart = String.format(mVersionResolver, gameVersion, modLoaderVersion);
        for (String versionName : versions) {
            if (!versionName.startsWith(versionStart)) continue;
            return createInstaller(versionName);
        }
        return null;
    }

    public InstanceInstaller createInstaller(String fullVersion) throws IOException {
        String downloadUrl = getInstallerUrl(fullVersion);
        String hash = DownloadUtils.downloadString(downloadUrl + ".sha1");
        File installerLocation = new File(Tools.DIR_CACHE, mCachePrefix + "-installer-" + fullVersion + ".jar");
        InstanceInstaller instanceInstaller = new InstanceInstaller();
        instanceInstaller.commandLineArgs = List.of("-Duser.language=en", "-Duser.country=US", "-javaagent:"+Tools.DIR_DATA+"/forge_installer/forge_installer.jar");
        instanceInstaller.installerJar = installerLocation.getAbsolutePath();
        instanceInstaller.installerSha1 = hash;
        instanceInstaller.installerDownloadUrl = downloadUrl;
        return instanceInstaller;
    }

    public String getName() {
        return mName;
    }

    public String getIconName() {
        return mIconName;
    }

    public abstract String processVersionString(String version);

    public abstract boolean shouldSkipVersion(String version);

    public boolean isVersionOrderInversed() {
        return mVersionOrderInversed;
    }

    private static String getMcVersionForNeoVersion(String neoVersion) {
        // I feel like it's necessary to explain the NeoForge versioning format
        // basically, what it does is it trims the major version from minecrafts version
        // e.g.: 1.20.1 -> 20.1, and then appends its own "patch" version to that
        // e.g.: 20.1 -> 20.1.8, which means the version string includes both, the minecraft
        // and the loader version at once
        try {
            int firstIndex = neoVersion.indexOf('.');
            int secondIndex = neoVersion.indexOf('.', firstIndex + 1);
            if (firstIndex == -1 || secondIndex == -1) {
                Log.e("NeoforgeUtils", "Failed to parse neoforge version: " + neoVersion + "; not enough '.' found");
            }
            return "1." + neoVersion.substring(0, secondIndex);
        } catch (StringIndexOutOfBoundsException e) {
            Log.e("NeoforgeUtils", "Failed to parse neoforge version: " + neoVersion, e);
            return neoVersion;
        }
    }

    private static class ForgeUtils extends ForgelikeUtils {
        public ForgeUtils() {
            super(
                    "Forge", "forge", "forge", "%1$s-%2$s",
                    "https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml",
                    "https://maven.minecraftforge.net/net/minecraftforge/forge/%1$s/forge-%1$s-installer.jar",
                    false
            );
        }

        @Override
        public String processVersionString(String version) {
            int dashIndex = version.indexOf("-");
            return version.substring(0, dashIndex);
        }

        @Override
        public boolean shouldSkipVersion(String version) {
            return false;
        }
    }

    private static class NeoforgeUtils extends ForgelikeUtils {
        public NeoforgeUtils() {
            super(
                    "NeoForge", "neoforge", "neoforge", "%2$s",
                    "https://maven.neoforged.net/net/neoforged/neoforge/maven-metadata.xml",
                    "https://maven.neoforged.net/releases/net/neoforged/neoforge/%1$s/neoforge-%1$s-installer.jar",
                    true
            );
        }

        @Override
        public String processVersionString(String version) {
            return ComparableVersionString.parse(getMcVersionForNeoVersion(version)).getProper();
        }

        @Override
        public boolean shouldSkipVersion(String version) {
            return version.startsWith("0");
        }
    }
}
