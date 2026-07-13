package net.kdt.pojavlaunch.value;

import androidx.annotation.Keep;

import java.util.HashMap;
import java.util.Map;

@Keep
public class DependentLibrary {
    public MoJsonRule[] rules;
    public String name;
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

    public static class LibraryClassifierMap extends HashMap<String, LibraryArtifact> {}
}

