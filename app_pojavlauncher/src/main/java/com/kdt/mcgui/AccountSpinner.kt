package com.kdt.mcgui

import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import fr.spse.extended_view.ExtendedTextView
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.PojavApplication
import net.kdt.pojavlaunch.Tools.dialog
import net.kdt.pojavlaunch.Tools.runOnUiThread
import net.kdt.pojavlaunch.Tools.showError
import net.kdt.pojavlaunch.authenticator.AuthType
import net.kdt.pojavlaunch.authenticator.accounts.Accounts
import net.kdt.pojavlaunch.authenticator.accounts.MinecraftAccount
import net.kdt.pojavlaunch.authenticator.impl.PresentedException
import net.kdt.pojavlaunch.authenticator.listener.LoginListener
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.extra.ExtraListener
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper.waitUntilDone
import net.kdt.pojavlaunch.utils.SkinUtils
import java.io.IOException

class AccountSpinner : AppCompatSpinner, LoginListener, AdapterView.OnItemSelectedListener,
    AnimatorUpdateListener {
    private var mAdapter: Adapter? = null

    /* Login progress bar stuff */
    private var mMaxSteps = 5
    private val mLoginStepAnimator: ValueAnimator = ValueAnimator.ofFloat(mMaxSteps.toFloat())
    private val mLoginBarPaint = Paint()
    private var mLoginStep = 0f

    internal inner class LoginExtraListener(private val mAuthType: AuthType) :
        ExtraListener<String?> {
        override fun onValueSet(key: String?, value: String?): Boolean {
            mLoginBarPaint.color = resources.getColor(R.color.minebutton_color)
            val backgroundLogin = mAuthType.createAuth()
            backgroundLogin!!.createAccount(this@AccountSpinner, value)
            return false
        }
    }

    /* Login listeners */
    private val mMicrosoftLoginListener: ExtraListener<String?> =
        LoginExtraListener(AuthType.MICROSOFT)
    private val mElyByLoginListener: ExtraListener<String?> = LoginExtraListener(AuthType.ELY_BY)
    private val mMojangLoginListener: ExtraListener<Array<String?>?> =
        ExtraListener { key: String?, value: Array<String?>? ->
            try {
                if (value != null) {
                    val minecraftAccount: MinecraftAccount = Accounts.upsertByUsername { acc ->
                        acc.username = value[0] ?: "Steve"
                        acc.authType = AuthType.LOCAL
                        // Generate a proper local UUID encoding the default classic model
                        acc.profileId = SkinUtils.getFormattedLocalUUID(
                            value[0] ?: "Steve",
                            SkinUtils.SkinModelType.CLASSIC
                        )
                        acc.accessToken = "0"
                        acc.refreshToken = "0"
                    }
                    onLoginDone(minecraftAccount)
                }
            } catch (e: IOException) {
                onLoginError(e)
            }
            false
        }

    /* Account main menu refresh listener */
    private val mRefreshAccountsListener: ExtraListener<Boolean?> =
        ExtraListener { k: String?, v: Boolean? ->
            reload()
            false
        }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }


    private fun init() {
        mAdapter = Adapter(context)
        adapter = mAdapter
        onItemSelectedListener = this
        reload()

        setBackgroundColor(resources.getColor(R.color.background_status_bar))
        mLoginBarPaint.color = resources.getColor(R.color.minebutton_color)
        mLoginBarPaint.strokeWidth =
            resources.getDimensionPixelOffset(R.dimen._2sdp).toFloat()
        mLoginStepAnimator.addUpdateListener(this)
        mLoginStep = mMaxSteps.toFloat()

        ExtraCore.addExtraListener(ExtraConstants.MOJANG_LOGIN_TODO, mMojangLoginListener)
        ExtraCore.addExtraListener(ExtraConstants.MICROSOFT_LOGIN_TODO, mMicrosoftLoginListener)
        ExtraCore.addExtraListener(ExtraConstants.ELYBY_LOGIN_TODO, mElyByLoginListener)
        ExtraCore.addExtraListener(ExtraConstants.REFRESH_ACCOUNT_SPINNER, mRefreshAccountsListener)
    }

    private fun reload() {
        PojavApplication.sExecutorService.execute(Runnable {
            try {
                val accounts = Accounts.load()
                runOnUiThread(Runnable { refresh(accounts) })
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        })
    }

    private fun refresh(accounts: Accounts) {
        mAdapter!!.setNotifyOnChange(false)
        mAdapter!!.clear()
        mAdapter!!.add(null)
        mAdapter!!.setNotifyOnChange(true)
        if (accounts.accounts != null) mAdapter!!.addAll(accounts.accounts)

        if (accounts.accounts == null || accounts.accounts.isEmpty()) {
            setSelection(0)
        } else {
            setSelection(accounts.selectionIndex + 1)
            val selected = selectedItem as? MinecraftAccount
            if (selected != null) {
                refreshAccount(selected)
            }
        }
    }

    private fun refreshAccount(minecraftAccount: MinecraftAccount) {
        // Wait until all tasks (including other possible login tasks) are done before
        // attempting to refresh the account.
        waitUntilDone(Runnable {
            // Reload the account data before attempting to refresh (what if it was already refreshed in the background?)
            val refreshAccount = minecraftAccount.reload()
            if (refreshAccount == null) return@Runnable
            val authType = refreshAccount.authType
            if (authType.requiresLogin() && System.currentTimeMillis() > refreshAccount.expiresAt) {
                authType.createAuth()!!.refreshAccount(this, refreshAccount)
            }
        })
    }

    private fun dismissPopup() {
        onDetachedFromWindow()
        onAttachedToWindow()
    }

    private fun createAccount() {
        setSelection(0)
        dismissPopup()
        ExtraCore.setValue(ExtraConstants.SELECT_AUTH_METHOD, true)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bottom = height - mLoginBarPaint.strokeWidth / 2
        val lineFillPercent = (mLoginStep / mMaxSteps)
        canvas.drawLine(0f, bottom, lineFillPercent * width, bottom, mLoginBarPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.x < paddingLeft || event.x > width - paddingRight) {
            return false
        }
        return super.onTouchEvent(event)
    }

    override fun onLoginDone(account: MinecraftAccount?) {
        mLoginStep = mMaxSteps.toFloat()
        invalidate()

        Toast.makeText(context, R.string.main_login_done, Toast.LENGTH_SHORT).show()
        Accounts.current = account
        reload()
    }


    override fun onLoginError(errorMessage: Throwable?) {
        mLoginBarPaint.color = Color.RED
        invalidate()

        val context = context
        if (context !is Activity) return
        if (context is LifecycleOwner) {
            val lifecycleOwner = context as LifecycleOwner
            val state = lifecycleOwner.lifecycle.currentState
            if (state != Lifecycle.State.RESUMED) return
        }

        if (errorMessage is PresentedException) {
            if (errorMessage.cause == null) {
                dialog(
                    context,
                    context.getString(R.string.global_error),
                    errorMessage.toString(context)
                )
            } else {
                showError(context, errorMessage.toString(context), errorMessage.cause!!)
            }
        } else if (errorMessage != null) {
            showError(context, errorMessage)
        }
    }

    override fun onLoginProgress(step: Int) {
        mLoginStepAnimator.cancel()
        mLoginStepAnimator.setFloatValues(mLoginStep, step.toFloat())
        mLoginStepAnimator.start()
    }

    override fun setMaxLoginProgress(max: Int) {
        mMaxSteps = max
    }

    override fun onItemSelected(adapterView: AdapterView<*>, view: View?, i: Int, l: Long) {
        val minecraftAccount = mAdapter!!.getItem(i)
        if (minecraftAccount == null) {
            if (i == 0) {
                createAccount()
            } else {
                showError(adapterView.context, NullPointerException())
            }
            return
        }
        Accounts.current = minecraftAccount
        refreshAccount(minecraftAccount)
        dismissPopup()
    }

    override fun onNothingSelected(adapterView: AdapterView<*>?) {}

    override fun onAnimationUpdate(valueAnimator: ValueAnimator) {
        mLoginStep = valueAnimator.animatedValue as Float
        invalidate()
    }

    private inner class Adapter(context: Context) :
        ArrayAdapter<MinecraftAccount?>(context, R.layout.item_minecraft_account) {
        private val mSkinHeadCache = HashMap<Int?, BitmapDrawable?>()
        private val mInflater: LayoutInflater = LayoutInflater.from(context)

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var cv = convertView
            if (cv == null) {
                cv = mInflater.inflate(R.layout.item_minecraft_account, parent, false)
            }
            populateView(cv!!, position, false)
            return cv
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            var cv = convertView
            if (cv == null) {
                cv = mInflater.inflate(R.layout.item_minecraft_account, parent, false)
            }
            populateView(cv!!, position, true)
            return cv
        }

        fun populateView(view: View, position: Int, isDropDown: Boolean) {
            val resources = resources
            val theme = context.theme

            val textview = view.findViewById<ExtendedTextView>(R.id.account_item)
            val deleteButton = view.findViewById<ImageView>(R.id.delete_account_button)

            if (position == 0) {
                // "Add account" button
                var plusDrawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_add, theme)
                if (plusDrawable != null) {
                    plusDrawable = DrawableCompat.wrap(plusDrawable).mutate()
                    DrawableCompat.setTint(plusDrawable, LauncherPreferences.PREF_GLOBAL_ICON_COLOR)
                }
                textview.setCompoundDrawables(plusDrawable, null, null, null)
                textview.setText(R.string.main_add_account)
                deleteButton.visibility = GONE
                // Only activate the listener behaviour when in drop-down mode
                // or when there's no accounts
                if (isDropDown || count == 1) view.setOnClickListener { createAccount() }
                return
            }

            if (isDropDown) {
                deleteButton.visibility = VISIBLE
                deleteButton.setOnClickListener { v: View? ->
                    showDeleteDialog(
                        v!!.context,
                        position
                    )
                }
            } else {
                deleteButton.visibility = GONE
            }


            val account = getItem(position) ?: return

            val authTypeResource = account.authType.iconResource

            var authType: Drawable? = null
            if (authTypeResource != 0) {
                authType = ResourcesCompat.getDrawable(resources, authTypeResource, theme)
            }

            val headCacheHash = System.identityHashCode(account)
            var accountHead = mSkinHeadCache[headCacheHash]
            if (accountHead == null) {
                val accountSkinFace = account.skinFace
                accountHead = BitmapDrawable(resources, accountSkinFace)
                mSkinHeadCache[headCacheHash] = accountHead
            }

            textview.text = account.username
            textview.setCompoundDrawablesRelative(accountHead, null, authType, null)
        }

        fun showDeleteDialog(context: Context, position: Int) {
            MaterialAlertDialogBuilder(context)
                .setMessage(R.string.warning_remove_account)
                .setPositiveButton(android.R.string.cancel, null)
                .setNeutralButton(
                    R.string.global_delete
                ) { _, _ ->
                    val account = getItem(position)
                    if (account != null) {
                        Accounts.delete(account)
                    }
                    reload()
                }
                .show()
        }
    }
}
