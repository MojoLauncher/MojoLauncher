package net.kdt.pojavlaunch.awt;

import android.content.*;
import android.graphics.*;
import android.os.Build;
import android.text.*;
import android.util.*;
import android.view.*;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.*;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.game.platform.Platform;
import net.kdt.pojavlaunch.render.SurfaceProvider;
import net.kdt.pojavlaunch.utils.*;

public class AWTView extends SurfaceView implements SurfaceHolder.Callback {
    public static final int AWT_CANVAS_WIDTH = 720;
    public static final int AWT_CANVAS_HEIGHT = 600;
    private static final int MAX_SIZE = 100;
    private static final double NANOS = 1000000000.0;
    private boolean mIsDestroyed = false;
    
    public AWTView(Context ctx) {
        this(ctx, null);
    }
    
    public AWTView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        this.getHolder().addCallback(this);

        post(this::refreshSize);
    }

    /** Make the view fit the proper aspect ratio of the surface */
    private void refreshSize(){
        ViewGroup.LayoutParams layoutParams = getLayoutParams();

        if(getHeight() < getWidth()){
            layoutParams.width = AWT_CANVAS_WIDTH * getHeight() / AWT_CANVAS_HEIGHT;
        }else{
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
        AWTWindow.setNativeSize(AWT_CANVAS_WIDTH, AWT_CANVAS_HEIGHT);
        getHolder().setFixedSize(AWT_CANVAS_WIDTH, AWT_CANVAS_HEIGHT);
        Platform.PLATFORM.surfaceCreated(holder.getSurface());
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        Platform.PLATFORM.surfaceDestroyed();
    }
}
