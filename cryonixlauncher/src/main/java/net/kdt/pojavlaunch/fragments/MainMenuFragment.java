package net.kdt.pojavlaunch.fragments;

import static net.kdt.pojavlaunch.Tools.openPath;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.Slide;
import androidx.transition.TransitionManager;

import com.kdt.mcgui.mcVersionSpinner;

import git.artdeell.mojo.R;
import net.kdt.pojavlaunch.CustomControlsActivity;
import net.kdt.pojavlaunch.CursorCustomizationDialog;
import net.kdt.pojavlaunch.LauncherActivity;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.contracts.OpenDocumentWithExtension;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.instances.DisplayInstance;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.instances.InstanceIconProvider;
import net.kdt.pojavlaunch.instances.Instances;
import net.kdt.pojavlaunch.prefs.screens.LauncherPreferenceFragment;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.utils.FileUtils;

import java.io.File;
import java.util.List;

public class MainMenuFragment extends Fragment {
    public static final String TAG = "MainMenuFragment";

    private mcVersionSpinner mVersionSpinner;
    private RecyclerView mInstancesList;
    private InstanceAdapter mAdapter;

    private final ActivityResultLauncher<Object> mModInstallerLauncher =
            registerForActivityResult(new OpenDocumentWithExtension("jar"), (data) -> {
                if (data != null) Tools.launchModInstaller(requireContext(), data);
            });

    public MainMenuFragment() {
        super(R.layout.fragment_launcher);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Button mNewsButton = view.findViewById(R.id.news_button);
        Button mDiscordButton = view.findViewById(R.id.social_media_button);
        View mCustomControlButton = view.findViewById(R.id.custom_control_button);
        View mInstallJarButton = view.findViewById(R.id.install_jar_button);
        View mAiAssistButton = view.findViewById(R.id.sidebar_ai_assist);
        View mCursorButton = view.findViewById(R.id.sidebar_mouse);
        View mShareLogsButton = view.findViewById(R.id.share_logs_button);
        View mOpenDirectoryButton = view.findViewById(R.id.open_files_button);

        ImageButton mEditProfileButton = view.findViewById(R.id.edit_profile_button);
        Button mPlayButton = view.findViewById(R.id.play_button);
        mVersionSpinner = view.findViewById(R.id.mc_version_spinner);

        // Entrance motion animations
        animateEntrance(view);

        // Click listeners with jelly motion
        if (mNewsButton != null) {
            applyJellyTouch(mNewsButton);
            mNewsButton.setOnClickListener(v -> Tools.openURL(requireActivity(), Tools.URL_HOME));
            mNewsButton.setOnLongClickListener((v) -> {
                Tools.swapFragment(requireActivity(), GamepadMapperFragment.class, GamepadMapperFragment.TAG, null);
                return true;
            });
        }

        if (mDiscordButton != null) {
            applyJellyTouch(mDiscordButton);
            mDiscordButton.setOnClickListener(v -> Tools.openURL(requireActivity(), getString(R.string.social_media_invite)));
        }

        if (mCustomControlButton != null) {
            applyJellyTouch(mCustomControlButton);
            mCustomControlButton.setOnClickListener(v -> {
                Tools.jellyClick(v);
                startActivity(new Intent(requireContext(), CustomControlsActivity.class));
            });
        }

        if (mInstallJarButton != null) {
            applyJellyTouch(mInstallJarButton);
            mInstallJarButton.setOnClickListener(v -> {
                Tools.jellyClick(v);
                runInstallerWithConfirmation();
            });
        }

        if (mAiAssistButton != null) {
            applyJellyTouch(mAiAssistButton);
            mAiAssistButton.setOnClickListener(v -> {
                Tools.jellyClick(v);
                Tools.swapFragment(requireActivity(), AiAssistFragment.class, AiAssistFragment.TAG, null);
            });
        }

        if (mCursorButton != null) {
            applyJellyTouch(mCursorButton);
            mCursorButton.setOnClickListener(v -> {
                Tools.jellyClick(v);
                CursorCustomizationDialog.show(requireContext());
            });
        }

        View sidebarHome = view.findViewById(R.id.sidebar_home);
        if (sidebarHome != null) {
            applyJellyTouch(sidebarHome);
            sidebarHome.setOnClickListener(v -> {
                Tools.jellyClick(v);
                Tools.backToMainMenu(requireActivity());
            });
        }

        View sidebarSettings = view.findViewById(R.id.sidebar_settings);
        if (sidebarSettings != null) {
            applyJellyTouch(sidebarSettings);
            sidebarSettings.setOnClickListener(v -> {
                if (!(requireActivity().getSupportFragmentManager().findFragmentById(R.id.container_fragment) instanceof LauncherPreferenceFragment)) {
                    Tools.jellyClick(v);
                    Tools.swapFragment(requireActivity(), LauncherPreferenceFragment.class, LauncherActivity.SETTING_FRAGMENT_TAG, null);
                }
            });
        }

        View aiAssistCard = view.findViewById(R.id.ai_assist_card);
        if (aiAssistCard != null) {
            applyJellyTouch(aiAssistCard);
            aiAssistCard.setOnClickListener(v -> {
                Tools.jellyClick(v);
                Tools.swapFragment(requireActivity(), AiAssistFragment.class, AiAssistFragment.TAG, null);
            });
        }

        View profileChip = view.findViewById(R.id.profile_chip);
        if (profileChip != null) {
            applyJellyTouch(profileChip);
            profileChip.setOnClickListener(v -> {
                Tools.jellyClick(v);
                if (mEditProfileButton != null && mVersionSpinner != null) {
                    mVersionSpinner.openProfileEditor(requireActivity());
                }
            });
        }

        View accountChip = view.findViewById(R.id.profile_chip);
        if (accountChip != null) {
            accountChip.setOnClickListener(v -> {
                Tools.jellyClick(v);
                ExtraCore.setValue(ExtraConstants.SELECT_AUTH_METHOD, true);
            });
        }

        TextView profileName = view.findViewById(R.id.profile_name);
        if (profileName != null) {
            try {
                net.kdt.pojavlaunch.authenticator.accounts.Account current =
                        net.kdt.pojavlaunch.authenticator.accounts.Accounts.getCurrent();
                if (current != null && current.username != null && !current.username.isEmpty()) {
                    profileName.setText(current.username);
                }
            } catch (Throwable ignored) {
            }
        }

        if (mEditProfileButton != null && mVersionSpinner != null) {
            mEditProfileButton.setOnClickListener(v -> mVersionSpinner.openProfileEditor(requireActivity()));
        }

        if (mPlayButton != null) mPlayButton.setOnClickListener(v -> ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true));
        if (mShareLogsButton != null) mShareLogsButton.setOnClickListener((v) -> Tools.shareLog(requireContext()));
        if (mOpenDirectoryButton != null) mOpenDirectoryButton.setOnClickListener((v) -> openGameDirectory(v.getContext()));

        mInstancesList = view.findViewById(R.id.instances_list);
        if (mInstancesList != null) {
            mInstancesList.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
            reloadInstances();
        }

        Button rightLaunchButton = view.findViewById(R.id.right_launch_button);
        Button sideEditButton = view.findViewById(R.id.side_edit_profile);
        Button sideDuplicateButton = view.findViewById(R.id.side_duplicate);
        Button sideDeleteButton = view.findViewById(R.id.side_delete);
        Button sideOpenFolderButton = view.findViewById(R.id.side_open_folder);
        ImageButton sideMenuButton = view.findViewById(R.id.side_menu_button);

        if (rightLaunchButton != null) {
            applyJellyTouch(rightLaunchButton);
            rightLaunchButton.setOnClickListener(v -> {
                Tools.jellyClick(v);
                ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true);
            });
        }

        if (sideEditButton != null) {
            applyJellyTouch(sideEditButton);
            sideEditButton.setOnClickListener(v -> {
                Tools.jellyClick(v);
                if (Instances.loadSelectedInstance() != null) {
                    Tools.swapFragment(requireActivity(), InstanceEditorFragment.class, InstanceEditorFragment.TAG, null);
                }
            });
        }

        if (sideDuplicateButton != null) {
            applyJellyTouch(sideDuplicateButton);
            sideDuplicateButton.setOnClickListener(v -> {
                Tools.jellyClick(v);
                if (Instances.loadSelectedInstance() != null) {
                    Tools.swapFragment(requireActivity(), InstanceEditorFragment.class, InstanceEditorFragment.TAG, null);
                }
            });
        }

        if (sideDeleteButton != null) {
            applyJellyTouch(sideDeleteButton);
            sideDeleteButton.setOnClickListener(v -> {
                Tools.jellyClick(v);
                confirmDeleteSelectedInstance();
            });
        }

        if (sideOpenFolderButton != null) {
            applyJellyTouch(sideOpenFolderButton);
            sideOpenFolderButton.setOnClickListener(v -> {
                Tools.jellyClick(v);
                openGameDirectory(v.getContext());
            });
        }

        if (sideMenuButton != null) {
            applyJellyTouch(sideMenuButton);
            sideMenuButton.setOnClickListener(v -> {
                Tools.jellyClick(v);
                if (Instances.loadSelectedInstance() != null) {
                    Tools.swapFragment(requireActivity(), InstanceEditorFragment.class, InstanceEditorFragment.TAG, null);
                }
            });
        }

        // Chevron navigation for instances
        ImageButton btnPrev = view.findViewById(R.id.btn_prev_instance);
        ImageButton btnNext = view.findViewById(R.id.btn_next_instance);
        if (btnPrev != null) {
            applyJellyTouch(btnPrev);
            btnPrev.setOnClickListener(v -> navigateInstance(-1));
        }
        if (btnNext != null) {
            applyJellyTouch(btnNext);
            btnNext.setOnClickListener(v -> navigateInstance(1));
        }

        View instancesPanel = view.findViewById(R.id.instances_panel);
        ImageButton toggleBtn = view.findViewById(R.id.btn_toggle_instances);
        if (toggleBtn != null && instancesPanel != null) {
            toggleBtn.setOnClickListener(v -> {
                Slide slide = new Slide(Gravity.END);
                slide.setDuration(350);
                TransitionManager.beginDelayedTransition((ViewGroup) view, slide);

                boolean isVisible = instancesPanel.getVisibility() == View.VISIBLE;
                instancesPanel.setVisibility(isVisible ? View.GONE : View.VISIBLE);
                toggleBtn.animate().rotation(isVisible ? 180f : 0f).setDuration(350).start();
            });
        }

        ImageButton mAddInstancePanel = view.findViewById(R.id.btn_add_instance_panel);
        if (mAddInstancePanel != null) {
            applyJellyTouch(mAddInstancePanel);
            mAddInstancePanel.setOnClickListener(v -> {
                Tools.jellyClick(v);
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

    private void animateEntrance(@NonNull View root) {
        View sidebar = root.findViewById(R.id.launcher_sidebar);
        View brand = root.findViewById(R.id.brand_block);
        View profileChip = root.findViewById(R.id.profile_chip);
        View mainContent = root.findViewById(R.id.main_content);
        View profilePanel = root.findViewById(R.id.profile_panel);

        if (sidebar != null) {
            sidebar.setTranslationX(-80f);
            sidebar.setAlpha(0f);
            sidebar.animate().translationX(0f).alpha(1f).setDuration(450)
                    .setInterpolator(new OvershootInterpolator(1.1f)).start();
        }
        if (brand != null) {
            brand.setTranslationY(-30f);
            brand.setAlpha(0f);
            brand.animate().translationY(0f).alpha(1f).setDuration(400).start();
        }
        if (profileChip != null) {
            profileChip.setTranslationY(-30f);
            profileChip.setAlpha(0f);
            profileChip.animate().translationY(0f).alpha(1f).setDuration(400).start();
        }
        if (mainContent != null) {
            mainContent.setScaleX(0.94f);
            mainContent.setScaleY(0.94f);
            mainContent.setAlpha(0f);
            mainContent.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(500)
                    .setInterpolator(new OvershootInterpolator(1.05f)).start();
        }
        if (profilePanel != null) {
            profilePanel.setTranslationX(100f);
            profilePanel.setAlpha(0f);
            profilePanel.animate().translationX(0f).alpha(1f).setDuration(500)
                    .setInterpolator(new OvershootInterpolator(1.1f)).start();
        }
    }

    private void applyJellyTouch(View view) {
        if (view == null) return;
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(120).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1.06f).scaleY(1.06f).setDuration(160)
                            .withEndAction(() -> v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start())
                            .start();
                    break;
            }
            return false;
        });
    }

    private void navigateInstance(int offset) {
        try {
            Instances instances = Instances.loadDisplay();
            if (instances.list.isEmpty()) return;
            int newPos = instances.selectedIndex + offset;
            if (newPos < 0) newPos = instances.list.size() - 1;
            if (newPos >= instances.list.size()) newPos = 0;

            DisplayInstance target = instances.list.get(newPos);
            Instances.setSelectedInstance(target);
            reloadInstances();
        } catch (Exception ignored) {
        }
    }

    private void confirmDeleteSelectedInstance() {
        Instance selected = Instances.loadSelectedInstance();
        if (selected == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.instance_delete)
                .setMessage("Are you sure you want to delete '" + selected.name + "'?")
                .setPositiveButton(R.string.global_delete, (dialog, which) -> {
                    try {
                        InstanceIconProvider.dropIcon(selected);
                        Instances.removeInstance(selected);
                        reloadInstances();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                .setNegativeButton(R.string.global_no, null)
                .show();
    }

    private void openGameDirectory(Context context) {
        Instance instance = Instances.loadSelectedInstance();
        if (instance == null) {
            Toast.makeText(context, R.string.no_instance, Toast.LENGTH_LONG).show();
            return;
        }
        File gameDirectory = instance.getGameDirectory();
        if (FileUtils.ensureDirectorySilently(gameDirectory)) {
            openPath(context, gameDirectory, false);
        } else {
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
            if (counter != null) {
                counter.setText((instances.selectedIndex + 1) + "/" + instances.list.size());
            }
            updateSelectedPanel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateSelectedPanel() {
        if (!isAdded() || getView() == null) return;
        View panel = getView().findViewById(R.id.profile_panel);
        TextView label = getView().findViewById(R.id.current_instance_label);
        TextView version = getView().findViewById(R.id.current_instance_version);

        Instance selected = Instances.loadSelectedInstance();

        Runnable updateAction = () -> {
            if (selected == null) {
                if (label != null) label.setText("Select an instance");
                if (version != null) version.setText("—");
                return;
            }
            DisplayInstance display = null;
            try {
                Instances data = Instances.loadDisplay();
                if (data.selectedIndex >= 0 && data.selectedIndex < data.list.size()) {
                    display = data.list.get(data.selectedIndex);
                }
            } catch (Exception ignored) {
            }

            if (display != null) {
                if (label != null) label.setText(display.name);
                if (version != null) version.setText(display.versionId != null ? display.versionId : "—");
            } else if (label != null) {
                label.setText(selected.name);
            }
        };

        if (panel != null) {
            panel.animate().alpha(0.4f).scaleX(0.98f).scaleY(0.98f).setDuration(100).withEndAction(() -> {
                updateAction.run();
                panel.animate().alpha(1.0f).scaleX(1.0f).scaleY(1.0f).setDuration(180).start();
            }).start();
        } else {
            updateAction.run();
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
            applyJellyTouch(v);
            return new InstanceViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            InstanceViewHolder vh = (InstanceViewHolder) holder;
            int bindingPos = vh.getBindingAdapterPosition();
            if (bindingPos == RecyclerView.NO_POSITION || bindingPos >= mList.size()) return;

            DisplayInstance instance = mList.get(bindingPos);
            vh.name.setText(instance.name);
            vh.version.setText(instance.versionId != null ? instance.versionId : "");

            boolean isSelected = bindingPos == mSelectedIndex;
            vh.itemView.setBackgroundResource(isSelected ? R.drawable.launcher_card_border_bg : R.drawable.launcher_card_flat);

            vh.itemView.setOnClickListener(v -> {
                int pos = vh.getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                Tools.jellyClick(v);
                Instances.setSelectedInstance(mList.get(pos));
                mSelectedIndex = pos;
                notifyDataSetChanged();
                updateSelectedPanel();
            });

            if (vh.playButton != null) {
                applyJellyTouch(vh.playButton);
                vh.playButton.setOnClickListener(v -> {
                    int pos = vh.getBindingAdapterPosition();
                    if (pos == RecyclerView.NO_POSITION) return;
                    Tools.jellyClick(v);
                    Instances.setSelectedInstance(mList.get(pos));
                    ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true);
                });
            }

            if (vh.menuButton != null) {
                applyJellyTouch(vh.menuButton);
                vh.menuButton.setOnClickListener(v -> {
                    int pos = vh.getBindingAdapterPosition();
                    if (pos == RecyclerView.NO_POSITION) return;
                    Tools.jellyClick(v);
                    Instances.setSelectedInstance(mList.get(pos));
                    Tools.swapFragment(requireActivity(), InstanceEditorFragment.class, InstanceEditorFragment.TAG, null);
                });
            }
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
