package net.kdt.pojavlaunch.prefs.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.BaseActivity
import net.kdt.pojavlaunch.LauncherActivity
import net.kdt.pojavlaunch.prefs.LauncherPreferences


/**
 * Preference for the main screen, any sub-screen should inherit this class for consistent behavior,
 * overriding only onCreatePreferences
 */
open class LauncherPreferenceFragment : PreferenceFragmentCompat(),
    OnSharedPreferenceChangeListener {
    @SuppressLint("UseRequireInsteadOfGet")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (LauncherPreferences.PREF_BACKGROUND_PATH == null) {
            val typedValue = TypedValue()
            if (requireContext().theme.resolveAttribute(
                    com.google.android.material.R.attr.colorSurface,
                    typedValue,
                    true
                )
            ) {
                view.setBackgroundColor(typedValue.data)
            } else {
                view.setBackgroundColor(resources.getColor(R.color.background_app, null))
            }
        } else {
            view.setBackgroundColor(Color.TRANSPARENT)
        }
        super.onViewCreated(view, savedInstanceState)

        val listView = listView
        if (listView != null) {
            listView.setPadding(0, 0, 0, 80) // Bottom padding for navigation bar
            listView.setClipToPadding(false)
        }
        setDivider(null)
    }

    override fun onCreatePreferences(b: Bundle?, str: String?) {
        addPreferencesFromResource(R.xml.pref_main)
        setupNotificationRequestPreference()
    }

    private fun setupNotificationRequestPreference() {
        val mRequestNotificationPermissionPreference =
            findPreference<Preference?>("notification_permission_request")
        if (mRequestNotificationPermissionPreference == null) return

        val activity: Activity? = activity
        if (activity is LauncherActivity) {
            mRequestNotificationPermissionPreference.isVisible = !activity.checkForNotificationPermission()
            mRequestNotificationPermissionPreference.setOnPreferenceClickListener {
                activity.askForNotificationPermission {
                    mRequestNotificationPermissionPreference.isVisible = false
                }
                true
            }
        } else {
            mRequestNotificationPermissionPreference.isVisible = false
        }
    }

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    override fun onSharedPreferenceChanged(p: SharedPreferences?, s: String?) {
        context?.let { LauncherPreferences.loadPreferences(it) }
        if (activity is BaseActivity) {
            (activity as BaseActivity).applyCustomBackground()
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is ListPreference) {
            showMaterialListPreferenceDialog(preference)
            return
        }
        if (preference is EditTextPreference) {
            showMaterialEditTextPreferenceDialog(preference)
            return
        }
        super.onDisplayPreferenceDialog(preference)
    }

    private fun showMaterialListPreferenceDialog(preference: ListPreference) {
        val entries = preference.entries
        val entryValues = preference.entryValues
        if (entries == null || entryValues == null) {
            super.onDisplayPreferenceDialog(preference)
            return
        }

        val currentValue = preference.value
        var checkedItem = -1
        if (currentValue != null) {
            for (i in entryValues.indices) {
                if (currentValue.contentEquals(entryValues[i])) {
                    checkedItem = i
                    break
                }
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(preference.title)
            .setSingleChoiceItems(
                entries,
                checkedItem
            ) { dialog, which ->
                val newValue = entryValues[which].toString()
                if (preference.callChangeListener(newValue)) {
                    preference.value = newValue
                }
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showMaterialEditTextPreferenceDialog(preference: EditTextPreference) {
        val view =
            LayoutInflater.from(requireContext()).inflate(R.layout.dialog_material_edit_text, null)
        val editText = view.findViewById<EditText>(R.id.edit_text)
        val textInputLayout = view.findViewById<TextInputLayout>(R.id.edit_text_layout)

        editText.setText(preference.text)
        textInputLayout.hint = preference.title

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(preference.title)
            .setView(view)
            .setPositiveButton(
                android.R.string.ok
            ) { _, _ ->
                val newValue = editText.text.toString()
                if (preference.callChangeListener(newValue)) {
                    preference.text = newValue
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    protected fun requirePreference(key: CharSequence): Preference {
        return findPreference(key) ?: throw IllegalStateException("Preference $key is null")
    }

    protected fun <T : Preference> requirePreference(
        key: CharSequence,
        preferenceClass: Class<T>
    ): T {
        val preference = requirePreference(key)
        if (preferenceClass.isInstance(preference)) return preference as T
        throw IllegalStateException("Preference $key is not an instance of ${preferenceClass.simpleName}")
    }
}
