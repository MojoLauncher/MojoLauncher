package net.kdt.pojavlaunch.authenticator.accounts;

import android.util.Log;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.authenticator.AuthType;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.utils.JSONUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class PojavProfile {
	private static final String PROFILE_PREF_FILE = "selected_account_file";

	private static final ArrayList<MinecraftAccount> sAccounts = new ArrayList<>();
	private static MinecraftAccount sSelectedAccount;

	private static void clear() {
		sAccounts.clear();
		sSelectedAccount = null;
	}

	private static void reload()  {
		File[] accountFiles = new File(Tools.DIR_ACCOUNT_NEW).listFiles();
		if(accountFiles == null) return;
		clear();
		String selectedAccount = getSelectedAccount();
		sAccounts.ensureCapacity(accountFiles.length);
		for(File accFile : accountFiles) {
			MinecraftAccount account = loadAccount(accFile);
			if(account == null) continue;
			sAccounts.add(account);
			if(accFile.getName().equals(selectedAccount)) {
				sSelectedAccount = account;
			}
		}
		if(sSelectedAccount == null && !sAccounts.isEmpty()) {
			sSelectedAccount = sAccounts.get(0);
		}
		sAccounts.trimToSize();
	}

	private static MinecraftAccount loadAccount(File source) {
		MinecraftAccount acc;
		try {
			acc = JSONUtils.readFromFile(source, MinecraftAccount.class);
		}catch (Exception e) {
			Log.w("PojavProfile", "Failed to load account", e);
			return null;
		}
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

	public static List<MinecraftAccount> reloadAccounts() {
		clear();
		return getAccounts();
	}

	public static List<MinecraftAccount> getAccounts() {
		if(sAccounts.isEmpty()) reload();
		return Collections.unmodifiableList(sAccounts);
	}

	private static String getSelectedAccount() {
		return LauncherPreferences.DEFAULT_PREF.getString(PROFILE_PREF_FILE, "");
	}

    public static MinecraftAccount getCurrentProfileContent(boolean independent) {
		if(sSelectedAccount != null) return sSelectedAccount;
		if(independent) {
			String selectedAccount = getSelectedAccount();
			return loadAccount(new File(Tools.DIR_ACCOUNT_NEW, selectedAccount));
		}else {
			if(sAccounts.isEmpty()) reload();
			return sSelectedAccount;
		}
    }

	private static File pickProfilePath() {
		File profilePath;
		do {
			String profileName = UUID.randomUUID().toString();
			profilePath = new File(Tools.DIR_ACCOUNT_NEW, profileName);
		} while(profilePath.exists());
		return profilePath;
	}

	public static MinecraftAccount createAccount(Setter setter) throws IOException {
		MinecraftAccount minecraftAccount = new MinecraftAccount();
		setter.writeAccount(minecraftAccount);
		minecraftAccount.mSaveLocation = pickProfilePath();
		minecraftAccount.save();
		sAccounts.add(minecraftAccount);
		return minecraftAccount;
	}

	public static void setCurrentProfile(MinecraftAccount minecraftAccount) {
		sSelectedAccount = minecraftAccount;
		LauncherPreferences.DEFAULT_PREF
				.edit().putString(PROFILE_PREF_FILE, minecraftAccount.mSaveLocation.getName())
				.apply();
	}

	public static void deleteProfile(MinecraftAccount minecraftAccount) {
		sAccounts.remove(minecraftAccount);
		if(sSelectedAccount == minecraftAccount) {
			sSelectedAccount = null;
		}
		boolean ignored = minecraftAccount.mSaveLocation.delete();
	}

	public interface Setter {
		void writeAccount(MinecraftAccount minecraftAccount) throws IOException;
	}
}
