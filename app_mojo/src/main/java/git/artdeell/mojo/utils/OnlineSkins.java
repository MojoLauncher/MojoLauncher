package git.artdeell.mojo.utils;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import android.util.Base64;

import git.artdeell.mojo.authenticator.accounts.SkinLibrary;

/**
 * Downloads skins by player name and exports skins for sites that have no upload API.
 * Mojang's public API is the same one for every game version, so it keeps working with new releases.
 */
public class OnlineSkins {
    public static final String ELY_UPLOAD_PAGE = "https://ely.by/skins/add";

    private static byte[] get(String address) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(address).openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("User-Agent", "MojoLauncher");
        try {
            int code = conn.getResponseCode();
            if(code != 200) throw new IOException("HTTP " + code);
            try (InputStream in = conn.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while((n = in.read(buf)) > 0) out.write(buf, 0, n);
                return out.toByteArray();
            }
        } finally {
            conn.disconnect();
        }
    }

    private static byte[] fromMojang(String name) throws IOException {
        String enc = URLEncoder.encode(name, "UTF-8");
        JsonObject profile = JsonParser.parseString(new String(
                get("https://api.mojang.com/users/profiles/minecraft/" + enc), StandardCharsets.UTF_8)).getAsJsonObject();
        String id = profile.get("id").getAsString();
        JsonObject session = JsonParser.parseString(new String(
                get("https://sessionserver.mojang.com/session/minecraft/profile/" + id), StandardCharsets.UTF_8)).getAsJsonObject();
        for(com.google.gson.JsonElement prop : session.getAsJsonArray("properties")) {
            JsonObject o = prop.getAsJsonObject();
            if(!"textures".equals(o.get("name").getAsString())) continue;
            String json = new String(Base64.decode(o.get("value").getAsString(), Base64.DEFAULT), StandardCharsets.UTF_8);
            JsonObject textures = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("textures");
            if(textures == null || !textures.has("SKIN")) break;
            return get(textures.getAsJsonObject("SKIN").get("url").getAsString());
        }
        throw new IOException("No skin");
    }

    private static byte[] fromEly(String name) throws IOException {
        return get("https://skinsystem.ely.by/skins/" + URLEncoder.encode(name, "UTF-8") + ".png");
    }

    /** Downloads the skin of a player (Mojang first, then Ely.by) and stores it in the library. Blocking. */
    public static SkinLibrary.Entry download(Context context, String name) throws IOException {
        byte[] data;
        try {
            data = fromMojang(name);
        } catch (Exception first) {
            try {
                data = fromEly(name);
            } catch (Exception second) {
                throw new IOException(second.getMessage() != null ? second.getMessage() : "No skin");
            }
        }
        Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
        if(bmp == null) throw new IOException("Unsupported image");
        return SkinLibrary.saveBitmap(context, name, bmp);
    }

    /** Copies the PNG to the public Downloads folder so it can be picked in the Ely.by upload page. Blocking. */
    public static String exportToDownloads(Context context, File png) throws IOException {
        String fileName = png.getName();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "image/png");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if(uri == null) throw new IOException("Cannot create the file");
            try (OutputStream out = context.getContentResolver().openOutputStream(uri);
                 InputStream in = new FileInputStream(png)) {
                if(out == null) throw new IOException("Cannot open the file");
                copy(in, out);
            }
        } else {
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if(!dir.isDirectory() && !dir.mkdirs()) throw new IOException("Cannot create " + dir);
            try (OutputStream out = new FileOutputStream(new File(dir, fileName));
                 InputStream in = new FileInputStream(png)) {
                copy(in, out);
            }
        }
        return fileName;
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        int n;
        while((n = in.read(buf)) > 0) out.write(buf, 0, n);
    }
}
