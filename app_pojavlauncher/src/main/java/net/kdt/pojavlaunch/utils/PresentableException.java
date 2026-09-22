package net.kdt.pojavlaunch.utils;

import android.app.Activity;
import android.content.Context;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.lifecycle.ContextExecutorTask;

import git.artdeell.mojo.R;

public class PresentableException extends Exception implements ContextExecutorTask {
    // Do not change. Android really hates when this value changes for some reason.
    private static final long serialVersionUID = -7482301619612640658L;

    private final int mCauseTitle;
    private final int mCauseSubtitle;
    private final Object[] mSubtileVa;

    public PresentableException(int mCauseSubtitle) {
        this(R.string.global_error, mCauseSubtitle, (Object[]) null);
    }

    public PresentableException(Throwable cause, int mCauseSubtitle) {
        this(cause, R.string.global_error, mCauseSubtitle);
    }

    public PresentableException(int mCauseSubtitle, Object... causeVa) {
        this(R.string.global_error, mCauseSubtitle, causeVa);
    }

    public PresentableException(Throwable cause, int mCauseSubtitle, Object... causeVa) {
        this(cause, R.string.global_error, mCauseSubtitle, causeVa);
    }

    public PresentableException(int mCauseTitle, int mCauseSubtitle) {
        this(mCauseTitle, mCauseSubtitle, (Object[]) null);
    }

    public PresentableException(int mCauseTitle, int mCauseSubtitle, Object... mCauseVa) {
        this.mCauseTitle = mCauseTitle;
        this.mCauseSubtitle = mCauseSubtitle;
        this.mSubtileVa = mCauseVa;
    }

    public PresentableException(Throwable cause, int mCauseTitle, int mCauseSubtitle) {
        this(cause, mCauseTitle, mCauseSubtitle, (Object[]) null);
    }

    public PresentableException(Throwable cause, int mCauseTitle, int mCauseSubtitle, Object... causeVa) {
        super(cause);
        this.mCauseTitle = mCauseTitle;
        this.mCauseSubtitle = mCauseSubtitle;
        this.mSubtileVa = causeVa;
    }

    @Override
    public void executeWithApplication(Context context) {}

    @Override
    public void executeWithActivity(Activity activity) {
        Throwable cause = getCause();
        String subtitle = mSubtileVa != null ?
                activity.getString(mCauseSubtitle, mSubtileVa) : activity.getString(mCauseSubtitle);
        if(cause == null) Tools.dialog(activity, activity.getString(mCauseTitle), subtitle);
        else Tools.showError(activity, mCauseTitle, subtitle, cause);
    }
}
