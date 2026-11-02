package git.artdeell.mojo.authenticator.accounts;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import git.artdeell.mojo.utils.FileUtils;

/** Skins the user imported. They are stored as 64x64 PNG files in the app private storage. */
public class SkinLibrary {
    public static class Entry {
        public final String name;
        public final File file;
        Entry(String name, File file) { this.name = name; this.file = file; }
        @Override public String toString() { return name; }
    }

    private static File dir(Context context) {
        return new File(context.getFilesDir(), "skin_library");
    }

    public static List<Entry> list(Context context) {
        List<Entry> result = new ArrayList<>();
        File[] files = dir(context).listFiles((d, n) -> n.endsWith(".png"));
        if(files == null) return result;
        Arrays.sort(files, Comparator.comparing(f -> f.getName().toLowerCase()));
        for(File f : files) {
            String n = f.getName();
            result.add(new Entry(n.substring(0, n.length() - 4), f));
        }
        return result;
    }

    private static String displayName(Context context, Uri uri) {
        String name = null;
        try (Cursor c = context.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if(c != null && c.moveToFirst()) name = c.getString(0);
        } catch (Exception ignored) {}
        if(name == null || name.isEmpty()) name = "skin";
        if(name.toLowerCase().endsWith(".png")) name = name.substring(0, name.length() - 4);
        name = FileUtils.escapeFileName(name);
        return name.isEmpty() ? "skin" : name;
    }

    /** Imports a 64x64 or 64x32 PNG (the old format is converted to 64x64). */
    public static Entry importSkin(Context context, Uri uri) throws IOException {
        Bitmap bitmap;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if(in == null) throw new IOException("Cannot open the image");
            bitmap = BitmapFactory.decodeStream(in);
        }
        if(bitmap == null) throw new IOException("Unsupported image");
        if(bitmap.getWidth() != 64 || (bitmap.getHeight() != 64 && bitmap.getHeight() != 32)) {
            bitmap.recycle();
            throw new IOException("A skin must be 64x64 or 64x32 pixels");
        }
        if(bitmap.getHeight() == 32) bitmap = convertLegacy(bitmap);

        String base = displayName(context, uri);
        File target = new File(dir(context), base + ".png");
        for(int i = 2; target.exists(); i++) target = new File(dir(context), base + " (" + i + ").png");
        FileUtils.ensureParentDirectory(target);
        try (FileOutputStream out = new FileOutputStream(target)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } finally {
            bitmap.recycle();
        }
        String n = target.getName();
        return new Entry(n.substring(0, n.length() - 4), target);
    }

    /** Saves a downloaded skin (64x64 or 64x32) under the given name. */
    public static Entry saveBitmap(Context context, String name, Bitmap bitmap) throws IOException {
        if(bitmap.getWidth() != 64 || (bitmap.getHeight() != 64 && bitmap.getHeight() != 32))
            throw new IOException("A skin must be 64x64 or 64x32 pixels");
        if(bitmap.getHeight() == 32) bitmap = convertLegacy(bitmap);
        String base = FileUtils.escapeFileName(name);
        if(base.isEmpty()) base = "skin";
        File target = new File(dir(context), base + ".png");
        for(int i = 2; target.exists(); i++) target = new File(dir(context), base + " (" + i + ").png");
        FileUtils.ensureParentDirectory(target);
        try (FileOutputStream out = new FileOutputStream(target)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        String n = target.getName();
        return new Entry(n.substring(0, n.length() - 4), target);
    }

    public static void delete(Entry entry) {
        if(entry.file.exists()) entry.file.delete();
    }

    public static Bitmap load(Entry entry) {
        return BitmapFactory.decodeFile(entry.file.getAbsolutePath());
    }

    public static Bitmap loadFile(File file) {
        return BitmapFactory.decodeFile(file.getAbsolutePath());
    }

    /** Alex (slim) skins have a transparent last column on the arms. */
    public static boolean isSlim(Bitmap skin) {
        if(skin.getWidth() != 64 || skin.getHeight() != 64) return false;
        for(int y = 20; y < 32; y++) {
            if(Color.alpha(skin.getPixel(55, y)) != 0) return false;
        }
        return true;
    }

    private static void mirrorFace(Bitmap src, Canvas dst, int sx, int sy, int w, int h, int dx, int dy) {
        Bitmap part = Bitmap.createBitmap(src, sx, sy, w, h);
        Matrix m = new Matrix();
        m.preScale(-1, 1);
        Bitmap flipped = Bitmap.createBitmap(part, 0, 0, w, h, m, false);
        dst.drawBitmap(flipped, dx, dy, null);
        part.recycle();
        flipped.recycle();
    }

    /** Builds the left arm and leg that 64x32 skins lack by mirroring the right ones. */
    private static Bitmap convertLegacy(Bitmap legacy) {
        Bitmap out = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        canvas.drawBitmap(legacy, 0, 0, null);
        // {source x, source y, destination x, destination y} for the 16x16 limb blocks
        int[][] blocks = {{0, 16, 16, 48}, {40, 16, 32, 48}};
        for(int[] b : blocks) {
            int sx = b[0], sy = b[1], dx = b[2], dy = b[3];
            mirrorFace(legacy, canvas, sx + 4, sy, 4, 4, dx + 4, dy);     // top
            mirrorFace(legacy, canvas, sx + 8, sy, 4, 4, dx + 8, dy);     // bottom
            mirrorFace(legacy, canvas, sx + 8, sy + 4, 4, 12, dx, dy + 4);      // new right face = old left
            mirrorFace(legacy, canvas, sx + 4, sy + 4, 4, 12, dx + 4, dy + 4);  // front
            mirrorFace(legacy, canvas, sx, sy + 4, 4, 12, dx + 8, dy + 4);      // new left face = old right
            mirrorFace(legacy, canvas, sx + 12, sy + 4, 4, 12, dx + 12, dy + 4); // back
        }
        legacy.recycle();
        return out;
    }

    /**
     * Uploads the skin to a Microsoft account through the Minecraft services API.
     * Must be called off the main thread.
     * @return null on success, or a short error description
     */
    public static String uploadToMicrosoft(Account account, File png, boolean slim) {
        HttpURLConnection connection = null;
        try {
            String boundary = "----MojoBoundary" + System.nanoTime();
            connection = (HttpURLConnection) new URL("https://api.minecraftservices.com/minecraft/profile/skins").openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("Authorization", "Bearer " + account.accessToken);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            ByteArrayOutputStream body = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(body);
            out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"variant\"\r\n\r\n"
                    + (slim ? "slim" : "classic") + "\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"skin.png\"\r\n"
                    + "Content-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            try (InputStream in = new FileInputStream(png)) {
                byte[] buf = new byte[4096];
                for(int n; (n = in.read(buf)) != -1; ) out.write(buf, 0, n);
            }
            out.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
            connection.setFixedLengthStreamingMode(body.size());
            try (OutputStream os = connection.getOutputStream()) {
                body.writeTo(os);
            }
            int code = connection.getResponseCode();
            if(code >= 200 && code < 300) return null;
            return "HTTP " + code;
        } catch (IOException e) {
            return e.getClass().getSimpleName();
        } finally {
            if(connection != null) connection.disconnect();
        }
    }
}
