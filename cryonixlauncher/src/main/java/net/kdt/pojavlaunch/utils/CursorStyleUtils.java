package net.kdt.pojavlaunch.utils;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

public final class CursorStyleUtils {
    public static final String SCALE_KEY = "cryonix_cursor_scale";
    public static final String MOTION_KEY = "cryonix_cursor_motion_jelly";
    public static final String COLOR_KEY = "cryonix_cursor_color";
    public static final String TEMPLATE_KEY = "cryonix_cursor_template";
    public static final String CUSTOM_PATH_KEY = "cryonix_cursor_custom_path";
    public static final String OUTLINE_KEY = "cryonix_cursor_outline";
    public static final String TRAIL_KEY = "cryonix_cursor_trail";
    public static final String OPACITY_KEY = "cryonix_cursor_opacity";
    public static final String ROTATION_KEY = "cryonix_cursor_rotation";
    public static final String SHADOW_KEY = "cryonix_cursor_shadow";

    public static final String[] TEMPLATES = {
            "Classic", "Dot", "Triangle", "Square", "Circle", "Crosshair",
            "Diamond", "Arrow", "Hand", "Star", "Ring", "Plus"
    };

    public static final int[] COLORS = {
            0xFFFFFFFF, 0xFF4A9BFF, 0xFF00D6A0, 0xFFFFD166,
            0xFFFF6B8A, 0xFFB98CFF, 0xFFFF8C42, 0xFF00E5FF,
            0xFF8EEBFF, 0xFF7DFF5B, 0xFFFF8ACB, 0xFF39FF88, 0xFF64FFDA, 0xFFB7FF00
    };

    private CursorStyleUtils() {}

    public static void drawTemplate(Canvas canvas, Paint paint, String template, int color) {
        drawTemplate(canvas, paint, template, color, false);
    }

    public static void drawTemplate(Canvas canvas, Paint paint, String template, int color, boolean outline) {
        if (outline) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5f);
            paint.setColor(0xCC020713);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            drawShape(canvas, paint, template);
        }

        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(3f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        drawShape(canvas, paint, template);
    }

    private static void drawShape(Canvas canvas, Paint paint, String template) {
        if ("Triangle".equals(template)) {
            Path triangle = new Path();
            triangle.moveTo(9f, 0f);
            triangle.lineTo(18f, 18f);
            triangle.lineTo(0f, 18f);
            triangle.close();
            canvas.drawPath(triangle, paint);
            return;
        }

        if ("Square".equals(template)) {
            canvas.drawRoundRect(1f, 1f, 17f, 17f, 3f, 3f, paint);
            return;
        }

        if ("Circle".equals(template)) {
            canvas.drawCircle(9f, 9f, 8f, paint);
            return;
        }

        if ("Star".equals(template)) {
            Path star = new Path();
            for (int i = 0; i < 10; i++) {
                double a = -Math.PI / 2d + i * Math.PI / 5d;
                float r = (i % 2 == 0) ? 9f : 4f;
                float x = 9f + (float) Math.cos(a) * r;
                float y = 9f + (float) Math.sin(a) * r;
                if (i == 0) star.moveTo(x, y); else star.lineTo(x, y);
            }
            star.close();
            canvas.drawPath(star, paint);
            return;
        }

        if ("Ring".equals(template)) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3f);
            canvas.drawCircle(9f, 9f, 7f, paint);
            return;
        }

        if ("Plus".equals(template)) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4f);
            canvas.drawLine(9f, 1f, 9f, 17f, paint);
            canvas.drawLine(1f, 9f, 17f, 9f, paint);
            return;
        }


        if ("Dot".equals(template)) {
            canvas.drawCircle(9f, 9f, 8f, paint);
            return;
        }

        if ("Crosshair".equals(template)) {
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(9f, 9f, 6f, paint);
            canvas.drawLine(9f, -1f, 9f, 3f, paint);
            canvas.drawLine(9f, 15f, 9f, 19f, paint);
            canvas.drawLine(-1f, 9f, 3f, 9f, paint);
            canvas.drawLine(15f, 9f, 19f, 9f, paint);
            return;
        }

        if ("Diamond".equals(template)) {
            Path diamond = new Path();
            diamond.moveTo(9f, 0f);
            diamond.lineTo(18f, 9f);
            diamond.lineTo(9f, 18f);
            diamond.lineTo(0f, 9f);
            diamond.close();
            canvas.drawPath(diamond, paint);
            return;
        }

        if ("Hand".equals(template)) {
            canvas.drawRoundRect(4f, 6f, 16f, 19f, 5f, 5f, paint);
            canvas.drawRoundRect(6f, 0f, 10f, 10f, 2f, 2f, paint);
            canvas.drawRoundRect(10f, 2f, 14f, 10f, 2f, 2f, paint);
            return;
        }

        Path arrow = new Path();
        arrow.moveTo(1f, 1f);
        arrow.lineTo(4f, 21f);
        arrow.lineTo(9f, 15f);
        arrow.lineTo(15f, 20f);
        arrow.lineTo(18f, 17f);
        arrow.lineTo(12f, 12f);
        arrow.close();
        canvas.drawPath(arrow, paint);
    }
}
