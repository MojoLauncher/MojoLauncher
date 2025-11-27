package net.kdt.pojavlaunch.theme;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import androidx.appcompat.view.ContextThemeWrapper;

public class PackageResourceContext extends ContextThemeWrapper {
    private final Resources resources;

    private PackageResourceContext(Context context, Resources resources, int theme) {
        super(context, theme);
        this.resources = resources;
    }

    @Override
    public Resources getResources() {
        return this.resources;
    }

    public static PackageResourceContext createContextFromPackage(Context context, String pkg, int theme) {
        try {
            Resources res = context.getPackageManager().getResourcesForApplication(pkg);
            return new PackageResourceContext(context, res, theme);
        } catch (Exception e) {
            Log.e("PkgContext", "Failed to resolve package resources! Returning null", e);
            return null;
        }
    }
}
