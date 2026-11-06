package git.artdeell.mojo.utils.profiles;

public interface VersionSelectorListener {
    void onVersionSelected(String versionId, boolean isSnapshot);
}
