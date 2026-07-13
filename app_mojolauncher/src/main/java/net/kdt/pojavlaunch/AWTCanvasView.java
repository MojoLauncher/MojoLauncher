package net.kdt.pojavlaunch;

import android.content.*;
import android.graphics.*;
import android.os.Build;
import android.text.*;
import android.util.*;
import android.view.*;

import java.nio.ByteBuffer;
import java.util.*;
import net.kdt.pojavlaunch.utils.*;

public class AWTCanvasView extends TextureView implements TextureView.SurfaceTextureListener, Runnable {
    public static final int AWT_CANVAS_WIDTH = 720;
    public static final int AWT_CANVAS_HEIGHT = 600;
    private static final int MAX_SIZE = 100;
    private static final double NANOS = 1000000000.0;
    private boolean mIsDestroyed = false;
    private final TextPaint mFpsPaint;

    // Temporary count fps https://stackoverflow.com/a/13729241
    private final LinkedList<Long> mTimes = new LinkedList<Long>(){{add(System.nanoTime());}};
    
    public AWTCanvasView(Context ctx) {
        this(ctx, null);
    }
    
    public AWTCanvasView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        
        mFpsPaint = new TextPaint();
        mFpsPaint.setColor(Color.WHITE);
        mFpsPaint.setTextSize(20);

        setOpaque(true);
        setSurfaceTextureListener(this);

        post(this::refreshSize);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture texture, int w, int h) {
        getSurfaceTexture().setDefaultBufferSize(AWT_CANVAS_WIDTH, AWT_CANVAS_HEIGHT);
        mIsDestroyed = false;
        new Thread(this, "AndroidAWTRenderer").start();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
        mIsDestroyed = true;
        return true;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int w, int h) {
        getSurfaceTexture().setDefaultBufferSize(AWT_CANVAS_WIDTH, AWT_CANVAS_HEIGHT);
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        getSurfaceTexture().setDefaultBufferSize(AWT_CANVAS_WIDTH, AWT_CANVAS_HEIGHT);
    }

    @Override
    public void run() {
        Canvas canvas;
        Surface surface = new Surface(getSurfaceTexture());
        Bitmap rgbArrayBitmap = Bitmap.createBitmap(AWT_CANVAS_WIDTH, AWT_CANVAS_HEIGHT, Bitmap.Config.ARGB_8888);
        ByteBuffer targetBuffer = ByteBuffer.allocateDirect(rgbArrayBitmap.getByteCount());
        Paint paint = new Paint();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            paint.setBlendMode(BlendMode.SRC);
        }else{
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        }
        boolean mDrawing;
        try {
            canvas = surface.lockCanvas(null);
            while (!mIsDestroyed && surface.isValid()) {
                surface.unlockCanvasAndPost(canvas);
                canvas = surface.lockCanvas(null);
                mDrawing = JREUtils.renderAWTScreenFrame(targetBuffer);
                targetBuffer.rewind();
                if (mDrawing) {
                    canvas.save();
                    rgbArrayBitmap.copyPixelsFromBuffer(targetBuffer);
                    canvas.drawBitmap(rgbArrayBitmap, 0, 0, paint);
                    canvas.restore();
                }else {
                    canvas.drawRGB(0,0,0);
                }
                canvas.drawText("FPS: " + (Math.round(fps() * 10) / 10) + ", drawing=" + mDrawing, 0, 20, mFpsPaint);
            }
        } catch (Throwable throwable) {
            Tools.showError(getContext(), throwable);
        }
        rgbArrayBitmap.recycle();
        surface.release();
    }

    /** Calculates and returns frames per second */
    private double fps() {
        long lastTime = System.nanoTime();
        double difference = (lastTime - mTimes.getFirst()) / NANOS;
        mTimes.addLast(lastTime);
        int size = mTimes.size();
        if (size > MAX_SIZE) {
            mTimes.removeFirst();
        }
        return difference > 0 ? mTimes.size() / difference : 0.0;
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

}
