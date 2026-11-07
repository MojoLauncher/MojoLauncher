package git.artdeell.mojo.vulkan.ui;

import android.content.Context;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;

import git.artdeell.mojo.R;
import git.artdeell.mojo.dialogs.PackageSelector;
import git.artdeell.mojo.dialogs.PackageAdapter;

public class DriverConfigDialog extends PackageSelector {

    @Override
    public void refresh() {
        ((DriverViewAdapter) mAdapter).refreshList();
        super.refresh();
    }

    protected int getTitleRes() { return R.string.driver_config_title; }
    protected int getPositiveButtonText() { return R.string.driver_config_import;}
    protected int getNeutralButtonText() { return R.string.driver_config_delete; }

    protected int getEditingButtonLabel() { return R.string.driver_config_select; }

    protected PackageAdapter<?> createAdapter(Context context, PackageSelector dialog) { return new DriverViewAdapter(this); }
    protected void onPositive(Context context, ActivityResultLauncher<Object> launcher) {
        Toast.makeText(context, R.string.driver_config_import_notify, Toast.LENGTH_LONG).show();
        launcher.launch(null);
    }
}