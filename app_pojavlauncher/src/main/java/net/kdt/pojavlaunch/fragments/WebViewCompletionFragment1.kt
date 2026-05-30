package net.kdt.pojavlaunch.fragments

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import net.ashmeet.hyperlauncher.R

abstract class WebViewCompletionFragment protected constructor(
    private val mTrackedUrl: String,
    private val mAuthUrl: String
) : Fragment() {
    private var mWebview: WebView? = null

    // Technically the client is blank (or there is none) when the fragment is initialized
    private var mBlankClient = true
    private var mIsCompleted = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mWebview = inflater.inflate(R.layout.fragment_microsoft_login, container, false) as WebView
        setWebViewSettings()
        if (savedInstanceState == null) startNewSession()
        else restoreWebViewState(savedInstanceState)
        return mWebview!!
    }

    // WebView.restoreState() does not restore the WebSettings or the client, so set them there
    // separately. Note that general state should not be altered here (aka no loading pages, no manipulating back/front lists),
    // to avoid "undesirable side-effects"
    @SuppressLint("SetJavaScriptEnabled")
    private fun setWebViewSettings() {
        val settings = mWebview!!.getSettings()
        settings.setJavaScriptEnabled(true)
        mWebview!!.setWebViewClient(WebViewTrackClient())
        mBlankClient = false
    }

    private fun startNewSession() {
        CookieManager.getInstance().removeAllCookies(ValueCallback { b: Boolean? ->
            mWebview!!.clearHistory()
            mWebview!!.clearCache(true)
            mWebview!!.clearFormData()
            mWebview!!.clearHistory()
            mWebview!!.loadUrl(mAuthUrl)
        })
    }

    private fun restoreWebViewState(savedInstanceState: Bundle) {
        Log.i("MSAuthFragment", "Restoring state...")
        if (mWebview!!.restoreState(savedInstanceState) == null) {
            Log.w("MSAuthFragment", "Failed to restore state, starting afresh")
            // if, for some reason, we failed to restore our session,
            // just start afresh
            startNewSession()
        }
    }

    override fun onStart() {
        super.onStart()
        // If we have switched to a blank client and haven't fully gone though the lifecycle callbacks to restore it,
        // restore it here.
        if (mBlankClient) mWebview!!.setWebViewClient(WebViewTrackClient())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // Since the value cannot be null, just create a "blank" client. This is done to not let Android
        // kill us if something happens after the state gets saved, when we can't do fragment transitions
        mWebview!!.setWebViewClient(WebViewClient())
        // For some dumb reason state is saved even when Android won't actually destroy the activity.
        // Let the fragment know that the client is blank so that we can restore it in onStart()
        // (it was the earliest lifecycle call actually invoked in this case)
        mBlankClient = true
        super.onSaveInstanceState(outState)
        mWebview!!.saveState(outState)
    }

    /* Expose webview actions to others */
    fun canGoBack(): Boolean {
        return mWebview!!.canGoBack()
    }

    fun goBack() {
        mWebview!!.goBack()
    }

    /** Client to track when to sent the data to the launcher  */
    internal inner class WebViewTrackClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, url: String): Boolean {
            if (url.startsWith(mTrackedUrl)) {
                internalSignalCompletion(url)
                return true
            }

            return super.shouldOverrideUrlLoading(view, url)
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {}

        override fun onPageFinished(view: WebView?, url: String) {
            if (url.startsWith(mTrackedUrl)) {
                internalSignalCompletion(url)
            }
        }
    }

    private fun internalSignalCompletion(fullUrl: String?) {
        if (mIsCompleted) return
        mIsCompleted = true
        signalCompletion(fullUrl)
    }

    protected abstract fun signalCompletion(fullUrl: String?)
}
