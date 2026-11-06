package git.artdeell.mojo.authenticator;

import androidx.annotation.NonNull;

import git.artdeell.mojo.authenticator.listener.LoginListener;
import git.artdeell.mojo.authenticator.accounts.Account;

public interface BackgroundLogin {
    void createAccount(@NonNull LoginListener loginListener, String code);
    void refreshAccount(@NonNull LoginListener loginListener, Account account);
    interface Creator {
        BackgroundLogin create();
    }
}
