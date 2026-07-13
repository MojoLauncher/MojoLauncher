package net.kdt.pojavlaunch.authenticator.accounts;

import android.util.Log;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.authenticator.AuthType;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.utils.FileUtils;
import net.kdt.pojavlaunch.utils.JSONUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Accounts {
	private static final String PROFILE_PREF_FILE = "selected_account_file";

	public final List<Account> accounts;
	public final int selectionIndex;

	private Accounts(List<Account> accounts, int selectionIndex) {
        this.accounts = accounts;
        this.selectionIndex = selectionIndex;
    }

	public static Accounts load() throws IOException {
		File accountsDir = new File(Tools.DIR_ACCOUNT_NEW);
        synchronized (Accounts.class) {
            FileUtils.ensureDirectory(accountsDir);
        }
		File[] accountFiles = accountsDir.listFiles();
		if(accountFiles == null) throw new IOException("Failed to create account directory");
		String selectedAccount = getSelectedAccount();
		ArrayList<Account> accounts = new ArrayList<>(accountFiles.length);
		int selectedAccountIdx = 0;
		for(File accFile : accountFiles) {
			Account account = loadAccount(accFile);
			if(account == null) continue;
			accounts.add(account);
			if(accFile.getName().equals(selectedAccount)) {
				selectedAccountIdx = accounts.size() - 1;
			}
		}
		accounts.trimToSize();
		return new Accounts(Collections.unmodifiableList(accounts), selectedAccountIdx);
	}

	private static Account loadAccount(File source) {
		Account acc;
		try {
			acc = JSONUtils.readFromFile(source, Account.class);
		}catch (Exception e) {
			Log.w("Accounts", "Failed to load account", e);
			return null;
		}
        if(acc == null) return null;
		acc.mSaveLocation = source;

		if (acc.accessToken == null) {
			acc.accessToken = "0";
		}
		if (acc.profileId == null) {
			acc.profileId = "00000000-0000-0000-0000-000000000000";
		}
		if (acc.username == null) {
			acc.username = "0";
		}
		if (acc.refreshToken == null) {
			acc.refreshToken = "0";
		}
		if(acc.authType == null) {
			acc.authType = acc.isMicrosoft ? AuthType.MICROSOFT : AuthType.LOCAL;
		}
		return acc;
	}

	private static String getSelectedAccount() {
		return LauncherPreferences.DEFAULT_PREF.getString(PROFILE_PREF_FILE, "");
	}

    public static Account getCurrent() {
		String selectedAccount = getSelectedAccount();
		return loadAccount(new File(Tools.DIR_ACCOUNT_NEW, selectedAccount));
    }

	private static File pickAccountPath() {
		File profilePath;
		do {
			String profileName = UUID.randomUUID().toString();
			profilePath = new File(Tools.DIR_ACCOUNT_NEW, profileName);
		} while(profilePath.exists());
		return profilePath;
	}

	public static Account create(Setter setter) throws IOException {
		Account account = new Account();
		setter.writeAccount(account);
		account.mSaveLocation = pickAccountPath();
		account.save();
		return account;
	}

	public static void setCurrent(Account account) {
		LauncherPreferences.DEFAULT_PREF
				.edit().putString(PROFILE_PREF_FILE, account.mSaveLocation.getName())
				.apply();
	}

	public static void delete(Account account) {
		boolean ignored = account.mSaveLocation.delete();
	}

	public interface Setter {
		void writeAccount(Account account) throws IOException;
	}
}
