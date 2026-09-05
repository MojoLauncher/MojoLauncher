package net.kdt.pojavlaunch.utils.maven;

import android.util.Log;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class MavenName {
    public final String provider;
    public final String module;
    public final String version;
    public final String extra;

    public MavenName(String provider, String module, String version, String extra) {
        this.provider = provider;
        this.module = module;
        this.version = version;
        this.extra = extra;
    }

    public MavenName(String provider, String module, String version) {
        this(provider, module, version, null);
    }

    public MavenName(String provider, String module) {
        this(provider, module, null);
    }

    public static MavenName parse(String name) {
        String[] components = new String[4];
        int start = 0;
        int end = name.indexOf(':');
        int componentIndex = 0;

        while (end != -1 && componentIndex < 3) {
            components[componentIndex] = name.substring(start, end);
            start = end + 1;
            end = name.indexOf(':', start);
            componentIndex++;
        }

        components[componentIndex] = name.substring(start);

        if(componentIndex < 2)
            throw new IllegalArgumentException("Not a valid library name");

        String version = components[2];
        if(version.equals("*")) version = null;

        return new MavenName(components[0], components[1], version, components[3]);
    }

    public MavenName anyVersion() {
        return new MavenName(provider, module, null, extra);
    }

    public boolean isAnyVersion() {
        return version == null;
    }

    /**
     * Arranges the library name components into a file system path.
     * For example, org.lwjgl:lwjgl:3.3.1 will become org/lwjgl/lwjgl/lwjgl-3.3.1[suffix][fileExtension]
     *              org.lwjgl:lwjgl:3.3.1:natives-linux will become org/lwjgl/lwjgl/lwjgl-3.3.1-natives-linux[suffix][fileExtension]
     * @return the resulting path
     */
    public String toPath(String suffix, @NotNull String fileExtension) {
        int moduleNameLen = module.length();
        int versionLen = version.length();
        int suffixLen = suffix != null ? 1 + suffix.length() : 0;
        int extraLen = extra != null ? 1 + extra.length() : 0;
        int pathLen = provider.length() + 1 + moduleNameLen + 1 + versionLen + 1 + moduleNameLen + 1 + versionLen + extraLen + suffixLen + fileExtension.length();
        StringBuilder builder = new StringBuilder(pathLen)
                .append(provider.replace('.', '/'))
                .append('/').append(module)
                .append('/').append(version)
                .append('/').append(module).append('-').append(version);
        if(extra != null) {
            builder.append('-').append(extra.replace(':', '-'));
        }
        if(suffix != null) {
            builder.append('-').append(suffix);
        }
        String path =  builder.append(fileExtension).toString();
        Log.i("MojoLauncher", "Path:"+path);
        return path;
    }

    public String toPath(String suffix) {
        return toPath(suffix, ".jar");
    }

    public String toPath() {
        return toPath(null, ".jar");
    }

    @NonNull
    @Override
    public String toString() {
        int extrasLen = extra != null ? 1 + extra.length() : 0;
        String version = this.version != null ? this.version : "*";
        StringBuilder builder = new StringBuilder(provider.length() + 1 + module.length() + 1 + version.length() + extrasLen)
                .append(provider).append(':').append(module).append(':').append(version);
        if(extrasLen != 0) {
            builder.append(':').append(extra);
        }
        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MavenName)) return false;
        MavenName mavenName = (MavenName) o;
        return Objects.equals(provider, mavenName.provider)
                && Objects.equals(module, mavenName.module)
                && Objects.equals(version, mavenName.version)
                && Objects.equals(extra, mavenName.extra);
    }

    @Override
    public int hashCode() {
        return Objects.hash(provider, module, version, extra);
    }
}
