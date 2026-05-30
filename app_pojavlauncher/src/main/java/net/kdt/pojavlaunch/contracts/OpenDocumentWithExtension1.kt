package net.kdt.pojavlaunch.contracts

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.result.contract.ActivityResultContract
import net.kdt.pojavlaunch.PojavApplication
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future

// Android's OpenDocument contract is the basicmost crap that doesn't allow
// you to specify practically anything. So i made this instead.
class OpenDocumentWithExtension(extension: String?) : ActivityResultContract<Any?, Uri?>() {
    private val extensionMimeTypeFuture: Future<String?>

    /**
     * Create a new OpenDocumentWithExtension contract.
     * If the extension provided to the constructor is not available in the device's MIME
     * type database, the filter will default to "all types"
     * @param extension the extension to filter by
     */
    init {
        // Who would have thought that loading the MIME map takes a significant amount of time?
        extensionMimeTypeFuture = PojavApplication.sExecutorService.submit<String?>(Callable {
            var extensionMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            if (extensionMimeType == null) extensionMimeType = "*/*"
            extensionMimeType
        })
    }

    override fun createIntent(context: Context, input: Any?): Intent {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        try {
            intent.setType(extensionMimeTypeFuture.get())
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        } catch (e: ExecutionException) {
            throw RuntimeException(e)
        }
        return intent
    }

    override fun getSynchronousResult(
        context: Context,
        input: Any?
    ): SynchronousResult<Uri?>? {
        return null
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        if (intent == null || resultCode != Activity.RESULT_OK) return null
        return intent.getData()
    }
}
