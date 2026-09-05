package net.kdt.pojavlaunch.fragments;

import static net.kdt.pojavlaunch.Tools.openPath;
import static net.kdt.pojavlaunch.Tools.shareLog;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kdt.mcgui.mcVersionSpinner;

import net.kdt.pojavlaunch.CustomControlsActivity;
import git.artdeell.mojo.R;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.contracts.OpenDocumentWithExtension;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.instances.DisplayInstance;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.instances.Instances;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

public class MainMenuFragment extends Fragment {
    public static final String TAG = "MainMenuFragment";

    private mcVersionSpinner mVersionSpinner;
    private RecyclerView mInstancesList;
    private InstanceAdapter mAdapter;

    private final ActivityResultLauncher<Object> mModInstallerLauncher =
            registerForActivityResult(new OpenDocumentWithExtension("jar"), (data)->{
                if(data != null) Tools.launchModInstaller(requireContext(), data);
            });

    public MainMenuFragment(){
        super(R.layout.fragment_launcher);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Button mNewsButton = view.findViewById(R.id.news_button);
        Button mDiscordButton = view.findViewById(R.id.social_media_button);
        Button mCustomControlButton = view.findViewById(R.id.custom_control_button);
        Button mInstallJarButton = view.findViewById(R.id.install_jar_button);
        Button mShareLogsButton = view.findViewById(R.id.share_logs_button);
        Button mOpenDirectoryButton = view.findViewById(R.id.open_files_button);

        ImageButton mEditProfileButton = view.findViewById(R.id.edit_profile_button);
        Button mPlayButton = view.findViewById(R.id.play_button);
        mVersionSpinner = view.findViewById(R.id.mc_version_spinner);

        if (mNewsButton != null) {
            mNewsButton.setOnClickListener(v -> Tools.openURL(requireActivity(), Tools.URL_HOME));
            mNewsButton.setOnLongClickListener((v)->{
                Tools.swapFragment(requireActivity(), GamepadMapperFragment.class, GamepadMapperFragment.TAG, null);
                return true;
            });
        }
        if (mDiscordButton != null) mDiscordButton.setOnClickListener(v -> Tools.openURL(requireActivity(), getString(R.string.social_media_invite)));
        if (mCustomControlButton != null) mCustomControlButton.setOnClickListener(v -> startActivity(new Intent(requireContext(), CustomControlsActivity.class)));
        if (mInstallJarButton != null) mInstallJarButton.setOnClickListener(v -> runInstallerWithConfirmation());
        if (mEditProfileButton != null) mEditProfileButton.setOnClickListener(v -> mVersionSpinner.openProfileEditor(requireActivity()));

        if (mPlayButton != null) mPlayButton.setOnClickListener(v -> ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true));

        if (mShareLogsButton != null) mShareLogsButton.setOnClickListener((v) -> Tools.shareLog(requireContext()));

        if (mOpenDirectoryButton != null) mOpenDirectoryButton.setOnClickListener((v)-> openGameDirectory(v.getContext()));

        mInstancesList = view.findViewById(R.id.instances_list);
        if (mInstancesList != null) {
            mInstancesList.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
            reloadInstances();
        }

        ExtraCore.addExtraListener("TRIGGER_INSTALLER", (key, value) -> {
            if (Boolean.TRUE.equals(value)) {
                runInstallerWithConfirmation();
                ExtraCore.setValue("TRIGGER_INSTALLER", false);
            }
            return false;
        });
    }

    private void openGameDirectory(Context context) {
        Instance instance = Instances.loadSelectedInstance();
        if(instance == null) {
            Toast.makeText(context, R.string.no_instance, Toast.LENGTH_LONG).show();
            return;
        }
        File gameDirectory = instance.getGameDirectory();
        if(FileUtils.ensureDirectorySilently(gameDirectory)) {
            openPath(context, gameDirectory, false);
        }else {
            Toast.makeText(context, R.string.gamedir_open_failed, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ExtraCore.setValue(ExtraConstants.REFRESH_ACCOUNT_SPINNER, true);
        reloadInstances();
    }

    private void reloadInstances() {
        if (mInstancesList == null) return;
        try {
            Instances instances = Instances.loadDisplay();
            mAdapter = new InstanceAdapter(instances.list, instances.selectedIndex);
            mInstancesList.setAdapter(mAdapter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void runInstallerWithConfirmation() {
        if (ProgressKeeper.getTaskCount() == 0) {
            mModInstallerLauncher.launch(null);
        } else Toast.makeText(requireContext(), R.string.tasks_ongoing, Toast.LENGTH_LONG).show();
    }

    private class InstanceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_INSTANCE = 0;
        private static final int TYPE_ADD = 1;

        private final List<DisplayInstance> mList;
        private int mSelectedIndex;

        InstanceAdapter(List<DisplayInstance> list, int selectedIndex) {
            mList = list;
            mSelectedIndex = selectedIndex;
        }

        @Override
        public int getItemViewType(int position) {
            return position < mList.size() ? TYPE_INSTANCE : TYPE_ADD;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_INSTANCE) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_instance, parent, false);
                return new InstanceViewHolder(v);
            } else {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_instance, parent, false);
                return new AddViewHolder(v);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof InstanceViewHolder) {
                InstanceViewHolder vh = (InstanceViewHolder) holder;
                int bindingPos = vh.getBindingAdapterPosition();
                if (bindingPos == RecyclerView.NO_POSITION || bindingPos >= mList.size()) return;
                
                DisplayInstance instance = mList.get(bindingPos);
                vh.name.setText(instance.name);
                vh.version.setText(instance.versionId);
                
                boolean isSelected = bindingPos == mSelectedIndex;
                vh.itemView.setBackgroundResource(isSelected ? R.drawable.launcher_card_border_bg : R.drawable.launcher_card_flat);

                vh.itemView.setOnClickListener(v -> {
                    int pos = vh.getBindingAdapterPosition();
                    if (pos == RecyclerView.NO_POSITION) return;
                    Instances.setSelectedInstance(mList.get(pos));
                    mSelectedIndex = pos;
                    notifyDataSetChanged();
                    
                    // Open Instance Editor (Fabric etc. settings)
                    Tools.swapFragment(requireActivity(), InstanceEditorFragment.class, InstanceEditorFragment.TAG, null);
                });

                vh.playButton.setOnClickListener(v -> {
                    int pos = vh.getBindingAdapterPosition();
                    if (pos == RecyclerView.NO_POSITION) return;
                    Instances.setSelectedInstance(mList.get(pos));
                    ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true);
                });
                
                vh.menuButton.setOnClickListener(v -> {
                    int pos = vh.getBindingAdapterPosition();
                    if (pos == RecyclerView.NO_POSITION) return;
                    Instances.setSelectedInstance(mList.get(pos));
                    Tools.swapFragment(requireActivity(), InstanceEditorFragment.class, InstanceEditorFragment.TAG, null);
                });

            } else if (holder instanceof AddViewHolder) {
                AddViewHolder vh = (AddViewHolder) holder;
                vh.name.setText("+ New Instance");
                vh.name.setTextColor(0xFF3D93FF);
                vh.version.setVisibility(View.GONE);
                vh.playButton.setVisibility(View.GONE);
                vh.menuButton.setVisibility(View.GONE);
                vh.icon.setVisibility(View.GONE);
                vh.itemView.setBackgroundResource(R.drawable.launcher_btn_new_instance);
                
                vh.itemView.setOnClickListener(v -> {
                    // Open Type Selection (Fabric, Forge, etc.)
                    Tools.swapFragment(requireActivity(), ProfileTypeSelectFragment.class, ProfileTypeSelectFragment.TAG, null);
                });
            }
        }

        @Override
        public int getItemCount() {
            return mList.size() + 1;
        }
    }

    private static class InstanceViewHolder extends RecyclerView.ViewHolder {
        TextView name, version;
        ImageView icon;
        ImageButton menuButton, playButton;

        InstanceViewHolder(View v) {
            super(v);
            name = v.findViewById(R.id.instance_name);
            version = v.findViewById(R.id.instance_version);
            icon = v.findViewById(R.id.instance_icon);
            menuButton = v.findViewById(R.id.instance_menu);
            playButton = v.findViewById(R.id.instance_play_small);
        }
    }

    private static class AddViewHolder extends RecyclerView.ViewHolder {
        TextView name, version;
        ImageView icon;
        ImageButton menuButton, playButton;

        AddViewHolder(View v) {
            super(v);
            name = v.findViewById(R.id.instance_name);
            version = v.findViewById(R.id.instance_version);
            icon = v.findViewById(R.id.instance_icon);
            menuButton = v.findViewById(R.id.instance_menu);
            playButton = v.findViewById(R.id.instance_play_small);
        }
    }
}
