package net.kdt.pojavlaunch.adrenotools.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import git.artdeell.mojo.R;

// TODO: Abstract this as this was based on MultiRT dialog
public class DriverConfigDialog {

    private AlertDialog mDialog;
    private RecyclerView mDialogView;

    public void show(){
        refresh();
        mDialog.show();
    }
    public void close(){
        mDialog.cancel();
    }
    public void onDismiss(Runnable runnable){
        mDialog.setOnDismissListener(dialog -> {
            runnable.run();
        });
    }

    @SuppressLint("NotifyDataSetChanged") //only used to completely refresh the list, it is necessary
    public void refresh() {
        RecyclerView.Adapter<?> adapter = mDialogView.getAdapter();
        if(adapter != null) adapter.notifyDataSetChanged();
    }

    public void prepare(Context activity, ActivityResultLauncher<Object> installDriver) {
        mDialogView = new RecyclerView(activity);
        mDialogView.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false));
        DriverViewAdapter driverViewAdapter = new DriverViewAdapter(this);
        mDialogView.setAdapter(driverViewAdapter);

        mDialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.driver_config_title)
                .setView(mDialogView)
                .setPositiveButton(R.string.driver_config_import, (dialog, which) -> installDriver.launch(null))
                .setNeutralButton(R.string.driver_config_delete, null)
                .create();

        // Custom button behavior without dismiss
        mDialog.setOnShowListener(dialog -> {
            Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEUTRAL);
            button.setOnClickListener(view -> {
                boolean isEditing = !driverViewAdapter.getIsEditing();
                driverViewAdapter.setIsEditing(isEditing);
                button.setText(isEditing ? R.string.driver_config_select : R.string.driver_config_delete);
            });
        });
    }
}
