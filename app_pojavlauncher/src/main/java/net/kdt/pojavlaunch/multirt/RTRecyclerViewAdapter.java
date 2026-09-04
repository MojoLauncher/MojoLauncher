package net.kdt.pojavlaunch.multirt;

import static net.kdt.pojavlaunch.PojavApplication.sExecutorService;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import net.kdt.pojavlaunch.Architecture;
import net.kdt.pojavlaunch.NewJREUtil;
import git.artdeell.mojo.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.utils.jre.RuntimeSelectionException;

import java.io.IOException;
import java.util.List;

public class RTRecyclerViewAdapter extends RecyclerView.Adapter<RTRecyclerViewAdapter.RTViewHolder> {

    private boolean mIsDeleting = false;
    private Activity mActivity;
    private boolean mIsDownloading = false;

    public void setActivity(Activity activity) {
        mActivity = activity;
    }

    @NonNull
    @Override
    public RTViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View recyclableView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_multirt_runtime,parent,false);
        return new RTViewHolder(recyclableView);
    }

    @Override
    public void onBindViewHolder(@NonNull RTViewHolder holder, int position) {
        final List<Runtime> installedRuntimes = MultiRTUtils.getRuntimes();
        final List<NewJREUtil.ExternalRuntime> downloadableRuntimes = MultiRTUtils.getRuntimesToDownload();

        if (position < installedRuntimes.size()) {
            holder.bindRuntime(installedRuntimes.get(position), position);
        } else if (position < installedRuntimes.size() + downloadableRuntimes.size()) {
            int downloadPos = position - installedRuntimes.size();
            holder.bindDownloadableRuntime(downloadableRuntimes.get(downloadPos), position);
        }
    }

    @Override
    public int getItemCount() {
        return MultiRTUtils.getRuntimes().size() + MultiRTUtils.getRuntimesToDownload().size();
    }

    public boolean isDefaultRuntime(Runtime rt) {
        return LauncherPreferences.PREF_DEFAULT_RUNTIME.equals(rt.name);
    }

    @SuppressLint("NotifyDataSetChanged") //not a problem, given the typical size of the list
    public void setDefault(Runtime rt){
        LauncherPreferences.PREF_DEFAULT_RUNTIME = rt.name;
        LauncherPreferences.DEFAULT_PREF.edit().putString("defaultRuntime",LauncherPreferences.PREF_DEFAULT_RUNTIME).apply();
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged") //not a problem, given the typical size of the list
    public void setIsEditing(boolean isEditing) {
        mIsDeleting = isEditing;
        notifyDataSetChanged();
    }

    public boolean getIsEditing(){
        return mIsDeleting;
    }


    public class RTViewHolder extends RecyclerView.ViewHolder {
        final TextView mJavaVersionTextView;
        final TextView mFullJavaVersionTextView;
        final ColorStateList mDefaultColors;
        final Button mSetDefaultButton;
        final ImageButton mDeleteButton;
        final Context mContext;
        Runtime mCurrentRuntime;
        NewJREUtil.ExternalRuntime mCurrentDownloadableRuntime;
        int mCurrentPosition;

        public RTViewHolder(View itemView) {
            super(itemView);
            mJavaVersionTextView = itemView.findViewById(R.id.multirt_view_java_version);
            mFullJavaVersionTextView = itemView.findViewById(R.id.multirt_view_java_version_full);
            mSetDefaultButton = itemView.findViewById(R.id.multirt_view_setdefaultbtn);
            mDeleteButton = itemView.findViewById(R.id.multirt_view_removebtn);

            mDefaultColors =  mFullJavaVersionTextView.getTextColors();
            mContext = itemView.getContext();

            setupOnClickListeners();
        }

        @SuppressLint("NotifyDataSetChanged") // same as all the other ones
        private void setupOnClickListeners(){
            mSetDefaultButton.setOnClickListener(v -> {
                if(mCurrentRuntime != null) {
                    setDefault(mCurrentRuntime);
                    RTRecyclerViewAdapter.this.notifyDataSetChanged();
                } else if(mCurrentDownloadableRuntime != null) {
                    downloadRuntime(mCurrentDownloadableRuntime);
                }
            });

            mDeleteButton.setOnClickListener(v -> {
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

            });
        }

        @SuppressLint("NotifyDataSetChanged")
        private void downloadRuntime(NewJREUtil.ExternalRuntime runtime) {
            if(mActivity == null) return;
            if(mIsDownloading) return;

            mIsDownloading = true;
            mSetDefaultButton.setEnabled(false);
            mSetDefaultButton.setText(R.string.global_installing);
            runtime.isDownloading = true;

            sExecutorService.execute(() -> {
                try {
                    AssetManager assetManager = mActivity.getAssets();
                    runtime.downloadRuntime(assetManager);

                    mSetDefaultButton.post(() -> {
                        runtime.isDownloading = false;
                        mIsDownloading = false;
                        notifyDataSetChanged();
                    });
                } catch (RuntimeSelectionException e) {
                    Tools.showError(mActivity, e);
                    mSetDefaultButton.post(() -> {
                        runtime.isDownloading = false;
                        mIsDownloading = false;
                        notifyDataSetChanged();
                    });
                }
            });
        }

        public void bindRuntime(Runtime runtime, int pos) {
            mCurrentRuntime = runtime;
            mCurrentDownloadableRuntime = null;
            mCurrentPosition = pos;

            if(runtime.versionString != null && Tools.DEVICE_ARCHITECTURE == Architecture.archAsInt(runtime.arch)) {
                mJavaVersionTextView.setText(runtime.name
                        .replace(".tar.xz", "")
                        .replace("-", " "));
                mFullJavaVersionTextView.setText(runtime.versionString);
                mFullJavaVersionTextView.setTextColor(mDefaultColors);

                updateButtonsVisibility();

                boolean defaultRuntime = isDefaultRuntime(runtime);
                mSetDefaultButton.setEnabled(!defaultRuntime);
                mSetDefaultButton.setText(defaultRuntime ? R.string.multirt_config_setdefault_already:R.string.multirt_config_setdefault);
                return;
            }

            // Problematic runtime moment, force propose deletion
            mDeleteButton.setVisibility(View.VISIBLE);
            if(runtime.versionString == null){
                mFullJavaVersionTextView.setText(R.string.multirt_runtime_corrupt);
            }else{
                mFullJavaVersionTextView.setText(mContext.getString(R.string.multirt_runtime_incompatiblearch, runtime.arch));
            }
            mJavaVersionTextView.setText(runtime.name);
            mFullJavaVersionTextView.setTextColor(Color.RED);
            mSetDefaultButton.setVisibility(View.GONE);
        }

        public void bindDownloadableRuntime(NewJREUtil.ExternalRuntime runtime, int pos) {
            mCurrentRuntime = null;
            mCurrentDownloadableRuntime = runtime;
            mCurrentPosition = pos;

            mJavaVersionTextView.setText(runtime.name
                    .replace(".tar.xz", "")
                    .replace("-", " "));

            mFullJavaVersionTextView.setText(R.string.global_not_installed);
            mFullJavaVersionTextView.setTextColor(mDefaultColors);

            mSetDefaultButton.setVisibility(View.VISIBLE);
            mDeleteButton.setVisibility(View.GONE);

            if (mIsDownloading || runtime.isDownloading) {
                mSetDefaultButton.setEnabled(false);
                mSetDefaultButton.setText(R.string.global_installing);
                mSetDefaultButton.setAlpha(0.5f);
            } else {
                mSetDefaultButton.setEnabled(true);
                mSetDefaultButton.setText(R.string.global_download);
                mSetDefaultButton.setAlpha(1.0f);
            }
        }

        private void updateButtonsVisibility(){
            mSetDefaultButton.setVisibility(mIsDeleting ? View.GONE : View.VISIBLE);
            mDeleteButton.setVisibility(mIsDeleting ? View.VISIBLE : View.GONE);
        }
    }
}
