package net.kdt.pojavlaunch.fragments;

import static net.kdt.pojavlaunch.Tools.openPath;
import static net.kdt.pojavlaunch.Tools.shareLog;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.kdt.mcgui.mcVersionSpinner;

import net.kdt.pojavlaunch.CustomControlsActivity;
import git.artdeell.mojo.R;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.contracts.OpenDocumentWithExtension;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.instances.Instances;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.utils.FileUtils;

import java.io.File;

public class MainMenuFragment extends Fragment {

    public static final String TAG = "MainMenuFragment";

    private mcVersionSpinner mVersionSpinner;

    private final ActivityResultLauncher<Object> mModInstallerLauncher =
            registerForActivityResult(new OpenDocumentWithExtension("jar"), (data)->{
                if(data != null) Tools.launchModInstaller(requireContext(), data);
            });

    public MainMenuFragment() {
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
        Button mPlayButton = view.findViewById(R.id.play_button);

        ImageButton mEditProfileButton = view.findViewById(R.id.edit_profile_button);

        mVersionSpinner = view.findViewById(R.id.mc_version_spinner);

        setupButtonAnimation(mPlayButton);

        setupCardAnimation(mNewsButton);
        setupCardAnimation(mDiscordButton);
        setupCardAnimation(mCustomControlButton);
        setupCardAnimation(mInstallJarButton);
        setupCardAnimation(mShareLogsButton);
        setupCardAnimation(mOpenDirectoryButton);

        animateEntrance(view);

        mNewsButton.setOnClickListener(v -> {
            buttonPop(v);
            Tools.openURL(requireActivity(), Tools.URL_HOME);
        });

        mDiscordButton.setOnClickListener(v -> {
            buttonPop(v);
            Tools.openURL(requireActivity(), getString(R.string.social_media_invite));
        });

        mCustomControlButton.setOnClickListener(v -> {
            buttonPop(v);
            startActivity(new Intent(requireContext(), CustomControlsActivity.class));
        });

        mInstallJarButton.setOnClickListener(v -> {
            buttonPop(v);
            runInstallerWithConfirmation();
        });

        mEditProfileButton.setOnClickListener(v -> {
            buttonPop(v);
            mVersionSpinner.openProfileEditor(requireActivity());
        });

        mPlayButton.setOnClickListener(v -> {
            buttonLaunchAnimation(v);
            ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true);
        });

        mShareLogsButton.setOnClickListener(v -> {
            buttonPop(v);
            shareLog(requireContext());
        });

        mOpenDirectoryButton.setOnClickListener(v -> {
            buttonPop(v);
            openGameDirectory(v.getContext());
        });

        mNewsButton.setOnLongClickListener(v -> {
            Tools.swapFragment(requireActivity(),
                    GamepadMapperFragment.class,
                    GamepadMapperFragment.TAG,
                    null);
            return true;
        });
    }

    private void animateEntrance(View view) {
        view.setAlpha(0f);
        view.setTranslationY(40f);

        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(700)
                .setInterpolator(new OvershootInterpolator())
                .start();
    }

    private void setupCardAnimation(View view) {
        view.setOnTouchListener((v, event) -> {
            v.animate()
                    .scaleX(0.97f)
                    .scaleY(0.97f)
                    .setDuration(80)
                    .start();

            v.postDelayed(() -> v.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .start(), 100);

            return false;
        });
    }

    private void setupButtonAnimation(View view) {
        ObjectAnimator pulse = ObjectAnimator.ofPropertyValuesHolder(
                view,
                PropertyValuesHolder.ofFloat("scaleX", 1f, 1.03f, 1f),
                PropertyValuesHolder.ofFloat("scaleY", 1f, 1.03f, 1f)
        );

        pulse.setDuration(1800);
        pulse.setRepeatCount(ObjectAnimator.INFINITE);
        pulse.start();
    }

    private void buttonPop(View view) {
        view.animate()
                .scaleX(0.94f)
                .scaleY(0.94f)
                .setDuration(60)
                .withEndAction(() ->
                        view.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start())
                .start();
    }

    private void buttonLaunchAnimation(View view) {
        view.animate()
                .scaleX(1.08f)
                .scaleY(1.08f)
                .setDuration(120)
                .withEndAction(() ->
                        view.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(120)
                                .start())
                .start();
    }

    private void openGameDirectory(Context context) {
        Instance instance = Instances.loadSelectedInstance();

        if(instance == null) {
            Toast.makeText(context,
                    R.string.no_instance,
                    Toast.LENGTH_LONG).show();
            return;
        }

        File gameDirectory = instance.getGameDirectory();

        if(FileUtils.ensureDirectorySilently(gameDirectory)) {
            openPath(context, gameDirectory, false);
        } else {
            Toast.makeText(context,
                    R.string.gamedir_open_failed,
                    Toast.LENGTH_LONG).show();
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
        } else {
            Toast.makeText(requireContext(),
                    R.string.tasks_ongoing,
                    Toast.LENGTH_LONG).show();
        }
    }
}
