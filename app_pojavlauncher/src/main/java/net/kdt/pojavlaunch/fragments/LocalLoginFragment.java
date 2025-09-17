package net.kdt.pojavlaunch.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import git.artdeell.mojo.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.authenticator.accounts.MinecraftAccount;
import net.kdt.pojavlaunch.authenticator.accounts.PojavProfile;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalLoginFragment extends Fragment {
    public static final String TAG = "LOCAL_LOGIN_FRAGMENT";

    private final Pattern mUsernameValidationPattern;
    private EditText mUsernameEditText;

    public LocalLoginFragment(){
        super(R.layout.fragment_local_login);
        mUsernameValidationPattern = Pattern.compile("^[a-zA-Z0-9_]*$");
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mUsernameEditText = view.findViewById(R.id.login_edit_email);
        view.findViewById(R.id.login_button).setOnClickListener(v -> {
            Context context = v.getContext();
            if(!checkEditText()) {
                Tools.dialog(context, context.getString(R.string.local_login_bad_username_title), context.getString(R.string.local_login_bad_username_text));
                return;
            }
            // License nag notification (tells first launch user to buy the game in the future)
            // TODO: show the message only once
            MinecraftAccount profile;
            if((profile = PojavProfile.getCurrentProfileContent(true)) == null || !profile.isMicrosoft){
                Tools.dialog(context, context.getString(R.string.local_login_buy_game_title), context.getString(R.string.local_login_buy_game));
            }
            ExtraCore.setValue(ExtraConstants.MOJANG_LOGIN_TODO, new String[]{
                    mUsernameEditText.getText().toString(), "" });
            Tools.swapFragment(requireActivity(), MainMenuFragment.class, MainMenuFragment.TAG, null);
        });
    }


    /** @return Whether the mail (and password) text are eligible to make an auth request  */
    private boolean checkEditText(){

        String text = mUsernameEditText.getText().toString();

        Matcher matcher = mUsernameValidationPattern.matcher(text);
        return !(text.isEmpty()
                || text.length() < 3
                || text.length() > 16
                || !matcher.find()
                || new File(Tools.DIR_ACCOUNT_NEW + "/" + text + ".json").exists()
        );
    }
}
