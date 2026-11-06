package git.artdeell.mojo;

import android.content.*;
import android.content.res.Configuration;
import android.os.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.*;
import git.artdeell.mojo.utils.*;

import static git.artdeell.mojo.prefs.LauncherPreferences.PREF_IGNORE_NOTCH;

import git.artdeell.mojo.utils.LocaleUtils;

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

    @Override
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
        super.onMultiWindowModeChanged(isInMultiWindowMode);
        onMultiWindowModeChangedInner();
    }

    @Override
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode, @NonNull Configuration newConfig) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig);
        onMultiWindowModeChangedInner();
    }

    private void onMultiWindowModeChangedInner() {
        boolean wantsFullscreen = setFullscreen();
        if(wantsFullscreen) {
            Tools.setInsetsMode(this, true, shouldIgnoreNotch());
        }
    }

    /** @return Whether or not the notch should be ignored */
    protected boolean shouldIgnoreNotch(){
        return PREF_IGNORE_NOTCH;
    }
}
