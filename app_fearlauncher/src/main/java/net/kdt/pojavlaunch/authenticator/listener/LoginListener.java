package net.kdt.pojavlaunch.authenticator.listener;

import net.kdt.pojavlaunch.authenticator.accounts.MinecraftAccount;

public interface LoginListener{
    void onLoginDone(MinecraftAccount account);
    void onLoginError(Throwable errorMessage);
    void onLoginProgress(int step);
    void setMaxLoginProgress(int max);
}
