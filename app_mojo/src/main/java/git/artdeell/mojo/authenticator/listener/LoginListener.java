package git.artdeell.mojo.authenticator.listener;

import git.artdeell.mojo.authenticator.accounts.Account;

public interface LoginListener{
    void onLoginDone(Account account);
    void onLoginError(Throwable errorMessage);
    void onLoginProgress(int step);
    void setMaxLoginProgress(int max);
}
