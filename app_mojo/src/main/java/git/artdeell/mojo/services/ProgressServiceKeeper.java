package git.artdeell.mojo.services;

import android.content.Context;

import git.artdeell.mojo.progresskeeper.TaskCountListener;

public class ProgressServiceKeeper implements TaskCountListener {
    private final Context context;
    public ProgressServiceKeeper(Context ctx) {
        this.context = ctx;
    }

    @Override
    public boolean onUpdateTaskCount(int taskCount) {
        if(taskCount > 0) ProgressService.startService(context);
        return false;
    }
}
