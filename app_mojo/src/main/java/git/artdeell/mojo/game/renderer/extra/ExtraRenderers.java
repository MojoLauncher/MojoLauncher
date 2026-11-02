package git.artdeell.mojo.game.renderer.extra;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import git.artdeell.mojo.Tools;
import git.artdeell.mojo.game.renderer.RenderSpec;
import git.artdeell.mojo.game.renderer.impl.GLESRenderSpec;
import git.artdeell.mojo.prefs.LauncherPreferences;
import git.artdeell.mojo.R;
import git.artdeell.mojo.utils.FileUtils;
import git.artdeell.mojo.utils.JREUtils;

/**
 * Desktop OpenGL wrappers the user imports as .so files. Each one becomes an entry in the
 * renderer list. The launcher does not ship these libraries, it only loads them the same way
 * as the built-in GLES wrappers (GL4ES, OpenLTW, MobileGlues).
 */
public class ExtraRenderers {
    public static final String TAG_PREFIX = "extra:";
    private static final String PREF = "extra_renderers";

    public static class Entry {
        public final String file;
        public final String name;
        public final int gles;

        Entry(String file, String name, int gles) {
            this.file = file;
            this.name = name;
            this.gles = gles;
        }

        public String tag() {
            return TAG_PREFIX + file;
        }
    }

    /** Names and settings for libraries that are known by file name. */
    public static class Preset {
        public final String name;
        public final int gles;

        Preset(String name, int gles) {
            this.name = name;
            this.gles = gles;
        }
    }

    public static Preset presetFor(String fileName) {
        switch (fileName) {
            case "libng_gl4es.so": return new Preset("Krypton Wrapper", 3);
            case "libgl4es_115.so": return new Preset("GL4ES 1.1.5", 2);
            case "libgl4es_114.so": return new Preset("GL4ES 1.1.4", 2);
            case "libvgpu.so": return new Preset("VGPU", 2);
            case "libltw.so": return new Preset("OpenLTW", 3);
            case "libmobileglues.so": return new Preset("MobileGlues", 3);
            default: return null;
        }
    }

    public static File dir() {
        return new File(Tools.DIR_DATA, "extra_renderers");
    }

    public static List<Entry> list() {
        List<Entry> result = new ArrayList<>();
        try {
            String raw = LauncherPreferences.DEFAULT_PREF.getString(PREF, "[]");
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                result.add(new Entry(o.getString("file"), o.getString("name"), o.optInt("gles", 2)));
            }
        } catch (Exception ignored) {}
        return result;
    }

    public static List<String> tags() {
        List<String> tags = new ArrayList<>();
        for (Entry e : list()) tags.add(e.tag());
        return tags;
    }

    private static void save(List<Entry> entries) {
        JSONArray array = new JSONArray();
        try {
            for (Entry e : entries) {
                JSONObject o = new JSONObject();
                o.put("file", e.file);
                o.put("name", e.name);
                o.put("gles", e.gles);
                array.put(o);
            }
        } catch (Exception ignored) {}
        LauncherPreferences.DEFAULT_PREF.edit().putString(PREF, array.toString()).apply();
    }

    public static RenderSpec find(String tag) {
        for (Entry e : list()) {
            if (e.tag().equals(tag)) return new ExtraRenderSpec(e);
        }
        return null;
    }

    /** Display name of the file the user picked (used to suggest a name and to keep the file name). */
    public static String fileNameOf(Context context, Uri uri) {
        String name = null;
        try (android.database.Cursor c = context.getContentResolver().query(uri,
                new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) name = c.getString(0);
        } catch (Exception ignored) {}
        if (name == null || name.isEmpty()) name = uri.getLastPathSegment();
        return name == null ? "renderer.so" : name;
    }

    /** Copies the library to the launcher storage and adds it to the list. */
    public static Entry importLibrary(Context context, Uri uri, String fileName, String name, int gles) throws IOException {
        String safe = FileUtils.escapeFileName(fileName);
        if (!safe.endsWith(".so")) safe = safe + ".so";
        File dir = dir();
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("Cannot create " + dir);
        File target = new File(dir, safe);
        ContentResolver resolver = context.getContentResolver();
        try (InputStream in = resolver.openInputStream(uri);
             FileOutputStream out = new FileOutputStream(target)) {
            if (in == null) throw new IOException("Cannot open the file");
            byte[] buffer = new byte[64 * 1024];
            int n;
            while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
        }
        target.setReadOnly();
        List<Entry> entries = list();
        for (int i = entries.size() - 1; i >= 0; i--) if (entries.get(i).file.equals(safe)) entries.remove(i);
        Entry entry = new Entry(safe, name, gles);
        entries.add(entry);
        save(entries);
        return entry;
    }

    public static void remove(Entry entry) {
        List<Entry> entries = list();
        for (int i = entries.size() - 1; i >= 0; i--) if (entries.get(i).file.equals(entry.file)) entries.remove(i);
        save(entries);
        File f = new File(dir(), entry.file);
        if (f.exists() && !f.delete()) f.deleteOnExit();
    }

    /** RenderSpec of an imported GLES wrapper. */
    static class ExtraRenderSpec extends GLESRenderSpec {
        private final Entry entry;

        ExtraRenderSpec(Entry entry) {
            this.entry = entry;
        }

        protected int glesVersion() {
            return entry.gles;
        }

        public boolean compatibleDevice(Context context) {
            if (entry.gles >= 3 && JREUtils.getDetectedVersion() < 3) return false;
            return new File(dir(), entry.file).exists();
        }

        public void setupEnvironment(Context context, Map<String, String> envMap) {
            super.setupEnvironment(context, envMap);
            if (entry.file.equals("libng_gl4es.so")) {
                envMap.put("LIBGL_USE_MC_COLOR", "1");
                envMap.put("LIBGL_GL", "31");
                envMap.put("LIBGL_ES", "3");
                envMap.put("LIBGL_NORMALIZE", "1");
            }
        }

        public String name() {
            return entry.name;
        }

        public int displayName() {
            return R.string.fork_renderer_imported;
        }

        public String displayLabel(Context context) {
            return entry.name + " " + context.getString(R.string.fork_renderer_imported_suffix);
        }

        public String tag() {
            return entry.tag();
        }

        public String library() {
            return entry.file;
        }

        public String librarySearchPath() {
            return dir().getAbsolutePath();
        }
    }
}
