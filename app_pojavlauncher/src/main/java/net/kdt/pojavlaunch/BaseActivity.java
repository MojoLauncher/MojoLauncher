package net.kdt.pojavlaunch;

import android.content.*;
import android.os.*;
import androidx.appcompat.app.*;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.utils.*;

import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_IGNORE_NOTCH;

import net.ashmeet.hyperlauncher.R;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleUtils.setLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        applyPendingTransition(true);
    }

    @Override
    public void finish() {
        super.finish();
        applyPendingTransition(false);
    }

    protected void applyPendingTransition(boolean opening) {
        if ("none".equals(LauncherPreferences.PREF_ANIMATION_TYPE)) {
            overridePendingTransition(0, 0);
        } else if ("jelly".equals(LauncherPreferences.PREF_ANIMATION_TYPE)) {
            overridePendingTransition(R.anim.jelly_in, R.anim.jelly_out);
        } else if ("default".equals(LauncherPreferences.PREF_ANIMATION_TYPE)) {
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
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
