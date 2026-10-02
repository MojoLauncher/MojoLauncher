package net.kdt.pojavlaunch.value;

import java.util.HashMap;
import java.util.Map;

public class SubstitutionMap {
    public LibraryMap libraries;
    public Map<String, String> artifactMapping;

    public LibrarySubstitution findSubstitution(String name) {
        if(!name.startsWith("org.lwjgl") && !name.startsWith("net.java.jinput")) return null;

        LibrarySubstitution library = libraries.get(name);
        if(library != null) return library;
        String mapping = artifactMapping.get(name);
        if(mapping == null) return null;
        return libraries.get(mapping);
    }

    public static class LibraryMap extends HashMap<String, LibrarySubstitution> {}
}
