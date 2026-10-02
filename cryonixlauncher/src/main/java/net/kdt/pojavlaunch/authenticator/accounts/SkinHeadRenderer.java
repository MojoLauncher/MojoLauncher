package net.kdt.pojavlaunch.authenticator.accounts;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

public class SkinHeadRenderer {
    private static final Matrix MIRROR_MATRIX = new Matrix();
    static {
        MIRROR_MATRIX.setScale(-1, 1);
    }

    // Points of an isometric cube
    private static final float[][] ISO_POINTS = {
            {0.5f,         1.0f }, // 0 Bottom-most point
            {0.9330127f,   0.75f}, // 1 Bottom right point
            {0.066987306f, 0.75f}, // 2 Bottom left point
            {0.5f,         0.0f }, // 3 Topmost point
            {0.9330127f,   0.25f}, // 4 Top right point
            {0.066987306f, 0.25f}, // 5 Top left point
            {0.5f,         0.5f }  // 6 Center point
    };

    // Faces of an isometric cube
    private static final int FACE_LEFT = 0;
    private static final int FACE_RIGHT = 1;
    private static final int FACE_TOP = 2;
    private static final int FACE_REAR_RIGHT = 3;
    private static final int FACE_REAR_LEFT = 4;
    private static final int FACE_BOTTOM = 5;

    private int mCoordScale = 1;
    private final float[] mMeshBuffer = new float[8];
    private final Set<Bitmap> mTempBitmaps = new HashSet<>();

    private Bitmap getSubregion(Bitmap src, int left, int top, int right, int bottom) {
        return getSubregion(src, left, top, right, bottom, false);
    }

    private Bitmap internalGetSubregion(Bitmap src, int left, int top, int right, int bottom, boolean mirror) {
        Bitmap bitmap =  Bitmap.createBitmap(src, left, top, right - left, bottom - top);
        if(!mirror) return bitmap;
        Bitmap mirroredBitmap = Bitmap.createBitmap(
                bitmap,
                0, 0,
                bitmap.getWidth(), bitmap.getHeight(),
                MIRROR_MATRIX, false
        );
        bitmap.recycle();
        return mirroredBitmap;
    }

    private Bitmap getSubregion(Bitmap src, int left, int top, int right, int bottom, boolean mirror) {
        // Provision for HD skins: scale regular skin coordinate inputs
        left   *= mCoordScale;
        top    *= mCoordScale;
        right  *= mCoordScale;
        bottom *= mCoordScale;

        Bitmap subregion = internalGetSubregion(src, left, top, right, bottom, mirror);
        mTempBitmaps.add(subregion);

        return subregion;
    }

    /**
     * Write one face of an isometric cube as mesh points suitable for drawBitmapMesh()
     * @param dst destination array for mesh (should have length of 8)
     * @param face the face to write (one of the face constants)
     * @param mul the amount by how much each point should be multiplied
     * @param off the amount by how much each point should be offset to the right
     */
    private void applyFace(float[] dst, int face, float mul, float off) {
        switch (face) {
            case FACE_LEFT:
                dst[0] = ISO_POINTS[5][0] * mul + off; dst[1] = ISO_POINTS[5][1] * mul + off;
                dst[2] = ISO_POINTS[6][0] * mul + off; dst[3] = ISO_POINTS[6][1] * mul + off;
                dst[4] = ISO_POINTS[2][0] * mul + off; dst[5] = ISO_POINTS[2][1] * mul + off;
                dst[6] = ISO_POINTS[0][0] * mul + off; dst[7] = ISO_POINTS[0][1] * mul + off;
                return;
            case FACE_RIGHT:
                dst[0] = ISO_POINTS[6][0] * mul + off; dst[1] = ISO_POINTS[6][1] * mul + off;
                dst[2] = ISO_POINTS[4][0] * mul + off; dst[3] = ISO_POINTS[4][1] * mul + off;
                dst[4] = ISO_POINTS[0][0] * mul + off; dst[5] = ISO_POINTS[0][1] * mul + off;
                dst[6] = ISO_POINTS[1][0] * mul + off; dst[7] = ISO_POINTS[1][1] * mul + off;
                return;
            case FACE_TOP:
                dst[0] = ISO_POINTS[5][0] * mul + off; dst[1] = ISO_POINTS[5][1] * mul + off;
                dst[2] = ISO_POINTS[3][0] * mul + off; dst[3] = ISO_POINTS[3][1] * mul + off;
                dst[4] = ISO_POINTS[6][0] * mul + off; dst[5] = ISO_POINTS[6][1] * mul + off;
                dst[6] = ISO_POINTS[4][0] * mul + off; dst[7] = ISO_POINTS[4][1] * mul + off;
                return;
            case FACE_REAR_RIGHT:
                dst[0] = ISO_POINTS[3][0] * mul + off; dst[1] = ISO_POINTS[3][1] * mul + off;
                dst[2] = ISO_POINTS[4][0] * mul + off; dst[3] = ISO_POINTS[4][1] * mul + off;
                dst[4] = ISO_POINTS[6][0] * mul + off; dst[5] = ISO_POINTS[6][1] * mul + off;
                dst[6] = ISO_POINTS[1][0] * mul + off; dst[7] = ISO_POINTS[1][1] * mul + off;
                return;
            case FACE_REAR_LEFT:
                dst[0] = ISO_POINTS[5][0] * mul + off; dst[1] = ISO_POINTS[5][1] * mul + off;
                dst[2] = ISO_POINTS[3][0] * mul + off; dst[3] = ISO_POINTS[3][1] * mul + off;
                dst[4] = ISO_POINTS[2][0] * mul + off; dst[5] = ISO_POINTS[2][1] * mul + off;
                dst[6] = ISO_POINTS[6][0] * mul + off; dst[7] = ISO_POINTS[6][1] * mul + off;
                return;
            case FACE_BOTTOM:
                dst[0] = ISO_POINTS[2][0] * mul + off; dst[1] = ISO_POINTS[2][1] * mul + off;
                dst[2] = ISO_POINTS[6][0] * mul + off; dst[3] = ISO_POINTS[6][1] * mul + off;
                dst[4] = ISO_POINTS[0][0] * mul + off; dst[5] = ISO_POINTS[0][1] * mul + off;
                dst[6] = ISO_POINTS[1][0] * mul + off; dst[7] = ISO_POINTS[1][1] * mul + off;
        }
    }

    private void drawMesh(Canvas canvas, Bitmap bitmap, int face, float multiplier, float offset) {
        applyFace(mMeshBuffer, face, multiplier, offset);
        canvas.drawBitmapMesh(bitmap,
                1, 1, mMeshBuffer,
                0, null, 0,
                null
        );
    }

    private boolean prepareCoordScale(int sourceDimension) {
        if(sourceDimension % 64 != 0) {
            Log.e("SkinHeadRenderer", "Invalid skin dimension: "+ sourceDimension);
            return false;
        }
        mCoordScale = sourceDimension / 64;
        return true;
    }

    public Bitmap render(int side, Bitmap sourceSkin) {
        if(!prepareCoordScale(sourceSkin.getWidth())) return null;

        // Bitmap overlay regions
        Bitmap overlayTopFace = getSubregion(sourceSkin, 40, 0, 48,8);
        Bitmap overlayLeftFace = getSubregion(sourceSkin, 32, 8, 40,16);
        Bitmap overlayRightFace = getSubregion(sourceSkin, 40, 8, 48,16);
        Bitmap overlayBottomFace = getSubregion(sourceSkin, 48, 0, 56, 8);
        Bitmap overlayRearLeftFace = getSubregion(sourceSkin, 56, 8, 64, 16, true);
        Bitmap overlayRearRightFace = getSubregion(sourceSkin, 48, 8, 56, 16, true);

        // Bitmap head regions
        Bitmap topFace = getSubregion(sourceSkin, 8,0,16,8);
        Bitmap leftFace = getSubregion(sourceSkin, 0,8,8,16);
        Bitmap rightFace = getSubregion(sourceSkin, 8,8,16,16);

        Bitmap renderTarget = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(renderTarget);

        float multiplier = 1f * side;

        // The head should be slightly smaller than the accessory overlay around it,
        // and should appear to be in the middle of the accessory overlay.
        float headOffset = multiplier / 16f;
        float headMultiplier = multiplier * 14f/16f;

        // Rear side of overlay layer
        drawMesh(canvas, overlayRearLeftFace, FACE_REAR_LEFT, multiplier, 0);
        drawMesh(canvas, overlayRearRightFace, FACE_REAR_RIGHT, multiplier, 0);
        drawMesh(canvas, overlayBottomFace, FACE_BOTTOM, multiplier, 0);

        // Player head
        drawMesh(canvas, leftFace, FACE_LEFT, headMultiplier,  headOffset);
        drawMesh(canvas, rightFace, FACE_RIGHT, headMultiplier,  headOffset);
        drawMesh(canvas, topFace, FACE_TOP, headMultiplier,  headOffset);

        // Front side of the overlay layer
        drawMesh(canvas, overlayLeftFace, FACE_LEFT, multiplier, 0);
        drawMesh(canvas, overlayRightFace, FACE_RIGHT, multiplier, 0);
        drawMesh(canvas, overlayTopFace, FACE_TOP, multiplier, 0);

        // Free all regions
        for(Bitmap region : mTempBitmaps) region.recycle();
        mTempBitmaps.clear();
        // Done!
        return renderTarget;
    }
}
