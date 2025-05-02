package net.kdt.pojavlaunch.authenticator;

import com.google.gson.annotations.SerializedName;

import net.kdt.pojavlaunch.authenticator.impl.ElyByBackgroundLogin;
import net.kdt.pojavlaunch.authenticator.impl.MicrosoftBackgroundLogin;
import net.kdt.pojavlaunch.authenticator.accounts.MinecraftAccount;

import git.artdeell.mojo.R;

public enum AuthType {
    @SerializedName("microsoft")
    MICROSOFT(MicrosoftBackgroundLogin.CREATOR, R.drawable.ic_auth_ms),
    @SerializedName("elyby")
    ELY_BY(ElyByBackgroundLogin.CREATOR, R.drawable.ic_auth_elyby),
    @SerializedName("local")
    LOCAL(null, 0);

    private final BackgroundLogin.Creator mCreator;
    public final int iconResource;

    AuthType(BackgroundLogin.Creator creator, int iconResource) {
        this.mCreator = creator;
        this.iconResource = iconResource;
    }

    public boolean requiresLogin() {
        return mCreator != null;
    }

    public BackgroundLogin createAuth() {
        if(mCreator == null) throw new RuntimeException("This account does not support login");
        return mCreator.create();
    }
}
