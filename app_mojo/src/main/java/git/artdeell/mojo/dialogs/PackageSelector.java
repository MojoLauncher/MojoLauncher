package git.artdeell.mojo.dialogs;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public abstract class PackageSelector {
    protected AlertDialog mDialog;
    protected RecyclerView mDialogView;
    protected EditAdapter mAdapter;

    public void show() {
        this.refresh();
        if(mDialog != null) mDialog.show();
    }
    public void close() {
        if(mDialog != null) mDialog.cancel();
    }
    public void onDismiss(Runnable runnable) {
        if(mDialog != null) mDialog.setOnDismissListener(d -> runnable.run());
    }

    public void prepare(Context context, ActivityResultLauncher<Object> launcher) {
        mDialogView = new RecyclerView(context);
        mDialogView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        PackageAdapter<?> adapter = createAdapter(context, this);
        mDialogView.setAdapter(adapter);
        mAdapter = adapter;

        mDialog = new AlertDialog.Builder(context)
                .setTitle(getTitleRes())
                .setView(mDialogView)
                .setPositiveButton(getPositiveButtonText(), (dialog, which) -> onPositive(context, launcher))
                .setNeutralButton(getNeutralButtonText(), null)
                .create();

        // Custom button behavior without dismiss
        mDialog.setOnShowListener(dialog -> {
            Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEUTRAL);
            button.setOnClickListener(view -> {
                boolean isEditing = !mAdapter.isEditing();
                mAdapter.setEditing(isEditing);
                button.setText(isEditing ? getEditingButtonLabel() : getNeutralButtonText());
            });
        });
    }

    @SuppressLint("NotifyDataSetChanged") //only used to completely refresh the list, it is necessary
    public void refresh() {
        if(mDialogView == null) return;
        RecyclerView.Adapter<?> adapter = mDialogView.getAdapter();
        if(adapter != null) adapter.notifyDataSetChanged();
    }

    protected abstract int getTitleRes();
    protected abstract int getPositiveButtonText();
    protected abstract int getNeutralButtonText();
    protected abstract int getEditingButtonLabel();
    protected abstract PackageAdapter<?> createAdapter(Context context, PackageSelector dialog);
    protected abstract void onPositive(Context context, ActivityResultLauncher<Object> launcher);
}
