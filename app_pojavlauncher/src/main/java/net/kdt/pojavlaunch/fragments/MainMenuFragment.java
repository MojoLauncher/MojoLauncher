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
import androidx.transition.TransitionManager;
import androidx.transition.Slide;
import android.view.Gravity;
import androidx.transition.TransitionManager;
import androidx.transition.Slide;
import android.view.Gravity;

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
        // Sidebar actions are ImageButtons in the new Cryonix home UI.
        View mCustomControlButton = view.findViewById(R.id.custom_control_button);
        View mInstallJarButton = view.findViewById(R.id.install_jar_button);
        View mAiAssistButton = view.findViewById(R.id.sidebar_ai_assist);
        View mShareLogsButton = view.findViewById(R.id.share_logs_button);
        View mOpenDirectoryButton = view.findViewById(R.id.open_files_button);

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
        if (mAiAssistButton != null) mAiAssistButton.setOnClickListener(v -> Tools.swapFragment(requireActivity(), AiAssistFragment.class, AiAssistFragment.TAG, null));
        if (mEditProfileButton != null && mVersionSpinner != null) mEditProfileButton.setOnClickListener(v -> mVersionSpinner.openProfileEditor(requireActivity()));

        if (mPlayButton != null) mPlayButton.setOnClickListener(v -> ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true));

        if (mShareLogsButton != null) mShareLogsButton.setOnClickListener((v) -> Tools.shareLog(requireContext()));

        if (mOpenDirectoryButton != null) mOpenDirectoryButton.setOnClickListener((v)-> openGameDirectory(v.getContext()));

        mInstancesList = view.findViewById(R.id.instances_list);
        if (mInstancesList != null) {
            mInstancesList.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
            reloadInstances();
        }

        Button rightLaunchButton = view.findViewById(R.id.right_launch_button);
        Button sideEditButton = view.findViewById(R.id.side_edit_profile);
        Button sideOpenFolderButton = view.findViewById(R.id.side_open_folder);
        if (rightLaunchButton != null) rightLaunchButton.setOnClickListener(v -> ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true));
        if (sideEditButton != null) sideEditButton.setOnClickListener(v -> {
            if (Instances.loadSelectedInstance() != null) Tools.swapFragment(requireActivity(), InstanceEditorFragment.class, InstanceEditorFragment.TAG, null);
        });
        if (sideOpenFolderButton != null) sideOpenFolderButton.setOnClickListener(v -> openGameDirectory(v.getContext()));

        View instancesPanel = view.findViewById(R.id.instances_panel);
        ImageButton toggleBtn = view.findViewById(R.id.btn_toggle_instances);
        if (toggleBtn != null && instancesPanel != null) {
            toggleBtn.setOnClickListener(v -> {
                Slide slide = new Slide(Gravity.END);
                slide.setDuration(350);
                TransitionManager.beginDelayedTransition((ViewGroup) view, slide);
                
                boolean isVisible = instancesPanel.getVisibility() == View.VISIBLE;
                instancesPanel.setVisibility(isVisible ? View.GONE : View.VISIBLE);
                
                // Rotate toggle button or change icon
                toggleBtn.animate().rotation(isVisible ? 180f : 0f).setDuration(350).start();
            });
        }

        ImageButton mAddInstancePanel = view.findViewById(R.id.btn_add_instance_panel);
        if (mAddInstancePanel != null) {
            mAddInstancePanel.setOnClickListener(v -> {
                Tools.swapFragment(requireActivity(), ProfileTypeSelectFragment.class, ProfileTypeSelectFragment.TAG, null);
            });
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
            View root = getView();
            TextView counter = root == null ? null : root.findViewById(R.id.instance_counter);
            if (counter != null) counter.setText((instances.selectedIndex + 1) + "/" + instances.list.size());
            updateSelectedPanel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateSelectedPanel() {
        if (!isAdded() || getView() == null) return;
        Instance selected = Instances.loadSelectedInstance();
        TextView label = getView().findViewById(R.id.current_instance_label);
        TextView version = getView().findViewById(R.id.current_instance_version);
        if (selected == null) {
            if (label != null) label.setText("Select an instance");
            if (version != null) version.setText("—");
            return;
        }
        DisplayInstance display = null;
        try {
            Instances data = Instances.loadDisplay();
            if (data.selectedIndex >= 0 && data.selectedIndex < data.list.size()) display = data.list.get(data.selectedIndex);
        } catch (Exception ignored) { }
        if (display != null) {
            if (label != null) label.setText(display.name);
            if (version != null) version.setText(display.versionId);
        } else if (label != null) {
            label.setText("Selected instance");
        }
    }

    private void runInstallerWithConfirmation() {
        if (ProgressKeeper.getTaskCount() == 0) {
            mModInstallerLauncher.launch(null);
        } else Toast.makeText(requireContext(), R.string.tasks_ongoing, Toast.LENGTH_LONG).show();
    }

    private class InstanceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<DisplayInstance> mList;
        private int mSelectedIndex;

        InstanceAdapter(List<DisplayInstance> list, int selectedIndex) {
            mList = list;
            mSelectedIndex = selectedIndex;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_instance, parent, false);
            return new InstanceViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
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
                updateSelectedPanel();
            });

            if (vh.playButton != null) vh.playButton.setOnClickListener(v -> {
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
        }

        @Override
        public int getItemCount() {
            return mList.size();
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
}
