package net.kdt.pojavlaunch.theme;

import android.content.Context;

import androidx.arch.core.util.Function;

public class ThemeInfo {
    private final String themeName;
    private final int themeResource;
    private final Function<Context, Context> contextSupplier;

    public ThemeInfo(String themeName, int themeResource, Function<Context, Context> contextSupplier) {
        this.themeName = themeName;
        this.themeResource = themeResource;
        this.contextSupplier = contextSupplier;
    }

    public String getThemeName() {
        return this.themeName;
    }

    public int getThemeResource() {
        return this.themeResource;
    }

    public void applyTheme(Context ctx) {
        Context resourceContext = contextSupplier.apply(ctx);
        resourceContext.setTheme(this.themeResource);
    }
}
