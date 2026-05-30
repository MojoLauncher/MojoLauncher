package net.kdt.pojavlaunch.utils

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import android.widget.ToggleButton
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.PojavApplication
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.imgcropper.BitmapCropBehaviour
import net.kdt.pojavlaunch.imgcropper.CropperBehaviour
import net.kdt.pojavlaunch.imgcropper.CropperView
import net.kdt.pojavlaunch.imgcropper.RegionDecoderCropBehaviour
import java.io.IOException

object CropperUtils {
    @JvmStatic
    fun registerCropper(
        activity: AppCompatActivity,
        cropperReceiver: CropperReceiver
    ): ActivityResultLauncher<Array<String>> {
        return registerCropper(ActivityContextProvider(activity), cropperReceiver)
    }

    @JvmStatic
    fun registerCropper(
        fragment: Fragment,
        cropperReceiver: CropperReceiver
    ): ActivityResultLauncher<Array<String>> {
        return registerCropper(FragmentContextProvider(fragment), cropperReceiver)
    }

    private fun registerCropper(
        contextProvider: ContextProvider,
        cropperReceiver: CropperReceiver
    ): ActivityResultLauncher<Array<String>> {
        return contextProvider.resultCaller.registerForActivityResult(OpenDocument()) { result: Uri? ->
            val context = contextProvider.context ?: return@registerForActivityResult
            if (result == null) {
                Toast.makeText(context, R.string.cropper_select_cancelled, Toast.LENGTH_SHORT)
                    .show()
                return@registerForActivityResult
            }
            openCropperDialog(context, result, cropperReceiver)
        }
    }

    private fun openCropperDialog(
        context: Context, selectedUri: Uri,
        cropperReceiver: CropperReceiver
    ) {
        val contentResolver = context.getContentResolver()
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.cropper_title)
            .setView(R.layout.dialog_cropper)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .show()
        val cropImageView = dialog.findViewById<CropperView?>(R.id.crop_dialog_view)
        val finishProgressBar = dialog.findViewById<View?>(R.id.crop_dialog_progressbar)
        checkNotNull(cropImageView)
        checkNotNull(finishProgressBar)
        bindViews(dialog, cropImageView)
        cropImageView.setAspectRatio(cropperReceiver.aspectRatio)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setOnClickListener(View.OnClickListener { v: View? ->
                dialog.dismiss()
                cropperReceiver.onCropped(cropImageView.crop(cropperReceiver.targetMaxSide))
            })
        PojavApplication.sExecutorService.execute(Runnable {
            var cropperBehaviour: CropperBehaviour? = null
            try {
                cropperBehaviour = createBehaviour(cropImageView, contentResolver, selectedUri)
            } catch (e: Exception) {
                cropperReceiver.onFailed(e)
            }
            val finalBehaviour = cropperBehaviour
            Tools.runOnUiThread(Runnable {
                finishSetup(
                    dialog,
                    finishProgressBar,
                    cropImageView,
                    finalBehaviour
                )
            })
        })
    }

    // Fixes the chin that the dialog has on my huawei fon
    private fun fixDialogHeight(dialog: AlertDialog) {
        val dialogWindow = dialog.getWindow()
        if (dialogWindow != null) dialogWindow.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,  // width
            WindowManager.LayoutParams.WRAP_CONTENT // height
        )
    }

    private fun finishSetup(
        dialog: AlertDialog, progressBar: View,
        cropImageView: CropperView, cropperBehaviour: CropperBehaviour?
    ) {
        if (cropperBehaviour == null) {
            dialog.dismiss()
            return
        }
        progressBar.setVisibility(View.GONE)
        cropImageView.setCropperBehaviour(cropperBehaviour)
        cropperBehaviour.applyImage()
        cropImageView.post(Runnable {
            fixDialogHeight(dialog)
            cropImageView.requestLayout()
        })
    }


    @Throws(Exception::class)
    private fun createBehaviour(
        cropImageView: CropperView,
        contentResolver: ContentResolver,
        selectedUri: Uri
    ): CropperBehaviour? {
        contentResolver.openInputStream(selectedUri).use { inputStream ->
            if (inputStream == null) return null
            try {
                val regionDecoder = BitmapRegionDecoder.newInstance(inputStream, false)
                val cropBehaviour = RegionDecoderCropBehaviour(cropImageView)
                cropBehaviour.setRegionDecoder(regionDecoder)
                return cropBehaviour
            } catch (e: IOException) {
                // Catch IOE here to detect the case when BitmapRegionDecoder does not support this image format.
                // If it does not, we will just have to load the bitmap in full resolution using BitmapFactory.
                Log.w("CropperUtils", "Failed to load image into BitmapRegionDecoder", e)
            }
        }
        contentResolver.openInputStream(selectedUri).use { inputStream ->
            if (inputStream == null) return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            if (originalBitmap == null) throw IOException("Image format not supported")
            val cropBehaviour = BitmapCropBehaviour(cropImageView)
            cropBehaviour.setBitmap(originalBitmap)
            return cropBehaviour
        }
    }

    private fun bindViews(alertDialog: AlertDialog, imageCropperView: CropperView) {
        val horizontalLock = alertDialog.findViewById<ToggleButton?>(R.id.crop_dialog_hlock)
        val verticalLock = alertDialog.findViewById<ToggleButton?>(R.id.crop_dialog_vlock)
        val reset = alertDialog.findViewById<View?>(R.id.crop_dialog_reset)
        checkNotNull(horizontalLock)
        checkNotNull(verticalLock)
        checkNotNull(reset)
        horizontalLock.setOnClickListener(View.OnClickListener { v: View? ->
            imageCropperView.horizontalLock = horizontalLock.isChecked()
        }
        )
        verticalLock.setOnClickListener(View.OnClickListener { v: View? ->
            imageCropperView.verticalLock = verticalLock.isChecked()
        }
        )
        reset.setOnClickListener(View.OnClickListener { v: View? -> imageCropperView.resetTransforms() }
        )
    }

    fun startCropper(resultLauncher: ActivityResultLauncher<*>?) {
        @Suppress("UNCHECKED_CAST")
        val launcher = resultLauncher as? ActivityResultLauncher<Array<String>>
        launcher?.launch(arrayOf("image/*"))
    }

    interface CropperReceiver {
        val aspectRatio: Float
        val targetMaxSide: Int
        fun onCropped(contentBitmap: Bitmap?)
        fun onFailed(exception: Exception?)
    }

    private interface ContextProvider {
        val context: Context?
        val resultCaller: ActivityResultCaller
    }

    private class FragmentContextProvider(private val mFragment: Fragment) : ContextProvider {
        override val context: Context?
            get() = mFragment.context

        override val resultCaller: ActivityResultCaller
            get() = mFragment
    }

    private class ActivityContextProvider(private val mActivity: AppCompatActivity) :
        ContextProvider {
        override val context: Context?
            get() {
                if (mActivity.isDestroyed || mActivity.isFinishing) return null
                return mActivity
            }

        override val resultCaller: ActivityResultCaller
            get() = mActivity
    }
}
