package git.artdeell.mojo.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.util.AttributeSet;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 3D preview of a Minecraft skin using OpenGL ES 1.x (fixed function, no shaders).
 * Builds the player from six boxes (head, body, two arms, two legs) with their
 * outer "overlay" layers, and turns it slowly. Tap to pause the turning.
 */
public class SkinModelView extends GLSurfaceView {
    private final Renderer mRenderer;

    public SkinModelView(Context context) { this(context, null); }

    public SkinModelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(1);
        // Opaque surface without alpha: a translucent one lets the dialog show through the model
        getHolder().setFormat(android.graphics.PixelFormat.OPAQUE);
        setEGLConfigChooser(8, 8, 8, 0, 16, 0);
        mRenderer = new Renderer();
        setRenderer(mRenderer);
        setRenderMode(RENDERMODE_CONTINUOUSLY);
        setOnClickListener(v -> mRenderer.spinning = !mRenderer.spinning);
    }

    /** Uploads a 64x64 skin. Must be called from any thread; the upload happens on the GL thread. */
    public void setSkin(Bitmap skin, boolean slim) {
        mRenderer.pendingSkin = skin;
        mRenderer.slim = slim;
        mRenderer.skinChanged = true;
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        onResume();
    }

    @Override protected void onDetachedFromWindow() {
        onPause();
        super.onDetachedFromWindow();
    }

    private static class Renderer implements GLSurfaceView.Renderer {
        volatile Bitmap pendingSkin;
        volatile boolean skinChanged;
        volatile boolean slim;
        volatile boolean spinning = true;
        private int texture = -1;
        private float angle = 25f; // front (+Z) faces the camera, turned a little
        private long last = System.currentTimeMillis();

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            gl.glDisable(GL10.GL_DITHER);
            gl.glEnable(GL10.GL_TEXTURE_2D);
            // Without depth testing the back faces were painted over the front ones (hair over the face)
            gl.glEnable(GL10.GL_DEPTH_TEST);
            gl.glDepthFunc(GL10.GL_LEQUAL);
            gl.glEnable(GL10.GL_ALPHA_TEST);
            gl.glAlphaFunc(GL10.GL_GREATER, 0.5f);
            gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);
            gl.glClearColor(0.267f, 0.267f, 0.267f, 1f); // same gray as the dialog
            int[] ids = new int[1];
            gl.glGenTextures(1, ids, 0);
            texture = ids[0];
            gl.glBindTexture(GL10.GL_TEXTURE_2D, texture);
            gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
            gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST);
            gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
            gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
            // Default texture: a plain gray skin until one is set
            Bitmap blank = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
            blank.eraseColor(0xFF9E9E9E);
            GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, blank, 0);
            blank.recycle();
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            gl.glViewport(0, 0, width, height);
            gl.glMatrixMode(GL10.GL_PROJECTION);
            gl.glLoadIdentity();
            float ratio = (float) width / Math.max(1, height);
            gl.glFrustumf(-ratio * 0.12f, ratio * 0.12f, -0.12f, 0.12f, 0.3f, 20f);
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            if (skinChanged) {
                skinChanged = false;
                Bitmap bmp = pendingSkin;
                if (bmp != null && bmp.getWidth() == 64 && bmp.getHeight() == 64) {
                    gl.glBindTexture(GL10.GL_TEXTURE_2D, texture);
                    GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bmp, 0);
                }
            }

            long now = System.currentTimeMillis();
            if (spinning) angle += (now - last) * 0.03f;
            last = now;

            gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
            gl.glMatrixMode(GL10.GL_MODELVIEW);
            gl.glLoadIdentity();
            gl.glTranslatef(0f, -1f, -4f);
            gl.glRotatef(-10f, 1f, 0f, 0f);
            gl.glRotatef(angle, 0f, 1f, 0f);
            gl.glBindTexture(GL10.GL_TEXTURE_2D, texture);

            // Model units: 1 pixel = 1/16 block; the figure spans y 0..32 (feet to top of head)
            float s = 1f / 16f;
            int aw = slim ? 3 : 4;
            float rightArmX = -(4 + aw / 2f);
            // Base layer: {x, y(bottom), z, u, v, w, h, d}
            drawBox(gl, 0, 24, 0, 0, 0, 8, 8, 8, 0f, s);            // head
            drawBox(gl, 0, 12, 0, 16, 16, 8, 12, 4, 0f, s);         // body
            drawBox(gl, rightArmX, 12, 0, 40, 16, aw, 12, 4, 0f, s); // arm (viewer's left)
            drawBox(gl, 6, 12, 0, 32, 48, 4, 12, 4, 0f, s);          // arm (viewer's right)
            drawBox(gl, -2, 0, 0, 0, 16, 4, 12, 4, 0f, s);           // leg
            drawBox(gl, 2, 0, 0, 16, 48, 4, 12, 4, 0f, s);           // leg
            // Overlay layer: same shapes, a little bigger, UVs shifted by the second layer
            float g = 0.5f;
            drawBox(gl, 0, 24, 0, 32, 0, 8, 8, 8, g, s);
            drawBox(gl, 0, 12, 0, 16, 32, 8, 12, 4, g, s);
            drawBox(gl, rightArmX, 12, 0, 40, 32, aw, 12, 4, g, s);
            drawBox(gl, 6, 12, 0, 48, 48, 4, 12, 4, g, s);
            drawBox(gl, -2, 0, 0, 0, 32, 4, 12, 4, g, s);
            drawBox(gl, 2, 0, 0, 0, 48, 4, 12, 4, g, s);
        }

        /**
         * Draws one box. (x, y, z) is the center of the bottom face in pixel units, and
         * (u, v) is the texture origin of the part. Faces follow the Minecraft UV layout.
         */
        private void drawBox(GL10 gl, float x, float y, float z, int u, int v, int w, int h, int d, float grow, float s) {
            float x0 = (x - w / 2f - grow) * s, x1 = (x + w / 2f + grow) * s;
            float y0 = (y - grow) * s,          y1 = (y + h + grow) * s;
            float z0 = (z - d / 2f - grow) * s, z1 = (z + d / 2f + grow) * s;
            // Right (-X side in viewer terms), front (+Z), left, back, top, bottom
            face(gl, x0, y0, z0, x0, y1, z0, x0, y1, z1, x0, y0, z1, u, v + d, d, h);            // right
            face(gl, x0, y0, z1, x0, y1, z1, x1, y1, z1, x1, y0, z1, u + d, v + d, w, h);        // front
            face(gl, x1, y0, z1, x1, y1, z1, x1, y1, z0, x1, y0, z0, u + d + w, v + d, d, h);    // left
            face(gl, x1, y0, z0, x1, y1, z0, x0, y1, z0, x0, y0, z0, u + 2 * d + w, v + d, w, h);// back
            face(gl, x0, y1, z1, x0, y1, z0, x1, y1, z0, x1, y1, z1, u + d, v, w, d);            // top
            face(gl, x0, y0, z0, x0, y0, z1, x1, y0, z1, x1, y0, z0, u + d + w, v, w, d);        // bottom
        }

        private void face(GL10 gl, float ax, float ay, float az, float bx, float by, float bz,
                          float cx, float cy, float cz, float dx, float dy, float dz,
                          int u, int v, int w, int h) {
            float u0 = u / 64f, u1 = (u + w) / 64f, v0 = v / 64f, v1 = (v + h) / 64f;
            float[] verts = {ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz};
            float[] uvs = {u0, v1, u0, v0, u1, v0, u1, v1};
            java.nio.FloatBuffer vb = java.nio.ByteBuffer.allocateDirect(verts.length * 4)
                    .order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer().put(verts);
            vb.position(0);
            java.nio.FloatBuffer tb = java.nio.ByteBuffer.allocateDirect(uvs.length * 4)
                    .order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer().put(uvs);
            tb.position(0);
            drawQuad(gl, vb, tb);
        }

        private void drawQuad(GL10 gl, java.nio.FloatBuffer vb, java.nio.FloatBuffer tb) {
            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vb);
            gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, tb);
            gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, 4);
            gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
            gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        }
    }
}
