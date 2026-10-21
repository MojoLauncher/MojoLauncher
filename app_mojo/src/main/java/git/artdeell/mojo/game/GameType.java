package git.artdeell.mojo.game;

// List of managed games we support
public enum GameType {
    MC(true, true),
    VINTAGE_STORY(false, false);

    public final boolean hasAccounts;
    public final boolean downloadMetadata;

    GameType(boolean hasAccounts, boolean downloadMetadata) {
        this.hasAccounts = hasAccounts;
        this.downloadMetadata = downloadMetadata;
    }
}
