package net.kdt.pojavlaunch.utils

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.Build.VERSION
import android.os.LocaleList
import androidx.preference.PreferenceManager
import net.kdt.pojavlaunch.prefs.LauncherPreferences.DEFAULT_PREF
import net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_FORCE_ENGLISH
import java.util.Locale


class LocaleUtils(base: Context?) : ContextWrapper(base) {
    companion object {
        @JvmStatic
        fun setLocale(context: Context): ContextWrapper {
            var context = context
            if (DEFAULT_PREF == null) {
                DEFAULT_PREF = PreferenceManager.getDefaultSharedPreferences(context)
                // Too early to initialize all prefs here, as this is called by PojavApplication
                // before storage checks are done and before the storage paths are initialized.
                // So only initialize PREF_FORCE_ENGLISH for the check below.
                PREF_FORCE_ENGLISH = DEFAULT_PREF?.getBoolean("force_english", false) ?: false
            }

            if (PREF_FORCE_ENGLISH) {
                val resources = context.resources
                val configuration = resources.configuration

                configuration.setLocale(Locale.ENGLISH)
                Locale.setDefault(Locale.ENGLISH)
                if (VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val localeList = LocaleList(Locale.ENGLISH)
                    LocaleList.setDefault(localeList)
                    configuration.setLocales(localeList)
                }

                @Suppress("DEPRECATION")
                resources.updateConfiguration(configuration, resources.displayMetrics)
                if (VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                    context = context.createConfigurationContext(configuration)
                }
            }

            return LocaleUtils(context)
        }
    }
}
