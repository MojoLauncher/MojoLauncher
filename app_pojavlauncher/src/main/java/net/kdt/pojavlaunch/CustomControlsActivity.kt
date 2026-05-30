package net.kdt.pojavlaunch

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.drawerlayout.widget.DrawerLayout
import com.google.gson.JsonSyntaxException
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.Tools.showError
import net.kdt.pojavlaunch.customcontrols.ControlData
import net.kdt.pojavlaunch.customcontrols.ControlDrawerData
import net.kdt.pojavlaunch.customcontrols.ControlJoystickData
import net.kdt.pojavlaunch.customcontrols.ControlLayout
import net.kdt.pojavlaunch.customcontrols.EditorExitable
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.utils.CropperUtils.CropperReceiver
import net.kdt.pojavlaunch.utils.CropperUtils.registerCropper
import net.kdt.pojavlaunch.utils.CropperUtils.startCropper
import java.io.IOException

class CustomControlsActivity : BaseActivity(), EditorExitable, CropperReceiver {
    private var mDrawerLayout: DrawerLayout? = null
    private var mDrawerNavigationView: ListView? = null
    private var mControlLayout: ControlLayout? = null
    private var mCropperReceiver: CropperReceiver? = null
    private var mCropperLauncher: ActivityResultLauncher<*>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mCropperLauncher = registerCropper(this, this)

        setContentView(R.layout.activity_custom_controls)

        mControlLayout = findViewById<ControlLayout>(R.id.customctrl_controllayout)
        mDrawerLayout = findViewById<DrawerLayout>(R.id.customctrl_drawerlayout)
        mDrawerNavigationView = findViewById<ListView>(R.id.customctrl_navigation_view)
        val mDrawerContainer = findViewById<View>(R.id.customctrl_drawer_container)
        val mPullDrawerButton = findViewById<View>(R.id.drawer_button)

        mPullDrawerButton.setOnClickListener(View.OnClickListener { v: View? ->
            mDrawerLayout!!.openDrawer(
                mDrawerContainer
            )
        })
        mDrawerLayout!!.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        mDrawerNavigationView!!.setAdapter(object : ArrayAdapter<String?>(
            this,
            android.R.layout.simple_list_item_1,
            getResources().getStringArray(R.array.menu_customcontrol_customactivity)
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent)
                if (v is TextView) {
                    v.setTextColor(Color.WHITE)
                    v.setPadding(32, 16, 32, 16)
                }
                return v
            }
        })
        mDrawerNavigationView!!.setOnItemClickListener(OnItemClickListener { parent: AdapterView<*>?, view: View?, position: Int, id: Long ->
            when (position) {
                0 -> mControlLayout!!.addControlButton(ControlData("New"))
                1 -> mControlLayout!!.addDrawer(ControlDrawerData())
                2 -> mControlLayout!!.addJoystickButton(ControlJoystickData())
                3 -> mControlLayout!!.openLoadDialog()
                4 -> mControlLayout!!.openSaveDialog(this)
                5 -> mControlLayout!!.openSetDefaultDialog()
                6 -> try {
                    val contentUri = DocumentsContract.buildDocumentUri(
                        getString(R.string.storageProviderAuthorities),
                        mControlLayout!!.saveToDirectory(mControlLayout!!.mLayoutFileName)
                    )

                    val shareIntent = Intent()
                    shareIntent.setAction(Intent.ACTION_SEND)
                    shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    shareIntent.setType("application/json")
                    startActivity(shareIntent)

                    val sendIntent =
                        Intent.createChooser(shareIntent, mControlLayout!!.mLayoutFileName)
                    startActivity(sendIntent)
                } catch (e: Exception) {
                    showError(this, e)
                }
            }
            mDrawerLayout!!.closeDrawers()
        })
        mControlLayout!!.modifiable = true
    }

    override fun onAttachedToWindow() {
        mControlLayout!!.post(Runnable {
            try {
                mControlLayout!!.loadLayout(LauncherPreferences.PREF_DEFAULTCTRL_PATH!!)
            } catch (e: IOException) {
                showError(this, e)
            } catch (e: JsonSyntaxException) {
                showError(this, e)
            }
        })
    }

    fun startCropping(cropperReceiver: CropperReceiver?) {
        mCropperReceiver = cropperReceiver
        startCropper(mCropperLauncher)
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        mControlLayout!!.askToExit(this)
    }

    override fun exitEditor() {
        super.onBackPressed()
    }

    override val aspectRatio: Float
        get() {
            if (mCropperReceiver != null) return mCropperReceiver!!.aspectRatio
            return 1f
        }

    override val targetMaxSide: Int
        get() {
            if (mCropperReceiver != null) return mCropperReceiver!!.targetMaxSide
            return 128
        }

    override fun onCropped(contentBitmap: Bitmap?) {
        if (mCropperReceiver != null) mCropperReceiver!!.onCropped(contentBitmap)
    }

    override fun onFailed(exception: Exception?) {
        if (mCropperReceiver != null) mCropperReceiver!!.onFailed(exception)
    }
}
