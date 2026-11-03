package git.artdeell.mojo.utils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import java.util.Random;

import git.artdeell.mojo.R;

/**
 * In-game look for the launcher screens: gray beveled buttons, pixel font with shadow and a
 * tiled dark dirt background. Everything is drawn in code, no textures are copied from other mods.
 */
public class McStyle {
    private static Drawable sDirt;

    /** Font for normal text (the game's regular font). */
    public static Typeface font(Context c) {
        try {
            Typeface t = ResourcesCompat.getFont(c, R.font.minecraft_regular);
            if(t != null) return t;
        } catch (Exception ignored) {}
        return Typeface.DEFAULT;
    }

    /** Bold pixel font for the capitalized titles (MINECRAFT: JAVA EDITION, PLAY). */
    public static Typeface heading(Context c) {
        try {
            Typeface t = ResourcesCompat.getFont(c, R.font.minecraft_ten);
            if(t != null) return t;
        } catch (Exception ignored) {}
        return font(c);
    }

    /** A flat rectangle with a 2px black border and a light / dark bevel, like the game's buttons. */
    private static class ButtonDrawable extends Drawable {
        private final Paint mPaint = new Paint();
        private final int mFill, mLight, mDark, mBorder;
        private final float mPx;

        ButtonDrawable(Context c, int fill, int light, int dark, int border) {
            mFill = fill; mLight = light; mDark = dark; mBorder = border;
            mPx = Math.max(1f, c.getResources().getDisplayMetrics().density * 2f);
        }

        @Override public void draw(@NonNull Canvas canvas) {
            float w = getBounds().width(), h = getBounds().height();
            mPaint.setColor(mBorder);
            canvas.drawRect(0, 0, w, h, mPaint);
            mPaint.setColor(mDark);
            canvas.drawRect(mPx, mPx, w - mPx, h - mPx, mPaint);
            mPaint.setColor(mLight);
            canvas.drawRect(mPx, mPx, w - 2 * mPx, h - 2 * mPx, mPaint);
            mPaint.setColor(mFill);
            canvas.drawRect(2 * mPx, 2 * mPx, w - 2 * mPx, h - 2 * mPx, mPaint);
        }

        @Override public void setAlpha(int alpha) {}
        @Override public void setColorFilter(ColorFilter colorFilter) {}
        @Override public int getOpacity() { return PixelFormat.OPAQUE; }
    }

    public static Drawable buttonBackground(Context c, boolean green) {
        int base = green ? PlayColor.get() : 0xFF6E6E6E;
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{-android.R.attr.state_enabled},
                new ButtonDrawable(c, 0xFF2E2E2E, 0xFF4A4A4A, 0xFF1E1E1E, 0xFF000000));
        s.addState(new int[]{android.R.attr.state_pressed},
                new ButtonDrawable(c, green ? shade(base, 0.8f) : 0xFF5C6A9A, shade(base, 1.2f), 0xFF22284A, 0xFFFFFFFF));
        s.addState(new int[]{},
                new ButtonDrawable(c, base, green ? shade(base, 1.35f) : 0xFFA6A6A6, green ? shade(base, 0.6f) : 0xFF3B3B3B, 0xFF000000));
        return s;
    }

    private static int shade(int color, float f) {
        return Color.argb(255, Math.min(255, Math.round(Color.red(color) * f)),
                Math.min(255, Math.round(Color.green(color) * f)), Math.min(255, Math.round(Color.blue(color) * f)));
    }

    /** Pixel font, white text with the game's dark drop shadow. */
    public static void text(TextView t) {
        t.setTypeface(font(t.getContext()));
        float d = t.getContext().getResources().getDisplayMetrics().density;
        t.setShadowLayer(1f, d, d, 0xFF3F3F3F);
    }

    public static void headingText(TextView t) {
        t.setTypeface(heading(t.getContext()));
        float d = t.getContext().getResources().getDisplayMetrics().density;
        t.setShadowLayer(1f, d, d, 0xFF3F3F3F);
    }

    public static void button(TextView b, boolean green) {
        Context c = b.getContext();
        b.setBackground(buttonBackground(c, green));
        text(b);
        b.setTextColor(new ColorStateList(
                new int[][]{{-android.R.attr.state_enabled}, {}},
                new int[]{0xFF777777, Color.WHITE}));
        b.setAllCaps(false);
    }

    /** Dirt background dimmed so the text stays readable. */
    public static Drawable screenBackground(Context c) {
        return new android.graphics.drawable.LayerDrawable(new Drawable[]{
                dirt(c), new android.graphics.drawable.ColorDrawable(0x99000000)});
    }

    /** Tiled dark dirt, generated once. */
    public static Drawable dirt(Context c) {
        if(sDirt == null) {
            Random rnd = new Random(7);
            Bitmap tile = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888);
            for(int x = 0; x < 16; x++) {
                for(int y = 0; y < 16; y++) {
                    int v = 40 + rnd.nextInt(18);
                    int r = v + 14, g = v + 6, b = v - 4;
                    if(rnd.nextInt(14) == 0) { r += 14; g += 10; b += 6; }
                    tile.setPixel(x, y, Color.rgb(Math.max(0, r), Math.max(0, g), Math.max(0, b)));
                }
            }
            Bitmap big = Bitmap.createScaledBitmap(tile, 96, 96, false);
            BitmapDrawable d = new BitmapDrawable(c.getResources(), big);
            d.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
            d.setFilterBitmap(false);
            sDirt = d;
        }
        Drawable.ConstantState state = sDirt.getConstantState();
        return state != null ? state.newDrawable(c.getResources()) : sDirt;
    }
}
