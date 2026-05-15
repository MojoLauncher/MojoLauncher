package net.kdt.pojavlaunch.customcontrols.handleview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.ashmeet.hyperlauncher.R;

public class DrawerPullButton extends View {
    public DrawerPullButton(Context context) {super(context); init();}
    public DrawerPullButton(Context context, @Nullable AttributeSet attrs) {super(context, attrs); init();}

    private final Paint mBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mBitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private VectorDrawableCompat mDefaultDrawable;
    private Bitmap mCustomBitmap;
    private final Rect mSrcRect = new Rect();
    private final RectF mDestRect = new RectF();

    private void init(){
        mDefaultDrawable = VectorDrawableCompat.create(getContext().getResources(), R.drawable.ic_sharp_settings_24, null);
        mBackgroundPaint.setColor(Color.BLACK);
        updateCustomImage();
    }

    public void updateCustomImage() {
        if (LauncherPreferences.PREF_DRAWER_BUTTON_IMAGE_PATH != null) {
            mCustomBitmap = BitmapFactory.decodeFile(LauncherPreferences.PREF_DRAWER_BUTTON_IMAGE_PATH);
        } else {
            mCustomBitmap = null;
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Draw a rounded square background
        float radius = LauncherPreferences.PREF_DRAWER_BUTTON_CORNER_RADIUS * getResources().getDisplayMetrics().density;
        mDestRect.set(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());
        
        mBackgroundPaint.setAlpha((int) (LauncherPreferences.PREF_DRAWER_BUTTON_BG_OPACITY * 255));
        canvas.drawRoundRect(mDestRect, radius, radius, mBackgroundPaint);

        int iconAlpha = (int) (LauncherPreferences.PREF_DRAWER_BUTTON_ICON_OPACITY * 255);

        if (mCustomBitmap != null) {
            mSrcRect.set(0, 0, mCustomBitmap.getWidth(), mCustomBitmap.getHeight());
            // Center bitmap inside the rounded rect
            float innerPadding = 4f * getResources().getDisplayMetrics().density;
            mDestRect.inset(innerPadding, innerPadding);
            mBitmapPaint.setAlpha(iconAlpha);
            canvas.drawBitmap(mCustomBitmap, mSrcRect, mDestRect, mBitmapPaint);
        } else {
            int iconPadding = (int) (4f * getResources().getDisplayMetrics().density);
            mDefaultDrawable.setBounds(
                getPaddingLeft() + iconPadding, 
                getPaddingTop() + iconPadding, 
                getWidth() - getPaddingRight() - iconPadding, 
                getHeight() - getPaddingBottom() - iconPadding
            );
            mDefaultDrawable.setAlpha(iconAlpha);
            mDefaultDrawable.draw(canvas);
        }
    }
}
