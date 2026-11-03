package git.artdeell.mojo.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

/** Flat front view (16x32 pixels) of a 64x64 skin, shown in the skin library grid. */
public class SkinFigure {
    private static void part(Canvas canvas, Paint paint, Bitmap skin, int sx, int sy, int w, int h, int dx, int dy) {
        canvas.drawBitmap(skin, new Rect(sx, sy, sx + w, sy + h), new Rect(dx, dy, dx + w, dy + h), paint);
    }

    public static Bitmap render(Bitmap skin, boolean slim) {
        Bitmap out = Bitmap.createBitmap(16, 32, Bitmap.Config.ARGB_8888);
        if(skin.getWidth() != 64 || skin.getHeight() != 64) return out;
        Canvas canvas = new Canvas(out);
        Paint paint = new Paint();
        paint.setFilterBitmap(false);
        int arm = slim ? 3 : 4;
        int rightArmX = slim ? 1 : 0;
        // base layers first, then the overlay layers
        part(canvas, paint, skin, 8, 8, 8, 8, 4, 0);
        part(canvas, paint, skin, 20, 20, 8, 12, 4, 8);
        part(canvas, paint, skin, 44, 20, arm, 12, rightArmX, 8);
        part(canvas, paint, skin, 36, 52, arm, 12, 12, 8);
        part(canvas, paint, skin, 4, 20, 4, 12, 4, 20);
        part(canvas, paint, skin, 20, 52, 4, 12, 8, 20);
        part(canvas, paint, skin, 40, 8, 8, 8, 4, 0);
        part(canvas, paint, skin, 20, 36, 8, 12, 4, 8);
        part(canvas, paint, skin, 44, 36, arm, 12, rightArmX, 8);
        part(canvas, paint, skin, 52, 52, arm, 12, 12, 8);
        part(canvas, paint, skin, 4, 36, 4, 12, 4, 20);
        part(canvas, paint, skin, 4, 52, 4, 12, 8, 20);
        return out;
    }
}
