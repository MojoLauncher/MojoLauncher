package git.artdeell.mojo.multirt;

import static git.artdeell.mojo.MojoApplication.sExecutorService;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import git.artdeell.mojo.Architecture;
import git.artdeell.mojo.R;
import git.artdeell.mojo.Tools;
import git.artdeell.mojo.dialogs.PackageSelector;
import git.artdeell.mojo.dialogs.PackageAdapter;
import git.artdeell.mojo.prefs.LauncherPreferences;

import java.io.IOException;
import java.util.List;

public class RTRecyclerViewAdapter extends PackageAdapter<RTRecyclerViewAdapter.RTViewHolder> {
    public RTRecyclerViewAdapter(PackageSelector dialog) { super(dialog); }
    public void onBindViewHolder(@NonNull RTViewHolder holder, int position) {
        final List<Runtime> runtimes = MultiRTUtils.getRuntimes();
        holder.bind(runtimes.get(position),position);
    }
    protected RTViewHolder abstractHolder(View recyclableView) { return new RTViewHolder(recyclableView); }
    public int getItemCount() { return MultiRTUtils.getRuntimes().size(); }
    public boolean isDefaultRuntime(Runtime rt) { return LauncherPreferences.PREF_DEFAULT_RUNTIME.equals(rt.name); }

    @SuppressLint("NotifyDataSetChanged") //not a problem, given the typical size of the list
    public void setDefault(Runtime rt){
        LauncherPreferences.PREF_DEFAULT_RUNTIME = rt.name;
        LauncherPreferences.DEFAULT_PREF.edit().putString("defaultRuntime",LauncherPreferences.PREF_DEFAULT_RUNTIME).apply();
        notifyDataSetChanged();
    }

    public class RTViewHolder extends AbstractHolder<Runtime> implements View.OnClickListener {
        Runtime mCurrentRuntime;
        int mCurrentPosition;

        public RTViewHolder(View itemView) {
            super(itemView);
            setupOnClickListeners();
        }

        @SuppressLint("NotifyDataSetChanged") // same as all the other ones
        private void setupOnClickListeners(){
            mSetDefaultButton.setOnClickListener(v -> {
                if(mCurrentRuntime != null) {
                    setDefault(mCurrentRuntime);
                    RTRecyclerViewAdapter.this.notifyDataSetChanged();
                }
            });
            mDeleteButton.setOnClickListener(this);
        }

        public void bind(Runtime runtime, int pos) {
            mCurrentRuntime = runtime;
            mCurrentPosition = pos;
            if(runtime.versionString != null && Tools.DEVICE_ARCHITECTURE == Architecture.archAsInt(runtime.arch)) {
                mNameTextView.setText(runtime.name
                        .replace(".tar.xz", "")
                        .replace("-", " "));
                mExtraNameTextView.setText(runtime.versionString);
                mExtraNameTextView.setTextColor(mDefaultColors);

                updateButtonsVisibility();

                boolean defaultRuntime = isDefaultRuntime(runtime);
                mSetDefaultButton.setEnabled(!defaultRuntime);
                mSetDefaultButton.setText(defaultRuntime ? R.string.multirt_config_setdefault_already:R.string.multirt_config_setdefault);
                return;
            }

            // Problematic runtime moment, force propose deletion
            mDeleteButton.setVisibility(View.VISIBLE);
            if(runtime.versionString == null){
                mExtraNameTextView.setText(R.string.multirt_runtime_corrupt);
            }else{
                mExtraNameTextView.setText(mContext.getString(R.string.multirt_runtime_incompatiblearch, runtime.arch));
            }
            mNameTextView.setText(runtime.name);
            mExtraNameTextView.setTextColor(Color.RED);
            mSetDefaultButton.setVisibility(View.GONE);
        }

        private void updateButtonsVisibility(){
            mSetDefaultButton.setVisibility(isEditing() ? View.GONE : View.VISIBLE);
            mDeleteButton.setVisibility(isEditing() ? View.VISIBLE : View.GONE);
        }

        @Override
        public void onClick(View v) {
            if (mCurrentRuntime == null) return;

            if(MultiRTUtils.getRuntimes().size() < 2) {
                new AlertDialog.Builder(mContext)
                        .setTitle(R.string.global_error)
                        .setMessage(R.string.multirt_config_removeerror_last)
                        .setPositiveButton(android.R.string.ok,(adapter, which)->adapter.dismiss())
                        .show();
                return;
            }

            sExecutorService.execute(() -> {
                try {
                    MultiRTUtils.removeRuntimeNamed(mCurrentRuntime.name);
                    mDeleteButton.post(() -> {
                        if(getBindingAdapter() != null)
                            getBindingAdapter().notifyDataSetChanged();
                    });

                } catch (IOException e) {
                    Tools.showError(itemView.getContext(), e);
                }
            });
        }
    }
}
