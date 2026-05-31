package net.kdt.pojavlaunch

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.customcontrols.LayoutBitmaps
import net.kdt.pojavlaunch.utils.FileUtils.exists
import org.apache.commons.io.IOUtils
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * An activity dedicated to importing control files.
 */
class ImportControlActivity : BaseActivity() {
    private var mUriData: Uri? = null
    private var mHasIntentChanged = true

    @Volatile
    private var mIsFileVerified = false

    private var mEditText: EditText? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Tools.checkStorageInteractive(this)) {
            Tools.initStorageConstants(applicationContext)
        } else {
            // Return early, no initialization needed.
            return
        }

        setContentView(R.layout.activity_import_control)
        mEditText = findViewById<EditText>(R.id.editText_import_control_file_name)
    }

    /**
     * Override the previous loaded intent
     * @param intent the intent used to replace the old one.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        mHasIntentChanged = true
    }

    /**
     * Update all over again if the intent changed.
     */
    override fun onPostResume() {
        super.onPostResume()
        if (!Tools.checkStorageInteractive(this)) {
            // Don't try to read the file as when this check fails, external storage paths
            // are no longer valid (likely unmounted).
            // checkStorageInteractive() will finish this activity for us.
            return
        }
        if (!mHasIntentChanged) return
        mIsFileVerified = false
        this.fetchUriData()
        val uriData = mUriData
        if (uriData == null) {
            finishAndRemoveTask()
            return
        }
        mEditText!!.setText(trimFileName(Tools.getFileName(this, uriData) ?: ""))
        mHasIntentChanged = false

        //Import and verify thread
        //Kill the app if the file isn't valid.
        Thread(Runnable {
            importControlFile()
            if (verify()) mIsFileVerified = true
            else runOnUiThread(Runnable {
                Toast.makeText(
                    this@ImportControlActivity,
                    getText(R.string.import_control_invalid_file),
                    Toast.LENGTH_SHORT
                ).show()
                finishAndRemoveTask()
            })
        }).start()

        //Auto show the keyboard
        Tools.MAIN_HANDLER.postDelayed(Runnable {
            val imm =
                applicationContext.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0)
            mEditText!!.setSelection(mEditText!!.text.length)
        }, 100)
    }

    /**
     * Start the import.
     * @param view the view which called the function
     */
    fun startImport(view: View?) {
        val fileName: String = trimFileName(mEditText!!.text.toString())
        //Step 1 check for suffixes.
        if (!isFileNameValid(fileName)) {
            Toast.makeText(this, getText(R.string.import_control_invalid_name), Toast.LENGTH_SHORT)
                .show()
            return
        }
        if (!mIsFileVerified) {
            Toast.makeText(this, getText(R.string.import_control_verifying_file), Toast.LENGTH_LONG)
                .show()
            return
        }

        File(Tools.CTRLMAP_PATH + "/TMP_IMPORT_FILE.json").renameTo(File(Tools.CTRLMAP_PATH + "/" + fileName + ".json"))
        Toast.makeText(
            applicationContext,
            getText(R.string.import_control_done),
            Toast.LENGTH_SHORT
        ).show()
        finishAndRemoveTask()
    }

    /**
     * Copy a the file from the Intent data with a provided name into the controlmap folder.
     */
    private fun importControlFile() {
        val uriData = mUriData ?: return
        var `is`: InputStream? = null
        try {
            `is` = contentResolver.openInputStream(uriData)
            val os: OutputStream =
                FileOutputStream(Tools.CTRLMAP_PATH + "/" + "TMP_IMPORT_FILE" + ".json")
            IOUtils.copy(`is`, os)

            os.close()
            `is`!!.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            IOUtils.closeQuietly(`is`)
        }
    }

    /**
     * Tries to get an Uri from the various sources
     */
    private fun fetchUriData() {
        mUriData = intent.data
        if (mUriData != null) return
        try {
            mUriData = intent.clipData!!.getItemAt(0).uri
        } catch (ignored: Exception) {
        }
    }

    companion object {
        /**
         * Tell if the clean version of the filename is valid.
         * @param fileName the string to test
         * @return whether the filename is valid
         */
        private fun isFileNameValid(fileName: String): Boolean {
            val trimmedName = trimFileName(fileName)

            if (trimmedName.isEmpty()) return false
            return !exists(Tools.CTRLMAP_PATH + "/" + trimmedName + ".json")
        }

        /**
         * Remove or undesirable chars from the string
         * @param fileName The string to trim
         * @return The trimmed string
         */
        private fun trimFileName(fileName: String): String {
            return fileName
                .replace(".json", "")
                .replace("%..".toRegex(), "/")
                .replace("/", "")
                .replace("\\", "")
                .trim { it <= ' ' }
        }

        /**
         * Verify if the control file is valid
         * @return Whether the control file is valid
         */
        private fun verify(): Boolean {
            try {
                val layout = LayoutBitmaps.load(File(Tools.CTRLMAP_PATH, "TMP_IMPORT_FILE.json"))
                val layoutJobj = JSONObject(layout.mControlsJson!!)
                return layoutJobj.has("version") && layoutJobj.has("mControlDataList")
            } catch (e: IOException) {
                Log.w("ImportControlActivity", "Failed to validate layout", e)
                return false
            } catch (e: JSONException) {
                Log.w("ImportControlActivity", "Failed to validate layout", e)
                return false
            }
        }
    }
}
