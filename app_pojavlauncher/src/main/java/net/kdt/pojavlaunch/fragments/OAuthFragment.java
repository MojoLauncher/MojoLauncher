package net.kdt.pojavlaunch.fragments;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.widget.Toast;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.extra.ExtraCore;

import git.artdeell.mojo.R;

public class OAuthFragment extends WebViewCompletionFragment {

    private static final String QUERY_ERROR_NAME = "error";
    private static final String QUERY_ERROR_DECRIPTION = "error_description";
    private static final String QUERY_OAUTH_CODE = "code";
    private static final String ERROR_ACCESS_DENIED = "access_denied";

    private final String mExtraCoreConstant;

    protected OAuthFragment(String mTrackedUrl, String mAuthUrl, String mExtraCoreConstant) {
        super(mTrackedUrl, mAuthUrl);
        this.mExtraCoreConstant = mExtraCoreConstant;
    }

    private void displayError(Context context, Uri uri) {
        String errorMessage = uri.getQueryParameter(QUERY_ERROR_DECRIPTION);
        if(errorMessage == null) errorMessage = uri.getQueryParameter(QUERY_ERROR_NAME);
        if(errorMessage == null) errorMessage = getString(R.string.oauth_unknown_error);
        Tools.dialog(context, getString(R.string.global_error), errorMessage);
    }

    @Override
    protected void signalCompletion(String fullUrl) {
        Activity activity = requireActivity();
        Uri uri = Uri.parse(fullUrl);
        String error = uri.getQueryParameter(QUERY_ERROR_NAME);
        String code = uri.getQueryParameter(QUERY_OAUTH_CODE);
        if(code == null) {
            activity.onBackPressed();
            // Access denied - means the user exited out of the oauth dialog. Just leave the fragment
            if(ERROR_ACCESS_DENIED.equals(error)) return;
            // On other unknown errors, show a dialog
            displayError(activity, uri);
            return;
        }
        // Captured by the listener in the mcAccountSpinner
        ExtraCore.setValue(mExtraCoreConstant, code);
        Toast.makeText(activity, R.string.oauth_web_complete, Toast.LENGTH_SHORT).show();
        Tools.backToMainMenu(requireActivity());
    }
}
