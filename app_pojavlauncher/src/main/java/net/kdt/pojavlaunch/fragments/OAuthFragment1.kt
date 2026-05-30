package net.kdt.pojavlaunch.fragments

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.core.net.toUri
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.Tools.backToMainMenu
import net.kdt.pojavlaunch.Tools.dialog
import net.kdt.pojavlaunch.extra.ExtraCore

open class OAuthFragment protected constructor(
    mTrackedUrl: String = "",
    mAuthUrl: String = "",
    private val mExtraCoreConstant: String? = null
) : WebViewCompletionFragment(mTrackedUrl, mAuthUrl) {
    private fun displayError(context: Context, uri: Uri) {
        var errorMessage = uri.getQueryParameter(QUERY_ERROR_DECRIPTION)
        if (errorMessage == null) errorMessage = uri.getQueryParameter(QUERY_ERROR_NAME)
        if (errorMessage == null) errorMessage = getString(R.string.oauth_unknown_error)
        dialog(context, getString(R.string.global_error), errorMessage)
    }

    override fun signalCompletion(fullUrl: String?) {
        val activity = activity ?: return
        val uri = fullUrl?.toUri() ?: return
        val error = uri.getQueryParameter(QUERY_ERROR_NAME)
        val code = uri.getQueryParameter(QUERY_OAUTH_CODE)
        if (code == null) {
            activity.onBackPressedDispatcher.onBackPressed()
            // Access denied - means the user exited out of the oauth dialog. Just leave the fragment
            if (ERROR_ACCESS_DENIED == error) return
            // On other unknown errors, show a dialog
            displayError(activity, uri)
            return
        }
        // Captured by the listener in the mcAccountSpinner
        ExtraCore.setValue(mExtraCoreConstant, code)
        Toast.makeText(activity, R.string.oauth_web_complete, Toast.LENGTH_SHORT).show()
        backToMainMenu(activity)
    }

    companion object {
        private const val QUERY_ERROR_NAME = "error"
        private const val QUERY_ERROR_DECRIPTION = "error_description"
        private const val QUERY_OAUTH_CODE = "code"
        private const val ERROR_ACCESS_DENIED = "access_denied"
    }
}
