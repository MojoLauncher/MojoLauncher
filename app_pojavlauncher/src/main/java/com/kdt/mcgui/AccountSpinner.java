package com.kdt.mcgui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.core.content.res.ResourcesCompat;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.authenticator.AuthType;
import net.kdt.pojavlaunch.authenticator.BackgroundLogin;
import net.kdt.pojavlaunch.authenticator.accounts.MinecraftAccount;
import net.kdt.pojavlaunch.authenticator.accounts.PojavProfile;
import net.kdt.pojavlaunch.authenticator.impl.PresentedException;
import net.kdt.pojavlaunch.authenticator.listener.LoginListener;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.extra.ExtraListener;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

import fr.spse.extended_view.ExtendedTextView;
import git.artdeell.mojo.R;

public class AccountSpinner extends AppCompatSpinner implements LoginListener, AdapterView.OnItemSelectedListener, ValueAnimator.AnimatorUpdateListener {
    private Adapter mAdapter;
    /* Login progress bar stuff */
    private int mMaxSteps = 5;
    private final ValueAnimator mLoginStepAnimator = ValueAnimator.ofFloat(mMaxSteps);
    private final Paint mLoginBarPaint = new Paint();
    private float mLoginStep;

    class LoginExtraListener implements ExtraListener<String> {
        private final AuthType mAuthType;

        LoginExtraListener(AuthType mAuthType) {
            this.mAuthType = mAuthType;
        }

        @Override
        public boolean onValueSet(String key, @NonNull String value) {
            mLoginBarPaint.setColor(getResources().getColor(R.color.minebutton_color));
            BackgroundLogin backgroundLogin = mAuthType.createAuth();
            backgroundLogin.createAccount(AccountSpinner.this, value);
            return false;
        }
    }

    /* Login listeners */
    private final ExtraListener<String> mMicrosoftLoginListener = new LoginExtraListener(AuthType.MICROSOFT);
    private final ExtraListener<String> mElyByLoginListener = new LoginExtraListener(AuthType.ELY_BY);
    private final ExtraListener<String[]> mMojangLoginListener = (key, value) -> {
        try {
            MinecraftAccount minecraftAccount = PojavProfile.createAccount(acc-> acc.username = value[0]);
            onLoginDone(minecraftAccount);
        }catch (IOException e) {
            onLoginError(e);
        }
        return false;
    };

    /* Account main menu refresh listener */
    private final ExtraListener<Boolean> mRefreshAccountsListener = (k,v)->{
        syncSelection();
        return false;
    };

    public AccountSpinner(@NonNull Context context, int mode) {
        super(context, mode);
        init();
    }

    public AccountSpinner(@NonNull Context context) {
        super(context);
        init();
    }

    public AccountSpinner(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AccountSpinner(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public AccountSpinner(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int mode) {
        super(context, attrs, defStyleAttr, mode);
        init();
    }

    public AccountSpinner(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int mode, Resources.Theme popupTheme) {
        super(context, attrs, defStyleAttr, mode, popupTheme);
        init();
    }



    private void init() {
        mAdapter = new Adapter(getContext());
        setAdapter(mAdapter);
        syncSelection();


        setBackgroundColor(getResources().getColor(R.color.background_status_bar));
        mLoginBarPaint.setColor(getResources().getColor(R.color.minebutton_color));
        mLoginBarPaint.setStrokeWidth(getResources().getDimensionPixelOffset(R.dimen._2sdp));
        mLoginStepAnimator.addUpdateListener(this);
        mLoginStep = mMaxSteps;

        ExtraCore.addExtraListener(ExtraConstants.MOJANG_LOGIN_TODO, mMojangLoginListener);
        ExtraCore.addExtraListener(ExtraConstants.MICROSOFT_LOGIN_TODO, mMicrosoftLoginListener);
        ExtraCore.addExtraListener(ExtraConstants.ELYBY_LOGIN_TODO, mElyByLoginListener);
        ExtraCore.addExtraListener(ExtraConstants.REFRESH_ACCOUNT_SPINNER, mRefreshAccountsListener);
    }

    public void syncSelection() {
        MinecraftAccount minecraftAccount= PojavProfile.getCurrentProfileContent(false);

        int currentPosition = mAdapter.getPosition(minecraftAccount);
        int defaultSelection = mAdapter.getCount() == 1 ? 0 : 1;
        if(currentPosition == -1) currentPosition = defaultSelection;
        setSelection(currentPosition);

        if(minecraftAccount == null) return;
        refreshAccount(minecraftAccount);
    }

    private void refreshAccount(MinecraftAccount minecraftAccount) {
        // Wait until all tasks (including other possible login tasks) are done before
        // attempting to refresh the account.
        ProgressKeeper.waitUntilDone(()->{
            AuthType authType = minecraftAccount.authType;
            if(authType.requiresLogin() && System.currentTimeMillis() > minecraftAccount.expiresAt) {
                authType.createAuth().refreshAccount(this, minecraftAccount);
            }
        });
    }

    private void dismissPopup() {
        onDetachedFromWindow();
        onAttachedToWindow();
    }

    private void createAccount() {
        setSelection(0);
        dismissPopup();
        ExtraCore.setValue(ExtraConstants.SELECT_AUTH_METHOD, true);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        float bottom = getHeight() - mLoginBarPaint.getStrokeWidth()/2;
        float lineFillPercent = (mLoginStep / mMaxSteps);
        canvas.drawLine(0, bottom, lineFillPercent * getWidth(), bottom, mLoginBarPaint);
    }

    @Override
    public void onLoginDone(MinecraftAccount account) {
        mLoginStep = mMaxSteps;
        invalidate();

        Toast.makeText(getContext(), R.string.main_login_done, Toast.LENGTH_SHORT).show();
        PojavProfile.setCurrentProfile(account);
        mAdapter.refresh();
        syncSelection();
    }

    @Override
    public void onLoginError(Throwable errorMessage) {
        mLoginBarPaint.setColor(Color.RED);
        Context context = getContext();
        if(errorMessage instanceof PresentedException) {
            PresentedException exception = (PresentedException) errorMessage;
            Throwable cause = exception.getCause();
            if(cause == null) {
                Tools.dialog(context, context.getString(R.string.global_error), exception.toString(context));
            }else {
                Tools.showError(context, exception.toString(context), exception.getCause());
            }
        }else {
            Tools.showError(getContext(), errorMessage);
        }
        invalidate();
    }

    @Override
    public void onLoginProgress(int step) {
        mLoginStepAnimator.cancel();
        mLoginStepAnimator.setFloatValues(mLoginStep, step);
        mLoginStepAnimator.start();
    }

    @Override
    public void setMaxLoginProgress(int max) {
        mMaxSteps = max;
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        MinecraftAccount minecraftAccount = mAdapter.getItem(i);
        if(minecraftAccount == null) {
            if(i == 0) {
                createAccount();
            }else {
                Tools.showError(adapterView.getContext(), new NullPointerException());
            }
            return;
        }
        PojavProfile.setCurrentProfile(minecraftAccount);
        refreshAccount(minecraftAccount);
        dismissPopup();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {}

    @Override
    public void onAnimationUpdate(@NonNull ValueAnimator valueAnimator) {
        mLoginStep = (float)valueAnimator.getAnimatedValue();
        invalidate();
    }

    private class Adapter extends ArrayAdapter<MinecraftAccount> {
        private final HashMap<Integer, BitmapDrawable> mSkinHeadCache = new HashMap<>();
        private final LayoutInflater mInflater;


        public Adapter(@NonNull Context context) {
            super(context, R.layout.item_minecraft_account);
            mInflater = LayoutInflater.from(context);
            refresh();
        }

        public void refresh() {
            setNotifyOnChange(false); // Disable change notifications to not spook the user
            clear();
            add(null); // "Add account" stub
            setNotifyOnChange(true);
            addAll(PojavProfile.getAccounts());
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if(convertView == null) {
                convertView = mInflater.inflate(R.layout.item_minecraft_account, parent, false);
            }
            populateView(convertView, position, false);
            return convertView;
        }

        @Override
        public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if(convertView == null) {
                convertView = mInflater.inflate(R.layout.item_minecraft_account, parent, false);
            }
            populateView(convertView, position, true);
            return convertView;
        }

        private void populateView(View view, int position, boolean isDropDown) {
            Resources resources = getResources();
            Resources.Theme theme = getContext().getTheme();

            ExtendedTextView textview = view.findViewById(R.id.account_item);
            ImageView deleteButton = view.findViewById(R.id.delete_account_button);

            if(position == 0) {
                // "Add account" button
                Drawable plusDrawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_add, theme);
                textview.setCompoundDrawables(plusDrawable, null, null, null);
                textview.setText(R.string.main_add_account);
                deleteButton.setVisibility(View.GONE);
                // Only activate the listener behaviour when in drop-down mode
                // or when there's no accounts
                if(isDropDown || getCount() == 1) view.setOnClickListener(v-> createAccount());
                return;
            }

            if(isDropDown) {
                deleteButton.setVisibility(View.VISIBLE);
                deleteButton.setOnClickListener((v)->showDeleteDialog(v.getContext(), position));
            }else {
                deleteButton.setVisibility(View.GONE);
            }


            MinecraftAccount account = Objects.requireNonNull(getItem(position));

            int authTypeResource = account.authType.iconResource;

            Drawable authType = null;
            if(authTypeResource != 0) {
                authType = ResourcesCompat.getDrawable(resources, authTypeResource, theme);
            }

            int headCacheHash = System.identityHashCode(account);
            BitmapDrawable accountHead = mSkinHeadCache.get(headCacheHash);
            if (accountHead == null){
                Bitmap accountSkinFace = account.getSkinFace();
                accountHead = new BitmapDrawable(resources, accountSkinFace);
                mSkinHeadCache.put(headCacheHash, accountHead);
            }

            textview.setText(account.username);
            textview.setCompoundDrawablesRelative(accountHead, null, authType, null);
        }

        private void showDeleteDialog(Context context, int position) {
            new AlertDialog.Builder(context)
                    .setMessage(R.string.warning_remove_account)
                    .setPositiveButton(android.R.string.cancel, null)
                    .setNeutralButton(R.string.global_delete, (dialog, which) -> {
                        MinecraftAccount account = getItem(position);
                        PojavProfile.deleteProfile(account);
                        refresh();
                    })
                    .show();
        }
    }
}
