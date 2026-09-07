package net.kdt.pojavlaunch.awt;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import net.kdt.pojavlaunch.game.platform.Platform;

public class AWTView extends SurfaceView implements SurfaceHolder.Callback {
    public static final int AWT_CANVAS_WIDTH = 1024;
    public static final int AWT_CANVAS_HEIGHT = 768;

    public AWTView(Context ctx) {
        this(ctx, null);
    }

    public AWTView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        this.getHolder().addCallback(this);

        post(this::refreshSize);
    }

    /**
     * Make the view fit the proper aspect ratio of the surface
     */
    private void refreshSize() {
        ViewGroup.LayoutParams layoutParams = getLayoutParams();

        if (getHeight() < getWidth()) {
            layoutParams.width = AWT_CANVAS_WIDTH * getHeight() / AWT_CANVAS_HEIGHT;
        } else {
            layoutParams.height = AWT_CANVAS_HEIGHT * getWidth() / AWT_CANVAS_WIDTH;
        }

        setLayoutParams(layoutParams);
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        this.refreshSize();
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        getHolder().setFixedSize(AWT_CANVAS_WIDTH, AWT_CANVAS_HEIGHT);
        Platform.PLATFORM.surfaceCreated(holder.getSurface());
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        Platform.PLATFORM.surfaceDestroyed();
    }
}
