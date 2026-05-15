package net.kdt.pojavlaunch;


import static net.kdt.pojavlaunch.Tools.dialogForceClose;
import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_ENABLE_GYRO;
import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_SUSTAINED_PERFORMANCE;
import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_USE_ALTERNATE_SURFACE;
import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_VIRTUAL_MOUSE_START;
import static org.lwjgl.glfw.CallbackBridge.sendKeyPress;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kdt.LoggerView;

import net.kdt.pojavlaunch.authenticator.accounts.Accounts;
import net.kdt.pojavlaunch.customcontrols.ControlButtonMenuListener;
import net.kdt.pojavlaunch.customcontrols.ControlData;
import net.kdt.pojavlaunch.customcontrols.ControlDrawerData;
import net.kdt.pojavlaunch.customcontrols.ControlJoystickData;
import net.kdt.pojavlaunch.customcontrols.ControlLayout;
import net.kdt.pojavlaunch.customcontrols.CustomControls;
import net.kdt.pojavlaunch.customcontrols.EditorExitable;
import net.kdt.pojavlaunch.customcontrols.keyboard.LwjglCharSender;
import net.kdt.pojavlaunch.customcontrols.keyboard.TouchCharInput;
import net.kdt.pojavlaunch.customcontrols.mouse.GyroControl;
import net.kdt.pojavlaunch.customcontrols.mouse.HotbarView;
import net.kdt.pojavlaunch.customcontrols.mouse.Touchpad;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.instances.Instances;
import net.kdt.pojavlaunch.lifecycle.ContextExecutor;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.prefs.QuickSettingSideDialog;
import net.kdt.pojavlaunch.services.GameService;
import net.kdt.pojavlaunch.tasks.AsyncAssetManager;
import net.kdt.pojavlaunch.utils.JREUtils;
import net.kdt.pojavlaunch.utils.MCOptionUtils;
import net.kdt.pojavlaunch.authenticator.accounts.MinecraftAccount;
import net.kdt.pojavlaunch.utils.RendererCompatUtil;
import net.kdt.pojavlaunch.utils.jre.GameRunner;
import net.kdt.pojavlaunch.customcontrols.handleview.DrawerPullButton;

import org.lwjgl.glfw.CallbackBridge;

import java.io.File;
import java.io.IOException;

import net.ashmeet.hyperlauncher.R;
import net.ashmeet.hyperlauncher.BuildConfig;

public class MainActivity extends BaseActivity implements ControlButtonMenuListener, EditorExitable, ServiceConnection {
    public static volatile ClipboardManager GLOBAL_CLIPBOARD;
    public static final String INTENT_MINECRAFT_VERSION = "intent_version";

    public static TouchCharInput touchCharInput;
    private MinecraftGLSurface minecraftGLView;
    private static Touchpad touchpad;
    private LoggerView loggerView;
    private DrawerLayout drawerLayout;
    private ListView navDrawer;
    private DrawerPullButton mDrawerPullButton;
    private GyroControl mGyroControl = null;
    private ControlLayout mControlLayout;
    private HotbarView mHotbarView;
    private View mDrawerContainer;

    Instance instance;
    MinecraftAccount minecraftAccount;

    private ArrayAdapter<String> gameActionArrayAdapter;
    private AdapterView.OnItemClickListener gameActionClickListener;
    public ArrayAdapter<String> ingameControlsEditorArrayAdapter;
    public AdapterView.OnItemClickListener ingameControlsEditorListener;
    private GameService.LocalBinder mServiceBinder;

    private QuickSettingSideDialog mQuickSettingSideDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = Instances.loadSelectedInstance();
        minecraftAccount = Accounts.getCurrent();
        if(instance == null) {
            Toast.makeText(this, R.string.instance_dir_missing, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        AsyncAssetManager.extractDefaultSettings(this, instance.getGameDirectory());
        MCOptionUtils.load(instance.getGameDirectory().getAbsolutePath());

        Intent gameServiceIntent = new Intent(this, GameService.class);
        // Start the service a bit early
        ContextCompat.startForegroundService(this, gameServiceIntent);
        initLayout(R.layout.activity_basemain);
        CallbackBridge.addGrabListener(touchpad);
        CallbackBridge.addGrabListener(minecraftGLView);

        mGyroControl = new GyroControl(this);

        // Enabling this on TextureView results in a broken white result
        if(PREF_USE_ALTERNATE_SURFACE) getWindow().setBackgroundDrawable(null);
        else getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));

        // Set the sustained performance mode for available APIs
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            getWindow().setSustainedPerformanceMode(PREF_SUSTAINED_PERFORMANCE);

        ingameControlsEditorArrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.menu_customcontrol));
        ingameControlsEditorListener = (parent, view, position, id) -> {
            switch(position) {
                case 0: mControlLayout.addControlButton(new ControlData("New")); break;
                case 1: mControlLayout.addDrawer(new ControlDrawerData()); break;
                case 2: mControlLayout.addJoystickButton(new ControlJoystickData()); break;
                case 3: mControlLayout.openLoadDialog(); break;
                case 4: mControlLayout.openSaveDialog(this); break;
                case 5: mControlLayout.openSetDefaultDialog(); break;
                case 6: mControlLayout.openExitDialog(this);
            }
        };

        // Recompute the gui scale when options are changed
        MCOptionUtils.MCOptionListener optionListener = MCOptionUtils::getMcScale;
        MCOptionUtils.addMCOptionListener(optionListener);
        mControlLayout.setModifiable(false);

        // Set the activity for the executor. Must do this here, or else Tools.showErrorRemote() may not
        // execute the correct method
        ContextExecutor.setActivity(this);
        //Now, attach to the service. The game will only start when this happens, to make sure that we know the right state.
        bindService(gameServiceIntent, this, 0);
    }

    @SuppressLint("ClickableViewAccessibility")
    protected void initLayout(int resId) {
        setContentView(resId);
        bindValues();
        mControlLayout.setMenuListener(this);

        mDrawerPullButton.setOnClickListener(v -> onClickedMenu());
        mDrawerPullButton.setOnTouchListener(new View.OnTouchListener() {
            private float initialX, initialY;
            private float initialTouchX, initialTouchY;
            private boolean isDragging = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!LauncherPreferences.PREF_DRAWER_BUTTON_MOVABLE) return false;

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = v.getX();
                        initialY = v.getY();
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        isDragging = false;
                        return false; // Let ClickListener handle it if no move

                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - initialTouchX;
                        float dy = event.getRawY() - initialTouchY;
                        
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            isDragging = true;
                        }

                        if (isDragging) {
                            float nextX = initialX + dx;
                            float nextY = initialY + dy;

                            // Clamp
                            nextX = Math.max(0, Math.min(mControlLayout.getWidth() - v.getWidth(), nextX));
                            nextY = Math.max(0, Math.min(mControlLayout.getHeight() - v.getHeight(), nextY));

                            v.setX(nextX);
                            v.setY(nextY);

                            // Save as percent
                            float parentRangeX = mControlLayout.getWidth() - v.getWidth();
                            float parentRangeY = mControlLayout.getHeight() - v.getHeight();
                            LauncherPreferences.PREF_DRAWER_BUTTON_X = parentRangeX > 0 ? (nextX / parentRangeX) * 100f : 0;
                            LauncherPreferences.PREF_DRAWER_BUTTON_Y = parentRangeY > 0 ? (nextY / parentRangeY) * 100f : 0;
                            
                            return true;
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        if (isDragging) {
                            LauncherPreferences.DEFAULT_PREF.edit()
                                    .putInt("drawerButtonX", (int) LauncherPreferences.PREF_DRAWER_BUTTON_X)
                                    .putInt("drawerButtonY", (int) LauncherPreferences.PREF_DRAWER_BUTTON_Y)
                                    .putString("drawerButtonPreset", "custom")
                                    .commit(); // Use commit to ensure it's saved before potential force close
                            return true;
                        }
                        break;
                }
                return false;
            }
        });

        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        drawerLayout.setScrimColor(Color.TRANSPARENT);

        try {
            File latestLogFile = new File(Tools.DIR_GAME_HOME, "latestlog.txt");
            if(!latestLogFile.exists() && !latestLogFile.createNewFile())
                throw new IOException("Failed to create a new log file");
            Logger.begin(latestLogFile.getAbsolutePath());
            // FIXME: is it safe for multi thread?
            GLOBAL_CLIPBOARD = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            touchCharInput.setCharacterSender(new LwjglCharSender());

            String version = getIntent().getStringExtra(INTENT_MINECRAFT_VERSION);
            version = version == null ? instance.versionId : version;

            setTitle("Minecraft " + version);

            // Menu
            gameActionArrayAdapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.menu_ingame)) {
                @NonNull
                @Override
                public View getView(int position, View convertView, @NonNull android.view.ViewGroup parent) {
                    View v = super.getView(position, convertView, parent);
                    if(v instanceof TextView) {
                        ((TextView) v).setTextColor(Color.WHITE);
                        ((TextView) v).setPadding(32, 16, 32, 16);
                    }
                    return v;
                }
            };
            gameActionClickListener = (parent, view, position, id) -> {
                switch(position) {
                     case 0: dialogForceClose(MainActivity.this); break;
                     case 1: openLogOutput(); break;
                     case 2: dialogSendCustomKey(); break;
                     case 3: openQuickSettings(); break;
                     case 4: openCustomControls(); break;
                }
                drawerLayout.closeDrawers();
            };
            navDrawer.setAdapter(gameActionArrayAdapter);
            navDrawer.setOnItemClickListener(gameActionClickListener);
            drawerLayout.closeDrawers();

            final String finalVersion = version;
            minecraftGLView.setSurfaceReadyListener(() -> {
                try {
                    // Setup virtual mouse right before launching
                    if (PREF_VIRTUAL_MOUSE_START) {
                        touchpad.post(() -> touchpad.switchState());
                    }

                    runCraft(finalVersion);
                }catch (Throwable e){
                    Tools.showErrorRemote(e);
                }
            });
        } catch (Throwable e) {
            Tools.showError(this, e, true);
        }
    }

    private void loadControls() {
        try {
            // Load keys
            mControlLayout.loadLayout(instance.getLaunchControls());
        } catch(IOException e) {
            try {
                Log.w("MainActivity", "Unable to load the control file, loading the default now", e);
                mControlLayout.loadLayout(Tools.CTRLDEF_FILE);
            } catch (IOException ioException) {
                Tools.showError(this, ioException);
            }
        } catch (Throwable th) {
            Tools.showError(this, th);
        }
        updateDrawerButton();
        mControlLayout.toggleControlVisible();
    }

    private void updateDrawerButton() {
        if(mDrawerPullButton == null) return;
        mDrawerPullButton.setVisibility(mControlLayout.hasMenuButton() ? View.GONE : View.VISIBLE);
        mDrawerPullButton.updateCustomImage();
        
        mDrawerPullButton.post(() -> {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mDrawerPullButton.getLayoutParams();
            int parentWidth = mControlLayout.getWidth();
            int parentHeight = mControlLayout.getHeight();
            if (parentWidth == 0 || parentHeight == 0) return;

            int sizePx = (int) (LauncherPreferences.PREF_DRAWER_BUTTON_SIZE * getResources().getDisplayMetrics().density);
            lp.width = sizePx;
            lp.height = sizePx;

            float xPercent = LauncherPreferences.PREF_DRAWER_BUTTON_X / 100f;
            float yPercent = LauncherPreferences.PREF_DRAWER_BUTTON_Y / 100f;

            lp.gravity = android.view.Gravity.TOP | android.view.Gravity.START;
            lp.leftMargin = (int) (xPercent * (parentWidth - sizePx));
            lp.topMargin = (int) (yPercent * (parentHeight - sizePx));
            mDrawerPullButton.setLayoutParams(lp);
        });

        if (mDrawerContainer != null) {
            mDrawerContainer.setAlpha(LauncherPreferences.PREF_DRAWER_LIST_OPACITY);
        }
    }

    @Override
    public void onAttachedToWindow() {
        // Post to get the correct display dimensions after layout.
        mControlLayout.post(()->{
            Tools.getDisplayMetrics(this);
            loadControls();
        });
    }

    /** Boilerplate binding */
    private void bindValues(){
        mControlLayout = findViewById(R.id.main_control_layout);
        minecraftGLView = findViewById(R.id.main_game_render_view);
        touchpad = findViewById(R.id.main_touchpad);
        drawerLayout = findViewById(R.id.main_drawer_options);
        navDrawer = findViewById(R.id.main_navigation_view);
        loggerView = findViewById(R.id.mainLoggerView);
        touchCharInput = findViewById(R.id.mainTouchCharInput);
        mDrawerPullButton = findViewById(R.id.drawer_button);
        mHotbarView = findViewById(R.id.hotbar_view);
        mDrawerContainer = findViewById(R.id.main_drawer_list_container);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(PREF_ENABLE_GYRO) mGyroControl.enable();
        CallbackBridge.nativeSetWindowAttrib(LwjglGlfwKeycode.GLFW_HOVERED, 1);
        updateDrawerButton();
    }

    @Override
    protected void onPause() {
        mGyroControl.disable();
        if (CallbackBridge.isGrabbing()){
            sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_ESCAPE);
        }
        if(mQuickSettingSideDialog != null) {
            mQuickSettingSideDialog.cancel();
        }
        CallbackBridge.nativeSetWindowAttrib(LwjglGlfwKeycode.GLFW_HOVERED, 0);
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        CallbackBridge.nativeSetWindowAttrib(LwjglGlfwKeycode.GLFW_VISIBLE, 1);
    }

    @Override
    protected void onStop() {
        CallbackBridge.nativeSetWindowAttrib(LwjglGlfwKeycode.GLFW_VISIBLE, 0);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CallbackBridge.removeGrabListener(touchpad);
        CallbackBridge.removeGrabListener(minecraftGLView);
        ContextExecutor.clearActivity();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if(mGyroControl != null) mGyroControl.updateOrientation();
        // Layout resize is practically guaranteed on a configuration change, and `onConfigurationChanged`
        // does not implicitly start a layout. So, request a layout and expect the screen dimensions to be valid after the]
        // post.
        if(mControlLayout == null) return;
        mControlLayout.requestLayout();
        mControlLayout.post(()->{
            // Child of mControlLayout, so refreshing size here is correct
            minecraftGLView.refreshSize();
            mControlLayout.refreshControlButtonPositions();
            updateDrawerButton();
        });
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if(minecraftGLView != null)  // Useful when backing out of the app
            Tools.MAIN_HANDLER.postDelayed(() -> minecraftGLView.refreshSize(), 500);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            // Reload PREF_DEFAULTCTRL_PATH
            // If the storage root got unmounted/unreadable we won't be able to load the file anyway,
            // and MissingStorageActivity will be started.
            if(!Tools.checkStorageRoot(this)) return;
            LauncherPreferences.loadPreferences(getApplicationContext());
            try {
                mControlLayout.loadLayout(LauncherPreferences.PREF_DEFAULTCTRL_PATH);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void runCraft(String versionId) throws Throwable {
        String renderer = instance.getLaunchRenderer();
        if(!RendererCompatUtil.checkRendererCompatible(this, renderer)) {
            RendererCompatUtil.RenderersList renderersList = RendererCompatUtil.getCompatibleRenderers(this);
            String firstCompatibleRenderer = renderersList.rendererIds.get(0);
            Log.w("runCraft","Incompatible renderer "+renderer+ " will be replaced with "+firstCompatibleRenderer);
            renderer = firstCompatibleRenderer;
        }
        Logger.appendToLog("--------- Starting game with Launcher Debug!");
        Tools.printLauncherInfo(versionId, instance.getLaunchArgs());
        JREUtils.redirectAndPrintJRELog();
        GameRunner.launchMinecraft(this, minecraftAccount, instance, versionId, renderer);
        //Note that we actually stall in the above function, even if the game crashes. But let's be safe.
        Tools.runOnUiThread(()-> mServiceBinder.isActive = false);
    }

    private void dialogSendCustomKey() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.control_customkey)
                .setItems(EfficientAndroidLWJGLKeycode.generateKeyName(),
                        (dInterface, position) -> EfficientAndroidLWJGLKeycode.execKeyIndex(position))
                .show();
    }

    boolean isInEditor;
    private void openCustomControls() {
        if(ingameControlsEditorListener == null || ingameControlsEditorArrayAdapter == null) return;

        mControlLayout.setModifiable(true);
        navDrawer.setAdapter(ingameControlsEditorArrayAdapter);
        navDrawer.setOnItemClickListener(ingameControlsEditorListener);
        mDrawerPullButton.setVisibility(View.VISIBLE);
        isInEditor = true;
    }

    private void openLogOutput() {
        loggerView.setVisibility(View.VISIBLE);
    }

    private void openQuickSettings() {
        if(mQuickSettingSideDialog == null) {
            mQuickSettingSideDialog = new QuickSettingSideDialog(this, mControlLayout) {
                @Override
                public void onResolutionChanged() {
                    minecraftGLView.refreshSize();
                    mHotbarView.onResolutionChanged();
                }

                @Override
                public void onGyroStateChanged() {
                    mGyroControl.updateOrientation();
                    if (PREF_ENABLE_GYRO) {
                        mGyroControl.enable();
                    } else {
                        mGyroControl.disable();
                    }
                }
            };
        }
        mQuickSettingSideDialog.appear(false);
    }

    public static void toggleMouse(Context ctx) {
        if (CallbackBridge.isGrabbing()) return;

        Toast.makeText(ctx, touchpad.switchState()
                        ? R.string.control_mouseon : R.string.control_mouseoff,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if(isInEditor) {
            if(event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                if(event.getAction() == KeyEvent.ACTION_DOWN) mControlLayout.askToExit(this);
                return true;
            }
            return super.dispatchKeyEvent(event);
        }
        boolean handleEvent;
        if(!(handleEvent = minecraftGLView.processKeyEvent(event))) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && !touchCharInput.isEnabled()) {
                if(event.getAction() != KeyEvent.ACTION_UP) return true; // We eat it anyway
                sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_ESCAPE);
                return true;
            }
        }
        return handleEvent;
    }

    public static void switchKeyboardState() {
        if(touchCharInput != null) touchCharInput.switchKeyboardState();
    }

    @Keep
    public static void openLink(String link) {
        Context ctx = touchpad.getContext(); // no more better way to obtain a context statically
        ((Activity)ctx).runOnUiThread(() -> {
            try {
                if(link.startsWith("file:")) {
                    int truncLength = 5;
                    if(link.startsWith("file://")) truncLength = 7;
                    String path = link.substring(truncLength);
                    Tools.openPath(ctx, new File(path), false);
                }else {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse(link), "*/*");
                    ctx.startActivity(intent);
                }
            } catch (Throwable th) {
                Tools.showError(ctx, th);
            }
        });
    }

    @SuppressWarnings("unused") //TODO: actually use it
    public static void openPath(String path) {
        Context ctx = touchpad.getContext(); // no more better way to obtain a context statically
        ((Activity)ctx).runOnUiThread(() -> {
            try {
                Tools.openPath(ctx, new File(path), false);
            } catch (Throwable th) {
                Tools.showError(ctx, th);
            }
        });
    }

    @Keep
    public static void querySystemClipboard() {
        Tools.runOnUiThread(()->{
            ClipData clipData = GLOBAL_CLIPBOARD.getPrimaryClip();
            if(clipData == null) {
                AWTInputBridge.nativeClipboardReceived(null, null);
                return;
            }
            ClipData.Item firstClipItem = clipData.getItemAt(0);
            //TODO: coerce to HTML if the clip item is styled
            CharSequence clipItemText = firstClipItem.getText();
            if(clipItemText == null) {
                AWTInputBridge.nativeClipboardReceived(null, null);
                return;
            }
            AWTInputBridge.nativeClipboardReceived(clipItemText.toString(), "plain");
        });
    }

    @Keep
    public static void putClipboardData(String data, String mimeType) {
        Tools.runOnUiThread(()-> {
            ClipData clipData = null;
            switch(mimeType) {
                case "text/plain":
                    clipData = ClipData.newPlainText("AWT Paste", data);
                    break;
                case "text/html":
                    clipData = ClipData.newHtmlText("AWT Paste", data, data);
            }
            if(clipData != null) GLOBAL_CLIPBOARD.setPrimaryClip(clipData);
        });
    }

    @Override
    public void onClickedMenu() {
        drawerLayout.openDrawer(Gravity.RIGHT);
        navDrawer.requestLayout();
    }

    @Override
    public void exitEditor() {
        try {
            mControlLayout.loadLayout((CustomControls)null);
            mControlLayout.setModifiable(false);
            System.gc();
            mControlLayout.loadLayout(instance.getLaunchControls());
            updateDrawerButton();
        } catch (Exception e) {
            Tools.showError(this,e);
        }

        navDrawer.setAdapter(gameActionArrayAdapter);
        navDrawer.setOnItemClickListener(gameActionClickListener);
        isInEditor = false;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        GameService.LocalBinder localBinder = (GameService.LocalBinder) service;
        mServiceBinder = localBinder;
        minecraftGLView.start(localBinder.isActive, touchpad);
        localBinder.isActive = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    /*
     * Android 14 (or some devices, at least) seems to dispatch the the captured mouse events as trackball events
     * due to a bug(?) somewhere(????)
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private boolean checkCaptureDispatchConditions(MotionEvent event) {
        int eventSource = event.getSource();
        // On my device, the mouse sends events as a relative mouse device.
        // Not comparing with == here because apparently `eventSource` is a mask that can
        // sometimes indicate multiple sources, like in the case of InputDevice.SOURCE_TOUCHPAD
        // (which is *also* an InputDevice.SOURCE_MOUSE when controlling a cursor)
        return (eventSource & InputDevice.SOURCE_MOUSE_RELATIVE) != 0 ||
                (eventSource & InputDevice.SOURCE_MOUSE) != 0;
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent ev) {
        if(Tools.isAndroid8OrHigher() && checkCaptureDispatchConditions(ev))
            return minecraftGLView.dispatchCapturedPointerEvent(ev);
        else return super.dispatchTrackballEvent(ev);
    }
}
