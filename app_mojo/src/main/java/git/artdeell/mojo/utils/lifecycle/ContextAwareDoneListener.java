package git.artdeell.mojo.utils.lifecycle;


import static git.artdeell.mojo.game.bootstrap.MCRunner.INTENT_LAUNCH_CLASSPATH;
import static git.artdeell.mojo.game.bootstrap.MCRunner.INTENT_LAUNCH_VERSION;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import git.artdeell.mojo.game.GameActivity;
import git.artdeell.mojo.R;
import git.artdeell.mojo.Tools;
import git.artdeell.mojo.progresskeeper.ProgressKeeper;
import git.artdeell.mojo.tasks.mojson.MoJsonExtras;
import git.artdeell.mojo.utils.NotificationUtils;

import java.io.File;

public class ContextAwareDoneListener extends GameStartTask implements MoJsonExtras.DoneListener {
    private final String mErrorString;
    private final String mNormalizedVersionid;
    private File[] classpath;

    public ContextAwareDoneListener(Context baseContext, String versionId) {
        this.mErrorString = baseContext.getString(R.string.mc_download_failed);
        this.mNormalizedVersionid = versionId;
    }

    protected Intent createGameStartIntent(Context context) {
        Intent mainIntent = new Intent(context, GameActivity.class);
        mainIntent.putExtra(INTENT_LAUNCH_VERSION, mNormalizedVersionid);
        mainIntent.putExtra(INTENT_LAUNCH_CLASSPATH, classpath);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return mainIntent;
    }

    @Override
    public void onDownloadDone(File[] classpath) {
        this.classpath = classpath;
        ProgressKeeper.waitUntilDone(()->ContextExecutor.execute(this));
    }

    @Override
    public void onDownloadFailed(Throwable throwable) {
        Tools.showErrorRemote(mErrorString, throwable);
    }
}
