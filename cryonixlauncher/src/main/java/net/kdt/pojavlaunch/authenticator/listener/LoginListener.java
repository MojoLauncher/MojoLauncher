package net.kdt.pojavlaunch.authenticator.listener;

import net.kdt.pojavlaunch.authenticator.accounts.Account;

public interface LoginListener{
    void onLoginDone(Account account);
    void onLoginError(Throwable errorMessage);
    void onLoginProgress(int step);
    void setMaxLoginProgress(int max);
}
