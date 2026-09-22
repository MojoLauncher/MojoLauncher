package net.kdt.pojavlaunch.downloader;

import java.io.IOException;

public class VerificationException extends IOException {
    public VerificationException(String error) {
        super(error);
    }
}
