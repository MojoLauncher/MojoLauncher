package net.kdt.pojavlaunch.value;

import android.util.Log;

import net.kdt.pojavlaunch.utils.maven.MavenName;

import java.util.HashMap;
import java.util.Map;

public class SubstitutionMap {
    public LibraryMap libraries;
    public ExtraNameMap artifactMapping;

    public LibrarySubstitution findSubstitution(MavenName mavenName) {
        switch (mavenName.provider) {
            case "org.lwjgl":
            case "org.lwjgl.lwjgl":
            case "net.java.jinput":
                return libraries.get(mavenName);
            default:
                return null;
        }
    }

    public SubstitutionMap prepare() {
        for(Map.Entry<MavenName, MavenName> mappingPair : artifactMapping.entrySet()) {
            libraries.put(mappingPair.getKey(), libraries.get(mappingPair.getValue()));
        }
        artifactMapping.clear();
        return this;
    }

    public static class ExtraNameMap extends HashMap<MavenName, MavenName> {}
    public static class LibraryMap extends HashMap<MavenName, LibrarySubstitution> {}
}
