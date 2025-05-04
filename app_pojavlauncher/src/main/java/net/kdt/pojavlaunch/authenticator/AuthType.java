package net.kdt.pojavlaunch.authenticator;

import com.google.gson.annotations.SerializedName;

import net.kdt.pojavlaunch.authenticator.impl.ElyByBackgroundLogin;
import net.kdt.pojavlaunch.authenticator.impl.MicrosoftBackgroundLogin;

import git.artdeell.mojo.R;

public enum AuthType {
    @SerializedName("microsoft")
    MICROSOFT(MicrosoftBackgroundLogin.CREATOR, R.drawable.ic_auth_ms, null),
    @SerializedName("elyby")
    ELY_BY(ElyByBackgroundLogin.CREATOR, R.drawable.ic_auth_elyby, "ely.by"),
    @SerializedName("local")
    LOCAL(null, 0, null);

    private final BackgroundLogin.Creator mCreator;
    public final int iconResource;
    public final String injectorUrl;

    AuthType(BackgroundLogin.Creator creator, int iconResource, String injectorUrl) {
        this.mCreator = creator;
        this.iconResource = iconResource;
        this.injectorUrl = injectorUrl;
    }

    public boolean requiresLogin() {
        return mCreator != null;
    }

    public BackgroundLogin createAuth() {
        if(mCreator == null) throw new RuntimeException("This account does not support login");
        return mCreator.create();
    }
}
