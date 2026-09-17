package net.kdt.pojavlaunch.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutManager;
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

    public static UUID manageShortcut(Context context,
                                      UUID uuid,
                                      String label,
                                      String shortLabel,
                                      Intent intent,
                                      BitmapDrawable drawable,
                                      ShortcutAction action) {
        uuid = uuid == null ? UUID.randomUUID() : uuid;
        IconCompat icon = IconCompat.createWithBitmap(drawable.getBitmap());
        ShortcutInfoCompat si = new ShortcutInfoCompat.Builder(context, uuid.toString())
                .setIntent(intent)
                .setIcon(icon)
                .setShortLabel(shortLabel)
                .setLongLabel(label)
                .build();
        action.call(si);
        return uuid;
    }
    public static void disableShortcut(Context context, UUID uuid, String message) {
        List<String> shortcuts = List.of(uuid.toString());
        ShortcutManagerCompat.disableShortcuts(context, shortcuts, message);
        // In theory this should remove the shortcut. Yet, it doesn't happen on my device
        ShortcutManagerCompat.removeDynamicShortcuts(context, List.of(uuid.toString()));
    }
}
