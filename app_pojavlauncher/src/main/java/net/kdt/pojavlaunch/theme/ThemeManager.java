package net.kdt.pojavlaunch.theme;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import git.artdeell.mojo.R;

public class ThemeManager {
    public static final String THEME_REQUEST_INTENT = "git.artdeell.mojo.ACTION_THEME_REQUEST";
    public static final String THEME_RESPONSE_INTENT = "git.artdeell.mojo.ACTION_THEME_RESPONSE";
    public static final String PACKAGE_EXTRA = "git.artdeell.mojo.EXTRA_APP_PACKAGE";

    private final Map<String, ThemeInfo> themeMap = new LinkedHashMap<>(); // linked hash map to maintain order


    public void applyPrefTheme(Context ctx) {
        this.registerBaseThemes();

        ThemeInfo theme = Tools.THEME_MANAGER.getTheme(LauncherPreferences.PREF_SELECT_THEME);
        if(theme != null) theme.applyTheme(ctx);
        else ctx.setTheme(R.style.AppTheme); // fallback
    }

    public @Nullable ThemeInfo getTheme(String name) {
        return this.themeMap.get(name);
    }

    public List<String> getThemes() {
        return new ArrayList<>(this.themeMap.keySet());
    }

    public void registerBaseThemes() {
        this.themeMap.clear();

        ThemeInfo defaultTheme = new ThemeInfo("Default", R.style.AppTheme, ctx -> ctx);
        ThemeInfo pojavTheme = new ThemeInfo("Nostalgia (Pojav)", R.style.PojavTheme, ctx -> ctx);
        ThemeInfo midnightTheme = new ThemeInfo("Midnight", R.style.MidnightTheme, ctx -> ctx);
        ThemeInfo amethystTheme = new ThemeInfo("Amethyst", R.style.AmethystTheme, ctx -> ctx);
        ThemeInfo darkDecayTheme = new ThemeInfo("Dark Decay", R.style.DarkDecayTheme, ctx -> ctx);
        ThemeInfo lightDecayTheme = new ThemeInfo("Light Decay", R.style.LightDecayTheme, ctx -> ctx);

        this.themeMap.put(defaultTheme.getThemeName(), defaultTheme);
        this.themeMap.put(pojavTheme.getThemeName(), pojavTheme);
        this.themeMap.put(midnightTheme.getThemeName(), midnightTheme);
        this.themeMap.put(amethystTheme.getThemeName(), amethystTheme);
        this.themeMap.put(darkDecayTheme.getThemeName(), darkDecayTheme);
        this.themeMap.put(lightDecayTheme.getThemeName(), lightDecayTheme);
    }

    // unused code, may be useful in the future but not currently due to limitations
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void discoverThemePlugins(Context context) {
        CountDownLatch latch = new CountDownLatch(1);

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                if (intent != null && THEME_RESPONSE_INTENT.equals(intent.getAction())) {
                    Bundle extras = intent.getExtras();
                    if (extras != null) {
                        String pkg = extras.getString(PACKAGE_EXTRA);
                        if(pkg != null) {
                            for (String key : extras.keySet()) {
                                Object value = extras.get(key);
                                if (value instanceof Integer) {
                                    themeMap.put(key, new ThemeInfo(key, (Integer) value, (ctx1) -> PackageResourceContext.createContextFromPackage(ctx1, pkg, (Integer) value)));
                                    Log.i("ThemeManager", "Discovered and registered theme " + key + " from " + pkg);
                                }
                            }
                        }
                    }
                    latch.countDown();
                }
            }
        };

        context.registerReceiver(receiver, new IntentFilter(THEME_RESPONSE_INTENT), Context.RECEIVER_EXPORTED);

        Intent requestIntent = new Intent(THEME_REQUEST_INTENT);
        context.sendBroadcast(requestIntent);

        try {
            latch.await(3, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            context.unregisterReceiver(receiver);
        }
    }
}
