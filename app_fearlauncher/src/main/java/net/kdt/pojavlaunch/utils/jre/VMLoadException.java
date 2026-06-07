package net.kdt.pojavlaunch.utils.jre;

import android.content.Context;

import androidx.annotation.NonNull;

import git.artdeell.mojo.R;

public class VMLoadException extends Exception {
    private final int loadStep;
    private final int errorCode;
    public VMLoadException(String errorInfo, int loadStep, int errorCode) {
        super(errorInfo);
        this.loadStep = loadStep;
        this.errorCode = errorCode;
    }

    private static int getLoadStepRes(int loadStep) {
        switch (loadStep) {
            case 0: return R.string.vml_fail_load_runtime;
            case 1: return R.string.vml_fail_create_runtime;
            case 2: return R.string.vml_fail_find_hooks_native;
            case 3: return R.string.vml_fail_find_hooks;
            case 4: return R.string.vml_fail_insert_hooks;
            case 5: return R.string.vml_fail_load_classpath;
            case 6: return R.string.vml_fail_run_main;
            default: return R.string.vml_huh;
        }
    }

    private static int getErrorCodeRes(int errorCode) {
        switch (errorCode) {
            case 0: return R.string.vml_err_ok;
            case -2: return R.string.vml_err_detached;
            case -3: return R.string.vml_err_version;
            case -4: return R.string.vml_err_nomem;
            case -5: return R.string.vml_err_exists;
            case -6: return R.string.vml_err_inval;
            case -1:
            default: return R.string.vml_err_unknown;
        }
    }

    @NonNull
    public String toString(Context context) {
        int loadStepRes = getLoadStepRes(loadStep);
        switch (loadStep) {
            case 0:
                return context.getString(loadStepRes, getMessage());
            case 1:
            case 4:
                return context.getString(loadStepRes, context.getString(getErrorCodeRes(errorCode)));
            default:
                return context.getString(loadStepRes);
        }

    }
}
