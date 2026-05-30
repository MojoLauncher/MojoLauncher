package net.kdt.pojavlaunch

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build.VERSION
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.prefs.LauncherPreferences.loadPreferences
import net.kdt.pojavlaunch.tasks.AsyncAssetManager.unpackComponents
import net.kdt.pojavlaunch.tasks.AsyncAssetManager.unpackSingleFiles
import net.kdt.pojavlaunch.utils.LocaleUtils.Companion.setLocale

class TestStorageActivity : Activity() {
    private val REQUEST_STORAGE_REQUEST_CODE = 1
    private var mPermissionRequestDialog: AlertDialog? = null
    private var mPermsRequired = false
    private var mPermsDialogShown = false
    override fun onCreate(savedInstanceState: Bundle?) {
        // Load preferences early to get the notch setting
        setLocale(this)
        loadPreferences(this)
        Tools.setInsetsMode(this, true, ignoreNotch = LauncherPreferences.PREF_IGNORE_NOTCH)

        super.onCreate(savedInstanceState)

        mPermsDialogShown = false
        if (VERSION.SDK_INT >= 23 && VERSION.SDK_INT < 29 && !isStorageAllowed(this)) {
            mPermsRequired = true
        } else exit()
    }

    override fun onResume() {
        super.onResume()
        if (!mPermsRequired) return
        if (!mPermsDialogShown) requestStoragePermission()
        else showRerequestDialog()
    }

    override fun onPause() {
        super.onPause()
        if (mPermissionRequestDialog != null) mPermissionRequestDialog!!.dismiss()
    }

    private fun showRerequestDialog() {
        if (mPermissionRequestDialog != null) mPermissionRequestDialog!!.dismiss()
        mPermissionRequestDialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.global_error)
            .setMessage(R.string.toast_permission_denied)
            .setPositiveButton(
                android.R.string.ok,
                DialogInterface.OnClickListener { d: DialogInterface?, i: Int -> requestStoragePermission() })
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_STORAGE_REQUEST_CODE) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mPermsRequired = false
                exit()
            } else {
                mPermsDialogShown = true
                showRerequestDialog()
            }
        }
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf<String>(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ),
            REQUEST_STORAGE_REQUEST_CODE
        )
    }

    private fun exit() {
        if (!Tools.checkStorageRoot(this)) {
            startActivity(Intent(this, MissingStorageActivity::class.java))
            return
        }
        //Initialize constants (implicitly) and preferences after we confirm that we have storage.
        loadPreferences(this)
        unpackComponents(this)
        unpackSingleFiles(this)

        val intent = Intent(this, LauncherActivity::class.java)
        startActivity(intent)
        finish()
    }

    companion object {
        fun isStorageAllowed(context: Context): Boolean {
            //Getting the permission status
            val result1 = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val result2 = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )


            //If permission is granted returning true
            return result1 == PackageManager.PERMISSION_GRANTED &&
                    result2 == PackageManager.PERMISSION_GRANTED
        }
    }
}
