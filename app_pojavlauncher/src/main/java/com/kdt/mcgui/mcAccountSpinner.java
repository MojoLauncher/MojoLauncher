package com.kdt.mcgui;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.core.content.res.ResourcesCompat;


import net.kdt.pojavlaunch.authenticator.accounts.PojavProfile;
import git.artdeell.mojo.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.authenticator.AuthType;
import net.kdt.pojavlaunch.authenticator.BackgroundLogin;
import net.kdt.pojavlaunch.authenticator.listener.LoginListener;
import net.kdt.pojavlaunch.authenticator.impl.PresentedException;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.extra.ExtraListener;
import net.kdt.pojavlaunch.authenticator.accounts.MinecraftAccount;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import fr.spse.extended_view.ExtendedTextView;

public class mcAccountSpinner extends AppCompatSpinner implements AdapterView.OnItemSelectedListener, LoginListener {
    public mcAccountSpinner(@NonNull Context context) {
        this(context, null);
    }
    public mcAccountSpinner(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private List<MinecraftAccount> mAccountList = PojavProfile.getAccounts();

    /* Current animator to for the login bar, is swapped when changing step */
    private ObjectAnimator mLoginBarAnimator;
    private float mLoginBarWidth = -1;

    /* Paint used to display the bottom bar, to show the login progress. */
    private final Paint mLoginBarPaint = new Paint();

    /* When a login is performed in the background, we need to know where we are */
    private final static int MAX_LOGIN_STEP = 5;
    private int mLoginStep = 0;

    class LoginExtraListener implements ExtraListener<String> {
        private final AuthType mAuthType;

        LoginExtraListener(AuthType mAuthType) {
            this.mAuthType = mAuthType;
        }

        @Override
        public boolean onValueSet(String key, @NonNull String value) {
            mLoginBarPaint.setColor(getResources().getColor(R.color.minebutton_color));
            BackgroundLogin backgroundLogin = mAuthType.createAuth();
            backgroundLogin.createAccount(mcAccountSpinner.this, value);
            return false;
        }
    }

    /* Triggered when we need to do microsoft login */
    private final ExtraListener<String> mMicrosoftLoginListener = new LoginExtraListener(AuthType.MICROSOFT);

    /* Triggered when we need to do ely.by login */
    private final ExtraListener<String> mElyByLoginListener = new LoginExtraListener(AuthType.ELY_BY);

    /* Triggered when we need to perform local login */
    private final ExtraListener<String[]> mMojangLoginListener = (key, value) -> {
        try {
            MinecraftAccount minecraftAccount = PojavProfile.createAccount(acc->{
                acc.username = value[0];
            });
            onLoginDone(minecraftAccount);
        }catch (IOException e) {
            onLoginError(e);
        }
        return false;
    };

    @SuppressLint("ClickableViewAccessibility")
    private void init(){
        // Set visual properties
        setBackgroundColor(getResources().getColor(R.color.background_status_bar));
        mLoginBarPaint.setColor(getResources().getColor(R.color.minebutton_color));
        mLoginBarPaint.setStrokeWidth(getResources().getDimensionPixelOffset(R.dimen._2sdp));

        // Set behavior
        reloadAccounts(true);
        setOnItemSelectedListener(this);

        ExtraCore.addExtraListener(ExtraConstants.MOJANG_LOGIN_TODO, mMojangLoginListener);
        ExtraCore.addExtraListener(ExtraConstants.MICROSOFT_LOGIN_TODO, mMicrosoftLoginListener);
        ExtraCore.addExtraListener(ExtraConstants.ELYBY_LOGIN_TODO, mElyByLoginListener);
    }


    @Override
    public final void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if(position == 0){  // Add account button
            if(mAccountList.size() > 1){
                ExtraCore.setValue(ExtraConstants.SELECT_AUTH_METHOD, true);
            }
            return;
        }

        MinecraftAccount selectedAccount = mAccountList.get(position - 1);
        PojavProfile.setCurrentProfile(selectedAccount);
        performLogin(selectedAccount);
    }

    @Override
    public final void onNothingSelected(AdapterView<?> parent) {}


    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if(mLoginBarWidth == -1) mLoginBarWidth = getWidth(); // Initial draw

        float bottom = getHeight() - mLoginBarPaint.getStrokeWidth()/2;
        canvas.drawLine(0, bottom, mLoginBarWidth, bottom, mLoginBarPaint);
    }

    private void removeAccount(int position) {
        if(position == 0) return;
        PojavProfile.deleteProfile(mAccountList.get(position - 1));

        reloadAccounts(false);
    }

    @Keep
    public void setLoginBarWidth(float value){
        mLoginBarWidth = value;
        invalidate(); // Need to redraw each time this is changed
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setNoAccountBehavior(){
        // Set custom behavior when no account are present, to make it act as a button
        if(!mAccountList.isEmpty()){
            // Remove any touch listener
            setOnTouchListener(null);
            return;
        }

        // Make the spinner act like a button, since there is no item to really select
        setOnTouchListener((v, event) -> {
            if(event.getAction() != MotionEvent.ACTION_UP) return false;
            // The activity should intercept this and spawn another fragment
            ExtraCore.setValue(ExtraConstants.SELECT_AUTH_METHOD, true);
            return true;
        });
    }

    /**
     * Reload the spinner, from memory or from scratch. A default account can be selected
     * @param fromFiles Whether we use files as the source of truth
     */
    private void reloadAccounts(boolean fromFiles){
        mAccountList = fromFiles ? PojavProfile.reloadAccounts() : PojavProfile.getAccounts();

        ArrayList<MinecraftAccount> displayAccountList = new ArrayList<>(mAccountList.size() + 1);
        displayAccountList.add(null);
        displayAccountList.addAll(mAccountList);

        AccountAdapter accountAdapter = new AccountAdapter(getContext(), R.layout.item_minecraft_account, displayAccountList);
        accountAdapter.setDropDownViewResource(R.layout.item_minecraft_account);
        setAdapter(accountAdapter);

        MinecraftAccount selectedAccount = PojavProfile.getCurrentProfileContent(false);
        Log.i("mcAccountSpinner", "selectedAccount: "+selectedAccount);
        if(selectedAccount != null) {
            performLogin(selectedAccount);
            int selectionPos = displayAccountList.indexOf(selectedAccount);
            Log.i("mcAccountSpinner", "selectedPos: "+selectionPos + " account: "+selectedAccount + " list: "+displayAccountList);
            setSelection(selectionPos);
        }else {
            setSelection(mAccountList.isEmpty() ? 0 : 1);
        }

        // Remove or add the behavior if needed
        setNoAccountBehavior();
    }

    private void performLogin(MinecraftAccount minecraftAccount){
        if(minecraftAccount.isLocal()) return;

        mLoginBarPaint.setColor(getResources().getColor(R.color.minebutton_color));
        AuthType authType = minecraftAccount.authType;
        if(authType.requiresLogin() && System.currentTimeMillis() > minecraftAccount.expiresAt) {
            authType.createAuth().refreshAccount(this, minecraftAccount);
        }
    }

    @Override
    public void onLoginDone(MinecraftAccount account) {
        Toast.makeText(getContext(), R.string.main_login_done, Toast.LENGTH_SHORT).show();

        PojavProfile.setCurrentProfile(account);
        invalidate();
        reloadAccounts(false);
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
        // Animate the login bar, cosmetic purposes only
        mLoginStep = step;
        if(mLoginBarAnimator != null){
            mLoginBarAnimator.cancel();
            mLoginBarAnimator.setFloatValues( mLoginBarWidth, ((float) getWidth() /MAX_LOGIN_STEP * mLoginStep));
        }else{
            mLoginBarAnimator = ObjectAnimator.ofFloat(this, "LoginBarWidth", mLoginBarWidth, (getWidth()/MAX_LOGIN_STEP * mLoginStep));
        }
        mLoginBarAnimator.start();
    }

    private class AccountAdapter extends ArrayAdapter<MinecraftAccount> {

        private final HashMap<String, Drawable> mImageCache = new HashMap<>();
        public AccountAdapter(@NonNull Context context, int resource, @NonNull List<MinecraftAccount> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if(convertView == null){
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_minecraft_account, parent, false);
            }

            Resources resources = parent.getResources();
            Resources.Theme theme = getContext().getTheme();

            ExtendedTextView textview = convertView.findViewById(R.id.account_item);
            ImageView deleteButton = convertView.findViewById(R.id.delete_account_button);

            // Handle the "Add account" section
            if(position == 0) {
                Drawable plusDrawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_add, theme);
                textview.setCompoundDrawables(plusDrawable, null, null, null);
                textview.setText(R.string.main_add_account);
                deleteButton.setVisibility(View.GONE);
                return convertView;
            }

            MinecraftAccount account = Objects.requireNonNull(super.getItem(position));

            textview.setText(account.username);

            int authTypeResource = account.authType.iconResource;
            Drawable authTypeDrawable = null;
            if(authTypeResource != 0) {
                authTypeDrawable = ResourcesCompat.getDrawable(resources, authTypeResource, theme);
            }
            /*Drawable accountHead = mImageCache.get(username);
            if (accountHead == null){
                accountHead = new BitmapDrawable(parent.getResources(), MinecraftAccount.getSkinFace(username));
                mImageCache.put(username, accountHead);
            }*/
            textview.setCompoundDrawables(null, null, authTypeDrawable, null);

            deleteButton.setVisibility(View.VISIBLE);
            deleteButton.setOnClickListener(v -> showDeleteDialog(getContext(), position));

            return convertView;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View view = getDropDownView(position, convertView, parent);
            view.findViewById(R.id.delete_account_button).setVisibility(View.GONE);
            return view;
        }

        private void showDeleteDialog(Context context, int position) {
            new AlertDialog.Builder(context)
                    .setMessage(R.string.warning_remove_account)
                    .setPositiveButton(android.R.string.cancel, null)
                    .setNeutralButton(R.string.global_delete, (dialog, which) -> {
                        onDetachedFromWindow();
                        removeAccount(position);
                    })
                    .show();
        }
    }



}
