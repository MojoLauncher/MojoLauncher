package git.artdeell.mojo.fragments;

import static git.artdeell.mojo.Tools.openPath;
import static git.artdeell.mojo.Tools.shareLog;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.kdt.mcgui.mcVersionSpinner;

import git.artdeell.mojo.CustomControlsActivity;
import git.artdeell.mojo.R;

import git.artdeell.mojo.Tools;
import git.artdeell.mojo.contracts.OpenDocumentWithExtension;
import git.artdeell.mojo.extra.ExtraConstants;
import git.artdeell.mojo.extra.ExtraCore;
import git.artdeell.mojo.instances.Instance;
import git.artdeell.mojo.instances.Instances;
import git.artdeell.mojo.progresskeeper.ProgressKeeper;
import git.artdeell.mojo.utils.BannerManager;
import git.artdeell.mojo.utils.FileUtils;

import java.io.File;

public class MainMenuFragment extends Fragment {
    public static final String TAG = "MainMenuFragment";

    private mcVersionSpinner mVersionSpinner;

    private final ActivityResultLauncher<Object> mModInstallerLauncher =
            registerForActivityResult(new OpenDocumentWithExtension("jar"), (data)->{
                if(data != null) Tools.launchModInstaller(requireContext(), data);
            });

    private ImageView mBannerView;
    private View mBannerTitle;

    private final ActivityResultLauncher<String> mBannerPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), (Uri uri) -> {
                if(uri == null) return;
                try {
                    BannerManager.saveCustom(requireContext(), uri);
                    refreshBanner();
                } catch (Exception e) {
                    Toast.makeText(requireContext(), R.string.fork_banner_error, Toast.LENGTH_LONG).show();
                }
            });

    private void refreshBanner() {
        if(mBannerView == null) return;
        boolean custom = BannerManager.apply(requireContext(), mBannerView);
        if(mBannerTitle != null) BannerManager.setTitleVisible(mBannerTitle, custom);
    }

    public MainMenuFragment(){
        super(R.layout.fragment_launcher);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Button mNewsButton = view.findViewById(R.id.news_button);
        Button mDiscordButton = view.findViewById(R.id.social_media_button);
        Button mModsButton = view.findViewById(R.id.mods_button);
        Button mLanButton = view.findViewById(R.id.lan_button);
        Button mBedrockButton = view.findViewById(R.id.bedrock_button);
        Button mAppearanceButton = view.findViewById(R.id.appearance_button);
        Button mCustomControlButton = view.findViewById(R.id.custom_control_button);
        Button mInstallJarButton = view.findViewById(R.id.install_jar_button);
        Button mShareLogsButton = view.findViewById(R.id.share_logs_button);
        Button mOpenDirectoryButton = view.findViewById(R.id.open_files_button);

        ImageButton mEditProfileButton = view.findViewById(R.id.edit_profile_button);
        Button mPlayButton = view.findViewById(R.id.play_button);
        mPlayButton.setBackground(git.artdeell.mojo.utils.PlayColor.background(requireContext()));
        mPlayButton.setOnLongClickListener(v -> {
            git.artdeell.mojo.utils.PlayColor.choose(requireContext(), () ->
                    mPlayButton.setBackground(git.artdeell.mojo.utils.PlayColor.background(requireContext())));
            return true;
        });
        mBannerView = view.findViewById(R.id.bedrock_banner);
        mBannerTitle = view.findViewById(R.id.bedrock_banner_title);
        refreshBanner();
        mBannerView.setOnClickListener(v -> BannerManager.showChooser(requireContext(),
                () -> mBannerPicker.launch("image/*"), this::refreshBanner));
        mVersionSpinner = view.findViewById(R.id.mc_version_spinner);

        mNewsButton.setOnClickListener(v -> Tools.openURL(requireActivity(), Tools.URL_HOME));
        mDiscordButton.setOnClickListener(v -> Tools.openURL(requireActivity(), getString(R.string.social_media_invite)));
        mModsButton.setOnClickListener(v -> {
            Bundle modsArgs = new Bundle();
            modsArgs.putBoolean(SearchModFragment.ARG_MODPACK, false);
            Tools.swapFragment(requireActivity(), SearchModFragment.class, SearchModFragment.TAG, modsArgs);
        });
        mBedrockButton.setOnClickListener(v -> openBedrock());
        // Skins, Installations and Patch notes are tabs in the header
        ViewGroup tabsContainer = view.findViewById(R.id.launcher_tabs_container);
        if(tabsContainer != null) {
            tabsContainer.addView(git.artdeell.mojo.utils.ForkUi.pageHeader(requireContext(), requireActivity(),
                    git.artdeell.mojo.utils.ForkUi.TAB_PLAY));
        }
        Button javaButton = view.findViewById(R.id.java_button);
        if(javaButton != null) javaButton.setBackgroundColor(0xFF343434);
        mAppearanceButton.setOnClickListener(v -> showAppearanceDialog(mPlayButton));
        mLanButton.setOnClickListener(v -> Tools.swapFragment(requireActivity(), LanFragment.class, LanFragment.TAG, null));
        mCustomControlButton.setOnClickListener(v -> startActivity(new Intent(requireContext(), CustomControlsActivity.class)));
        mInstallJarButton.setOnClickListener(v -> runInstallerWithConfirmation());
        mEditProfileButton.setOnClickListener(v -> mVersionSpinner.openProfileEditor(requireActivity()));

        mPlayButton.setOnClickListener(v -> ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true));

        mShareLogsButton.setOnClickListener((v) -> shareLog(requireContext()));

        mOpenDirectoryButton.setOnClickListener((v)-> openGameDirectory(v.getContext()));


        mNewsButton.setOnLongClickListener((v)->{
            Tools.swapFragment(requireActivity(), GamepadMapperFragment.class, GamepadMapperFragment.TAG, null);
            return true;
        });
    }

    /** Bedrock is a separate app: open it if installed, otherwise its store page. */
    private void openBedrock() {
        Context context = requireContext();
        Intent launch = context.getPackageManager().getLaunchIntentForPackage("com.mojang.minecraftpe");
        try {
            if(launch != null) {
                startActivity(launch);
            } else {
                Toast.makeText(context, R.string.fork_bedrock_missing, Toast.LENGTH_LONG).show();
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.mojang.minecraftpe")));
            }
        } catch (Exception e) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=com.mojang.minecraftpe")));
            } catch (Exception ignored) {
                Toast.makeText(context, R.string.fork_bedrock_missing, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showAppearanceDialog(Button playButton) {
        String[] items = {getString(R.string.fork_banner_pick_title), getString(R.string.fork_play_color_title)};
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.fork_appearance_title)
                .setItems(items, (d, which) -> {
                    if(which == 0) {
                        BannerManager.showChooser(requireContext(), () -> mBannerPicker.launch("image/*"), this::refreshBanner);
                    } else {
                        git.artdeell.mojo.utils.PlayColor.choose(requireContext(), () ->
                                playButton.setBackground(git.artdeell.mojo.utils.PlayColor.background(requireContext())));
                    }
                })
                .show();
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
    }

    private void runInstallerWithConfirmation() {
        if (ProgressKeeper.getTaskCount() == 0) {
            mModInstallerLauncher.launch(null);
        } else Toast.makeText(requireContext(), R.string.tasks_ongoing, Toast.LENGTH_LONG).show();
    }
}
