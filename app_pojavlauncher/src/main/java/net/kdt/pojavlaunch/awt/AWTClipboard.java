package net.kdt.pojavlaunch.awt;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.game.platform.Platform;

public class AWTClipboard {

    @SuppressWarnings("unused") // Used from native
    public static void queryClipboardString() {
        Tools.runOnUiThread(() -> {
            String text = Platform.getClipboard().getClipboardString();
            nativeClipboardReceived(text, "plain");
        });
    }

    @SuppressWarnings("unused") // Used from native
    public static void putClipboardString(String data) {
        Tools.runOnUiThread(() -> {
            Platform.getClipboard().setClipboardString(data);
        });
    }

    public static native void nativeClipboardReceived(String data, String mimeTypeSub);
}
