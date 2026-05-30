package net.kdt.pojavlaunch

import androidx.annotation.Keep
import net.kdt.pojavlaunch.value.DependentLibrary
import net.kdt.pojavlaunch.value.MinecraftClientInfo

@Keep
@Suppress("unused") // all unused fields here are parts of JSON structures
class JMinecraftVersionList {
    var latest: MutableMap<String?, String?>? = null
    var versions: Array<Version?>? = null

    @Keep
    open class FileProperties {
        var id: String? = null
        var sha1: String? = null
        var url: String? = null
        var size: Long = 0
    }

    @Keep
    class Version : FileProperties() {
        // Since 1.13, so it's one of ways to check
        var arguments: Arguments? = null
        var assetIndex: AssetIndex? = null

        var assets: String? = null
        var downloads: MutableMap<String?, MinecraftClientInfo?>? = null
        var inheritsFrom: String? = null
        var javaVersion: JavaVersionInfo? = null
        var libraries: Array<DependentLibrary?>? = null
        var logging: LoggingConfig? = null
        var mainClass: String? = null
        var minecraftArguments: String? = null
        var minimumLauncherVersion: Int = 0
        var releaseTime: String? = null
        var time: String? = null
        var type: String? = null
    }

    @Keep
    class JavaVersionInfo {
        var component: String? = null
        var majorVersion: Int = 0
        var version: Int = 0 // parameter used by LabyMod 4
    }

    @Keep
    class LoggingConfig {
        var client: LoggingClientConfig? = null

        @Keep
        class LoggingClientConfig {
            var argument: String? = null
            var file: FileProperties? = null
            var type: String? = null
        }
    }

    // Since 1.13
    @Keep
    class Arguments {
        var game: Array<Any?>? = null
        var jvm: Array<Any?>? = null

        @Keep
        class ArgValue {
            var rules: Array<ArgRules?>? = null
            var value: String? = null

            // TLauncher styled argument...
            var values: Array<String?>? = null

            @Keep
            class ArgRules {
                var action: String? = null
                var features: String? = null
                var os: ArgOS? = null

                @Keep
                class ArgOS {
                    var name: String? = null
                    var version: String? = null
                }
            }
        }
    }

    @Keep
    class AssetIndex : FileProperties() {
        var totalSize: Long = 0
    }
}
