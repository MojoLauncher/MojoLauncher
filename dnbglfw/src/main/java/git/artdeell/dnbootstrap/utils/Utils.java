package git.artdeell.dnbootstrap.utils;

import android.os.Handler;
import android.os.Looper;

import java.lang.ref.WeakReference;

public class Utils {
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    public static <T> T getWeakReference(WeakReference<T> reference) {
        if(reference == null) return null;
        return reference.get();
    }
    public static void runOnUiThread(Runnable runnable) {
        MAIN_HANDLER.post(runnable);
    }
}
