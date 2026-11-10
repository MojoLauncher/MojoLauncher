package git.artdeell.mojo.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;

import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import java.util.List;
import java.util.UUID;

public class ShortcutUtils {
    public interface ShortcutAction {
        void call(ShortcutInfoCompat si);
    }

    public static boolean isSupported(Context context) {
        return ShortcutManagerCompat.isRequestPinShortcutSupported(context);
    }

    public static String manageShortcut(Context context,
                                      String uuid,
                                      String label,
                                      String shortLabel,
                                      Intent intent,
                                      BitmapDrawable drawable,
                                      ShortcutAction action) {
        uuid = uuid == null ? UUID.randomUUID().toString() : uuid;
        ShortcutInfoCompat si = new ShortcutInfoCompat.Builder(context, uuid.toString())
                .setIntent(intent)
                .setIcon(drawable != null ? IconCompat.createWithBitmap(drawable.getBitmap()) : null)
                .setShortLabel(shortLabel)
                .setLongLabel(label)
                .build();
        action.call(si);
        return uuid;
    }
    public static void disableShortcut(Context context, String uuid, String message) {
        List<String> shortcuts = List.of(uuid);
        ShortcutManagerCompat.disableShortcuts(context, shortcuts, message);
        // In theory this should remove the shortcut. Yet, it doesn't happen on my device
        ShortcutManagerCompat.removeDynamicShortcuts(context, List.of(uuid));
    }
}
