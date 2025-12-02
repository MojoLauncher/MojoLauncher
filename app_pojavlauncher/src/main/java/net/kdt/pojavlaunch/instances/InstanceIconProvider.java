package net.kdt.pojavlaunch.instances;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import net.kdt.pojavlaunch.Tools;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import git.artdeell.mojo.R;

public class InstanceIconProvider {
    public static final String FALLBACK_ICON_NAME = "default";
    private static final Map<Integer, Drawable> sIconCache = new HashMap<>();
    private static final Map<String, Drawable> sStaticIconCache = new HashMap<>();
    private static final Map<String, Integer> sStaticIcons = new HashMap<>();

    static {
        sStaticIcons.put("default", R.attr.drawableIconMojoFull);
        sStaticIcons.put("fabric", R.attr.drawableIconFabric);
        sStaticIcons.put("quilt", R.attr.drawableIconQuilt);
        sStaticIcons.put("forge", R.attr.drawableIconForge);
        sStaticIcons.put("neoforge", R.attr.drawableIconNeoforge);
    }

    /**
     * Fetch an icon from the cache, or load it if it's not cached.
     * @param theme the Resources object, used for creating drawables
     * @param instance the instance
     * @return an icon drawable
     */
    public static @NonNull Drawable fetchIcon(Resources.Theme theme, @NonNull DisplayInstance instance) {
        int identityHashCode = System.identityHashCode(instance);

        Drawable cachedIcon = sIconCache.get(identityHashCode);
        if(cachedIcon != null) return cachedIcon;

        Drawable instanceIcon = fetchInstanceFileIcon(theme.getResources(), identityHashCode, instance.getInstanceIconLocation());
        if(instanceIcon != null) return instanceIcon;

        return fetchStaticIcon(theme, identityHashCode, instance.icon);
    }

    /**
     * Drop an icon from the icon cache. When dropped, it's Drawable will be re-read from the
     * instance icon file (or re-fetched from the static cache)
     * @param key the instance
     */
    public static void dropIcon(@NonNull Instance key) {
        sIconCache.remove(System.identityHashCode(key));
    }

    private static Drawable fetchInstanceFileIcon(Resources resources, int identityHash, File iconLocation) {
        if(!iconLocation.isFile() || !iconLocation.canRead()) return null;
        Bitmap iconBitmap = BitmapFactory.decodeFile(iconLocation.getAbsolutePath());
        if(iconBitmap == null) return null;
        Drawable iconDrawable = new BitmapDrawable(resources, iconBitmap);
        sIconCache.put(identityHash, iconDrawable);
        return iconDrawable;
    }

    private static Drawable fetchStaticIcon(Resources.Theme theme, int identityHash, String icon) {
        Drawable staticIcon = sStaticIconCache.get(icon);
        if(staticIcon == null) {
            if(icon != null) staticIcon = getStaticIcon(theme, icon);
            if(staticIcon == null) staticIcon = fetchFallbackIcon(theme);
            sStaticIconCache.put(icon, staticIcon);
        }
        sIconCache.put(identityHash, staticIcon);
        return staticIcon;
    }

    private static @NonNull Drawable fetchFallbackIcon(Resources.Theme theme) {
        Drawable fallbackIcon = sStaticIconCache.get(FALLBACK_ICON_NAME);
        if(fallbackIcon == null) {
            fallbackIcon = Objects.requireNonNull(getStaticIcon(theme, FALLBACK_ICON_NAME));
            sStaticIconCache.put(FALLBACK_ICON_NAME, fallbackIcon);
        }
        return fallbackIcon;
    }

    private static Drawable getStaticIcon(Resources.Theme theme, @NonNull String icon) {
        int staticIconResource = getStaticIconResource(icon);
        if(staticIconResource == -1) return null;
        return Tools.getDrawableAttr(theme, staticIconResource);
    }

    private static int getStaticIconResource(String icon) {
        Integer iconResource = sStaticIcons.get(icon);
        if(iconResource == null) return -1;
        return iconResource;
    }

    /**
     * Check whether the icon under the specified name is a static icon available in the provider.
     * @param name static icon name to check
     * @return whether the icon is available or not
     */
    public static boolean hasStaticIcon(String name) {
        return sStaticIcons.containsKey(name);
    }
}
