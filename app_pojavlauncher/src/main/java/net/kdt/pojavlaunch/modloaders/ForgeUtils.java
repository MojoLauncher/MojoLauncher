package net.kdt.pojavlaunch.modloaders;

import android.content.Intent;

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

public class ForgeUtils {
    private static final String FORGE_METADATA_URL = "https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml";
    private static final String FORGE_INSTALLER_URL = "https://maven.minecraftforge.net/net/minecraftforge/forge/%1$s/forge-%1$s-installer.jar";
    public static List<String> downloadForgeVersions() throws IOException {
        SAXParser saxParser;
        try {
            SAXParserFactory parserFactory = SAXParserFactory.newInstance();
            saxParser = parserFactory.newSAXParser();
        }catch (SAXException | ParserConfigurationException e) {
            e.printStackTrace();
            // if we cant make a parser we might as well not even try to parse anything
            return null;
        }
        try {
            //of_test();
            return DownloadUtils.downloadStringCached(FORGE_METADATA_URL, "forge_versions", input -> {
                try {
                    ForgeVersionListHandler handler = new ForgeVersionListHandler();
                    saxParser.parse(new InputSource(new StringReader(input)), handler);
                    return handler.getVersions();
                    // IOException is present here StringReader throws it only if the parser called close()
                    // sooner than needed, which is a parser issue and not an I/O one
                }catch (SAXException | IOException e) {
                    throw new DownloadUtils.ParseException(e);
                }
            });
        }catch (DownloadUtils.ParseException e) {
            e.printStackTrace();
            return null;
        }

    }

    public static String getInstallerUrl(String version) {
        return String.format(FORGE_INSTALLER_URL, version);
    }

    public static InstanceInstaller createInstaller(String gameVersion, String modLoaderVersion) throws IOException {
        List<String> forgeVersions = ForgeUtils.downloadForgeVersions();
        if(forgeVersions == null) return null;
        String versionStart = gameVersion+"-"+modLoaderVersion;
        for(String versionName : forgeVersions) {
            if(!versionName.startsWith(versionStart)) continue;
            return createInstaller(versionName);
        }
        return null;
    }

    public static InstanceInstaller createInstaller(String fullVersion) throws IOException {
        String downloadUrl = getInstallerUrl(fullVersion);
        String hash = DownloadUtils.downloadString(downloadUrl+".sha1");
        File installerLocation = new File(Tools.DIR_CACHE, "forge-installer-"+fullVersion+".jar");
        InstanceInstaller instanceInstaller = new InstanceInstaller();
        instanceInstaller.commandLineArgs = "-javaagent:"+ Tools.DIR_DATA+"/forge_installer/forge_installer.jar";
        instanceInstaller.installerJar = installerLocation.getAbsolutePath();
        instanceInstaller.installerSha1 = hash;
        instanceInstaller.installerDownloadUrl = downloadUrl;
        return instanceInstaller;
    }
}
