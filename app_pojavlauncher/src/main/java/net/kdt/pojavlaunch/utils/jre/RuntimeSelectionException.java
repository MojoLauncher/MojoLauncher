package net.kdt.pojavlaunch.utils.jre;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;

import net.kdt.pojavlaunch.ShowErrorActivity;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.lifecycle.ContextExecutorTask;

import git.artdeell.mojo.R;

public class RuntimeSelectionException extends Exception implements ContextExecutorTask {
    // Do not change. Android really hates when this value changes for some reason.
    private static final long serialVersionUID = -7482301619612640658L;
    public static final int RUNTIME_STATE_INSTALLATION_FAILED = 0;
    public static final int RUNTIME_STATE_SELECTION_FAILED = 1;
    public static final int RUNTIME_STATE_INTERNAL_RUNTIME_MISSING = 2;
    private final int mRuntimeState;
    private final int mRuntimeVersion;

    public RuntimeSelectionException(int mRuntimeState, int mRuntimeRequiredVersion) {
        this.mRuntimeState = mRuntimeState;
        this.mRuntimeVersion = mRuntimeRequiredVersion;
    }

    @Override
    public void executeWithActivity(Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.runtime_error_title);
        int msgString;
        switch (mRuntimeState) {
            case RUNTIME_STATE_INSTALLATION_FAILED: msgString = R.string.runtime_error_install_failed; break;
            case RUNTIME_STATE_INTERNAL_RUNTIME_MISSING: msgString = R.string.runtime_error_missing; break;
            case RUNTIME_STATE_SELECTION_FAILED: msgString = R.string.multirt_nocompatiblert; break;
            default: throw new RuntimeException("Unknown runtime state");
        }
        builder.setMessage(activity.getString(msgString, mRuntimeVersion));
        builder.setPositiveButton(android.R.string.ok, (d,i)->{});
        if(mRuntimeState == RUNTIME_STATE_INSTALLATION_FAILED || getCause() != null) {
            builder.setNegativeButton(R.string.error_show_more, (d, i)->
                    Tools.showError(activity, R.string.runtime_error_title, getCause(), activity instanceof ShowErrorActivity)
            );
        }
        ShowErrorActivity.installRemoteDialogHandling(activity, builder);
        builder.show();
    }

    @Override
    public void executeWithApplication(Context context) {}
}
