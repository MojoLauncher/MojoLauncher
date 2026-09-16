package net.kdt.pojavlaunch.value;

import androidx.annotation.Keep;

import net.kdt.pojavlaunch.utils.maven.MavenName;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Keep
public class DependentLibrary {
    public MoJsonRule[] rules;
    public MavenName name;
    public LibraryDownloads downloads;
    public String url;
    public transient boolean replaced = false;
    public Map<String, String> natives;
    public ExtractSettings extract;

    @Keep
	public static class LibraryDownloads {
		public LibraryArtifact artifact;
        public LibraryClassifierMap classifiers;
		public LibraryDownloads(LibraryArtifact artifact) {
			this.artifact = artifact;
		}
	}

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DependentLibrary)) return false;
        DependentLibrary that = (DependentLibrary) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    public static class LibraryClassifierMap extends HashMap<String, LibraryArtifact> {}
}

