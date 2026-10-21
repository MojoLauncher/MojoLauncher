package git.artdeell.mojo.prefs;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.kdt.mcgui.ProgressLayout;

import java.io.IOException;
import java.util.concurrent.Future;

import git.artdeell.mojo.MojoApplication;
import git.artdeell.mojo.R;
import git.artdeell.mojo.Tools;
import git.artdeell.mojo.downloader.Downloader;

public class ClearPartfileCachePreference extends Preference {
    private final Future<?> mSizeCalcFuture;
    public ClearPartfileCachePreference(@NonNull Context context) {
        this(context, null);
    }

    public ClearPartfileCachePreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setSummary(R.string.pref_clean_partfiles_subtitle_wait);
        mSizeCalcFuture = MojoApplication.sExecutorService.submit(this::calculateSizeBackground);
    }


    private void clearCacheBackground() {
        ProgressLayout.setProgress(ProgressLayout.DATA_MIGRATION, 0, R.string.clean_partfiles_wait);
        try {
            Downloader.clearCache();
            calculateSizeBackground();
        } catch (IOException e) {
            Tools.showErrorRemote(e);
        }
        ProgressLayout.clearProgress(ProgressLayout.DATA_MIGRATION);
    }

    private void calculateSizeBackground() {
        double size = Downloader.calculateCacheSizeMegabytes();
        if(size == -1) return;
        Tools.runOnUiThread(()->
                setSummary(getContext().getString(R.string.pref_clean_partfiles_subtitle, size))
        );
    }

    @Override
    public void onDetached() {
        super.onDetached();
        mSizeCalcFuture.cancel(true);
    }

    @Override
    protected void onClick() {
        if(!ProgressLayout.ensureNoOngoingTasks(getContext())) return;
        MojoApplication.sExecutorService.execute(this::clearCacheBackground);
    }
}
