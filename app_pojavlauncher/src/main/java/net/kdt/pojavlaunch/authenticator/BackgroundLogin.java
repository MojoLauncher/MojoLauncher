package net.kdt.pojavlaunch.authenticator;

import androidx.annotation.NonNull;

import net.kdt.pojavlaunch.authenticator.listener.LoginListener;
import net.kdt.pojavlaunch.authenticator.accounts.MinecraftAccount;

public interface BackgroundLogin {
    void createAccount(@NonNull LoginListener loginListener, String code);
    void refreshAccount(@NonNull LoginListener loginListener, MinecraftAccount account);
    interface Creator {
        BackgroundLogin create();
    }
}
