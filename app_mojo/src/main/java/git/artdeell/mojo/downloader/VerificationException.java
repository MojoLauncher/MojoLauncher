package git.artdeell.mojo.downloader;

import java.io.IOException;

public class VerificationException extends IOException {
    public VerificationException(String error) {
        super(error);
    }
}
