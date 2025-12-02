package net.kdt.pojavlaunch;

import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_IGNORE_NOTCH;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import net.kdt.pojavlaunch.utils.LocaleUtils;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleUtils.setLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Tools.THEME_MANAGER.applyPrefTheme(this);
        super.onCreate(savedInstanceState);
        LocaleUtils.setLocale(this);
        Tools.setInsetsMode(this, setFullscreen(), shouldIgnoreNotch());
        Tools.getDisplayMetrics(this);
    }

    /** @return Whether the activity should be set as a fullscreen one */
    public boolean setFullscreen(){
        return true;
    }


    @Override
    public void startActivity(Intent i) {
        super.startActivity(i);
        //new Throwable("StartActivity").printStackTrace();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Tools.checkStorageInteractive(this);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        Tools.setInsetsMode(this, setFullscreen(), shouldIgnoreNotch());
        Tools.getDisplayMetrics(this);
    }

    /** @return Whether or not the notch should be ignored */
    protected boolean shouldIgnoreNotch(){
        return PREF_IGNORE_NOTCH;
    }
}
