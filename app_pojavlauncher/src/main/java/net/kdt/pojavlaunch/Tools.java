package net.kdt.pojavlaunch;

import static android.os.Build.VERSION.SDK_INT;
import static net.kdt.pojavlaunch.PojavApplication.sExecutorService;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Insets;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.lifecycle.ContextExecutor;
import net.kdt.pojavlaunch.lifecycle.ContextExecutorTask;
import net.kdt.pojavlaunch.memory.MemoryHoleFinder;
import net.kdt.pojavlaunch.memory.SelfMapsParser;
import net.kdt.pojavlaunch.multirt.MultiRTUtils;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.utils.FileUtils;
import net.kdt.pojavlaunch.utils.GLInfoUtils;
import net.kdt.pojavlaunch.value.DependentLibrary;
import net.kdt.pojavlaunch.value.MinecraftLibraryArtifact;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import git.artdeell.mojo.BuildConfig;
import git.artdeell.mojo.R;

@SuppressWarnings("IOStreamConstructor")
public final class Tools {
    public static final String MAVEN_CENTRAL = "https://maven-central-eu.storage-download.googleapis.com/maven2/";
    public  static final float BYTE_TO_MB = 1024 * 1024;
    public static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    public static String APP_NAME = "PojavLauncher";

    public static final Gson GLOBAL_GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final String URL_HOME = "https://pojavlauncherteam.github.io";
    public static String NATIVE_LIB_DIR;
    public static String DIR_DATA; //Initialized later to get context
    public static File DIR_CACHE;
    public static String MULTIRT_HOME;
    public static int DEVICE_ARCHITECTURE;

    // New since 3.3.1
    public static String DIR_ACCOUNT_NEW;
    public static String DIR_GAME_HOME = Environment.getExternalStorageDirectory().getAbsolutePath() + "/games/PojavLauncher";
    public static String DIR_GAME_NEW;

    // New since 2.4.2
    public static String DIR_HOME_VERSION;
    public static String DIR_HOME_LIBRARY;

    public static String DIR_HOME_CRASH;

    public static String ASSETS_PATH;
    public static String OBSOLETE_RESOURCES_PATH;
    public static String CTRLMAP_PATH;
    public static String CTRLDEF_FILE;


    private static File getPojavStorageRoot(Context ctx) {
        if(SDK_INT >= 29) {
            return ctx.getExternalFilesDir(null);
        }else{
            return new File(Environment.getExternalStorageDirectory(),"games/PojavLauncher");
        }
    }

    /**
     * Checks if the Pojav's storage root is accessible and read-writable
     * @param context context to get the storage root if it's not set yet
     * @return true if storage is fine, false if storage is not accessible
     */
    public static boolean checkStorageRoot(Context context) {
        File externalFilesDir = DIR_GAME_HOME  == null ? Tools.getPojavStorageRoot(context) : new File(DIR_GAME_HOME);
        //externalFilesDir == null when the storage is not mounted if it was obtained with the context call
        return externalFilesDir != null && Environment.getExternalStorageState(externalFilesDir).equals(Environment.MEDIA_MOUNTED);
    }

    /**
     * Checks if the Pojav's storage root is accessible and read-writable. If it's not, starts
     * the MissingStorageActivity and finishes the supplied activity.
     * @param context the Activity that checks for storage availability
     * @return whether the storage is available or not.
     */
    public static boolean checkStorageInteractive(Activity context) {
        if(!Tools.checkStorageRoot(context)) {
            context.startActivity(new Intent(context, MissingStorageActivity.class));
            context.finish();
            return false;
        }
        return true;
    }

    /**
     * Initialize context constants most necessary for launcher's early startup phase
     * that are not dependent on user storage.
     * All values that depend on DIR_DATA and are not dependent on DIR_GAME_HOME must
     * be initialized here.
     * @param ctx the context for initialization.
     */
    public static void initEarlyConstants(Context ctx) {
        DIR_CACHE = ctx.getCacheDir();
        DIR_DATA = ctx.getFilesDir().getParent();
        MULTIRT_HOME = DIR_DATA + "/runtimes";
        DIR_ACCOUNT_NEW = DIR_DATA + "/accounts";
        NATIVE_LIB_DIR = ctx.getApplicationInfo().nativeLibraryDir;
    }

    /**
     * Initialize context constants that depend on user storage.
     * Any value (in)directly dependent on DIR_GAME_HOME should be set only here.
     * You ABSOLUTELY MUST check for storage presence using checkStorageRoot() before calling this.
     */
    public static void initStorageConstants(Context ctx){
        initEarlyConstants(ctx);
        DIR_GAME_HOME = getPojavStorageRoot(ctx).getAbsolutePath();
        DIR_GAME_NEW = DIR_GAME_HOME + "/.minecraft";
        DIR_HOME_VERSION = DIR_GAME_NEW + "/versions";
        DIR_HOME_LIBRARY = DIR_GAME_NEW + "/libraries";
        DIR_HOME_CRASH = DIR_GAME_NEW + "/crash-reports";
        ASSETS_PATH = DIR_GAME_NEW + "/assets";
        OBSOLETE_RESOURCES_PATH = DIR_GAME_NEW + "/resources";
        CTRLMAP_PATH = DIR_GAME_HOME + "/controlmap";
        CTRLDEF_FILE = DIR_GAME_HOME + "/controlmap/default.json";
    }

    public static void buildNotificationChannel(Context context){
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel channel = new NotificationChannel(
                context.getString(R.string.notif_channel_id),
                context.getString(R.string.notif_channel_name), NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        manager.createNotificationChannel(channel);
    }

    public static String artifactToPath(DependentLibrary library) {
        if (library.downloads != null &&
            library.downloads.artifact != null &&
            library.downloads.artifact.path != null)
            return library.downloads.artifact.path;
        String[] libInfos = library.name.split(":");
        return libInfos[0].replaceAll("\\.", "/") + "/" + libInfos[1] + "/" + libInfos[2] + "/" + libInfos[1] + "-" + libInfos[2] + ".jar";
    }

    public static DisplayMetrics getDisplayMetrics(Activity activity) {
        DisplayMetrics displayMetrics = new DisplayMetrics();

        if(SDK_INT >= Build.VERSION_CODES.N && (activity.isInMultiWindowMode() || activity.isInPictureInPictureMode())){
            //For devices with free form/split screen, we need window size, not screen size.
            displayMetrics = activity.getResources().getDisplayMetrics();
        }else{
            if (SDK_INT >= Build.VERSION_CODES.R) {
                Objects.requireNonNull(activity.getDisplay()).getRealMetrics(displayMetrics);
            } else { // Removed the clause for devices with unofficial notch support, since it also ruins all devices with virtual nav bars before P
                activity.getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
            }
        }
        currentDisplayMetrics = displayMetrics;
        return displayMetrics;
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private static void setCutoutMode(Window window, boolean ignoreNotch) {
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        if (ignoreNotch) {
            layoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        } else {
            layoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;
        }
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
    }

    @SuppressWarnings("deprecation")
    private static void setLegacyFullscreen(View insetView, boolean fullscreen) {
        View.OnSystemUiVisibilityChangeListener listener = (visibility)->{
            if(fullscreen && (visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                insetView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            }else if(!fullscreen) {
                insetView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            }
        };
        listener.onSystemUiVisibilityChange(insetView.getSystemUiVisibility());
        insetView.setOnSystemUiVisibilityChangeListener(listener);
    }

    public static void setInsetsMode(Activity activity, boolean noSystemBars, boolean ignoreNotch) {
        Window window = activity.getWindow();
        View insetView = activity.findViewById(android.R.id.content);
        // Don't ignore system bars in window mode (will put game behind window button bar)
        if(SDK_INT >= Build.VERSION_CODES.N && activity.isInMultiWindowMode()) noSystemBars = false;

        int bgColor;
        // The status bars are completely transparent and will take their color from the inset view
        // background drawable.
        if(!noSystemBars) bgColor = activity.getResources().getColor(R.color.background_status_bar);
        else bgColor = Color.BLACK;

        // On API 35 onwards, apps are edge-to-edge by default and are controlled entirely though the
        // inset API. On levels below, we still need to set the correct cutout mode.
        if(SDK_INT >= Build.VERSION_CODES.P) setCutoutMode(window, ignoreNotch);

        // The AppCompat APIs don't work well, and break when opening alert dialogs on older Android
        // versions. Use the legacy fullscreen flags for lower APIs. (notch is already handled above)
        if(SDK_INT < Build.VERSION_CODES.R) {
            setLegacyFullscreen(insetView, noSystemBars);
            return;
        }
        // Code below expects this to be set to false, since that's the SDK 35 default.
        if(SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            window.setDecorFitsSystemWindows(false);
        }

        WindowInsetsController insetsController = window.getInsetsController();
        if(insetsController != null) {
            insetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            if(noSystemBars) insetsController.hide(WindowInsets.Type.systemBars());
            else insetsController.show(WindowInsets.Type.systemBars());
        }

        boolean fFullscreen = noSystemBars;
        insetView.setOnApplyWindowInsetsListener((v, windowInsets) -> {
            int insetMask = 0;
            if(!fFullscreen) insetMask |= WindowInsets.Type.systemBars();
            if(!ignoreNotch) insetMask |= WindowInsets.Type.displayCutout();
            if(insetMask != 0) {
                Insets insets = windowInsets.getInsets(insetMask);
                v.setBackground(new InsetBackground(insets,bgColor));
                insetView.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            }else {
                insetView.setPadding(0, 0, 0, 0);
                v.setBackground(null);
            }
            return WindowInsets.CONSUMED;
        });
        insetView.requestApplyInsets();
    }

    // Note: this should *NOT* be used for positioning and sizing things on the screen
    public static DisplayMetrics currentDisplayMetrics;

    public static float dpToPx(float dp) {
        //Better hope for the currentDisplayMetrics to be good
        return dp * currentDisplayMetrics.density;
    }

    public static float pxToDp(float px){
        //Better hope for the currentDisplayMetrics to be good
        return px / currentDisplayMetrics.density;
    }

    public static void copyAssetFile(Context ctx, String assetPath, String output, boolean overwrite) throws IOException {
        String fileName = FileUtils.getFileName(assetPath);
        if(fileName == null) fileName = assetPath;
        File outputFile = new File(output, fileName);
        copyAssetFile(ctx.getAssets(), assetPath, outputFile, overwrite);
    }

    public static void copyAssetFile(AssetManager assetManager, String fileName, File output, boolean overwrite) throws IOException {
        FileUtils.ensureParentDirectory(output);
        if(output.exists() && !overwrite) return;
        try (
                InputStream inputStream = assetManager.open(fileName);
                FileOutputStream fileOutputStream = new FileOutputStream(output)
        ){
            IOUtils.copy(inputStream, fileOutputStream);
        }
    }

    public static String printToString(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        printWriter.close();
        return stringWriter.toString();
    }

    public static void showError(Context ctx, Throwable e) {
        showError(ctx, e, false);
    }

    public static void showError(final Context ctx, final Throwable e, final boolean exitIfOk) {
        showError(ctx, R.string.global_error, null ,e, exitIfOk, false);
    }
    public static void showError(final Context ctx, final int rolledMessage, final Throwable e) {
        showError(ctx, R.string.global_error, ctx.getString(rolledMessage), e, false, false);
    }
    public static void showError(final Context ctx, final String rolledMessage, final Throwable e) {
        showError(ctx, R.string.global_error, rolledMessage, e, false, false);
    }
    public static void showError(final Context ctx, final String rolledMessage, final Throwable e, boolean exitIfOk) {
        showError(ctx, R.string.global_error, rolledMessage, e, exitIfOk, false);
    }
    public static void showError(final Context ctx, final int titleId, final Throwable e, final boolean exitIfOk) {
        showError(ctx, titleId, null, e, exitIfOk, false);
    }

    private static void showError(final Context ctx, final int titleId, final String rolledMessage, final Throwable e, final boolean exitIfOk, final boolean showMore) {
        if(e instanceof ContextExecutorTask) {
            ContextExecutor.execute((ContextExecutorTask) e);
            return;
        }

        Runnable runnable = () -> {
            final String errMsg = showMore ? printToString(e) : rolledMessage != null ? rolledMessage : e.getMessage();
            AlertDialog.Builder builder = new AlertDialog.Builder(ctx)
                    .setTitle(titleId)
                    .setMessage(errMsg)
                    .setPositiveButton(android.R.string.ok, (p1, p2) -> {
                        if(exitIfOk) {
                            if (ctx instanceof MainActivity) {
                                fullyExit();
                            } else if (ctx instanceof Activity) {
                                ((Activity) ctx).finish();
                            }
                        }
                    })
                    .setNegativeButton(showMore ? R.string.error_show_less : R.string.error_show_more, (p1, p2) -> showError(ctx, titleId, rolledMessage, e, exitIfOk, !showMore))
                    .setNeutralButton(android.R.string.copy, (p1, p2) -> {
                        ClipboardManager mgr = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                        mgr.setPrimaryClip(ClipData.newPlainText("error", printToString(e)));
                        if(exitIfOk) {
                            if (ctx instanceof MainActivity) {
                                fullyExit();
                            } else {
                                ((Activity) ctx).finish();
                            }
                        }
                    })
                    .setCancelable(!exitIfOk);
            builder.show();
        };

        if (ctx instanceof Activity) {
            ((Activity) ctx).runOnUiThread(runnable);
        } else {
            runnable.run();
        }
    }

    /**
     * Show the error remotely in a context-aware fashion. Has generally the same behaviour as
     * Tools.showError when in an activity, but when not in one, sends a notification that opens an
     * activity and calls Tools.showError().
     * NOTE: If the Throwable is a ContextExecutorTask and when not in an activity,
     * its executeWithApplication() method will never be called.
     * @param e the error (throwable)
     */
    public static void showErrorRemote(Throwable e) {
        showErrorRemote(null, e);
    }
    public static void showErrorRemote(Context context, int rolledMessage, Throwable e) {
        showErrorRemote(context.getString(rolledMessage), e);
    }
    public static void showErrorRemote(String rolledMessage, Throwable e) {
        // I WILL embrace layer violations because Android's concept of layers is STUPID
        // We live in the same process anyway, why make it any more harder with this needless
        // abstraction?

        // Add your Context-related rage here
        ContextExecutor.execute(new ShowErrorActivity.RemoteErrorTask(e, rolledMessage));
    }



    public static void dialogOnUiThread(final Activity activity, final CharSequence title, final CharSequence message) {
        activity.runOnUiThread(()->dialog(activity, title, message));
    }

    public static void dialog(final Context context, final CharSequence title, final CharSequence message) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    public static void openURL(Activity act, String url) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        act.startActivity(browserIntent);
    }

    public static boolean shouldSkipLibrary(DependentLibrary library) {
        // Don't use lwjgl from libraries, we have our own bundled in.
        return library.name.startsWith("org.lwjgl");
    }

    public static void preProcessLibraries(DependentLibrary[] libraries) {
        for (DependentLibrary libItem : libraries) {
            String[] version = libItem.name.split(":")[2].split("\\.");
            if (libItem.name.startsWith("net.java.dev.jna:jna:")) {
                // Special handling for LabyMod 1.8.9, Forge 1.12.2(?) and oshi
                // we have libjnidispatch 5.13.0 in jniLibs directory
                if (Integer.parseInt(version[0]) >= 5 && Integer.parseInt(version[1]) >= 13)
                    continue;
                Log.d(APP_NAME, "Library " + libItem.name + " has been changed to version 5.13.0");
                createLibraryInfo(libItem);
                libItem.name = "net.java.dev.jna:jna:5.13.0";
                libItem.downloads.artifact.path = "net/java/dev/jna/jna/5.13.0/jna-5.13.0.jar";
                libItem.downloads.artifact.sha1 = "1200e7ebeedbe0d10062093f32925a912020e747";
                libItem.downloads.artifact.url = MAVEN_CENTRAL+"net/java/dev/jna/jna/5.13.0/jna-5.13.0.jar";
                libItem.downloads.artifact.size = 1879325;
            } else if (libItem.name.startsWith("com.github.oshi:oshi-core:")) {
                //if (Integer.parseInt(version[0]) >= 6 && Integer.parseInt(version[1]) >= 3) return;
                // FIXME: ensure compatibility

                if (Integer.parseInt(version[0]) != 6 || Integer.parseInt(version[1]) != 2)
                    continue;
                Log.d(APP_NAME, "Library " + libItem.name + " has been changed to version 6.3.0");
                createLibraryInfo(libItem);
                libItem.name = "com.github.oshi:oshi-core:6.3.0";
                libItem.downloads.artifact.path = "com/github/oshi/oshi-core/6.3.0/oshi-core-6.3.0.jar";
                libItem.downloads.artifact.sha1 = "9e98cf55be371cafdb9c70c35d04ec2a8c2b42ac";
                libItem.downloads.artifact.url = MAVEN_CENTRAL+"com/github/oshi/oshi-core/6.3.0/oshi-core-6.3.0.jar";
                libItem.downloads.artifact.size = 957945;
            } else if (libItem.name.startsWith("org.ow2.asm:asm-all:")) {
                // Early versions of the ASM library get repalced with 5.0.4 because Pojav's LWJGL is compiled for
                // Java 8, which is not supported by old ASM versions. Mod loaders like Forge, which depend on this
                // library, often include lwjgl in their class transformations, which causes errors with old ASM versions.
                if (Integer.parseInt(version[0]) >= 5) continue;
                Log.d(APP_NAME, "Library " + libItem.name + " has been changed to version 5.0.4");
                createLibraryInfo(libItem);
                libItem.name = "org.ow2.asm:asm-all:5.0.4";
                libItem.url = null;
                libItem.downloads.artifact.path = "org/ow2/asm/asm-all/5.0.4/asm-all-5.0.4.jar";
                libItem.downloads.artifact.sha1 = "e6244859997b3d4237a552669279780876228909";
                libItem.downloads.artifact.url = MAVEN_CENTRAL+"org/ow2/asm/asm-all/5.0.4/asm-all-5.0.4.jar";
                libItem.downloads.artifact.size = 241810;
            }
        }
    }

    public static String read(InputStream is) throws IOException {
        String readResult = IOUtils.toString(is, StandardCharsets.UTF_8);
        is.close();
        return readResult;
    }

    public static String read(String path) throws IOException {
        return read(new FileInputStream(path));
    }

    public static String read(File path) throws IOException {
        return read(new FileInputStream(path));
    }

    public static void write(String path, String content) throws IOException {
        File file = new File(path);
        FileUtils.ensureParentDirectory(file);
        try(FileOutputStream outStream = new FileOutputStream(file)) {
            IOUtils.write(content, outStream);
        }
    }

    public static boolean isAndroid8OrHigher() {
        return SDK_INT >= 26;
    }

    public static void fullyExit() {
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    public static void printLauncherInfo(String gameVersion, String javaArguments) {
        Logger.appendToLog("Info: Launcher version: " + BuildConfig.VERSION_NAME);
        Logger.appendToLog("Info: Architecture: " + Architecture.archAsString(DEVICE_ARCHITECTURE));
        Logger.appendToLog("Info: Device model: " + Build.MANUFACTURER + " " +Build.MODEL);
        Logger.appendToLog("Info: API version: " + SDK_INT);
        Logger.appendToLog("Info: Selected Minecraft version: " + gameVersion);
        Logger.appendToLog("Info: Custom Java arguments: \"" + javaArguments + "\"");
        GLInfoUtils.GLInfo info = GLInfoUtils.getGlInfo();
        Logger.appendToLog("Info: RAM allocated: " + LauncherPreferences.PREF_RAM_ALLOCATION + " Mb");
        Logger.appendToLog("Info: Graphics device: "+info.vendor+ " "+info.renderer+" (OpenGL ES "+info.glesMajorVersion+")");
    }

    public static JMinecraftVersionList.Version getVersionInfo(String versionName) {
        return getVersionInfo(versionName, false);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static JMinecraftVersionList.Version getVersionInfo(String versionName, boolean skipInheriting) {
        try {
            JMinecraftVersionList.Version customVer = GLOBAL_GSON.fromJson(read(DIR_HOME_VERSION + "/" + versionName + "/" + versionName + ".json"), JMinecraftVersionList.Version.class);
            if (skipInheriting || customVer.inheritsFrom == null || customVer.inheritsFrom.equals(customVer.id)) {
                preProcessLibraries(customVer.libraries);
            } else {
                JMinecraftVersionList.Version inheritsVer;
                //If it won't download, just search for it
                try{
                    inheritsVer = GLOBAL_GSON.fromJson(read(DIR_HOME_VERSION + "/" + customVer.inheritsFrom + "/" + customVer.inheritsFrom + ".json"), JMinecraftVersionList.Version.class);
                }catch(IOException e) {
                    throw new RuntimeException("Can't find the source version for "+ versionName +" (req version="+customVer.inheritsFrom+")");
                }
                //inheritsVer.inheritsFrom = inheritsVer.id;
                insertSafety(inheritsVer, customVer,
                        "assetIndex", "assets", "id",
                        "mainClass", "minecraftArguments",
                        "releaseTime", "time", "type"
                );

                // Go through the libraries, remove the ones overridden by the custom version
                List<DependentLibrary> inheritLibraryList = new ArrayList<>(Arrays.asList(inheritsVer.libraries));
                outer_loop:
                for(DependentLibrary library : customVer.libraries){
                    // Clean libraries overridden by the custom version
                    String libName = library.name.substring(0, library.name.lastIndexOf(":"));

                    for(DependentLibrary inheritLibrary : inheritLibraryList) {
                        String inheritLibName = inheritLibrary.name.substring(0, inheritLibrary.name.lastIndexOf(":"));

                        if(libName.equals(inheritLibName)){
                            Log.d(APP_NAME, "Library " + libName + ": Replaced version " +
                                    libName.substring(libName.lastIndexOf(":") + 1) + " with " +
                                    inheritLibName.substring(inheritLibName.lastIndexOf(":") + 1));

                            // Remove the library , superseded by the overriding libs
                            inheritLibraryList.remove(inheritLibrary);
                            continue outer_loop;
                        }
                    }
                }

                // Fuse libraries
                inheritLibraryList.addAll(Arrays.asList(customVer.libraries));
                inheritsVer.libraries = inheritLibraryList.toArray(new DependentLibrary[0]);
                preProcessLibraries(inheritsVer.libraries);


                // Inheriting Minecraft 1.13+ with append custom args
                if (inheritsVer.arguments != null && customVer.arguments != null &&
                    inheritsVer.arguments.game != null && customVer.arguments.game != null) {
                    List totalArgList = new ArrayList(Arrays.asList(inheritsVer.arguments.game));

                    int nskip = 0;
                    for (int i = 0; i < customVer.arguments.game.length; i++) {
                        if (nskip > 0) {
                            nskip--;
                            continue;
                        }

                        Object perCustomArg = customVer.arguments.game[i];
                        if (perCustomArg instanceof String) {
                            String perCustomArgStr = (String) perCustomArg;
                            // Check if there is a duplicate argument on combine
                            if (perCustomArgStr.startsWith("--") && totalArgList.contains(perCustomArgStr)) {
                                perCustomArg = customVer.arguments.game[i + 1];
                                if (perCustomArg instanceof String) {
                                    perCustomArgStr = (String) perCustomArg;
                                    // If the next is argument value, skip it
                                    if (!perCustomArgStr.startsWith("--")) {
                                        nskip++;
                                    }
                                }
                            } else {
                                totalArgList.add(perCustomArgStr);
                            }
                        } else if (!totalArgList.contains(perCustomArg)) {
                            totalArgList.add(perCustomArg);
                        }
                    }

                    inheritsVer.arguments.game = totalArgList.toArray(new Object[0]);
                }

                customVer = inheritsVer;
            }

            // LabyMod 4 sets version instead of majorVersion
            if (customVer.javaVersion != null && customVer.javaVersion.majorVersion == 0) {
                customVer.javaVersion.majorVersion = customVer.javaVersion.version;
            }
            return customVer;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Prevent NullPointerException
    private static void insertSafety(JMinecraftVersionList.Version targetVer, JMinecraftVersionList.Version fromVer, String... keyArr) {
        for (String key : keyArr) {
            Object value = null;
            try {
                Field fieldA = fromVer.getClass().getDeclaredField(key);
                value = fieldA.get(fromVer);
                if (((value instanceof String) && !((String) value).isEmpty()) || value != null) {
                    Field fieldB = targetVer.getClass().getDeclaredField(key);
                    fieldB.set(targetVer, value);
                }
            } catch (Throwable th) {
                Log.w(APP_NAME, "Unable to insert " + key + "=" + value, th);
            }
        }
    }

    public static String getSelectedRuntime(Instance instance) {
        String runtime = LauncherPreferences.PREF_DEFAULT_RUNTIME;
        String profileRuntime = instance.selectedRuntime;
        if(profileRuntime != null) {
            if(MultiRTUtils.forceReread(profileRuntime).versionString != null) {
                runtime = profileRuntime;
            }
        }
        return runtime;
    }

    public static void createLibraryInfo(DependentLibrary library) {
        if(library.downloads == null || library.downloads.artifact == null)
            library.downloads = new DependentLibrary.LibraryDownloads(new MinecraftLibraryArtifact());
    }

    public interface DownloaderFeedback {
        void updateProgress(int curr, int max);
    }


    public static int getTotalDeviceMemory(Context ctx){
        ActivityManager actManager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        actManager.getMemoryInfo(memInfo);
        return (int) (memInfo.totalMem / 1048576L);
    }

    public static int getFreeDeviceMemory(Context ctx){
        ActivityManager actManager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        actManager.getMemoryInfo(memInfo);
        return (int) (memInfo.availMem / 1048576L);
    }

    private static int internalGetMaxContinuousAddressSpaceSize() throws Exception{
        MemoryHoleFinder memoryHoleFinder = new MemoryHoleFinder();
        new SelfMapsParser(memoryHoleFinder).run();
        long largestHole = memoryHoleFinder.getLargestHole();
        if(largestHole == -1) return -1;
        else return (int)(largestHole / 1048576L);
    }

    public static int getMaxContinuousAddressSpaceSize() {
        try {
            return internalGetMaxContinuousAddressSpaceSize();
        }catch (Exception e){
            Log.w("Tools", "Failed to find the largest uninterrupted address space");
            return -1;
        }
    }

    public static int getDisplayFriendlyRes(int displaySideRes, float scaling){
        displaySideRes = (int)(displaySideRes * scaling);
        if(displaySideRes % 2 != 0) displaySideRes --;
        return displaySideRes;
    }

    public static String getFileName(Context ctx, Uri uri) {
        Cursor c = ctx.getContentResolver().query(uri, null, null, null, null);
        if(c == null) return uri.getLastPathSegment(); // idk myself but it happens on asus file manager
        c.moveToFirst();
        int columnIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        if(columnIndex == -1) return uri.getLastPathSegment();
        String fileName = c.getString(columnIndex);
        c.close();
        return fileName;
    }

    /** Swap the main fragment with another */
    public static void swapFragment(FragmentActivity fragmentActivity , Class<? extends Fragment> fragmentClass,
                                    @Nullable String fragmentTag, @Nullable Bundle bundle) {
        // When people tab out, it might happen
        //TODO handle custom animations
        fragmentActivity.getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .addToBackStack(fragmentClass.getName())
                .replace(R.id.container_fragment, fragmentClass, bundle, fragmentTag).commit();
    }

    public static void backToMainMenu(FragmentActivity fragmentActivity) {
        fragmentActivity.getSupportFragmentManager()
                .popBackStack("ROOT", 0);
    }

    /** Remove the current fragment */
    public static void removeCurrentFragment(FragmentActivity fragmentActivity){
        fragmentActivity.getSupportFragmentManager().popBackStack();
    }

    /** Launch the mod installer activity. The Uri must be from our own content provider or
     * from ACTION_OPEN_DOCUMENT
     */
    public static void launchModInstaller(Context context, @NonNull Uri uri){
        Intent intent = new Intent(context, JavaGUILauncherActivity.class);
        intent.putExtra("modUri", uri);
        context.startActivity(intent);
    }


    public static void installRuntimeFromUri(Context context, Uri uri){
        sExecutorService.execute(() -> {
            try {
                String name = getFileName(context, uri);
                MultiRTUtils.installRuntimeNamed(
                        NATIVE_LIB_DIR,
                        context.getContentResolver().openInputStream(uri),
                        name);

                MultiRTUtils.postPrepare(name);
            } catch (IOException e) {
                Tools.showError(context, e);
            }
        });
    }

    public static String extractUntilCharacter(String input, String whatFor, char terminator) {
        int whatForStart = input.indexOf(whatFor);
        if(whatForStart == -1) return null;
        whatForStart += whatFor.length();
        int terminatorIndex = input.indexOf(terminator, whatForStart);
        if(terminatorIndex == -1) return null;
        return input.substring(whatForStart, terminatorIndex);
    }

    public static boolean isValidString(String string) {
        return string != null && !string.isEmpty();
    }

    public static String validOrNullString(String string) {
        if(!isValidString(string)) return null;
        return string;
    }

    public static void runOnUiThread(Runnable runnable) {
        MAIN_HANDLER.post(runnable);
    }

    /** Triggers the share intent chooser, with the latestlog file attached to it */
    public static void shareLog(Context context){
        openPath(context, new File(Tools.DIR_GAME_HOME, "latestlog.txt"), true);
    }

    /**
     * Determine the MIME type of a File.
     * @param file The file to determine the type of
     * @return the type, or the default value *slash* if cannot be determined
     */
    public static String getMimeType(File file) {
        if(file.isDirectory()) return DocumentsContract.Document.MIME_TYPE_DIR;
        String mimeType = null;
        try (FileInputStream fileInputStream = new FileInputStream(file)){
            // Theoretically we don't even need the buffer since we don't care about the
            // contents of the file after the guess, but mark-supported streams
            // are a requirement of URLConnection.guessContentTypeFromStream()
            try(BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {
                mimeType = URLConnection.guessContentTypeFromStream(bufferedInputStream);
            }
        }catch (IOException e) {
            Log.w("FileMimeType", "Failed to determine MIME type by stream", e);
        }
        if(mimeType != null) return mimeType;
        mimeType = URLConnection.guessContentTypeFromName(file.getName());
        if(mimeType != null) return mimeType;
        return "*/*";
    }

    /**
     * Open the path specified by a File in a file explorer or in a relevant application.
     * @param context the current Context
     * @param file the File to open
     * @param share whether to open a "Share" or an "Open" dialog.
     */
    public static void openPath(Context context, File file, boolean share) {
        Uri contentUri = DocumentsContract.buildDocumentUri(context.getString(R.string.storageProviderAuthorities), file.getAbsolutePath());
        String mimeType = getMimeType(file);
        Intent intent = new Intent();
        if(share) {
            intent.setAction(Intent.ACTION_SEND);
            intent.setType(getMimeType(file));
            intent.putExtra(Intent.EXTRA_STREAM, contentUri);
        }else {
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(contentUri, mimeType);
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Intent chooserIntent = Intent.createChooser(intent, file.getName());
        context.startActivity(chooserIntent);
    }

    /** Mesure the textview height, given its current parameters */
    public static int mesureTextviewHeight(TextView t) {
        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(t.getWidth(), View.MeasureSpec.AT_MOST);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        t.measure(widthMeasureSpec, heightMeasureSpec);
        return t.getMeasuredHeight();
    }

    /**
     * Check if the device is one of the devices that may be affected by the hanging linker issue.
     * The device is affected if the linker causes the process to lock up when dlopen() is called within
     * dl_iterate_phdr().
     * For now, the only affected firmware that I know of is Android 5.1, EMUI 3.1 on MTK-based Huawei
     * devices.
     * @return if the device is affected by the hanging linker issue.
     */
    public static boolean deviceHasHangingLinker() {
        // Android Oreo and onwards have GSIs and most phone firmwares at that point were not modified
        // *that* intrusively. So assume that we are not affected.
        if(SDK_INT >= Build.VERSION_CODES.O) return false;
        // Since the affected function in LWJGL is rarely used (and when used, it's mainly for debug prints)
        // we can make the search scope a bit more broad and check if we are running on a Huawei device.
        return Build.MANUFACTURER.toLowerCase(Locale.ROOT).contains("huawei");
    }

    public static <T> T getWeakReference(WeakReference<T> weakReference) {
        if(weakReference == null) return null;
        return weakReference.get();
    }

    public static boolean deviceSupportsGyro(@NonNull Context context) {
        return ((SensorManager)context.getSystemService(Context.SENSOR_SERVICE)).getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null;

    }

    public static void dialogForceClose(Context ctx) {
        new android.app.AlertDialog.Builder(ctx)
                .setMessage(R.string.mcn_exit_confirm)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (p1, p2) -> {
                    try {
                        Tools.fullyExit();
                    } catch (Throwable th) {
                        Log.w(Tools.APP_NAME, "Could not enable System.exit() method!", th);
                    }
                }).show();
    }
}
