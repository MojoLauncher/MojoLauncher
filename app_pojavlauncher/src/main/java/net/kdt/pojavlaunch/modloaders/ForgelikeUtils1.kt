package net.kdt.pojavlaunch.modloaders

import android.util.Log
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.instances.InstanceInstaller
import net.kdt.pojavlaunch.utils.DownloadUtils
import net.kdt.pojavlaunch.utils.DownloadUtils.ParseCallback
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.File
import java.io.IOException
import java.io.StringReader
import javax.xml.parsers.ParserConfigurationException
import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory

abstract class ForgelikeUtils private constructor(
    val name: String?,
    private val mCachePrefix: String?,
    val iconName: String?,
    private val mVersionResolver: String,
    private val mMetadataUrl: String?,
    private val mInstallerUrl: String,
    val isVersionOrderInversed: Boolean
) {
    @Throws(IOException::class)
    fun downloadVersions(): MutableList<String?>? {
        val saxParser: SAXParser
        try {
            val parserFactory = SAXParserFactory.newInstance()
            saxParser = parserFactory.newSAXParser()
        } catch (e: SAXException) {
            e.printStackTrace()
            // if we cant make a parser we might as well not even try to parse anything
            return null
        } catch (e: ParserConfigurationException) {
            e.printStackTrace()
            return null
        }
        try {
            //of_test();
            return DownloadUtils.downloadStringCached<MutableList<String?>>(
                mMetadataUrl,
                "${mCachePrefix}_versions",
                object : ParseCallback<MutableList<String?>?> {
                    override fun process(input: String?): MutableList<String?> {
                        try {
                            val handler = ForgelikeVersionListHandler()
                            saxParser.parse(InputSource(StringReader(input)), handler)
                            return handler.versions
                            // IOException is present here StringReader throws it only if the parser called close()
                            // sooner than needed, which is a parser issue and not an I/O one
                        } catch (e: SAXException) {
                            throw DownloadUtils.ParseException(e)
                        } catch (e: IOException) {
                            throw DownloadUtils.ParseException(e)
                        }
                    }
                })
        } catch (e: DownloadUtils.ParseException) {
            e.printStackTrace()
            return null
        }
    }

    fun getInstallerUrl(version: String): String {
        return String.format(mInstallerUrl, version)
    }

    @Throws(IOException::class)
    fun createInstaller(gameVersion: String, modLoaderVersion: String?): InstanceInstaller? {
        val versions = downloadVersions() ?: return null
        val versionStart = String.format(mVersionResolver, gameVersion, modLoaderVersion)
        for (versionName in versions) {
            if (versionName == null) continue
            if (!versionName.startsWith(versionStart)) continue
            return createInstaller(versionName)
        }
        return null
    }

    @Throws(IOException::class)
    fun createInstaller(fullVersion: String): InstanceInstaller {
        val downloadUrl = getInstallerUrl(fullVersion)
        val hash = DownloadUtils.downloadString("$downloadUrl.sha1")
        val installerLocation =
            File(Tools.DIR_CACHE, "$mCachePrefix-installer-$fullVersion.jar")
        val instanceInstaller = InstanceInstaller()
        instanceInstaller.commandLineArgs = mutableListOf(
            "-Duser.language=en",
            "-Duser.country=US",
            "-javaagent:${Tools.DIR_DATA}/forge_installer/forge_installer.jar"
        )
        instanceInstaller.installerJar = installerLocation.absolutePath
        instanceInstaller.installerSha1 = hash
        instanceInstaller.installerDownloadUrl = downloadUrl
        return instanceInstaller
    }

    abstract fun processVersionString(version: String?): String?

    abstract fun shouldSkipVersion(version: String?): Boolean

    private class ForgeUtils : ForgelikeUtils(
        "Forge", "forge", "forge", "%1\$s-%2\$s",
        "https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml",
        "https://maven.minecraftforge.net/net/minecraftforge/forge/%1\$s/forge-%1\$s-installer.jar",
        false
    ) {
        override fun processVersionString(version: String?): String? {
            if (version == null) return null
            val dashIndex = version.indexOf("-")
            return if (dashIndex != -1) version.substring(0, dashIndex) else version
        }

        override fun shouldSkipVersion(version: String?): Boolean {
            return false
        }
    }

    private class NeoforgeUtils : ForgelikeUtils(
        "NeoForge", "neoforge", "neoforge", "%2\$s",
        "https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml",
        "https://maven.neoforged.net/releases/net/neoforged/neoforge/%1\$s/neoforge-%1\$s-installer.jar",
        true
    ) {
        override fun processVersionString(version: String?): String? {
            if (version == null) return null
            return ComparableVersionString.parse(getMcVersionForNeoVersion(version)).proper
        }

        override fun shouldSkipVersion(version: String?): Boolean {
            return version?.startsWith("0") ?: false
        }
    }

    companion object {
        @JvmField
        val FORGE_UTILS: ForgelikeUtils = ForgeUtils()
        @JvmField
        val NEOFORGE_UTILS: ForgelikeUtils = NeoforgeUtils()

        private fun getMcVersionForNeoVersion(neoVersion: String): String {
            // I feel like it's necessary to explain the NeoForge versioning format
            // basically, what it does is it trims the major version from minecrafts version
            // e.g.: 1.20.1 -> 20.1, and then appends its own "patch" version to that
            // e.g.: 20.1 -> 20.1.8, which means the version string includes both, the minecraft
            // and the loader version at once
            try {
                val firstIndex = neoVersion.indexOf('.')
                val secondIndex = neoVersion.indexOf('.', firstIndex + 1)
                if (firstIndex == -1 || secondIndex == -1) {
                    Log.e(
                        "NeoforgeUtils",
                        "Failed to parse neoforge version: $neoVersion; not enough '.' found"
                    )
                }
                return "1." + neoVersion.substring(0, secondIndex)
            } catch (e: StringIndexOutOfBoundsException) {
                Log.e("NeoforgeUtils", "Failed to parse neoforge version: $neoVersion", e)
                return neoVersion
            }
        }
    }
}
