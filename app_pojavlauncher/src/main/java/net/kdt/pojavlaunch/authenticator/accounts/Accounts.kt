package net.kdt.pojavlaunch.authenticator.accounts

import android.util.Log
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.authenticator.AuthType
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.utils.FileUtils.ensureDirectory
import net.kdt.pojavlaunch.utils.JSONUtils
import java.io.File
import java.io.IOException
import java.util.Collections
import java.util.UUID

class Accounts private constructor(
    val accounts: MutableList<MinecraftAccount?>,
    val selectionIndex: Int
) {
    fun interface Setter {
        @Throws(IOException::class)
        fun writeAccount(minecraftAccount: MinecraftAccount)
    }

    companion object {
        private const val PROFILE_PREF_FILE = "selected_account_file"

        @Throws(IOException::class)
        fun load(): Accounts {
            val accountsDir = File(Tools.DIR_ACCOUNT_NEW!!)
            synchronized(Accounts::class.java) {
                ensureDirectory(accountsDir)
            }
            val accountFiles = accountsDir.listFiles()
            if (accountFiles == null) throw IOException("Failed to create account directory")
            val selectedAccount: String = selectedAccount
            val accounts = ArrayList<MinecraftAccount?>(accountFiles.size)
            var selectedAccountIdx = 0
            for (accFile in accountFiles) {
                val account: MinecraftAccount? = loadAccount(accFile)
                if (account == null) continue
                accounts.add(account)
                if (accFile.name == selectedAccount) {
                    selectedAccountIdx = accounts.size - 1
                }
            }
            accounts.trimToSize()
            return Accounts(
                Collections.unmodifiableList<MinecraftAccount?>(accounts),
                selectedAccountIdx
            )
        }

        private fun loadAccount(source: File?): MinecraftAccount? {
            val acc: MinecraftAccount?
            try {
                acc =
                    JSONUtils.readFromFile(source, MinecraftAccount::class.java)
            } catch (e: Exception) {
                Log.w("Accounts", "Failed to load account", e)
                return null
            }
            if (acc == null) return null
            acc.mSaveLocation = source

            if (acc.accessToken == null) {
                acc.accessToken = "0"
            }
            if (acc.profileId == null) {
                acc.profileId = "00000000-0000-0000-0000-000000000000"
            }
            if (acc.username == null) {
                acc.username = "0"
            }
            if (acc.refreshToken == null) {
                acc.refreshToken = "0"
            }
            if (acc.authType == null) {
                acc.authType = if (acc.isMicrosoft) AuthType.MICROSOFT else AuthType.LOCAL
            }
            return acc
        }

        private val selectedAccount: String
            get() = LauncherPreferences.DEFAULT_PREF?.getString(
                Accounts.PROFILE_PREF_FILE,
                ""
            ) ?: ""

        var current: MinecraftAccount?
            get() {
                val selectedAccount: String = selectedAccount
                if (selectedAccount.isEmpty()) return null
                return loadAccount(
                    File(
                        Tools.DIR_ACCOUNT_NEW!!,
                        selectedAccount
                    )
                )
            }
            set(minecraftAccount) {
                LauncherPreferences.DEFAULT_PREF!!
                    .edit().putString(
                        PROFILE_PREF_FILE,
                        minecraftAccount?.mSaveLocation?.name
                    )
                    .apply()
            }

        private fun pickAccountPath(): File {
            var profilePath: File
            do {
                val profileName = UUID.randomUUID().toString()
                profilePath = File(Tools.DIR_ACCOUNT_NEW!!, profileName)
            } while (profilePath.exists())
            return profilePath
        }

        @Throws(IOException::class)
        fun create(setter: Setter): MinecraftAccount {
            val minecraftAccount = MinecraftAccount()
            setter.writeAccount(minecraftAccount)
            minecraftAccount.mSaveLocation = pickAccountPath()
            minecraftAccount.save()
            return minecraftAccount
        }

        /**
         * Create a new account or update an existing one (dedup) based on [MinecraftAccount.authType]
         * + [MinecraftAccount.profileId]. Falls back to [.create] when the identity fields are absent.
         */
        @Throws(IOException::class)
        fun upsertByProfileId(setter: Setter): MinecraftAccount {
            val candidate = MinecraftAccount()
            setter.writeAccount(candidate)

            if (candidate.authType == null || candidate.profileId == null) {
                return create(setter)
            }

            val profileId = candidate.profileId.trim { it <= ' ' }
            if (profileId.isEmpty() || profileId == "00000000-0000-0000-0000-000000000000") {
                return create(setter)
            }

            val existing: MinecraftAccount? = findAccountByProfileId(candidate.authType!!, profileId)
            if (existing == null) {
                candidate.mSaveLocation = pickAccountPath()
                candidate.save()
                return candidate
            }

            setter.writeAccount(existing)
            existing.save()
            return existing
        }

        /**
         * Create a new account or update an existing one (dedup) based on [MinecraftAccount.authType]
         * + [MinecraftAccount.username]. Intended for LOCAL/offline accounts.
         */
        @Throws(IOException::class)
        fun upsertByUsername(setter: Setter): MinecraftAccount {
            val candidate = MinecraftAccount()
            setter.writeAccount(candidate)

            if (candidate.authType == null || candidate.username == null) {
                return create(setter)
            }

            val username = candidate.username.trim { it <= ' ' }
            if (username.isEmpty() || username == "0") {
                return create(setter)
            }

            val existing: MinecraftAccount? = findAccountByUsername(candidate.authType!!, username)
            if (existing == null) {
                candidate.mSaveLocation = pickAccountPath()
                candidate.save()
                return candidate
            }

            setter.writeAccount(existing)
            existing.save()
            return existing
        }

        @Throws(IOException::class)
        private fun findAccountByProfileId(
            authType: AuthType,
            profileId: String
        ): MinecraftAccount? {
            val accountsDir = File(Tools.DIR_ACCOUNT_NEW!!)
            synchronized(Accounts::class.java) {
                ensureDirectory(accountsDir)
            }
            val accountFiles = accountsDir.listFiles()
            if (accountFiles == null) return null
            for (accFile in accountFiles) {
                val account: MinecraftAccount? = loadAccount(accFile)
                if (account == null) continue
                if (authType != account.authType) continue
                if (profileId == account.profileId) return account
            }
            return null
        }

        @Throws(IOException::class)
        private fun findAccountByUsername(authType: AuthType, username: String): MinecraftAccount? {
            val accountsDir = File(Tools.DIR_ACCOUNT_NEW!!)
            synchronized(Accounts::class.java) {
                ensureDirectory(accountsDir)
            }
            val accountFiles = accountsDir.listFiles()
            if (accountFiles == null) return null
            for (accFile in accountFiles) {
                val account: MinecraftAccount? = loadAccount(accFile)
                if (account == null) continue
                if (authType != account.authType) continue
                if (username.equals(account.username, ignoreCase = true)) return account
            }
            return null
        }

        fun delete(minecraftAccount: MinecraftAccount) {
            val ignored = minecraftAccount.mSaveLocation?.delete()
        }
    }
}
