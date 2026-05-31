package net.kdt.pojavlaunch.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.app.ActivityManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.BaseActivity
import net.kdt.pojavlaunch.CustomControlsActivity
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.contracts.OpenDocumentWithExtension
import net.kdt.pojavlaunch.multirt.MultiRTConfigDialog
import net.kdt.pojavlaunch.plugins.LibraryPlugin
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.ui.theme.PojavTheme
import net.kdt.pojavlaunch.utils.RendererCompatUtil

enum class SettingsPage(val titleRes: Int, val iconRes: Int) {
    APPEARANCE(R.string.preference_appearance_title, R.drawable.ic_px_color),
    VIDEO(R.string.preference_video_title, R.drawable.ic_px_image),
    CONTROL(R.string.preference_control_title, R.drawable.ic_px_gamepad),
    JAVA(R.string.preference_java_title, R.drawable.ic_px_java),
    MISC(R.string.preference_misc_title, R.drawable.ic_px_settings),
    EXPERIMENTAL(R.string.preference_experimental_title, R.drawable.ic_px_animation),
    DRAWER_BUTTON(R.string.preference_appearance_title, R.drawable.ic_px_drawer_button)
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    var currentPage by remember { mutableStateOf(SettingsPage.APPEARANCE) }
    var isMainPage by remember { mutableStateOf(true) }
    val railScrollState = rememberScrollState()

    val isPreview = LocalInspectionMode.current
    val backgroundBitmap = if (isPreview) null else BaseActivity.getBackgroundBitmap()
    val hasBackground = backgroundBitmap != null

    Box(modifier = Modifier.fillMaxSize()) {
        // Shared Background Logic
        if (backgroundBitmap != null) {
            Image(
                bitmap = backgroundBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        }

        Box(modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (hasBackground) 0.4f else 0f))
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth > 600.dp
            
            if (isWide) {
                Row(modifier = Modifier.fillMaxSize()) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight().width(80.dp),
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (hasBackground) 0.4f else 1f),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        windowInsets = WindowInsets(0, 0, 0, 0)
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .verticalScroll(railScrollState),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            SettingsPage.entries.filter { it != SettingsPage.DRAWER_BUTTON }.forEach { page ->
                                NavigationRailItem(
                                    selected = currentPage == page && !isMainPage,
                                    onClick = { 
                                        currentPage = page
                                        isMainPage = false
                                    },
                                    icon = { Icon(painterResource(page.iconRes), contentDescription = null, modifier = Modifier.size(24.dp)) },
                                    label = { Text(stringResource(page.titleRes).substringBefore(" "), fontSize = 10.sp) },
                                    alwaysShowLabel = false,
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                )
                            }
                        }
                    }
                    
                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    
                    Scaffold(
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0)
                    ) { padding ->
                        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                            SettingsContent(currentPage, onNavigate = { currentPage = it })
                        }
                    }
                }
            } else {
                if (isMainPage) {
                    Scaffold(
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0)
                    ) { padding ->
                        Box(modifier = Modifier.padding(padding)) {
                            MainSettings(onNavigate = { 
                                currentPage = it
                                isMainPage = false
                            })
                        }
                    }
                } else {
                    Scaffold(
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0)
                    ) { padding ->
                        BackHandler { 
                            if (currentPage == SettingsPage.DRAWER_BUTTON) {
                                currentPage = SettingsPage.APPEARANCE
                            } else {
                                isMainPage = true 
                            }
                        }
                        Box(modifier = Modifier.padding(padding)) {
                            SettingsContent(currentPage, onNavigate = { currentPage = it })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsContent(page: SettingsPage, onNavigate: (SettingsPage) -> Unit) {
    when (page) {
        SettingsPage.APPEARANCE -> AppearanceSettings(onNavigate = onNavigate)
        SettingsPage.VIDEO -> VideoSettings()
        SettingsPage.CONTROL -> ControlSettings()
        SettingsPage.JAVA -> JavaSettings()
        SettingsPage.MISC -> MiscSettings()
        SettingsPage.EXPERIMENTAL -> ExperimentalSettings()
        SettingsPage.DRAWER_BUTTON -> DrawerButtonSettings()
    }
}

@Composable
fun MainSettings(onNavigate: (SettingsPage) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(vertical = 12.dp)) {
        item {
            PreferenceGroup(title = stringResource(R.string.preference_category_main_categories)) {
                PreferenceItem(
                    title = stringResource(R.string.preference_appearance_title),
                    summary = stringResource(R.string.preference_appearance_description),
                    icon = painterResource(R.drawable.ic_px_color),
                    onClick = { onNavigate(SettingsPage.APPEARANCE) }
                )
                PreferenceItem(
                    title = stringResource(R.string.preference_video_title),
                    summary = stringResource(R.string.preference_video_description),
                    icon = painterResource(R.drawable.ic_px_image),
                    onClick = { onNavigate(SettingsPage.VIDEO) }
                )
                PreferenceItem(
                    title = stringResource(R.string.preference_control_title),
                    summary = stringResource(R.string.preference_control_description),
                    icon = painterResource(R.drawable.ic_px_gamepad),
                    onClick = { onNavigate(SettingsPage.CONTROL) }
                )
                PreferenceItem(
                    title = stringResource(R.string.preference_java_title),
                    summary = stringResource(R.string.preference_java_description),
                    icon = painterResource(R.drawable.ic_px_java),
                    onClick = { onNavigate(SettingsPage.JAVA) }
                )
                PreferenceItem(
                    title = stringResource(R.string.preference_misc_title),
                    summary = stringResource(R.string.preference_misc_description),
                    icon = painterResource(R.drawable.ic_px_settings),
                    onClick = { onNavigate(SettingsPage.MISC) }
                )
                PreferenceItem(
                    title = stringResource(R.string.preference_experimental_title),
                    summary = stringResource(R.string.preference_experimental_description),
                    icon = painterResource(R.drawable.ic_px_animation),
                    onClick = { onNavigate(SettingsPage.EXPERIMENTAL) }
                )
            }
        }
        
        item {
            PreferenceGroup {
                var forceEnglish by remember { mutableStateOf(LauncherPreferences.PREF_FORCE_ENGLISH) }
                PreferenceSwitch(
                    title = stringResource(R.string.preference_force_english_title),
                    summary = stringResource(R.string.preference_force_english_description),
                    checked = forceEnglish,
                    onCheckedChange = {
                        forceEnglish = it
                        LauncherPreferences.PREF_FORCE_ENGLISH = it
                        LauncherPreferences.DEFAULT_PREF?.edit { putBoolean("force_english", it) }
                    }
                )
            }
        }
    }
}

@Composable
fun AppearanceSettings(onNavigate: (SettingsPage) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(vertical = 12.dp)) {
        item {
            PreferenceGroup(title = "General") {
                var theme by remember { mutableStateOf(LauncherPreferences.PREF_APP_THEME) }
                PreferenceList(
                    title = stringResource(R.string.preference_theme_title),
                    entries = stringArrayResource(R.array.theme_names),
                    entryValues = stringArrayResource(R.array.theme_values),
                    selectedValue = theme,
                    onValueChange = {
                        theme = it
                        LauncherPreferences.PREF_APP_THEME = it
                        LauncherPreferences.DEFAULT_PREF?.edit { putString("appTheme", it) }
                        LauncherPreferences.applyTheme()
                    },
                    icon = painterResource(R.drawable.ic_px_theme)
                )
                
                PreferenceItem(
                    title = "Drawer Button Settings",
                    summary = "Customise the drawer button and list appearance",
                    icon = painterResource(R.drawable.ic_px_drawer_button),
                    onClick = { onNavigate(SettingsPage.DRAWER_BUTTON) }
                )
                
                PreferenceItem(
                    title = "Launcher Background",
                    summary = "Change the launcher background image",
                    icon = painterResource(R.drawable.ic_px_image)
                )
                
                var backgroundBlur by remember { mutableStateOf(LauncherPreferences.PREF_BACKGROUND_BLUR) }
                PreferenceSwitch(
                    title = "Background Blur",
                    summary = "Apply a blur effect to the background image",
                    checked = backgroundBlur,
                    onCheckedChange = {
                        backgroundBlur = it
                        LauncherPreferences.PREF_BACKGROUND_BLUR = it
                        LauncherPreferences.DEFAULT_PREF?.edit { putBoolean("appBackgroundBlur", it) }
                    }
                )
                
                if (backgroundBlur) {
                    var blurIntensity by remember { mutableFloatStateOf(LauncherPreferences.PREF_BACKGROUND_BLUR_INTENSITY.toFloat()) }
                    PreferenceSlider(
                        title = "Blur Intensity",
                        summary = "Adjust the blur intensity",
                        value = blurIntensity,
                        onValueChange = {
                            blurIntensity = it
                            LauncherPreferences.PREF_BACKGROUND_BLUR_INTENSITY = it.toInt()
                            LauncherPreferences.DEFAULT_PREF?.edit { putInt("appBackgroundBlurIntensity", it.toInt()) }
                        },
                        valueRange = 1f..100f
                    )
                }
                
                var overlayEnabled by remember { mutableStateOf(LauncherPreferences.PREF_BACKGROUND_IMAGE_OVERLAY_ENABLED) }
                PreferenceSwitch(
                    title = "Background Overlay",
                    summary = "Add a transparent overlay on the background image for better readability",
                    checked = overlayEnabled,
                    onCheckedChange = {
                        overlayEnabled = it
                        LauncherPreferences.PREF_BACKGROUND_IMAGE_OVERLAY_ENABLED = it
                        LauncherPreferences.DEFAULT_PREF?.edit { putBoolean("backgroundImageOverlayEnabled", it) }
                    }
                )
                
                if (overlayEnabled) {
                    var overlayOpacity by remember { mutableFloatStateOf(LauncherPreferences.PREF_BACKGROUND_IMAGE_OVERLAY_ALPHA * 100f) }
                    PreferenceSlider(
                        title = "Overlay Transparency (%)",
                        summary = "Adjust overlay transparency",
                        value = overlayOpacity,
                        onValueChange = {
                            overlayOpacity = it
                            LauncherPreferences.PREF_BACKGROUND_IMAGE_OVERLAY_ALPHA = it / 100f
                            LauncherPreferences.DEFAULT_PREF?.edit { putInt("backgroundImageOverlayOpacity", it.toInt()) }
                        },
                        valueRange = 0f..100f
                    )
                }
                
                PreferenceItem(
                    title = "Reset Background",
                    summary = "Reset the launcher background to default"
                )
                
                var iconPreset by remember { mutableStateOf(LauncherPreferences.PREF_ICON_PRESET) }
                PreferenceList(
                    title = "Icon Color Preset",
                    entries = stringArrayResource(R.array.icon_preset_names),
                    entryValues = stringArrayResource(R.array.icon_preset_values),
                    selectedValue = iconPreset,
                    onValueChange = {
                        iconPreset = it
                        LauncherPreferences.PREF_ICON_PRESET = it
                        LauncherPreferences.DEFAULT_PREF?.edit { putString("iconPreset", it) }
                    },
                    icon = painterResource(R.drawable.ic_px_preset)
                )
                
                PreferenceItem(
                    title = "Global Icon Color",
                    summary = "Change the color of various launcher icons",
                    icon = painterResource(R.drawable.ic_px_color)
                )
            }
        }
        
        item {
            PreferenceGroup(title = "Minecraft Button Style") {
                var appearancePreset by remember { mutableStateOf(LauncherPreferences.PREF_APPEARANCE_PRESET) }
                PreferenceList(
                    title = "Appearance Preset",
                    entries = stringArrayResource(R.array.appearance_preset_names),
                    entryValues = stringArrayResource(R.array.appearance_preset_values),
                    selectedValue = appearancePreset,
                    onValueChange = {
                        appearancePreset = it
                        LauncherPreferences.PREF_APPEARANCE_PRESET = it
                        LauncherPreferences.DEFAULT_PREF?.edit { putString("appearancePreset", it) }
                    },
                    icon = painterResource(R.drawable.ic_px_preset)
                )
                
                PreferenceItem(
                    title = "Button Color",
                    summary = "Change the background color of main buttons",
                    icon = painterResource(R.drawable.ic_px_color)
                )
                
                var cornerRadius by remember { mutableFloatStateOf(LauncherPreferences.PREF_MINEBUTTON_CORNER_RADIUS.toFloat()) }
                PreferenceSlider(
                    title = "Button Corner Radius (dp)",
                    value = cornerRadius,
                    onValueChange = {
                        cornerRadius = it
                        LauncherPreferences.PREF_MINEBUTTON_CORNER_RADIUS = it.toInt()
                        LauncherPreferences.DEFAULT_PREF?.edit { putInt("minebutton_corner_radius", it.toInt()) }
                    },
                    valueRange = 0f..50f
                )
            }
        }
        
        item {
            PreferenceGroup(title = "Animations") {
                var animType by remember { mutableStateOf(LauncherPreferences.PREF_ANIMATION_TYPE) }
                PreferenceList(
                    title = stringResource(R.string.preference_animation_type_title),
                    entries = stringArrayResource(R.array.animation_type_names),
                    entryValues = stringArrayResource(R.array.animation_type_values),
                    selectedValue = animType,
                    onValueChange = {
                        animType = it
                        LauncherPreferences.PREF_ANIMATION_TYPE = it
                        LauncherPreferences.DEFAULT_PREF?.edit { putString("animationType", it) }
                    },
                    icon = painterResource(R.drawable.ic_px_animation)
                )
                
                var animIntensity by remember { mutableFloatStateOf(LauncherPreferences.PREF_ANIMATION_INTENSITY * 100f) }
                PreferenceSlider(
                    title = stringResource(R.string.preference_animation_intensity_title),
                    summary = stringResource(R.string.preference_animation_intensity_description),
                    value = animIntensity,
                    onValueChange = {
                        animIntensity = it
                        LauncherPreferences.PREF_ANIMATION_INTENSITY = it / 100f
                        LauncherPreferences.DEFAULT_PREF?.edit { putInt("animationIntensity", it.toInt()) }
                    },
                    valueRange = 0f..100f
                )
            }
        }
        
        item {
            PreferenceGroup(title = "Mouse Pointer") {
                PreferenceItem(title = stringResource(R.string.preference_mouse_cursor_title), summary = stringResource(R.string.preference_mouse_cursor_description))
                PreferenceItem(title = stringResource(R.string.preference_mouse_hotspot_title), summary = stringResource(R.string.preference_mouse_hotspot_description))
                PreferenceItem(title = stringResource(R.string.preference_mouse_reset_title), summary = stringResource(R.string.preference_mouse_reset_description))
            }
        }
        
        item {
            PreferenceGroup(title = "Reset All Appearance Settings") {
                PreferenceItem(title = "Reset Appearance", summary = "Reset all appearance and theme settings to default")
            }
        }
    }
}

@Composable
fun VideoSettings() {
    val context = LocalContext.current
    val compatibleRenderers = remember { RendererCompatUtil.getCompatibleRenderers(context) }
    val rendererNames = remember { compatibleRenderers.rendererDisplayNames?.filterNotNull()?.toTypedArray() ?: emptyArray() }
    val rendererValues = remember { compatibleRenderers.rendererIds.filterNotNull().toTypedArray() }
    
    val supportsSustainedPerf = remember { Build.VERSION.SDK_INT >= Build.VERSION_CODES.N }
    
    val allPlugins = remember { LibraryPlugin.discoverAllPlugins(context) }
    val hasAnglePlugin = remember { allPlugins.any { it.packageName == LibraryPlugin.ID_ANGLE_PLUGIN } }

    LazyColumn(contentPadding = PaddingValues(vertical = 12.dp)) {
        item {
            PreferenceGroup(title = stringResource(R.string.preference_category_video)) {
                var renderer by remember { mutableStateOf(LauncherPreferences.PREF_RENDERER) }
                PreferenceList(
                    title = stringResource(R.string.mcl_setting_category_renderer),
                    entries = rendererNames,
                    entryValues = rendererValues,
                    selectedValue = renderer,
                    onValueChange = {
                        renderer = it
                        LauncherPreferences.PREF_RENDERER = it
                        LauncherPreferences.DEFAULT_PREF?.edit { putString("renderer", it) }
                    },
                    icon = painterResource(R.drawable.ic_px_image_renderer)
                )
                
                var backend by remember { mutableStateOf(LauncherPreferences.PREF_PREFERRED_GRAPHICS_BACKEND) }
                PreferenceList(
                    title = stringResource(R.string.mcl_setting_title_preferred_graphics_backend),
                    entries = stringArrayResource(R.array.graphics_backend_names),
                    entryValues = stringArrayResource(R.array.graphics_backend_values),
                    selectedValue = backend,
                    onValueChange = {
                        backend = it
                        LauncherPreferences.PREF_PREFERRED_GRAPHICS_BACKEND = it
                        LauncherPreferences.DEFAULT_PREF?.edit { putString("preferredGraphicsBackend", it) }
                    },
                    icon = painterResource(R.drawable.ic_px_image)
                )
                
                var ignoreNotch by remember { mutableStateOf(LauncherPreferences.PREF_IGNORE_NOTCH) }
                PreferenceSwitch(
                    title = stringResource(R.string.mcl_setting_title_ignore_notch),
                    summary = stringResource(R.string.mcl_setting_subtitle_ignore_notch),
                    icon = painterResource(R.drawable.ic_px_viewport_expand),
                    checked = ignoreNotch,
                    onCheckedChange = {
                        ignoreNotch = it
                        LauncherPreferences.PREF_IGNORE_NOTCH = it
                        LauncherPreferences.DEFAULT_PREF?.edit { putBoolean("ignoreNotch", it) }
                    }
                )
                
                var resRatio by remember { mutableFloatStateOf(LauncherPreferences.PREF_SCALE_FACTOR * 100f) }
                PreferenceSlider(
                    title = stringResource(R.string.mcl_setting_title_resolution_scaler),
                    summary = stringResource(R.string.mcl_setting_subtitle_resolution_scaler),
                    value = resRatio,
                    onValueChange = {
                        resRatio = it
                        LauncherPreferences.PREF_SCALE_FACTOR = it / 100f
                        LauncherPreferences.DEFAULT_PREF?.edit { putInt("resolutionRatio", it.toInt()) }
                    },
                    valueRange = 25f..100f,
                    icon = painterResource(R.drawable.ic_px_resolution)
                )
                
                if (supportsSustainedPerf) {
                    var sustainedPerf by remember { mutableStateOf(LauncherPreferences.PREF_SUSTAINED_PERFORMANCE) }
                    PreferenceSwitch(
                        title = stringResource(R.string.preference_sustained_performance_title),
                        summary = stringResource(R.string.preference_sustained_performance_description),
                        checked = sustainedPerf,
                        onCheckedChange = {
                            sustainedPerf = it
                            LauncherPreferences.PREF_SUSTAINED_PERFORMANCE = it
                            LauncherPreferences.DEFAULT_PREF?.edit { putBoolean("sustainedPerformance", it) }
                        }
                    )
                }
                
                var altSurface by remember { mutableStateOf(LauncherPreferences.PREF_USE_ALTERNATE_SURFACE) }
                PreferenceSwitch(
                    title = stringResource(R.string.mcl_setting_title_use_surface_view),
                    summary = stringResource(R.string.mcl_setting_subtitle_use_surface_view),
                    checked = altSurface,
                    onCheckedChange = {
                        altSurface = it
                        LauncherPreferences.PREF_USE_ALTERNATE_SURFACE = it
                        LauncherPreferences.DEFAULT_PREF?.edit { putBoolean("alternate_surface", it) }
                    }
                )
                
                var forceVsync by remember { mutableStateOf(LauncherPreferences.PREF_FORCE_VSYNC) }
                PreferenceSwitch(
                    title = stringResource(R.string.preference_force_vsync_title),
                    summary = stringResource(R.string.preference_force_vsync_description),
                    checked = forceVsync,
                    onCheckedChange = {
                        forceVsync = it
                        LauncherPreferences.PREF_FORCE_VSYNC = it
                        LauncherPreferences.DEFAULT_PREF?.edit { putBoolean("force_vsync", it) }
                    }
                )
                
                if (hasAnglePlugin) {
                    var useAngle by remember { mutableStateOf(LauncherPreferences.PREF_USE_ANGLE) }
                    PreferenceSwitch(
                        title = stringResource(R.string.preference_use_angle_title),
                        summary = stringResource(R.string.preference_use_angle_description),
                        checked = useAngle,
                        onCheckedChange = {
                            useAngle = it
                            LauncherPreferences.PREF_USE_ANGLE = it
                            LauncherPreferences.DEFAULT_PREF?.edit { putBoolean("use_angle", it) }
                        }
                    )
                }
                
                var vsinkInZink by remember { mutableStateOf(LauncherPreferences.PREF_VSYNC_IN_ZINK) }
                PreferenceSwitch(
                    title = stringResource(R.string.preference_vsync_in_zink_title),
                    summary = stringResource(R.string.preference_vsync_in_zink_description),
                    checked = vsinkInZink,
                    onCheckedChange = {
                        vsinkInZink = it
                        LauncherPreferences.PREF_VSYNC_IN_ZINK = it
                        LauncherPreferences.DEFAULT_PREF?.edit { putBoolean("vsync_in_zink", it) }
                    }
                )
            }
        }
    }
}

@Composable
fun ControlSettings() {
    val context = LocalContext.current
    val supportsGyro = remember { Tools.deviceSupportsGyro(context) }

    LazyColumn(contentPadding = PaddingValues(vertical = 12.dp)) {
        item {
            PreferenceGroup {
                PreferenceItem(
                    title = stringResource(R.string.preference_edit_controls_title),
                    summary = stringResource(R.string.preference_edit_controls_summary),
                    icon = painterResource(R.drawable.ic_px_gamepad),
                    onClick = {
                        context.startActivity(Intent(context, CustomControlsActivity::class.java))
                    }
                )
            }
        }

        item {
            PreferenceGroup(title = stringResource(R.string.preference_category_gestures)) {
                var disableGestures by remember { mutableStateOf(LauncherPreferences.PREF_DISABLE_GESTURES) }
                PreferenceSwitch(
                    title = stringResource(R.string.mcl_disable_gestures),
                    summary = stringResource(R.string.mcl_disable_gestures_subtitle),
                    icon = painterResource(R.drawable.ic_px_nogestures),
                    checked = disableGestures,
                    onCheckedChange = {
                        disableGestures = it
                        LauncherPreferences.PREF_DISABLE_GESTURES = it
                        LauncherPreferences.DEFAULT_PREF?.edit { putBoolean("disableGestures", it) }
                    }
                )
                
                var disableDoubleTap by remember { mutableStateOf(LauncherPreferences.PREF_DISABLE_SWAP_HAND) }
                PreferenceSwitch(
                    title = stringResource(R.string.mcl_disable_swap_hand),
                    summary = stringResource(R.string.mcl_disable_swap_hand_subtitle),
                    checked = disableDoubleTap,
                    onCheckedChange = {
                        disableDoubleTap = it
                        LauncherPreferences.PREF_DISABLE_SWAP_HAND = it
                        LauncherPreferences.DEFAULT_PREF?.edit { putBoolean("disableDoubleTap", it) }
                    }
                )
                
                var longPressTrigger by remember { mutableFloatStateOf(LauncherPreferences.PREF_LONGPRESS_TRIGGER.toFloat()) }
                PreferenceSlider(
                    title = stringResource(R.string.mcl_setting_title_longpresstrigger),
                    summary = stringResource(R.string.mcl_setting_subtitle_longpresstrigger),
                    icon = painterResource(R.drawable.ic_px_gestures),
                    value = longPressTrigger,
                    onValueChange = {
                        longPressTrigger = it
                        LauncherPreferences.PREF_LONGPRESS_TRIGGER = it.toInt()
                        LauncherPreferences.DEFAULT_PREF?.edit { putInt("timeLongPressTrigger", it.toInt()) }
                    },
                    valueRange = 100f..1000f
                )
            }
        }
        
        item {
            PreferenceGroup(title = stringResource(R.string.preference_category_buttons)) {
                var buttonScale by remember { mutableFloatStateOf(LauncherPreferences.PREF_BUTTONSIZE) }
                PreferenceSlider(
                    title = stringResource(R.string.mcl_setting_title_buttonscale),
                    summary = stringResource(R.string.mcl_setting_subtitle_buttonscale),
                    icon = painterResource(R.drawable.ic_px_control_size),
                    value = buttonScale,
                    onValueChange = {
                        buttonScale = it
                        LauncherPreferences.PREF_BUTTONSIZE = it
                        LauncherPreferences.DEFAULT_PREF?.edit { putInt("buttonscale", it.toInt()) }
                    },
                    valueRange = 50f..200f
                )
                
                var buttonAllCaps by remember { mutableStateOf(LauncherPreferences.PREF_BUTTON_ALL_CAPS) }
                PreferenceSwitch(
                    title = stringResource(R.string.mcl_setting_title_buttonallcaps),
                    summary = stringResource(R.string.mcl_setting_subtitle_buttonallcaps),
                    checked = buttonAllCaps,
                    onCheckedChange = {
                        buttonAllCaps = it
                        LauncherPreferences.PREF_BUTTON_ALL_CAPS = it
                        LauncherPreferences.DEFAULT_PREF?.edit { putBoolean("buttonAllCaps", it) }
                    }
                )
            }
        }

        item {
            PreferenceGroup(title = stringResource(R.string.preference_category_virtual_mouse)) {
                var mouseScale by remember { mutableFloatStateOf(LauncherPreferences.PREF_MOUSESCALE * 100f) }
                PreferenceSlider(
                    title = stringResource(R.string.mcl_setting_title_mousescale),
                    summary = stringResource(R.string.mcl_setting_subtitle_mousescale),
                    icon = painterResource(R.drawable.ic_px_mouse),
                    value = mouseScale,
                    onValueChange = {
                        mouseScale = it
                        LauncherPreferences.PREF_MOUSESCALE = it / 100f
                        LauncherPreferences.DEFAULT_PREF?.edit { putInt("mousescale", it.toInt()) }
                    },
                    valueRange = 25f..300f
                )
                
                var mouseSpeed by remember { mutableFloatStateOf(LauncherPreferences.PREF_MOUSESPEED * 100f) }
                PreferenceSlider(
                    title = stringResource(R.string.mcl_setting_title_mousespeed),
                    summary = stringResource(R.string.mcl_setting_subtitle_mousespeed),
                    icon = painterResource(R.drawable.ic_px_speed),
                    value = mouseSpeed,
                    onValueChange = {
                        mouseSpeed = it
                        LauncherPreferences.PREF_MOUSESPEED = it / 100f
                        LauncherPreferences.DEFAULT_PREF?.edit { putInt("mousespeed", it.toInt()) }
                    },
                    valueRange = 25f..300f
                )
                
                var mouseStart by remember { mutableStateOf(LauncherPreferences.PREF_VIRTUAL_MOUSE_START) }
                PreferenceSwitch(
                    title = stringResource(R.string.preference_mouse_start_title),
                    summary = stringResource(R.string.preference_mouse_start_description),
                    checked = mouseStart,
                    onCheckedChange = {
                        mouseStart = it
                        LauncherPreferences.PREF_VIRTUAL_MOUSE_START = it
                        LauncherPreferences.DEFAULT_PREF?.edit { putBoolean("mouse_start", it) }
                    }
                )
            }
        }

        if (supportsGyro) {
            item {
                PreferenceGroup(title = stringResource(R.string.preference_category_gyro_controls)) {
                    var enableGyro by remember { mutableStateOf(LauncherPreferences.PREF_ENABLE_GYRO) }
                    PreferenceSwitch(
                        title = stringResource(R.string.preference_enable_gyro_title),
                        summary = stringResource(R.string.preference_enable_gyro_description),
                        checked = enableGyro,
                        onCheckedChange = {
                            enableGyro = it
                            LauncherPreferences.PREF_ENABLE_GYRO = it
                            LauncherPreferences.DEFAULT_PREF?.edit { putBoolean("enableGyro", it) }
                        }
                    )
                    
                    if (enableGyro) {
                        var gyroSens by remember { mutableFloatStateOf(LauncherPreferences.PREF_GYRO_SENSITIVITY * 100f) }
                        PreferenceSlider(
                            title = stringResource(R.string.preference_gyro_sensitivity_title),
                            summary = stringResource(R.string.preference_gyro_sensitivity_description),
                            value = gyroSens,
                            onValueChange = {
                                gyroSens = it
                                LauncherPreferences.PREF_GYRO_SENSITIVITY = it / 100f
                                LauncherPreferences.DEFAULT_PREF?.edit { putInt("gyroSensitivity", it.toInt()) }
                            },
                            valueRange = 25f..300f
                        )
                        
                        var gyroRate by remember { mutableFloatStateOf(LauncherPreferences.PREF_GYRO_SAMPLE_RATE.toFloat()) }
                        PreferenceSlider(
                            title = stringResource(R.string.preference_gyro_sample_rate_title),
                            summary = stringResource(R.string.preference_gyro_sample_rate_description),
                            value = gyroRate,
                            onValueChange = {
                                gyroRate = it
                                LauncherPreferences.PREF_GYRO_SAMPLE_RATE = it.toInt()
                                LauncherPreferences.DEFAULT_PREF?.edit { putInt("gyroSampleRate", it.toInt()) }
                            },
                            valueRange = 5f..50f
                        )
                        
                        var gyroSmoothing by remember { mutableStateOf(LauncherPreferences.PREF_GYRO_SMOOTHING) }
                        PreferenceSwitch(
                            title = stringResource(R.string.preference_gyro_smoothing_title),
                            summary = stringResource(R.string.preference_gyro_smoothing_description),
                            checked = gyroSmoothing,
                            onCheckedChange = {
                                gyroSmoothing = it
                                LauncherPreferences.PREF_GYRO_SMOOTHING = it
                                LauncherPreferences.DEFAULT_PREF?.edit { putBoolean("gyroSmoothing", it) }
                            }
                        )
                        
                        var gyroInvertX by remember { mutableStateOf(LauncherPreferences.PREF_GYRO_INVERT_X) }
                        PreferenceSwitch(
                            title = stringResource(R.string.preference_gyro_invert_x_axis),
                            summary = stringResource(R.string.preference_gyro_invert_x_axis_description),
                            checked = gyroInvertX,
                            onCheckedChange = {
                                gyroInvertX = it
                                LauncherPreferences.PREF_GYRO_INVERT_X = it
                                LauncherPreferences.DEFAULT_PREF?.edit { putBoolean("gyroInvertX", it) }
                            }
                        )
                        
                        var gyroInvertY by remember { mutableStateOf(LauncherPreferences.PREF_GYRO_INVERT_Y) }
                        PreferenceSwitch(
                            title = stringResource(R.string.preference_gyro_invert_y_axis),
                            summary = stringResource(R.string.preference_gyro_invert_y_axis_description),
                            checked = gyroInvertY,
                            onCheckedChange = {
                                gyroInvertY = it
                                LauncherPreferences.PREF_GYRO_INVERT_Y = it
                                LauncherPreferences.DEFAULT_PREF?.edit { putBoolean("gyroInvertY", it) }
                            }
                        )
                    }
                }
            }
        }

        item {
            PreferenceGroup(title = stringResource(R.string.preference_category_controller_settings)) {
                PreferenceItem(title = stringResource(R.string.preference_remap_controller_title), summary = stringResource(R.string.preference_remap_controller_description))
                PreferenceItem(title = stringResource(R.string.preference_wipe_controller_title), summary = stringResource(R.string.preference_wipe_controller_description))
                
                var deadzone by remember { mutableFloatStateOf(LauncherPreferences.PREF_DEADZONE_SCALE * 100f) }
                PreferenceSlider(
                    title = stringResource(R.string.preference_deadzone_scale_title),
                    summary = stringResource(R.string.preference_deadzone_scale_description),
                    value = deadzone,
                    onValueChange = {
                        deadzone = it
                        LauncherPreferences.PREF_DEADZONE_SCALE = it / 100f
                        LauncherPreferences.DEFAULT_PREF?.edit { putInt("gamepad_deadzone_scale", it.toInt()) }
                    },
                    valueRange = 50f..200f
                )
            }
        }
    }
}

@Composable
fun JavaSettings() {
    val context = LocalContext.current
    var showJvmArgsDialog by remember { mutableStateOf(false) }

    val mVmInstallLauncher = rememberLauncherForActivityResult(
        OpenDocumentWithExtension("xz")
    ) { uri ->
        if (uri != null) Tools.installRuntimeFromUri(context, uri)
    }

    val mDialogScreen = remember {
        MultiRTConfigDialog().apply {
            prepare(context, mVmInstallLauncher)
        }
    }

    if (showJvmArgsDialog) {
        var jvmArgs by remember { mutableStateOf(LauncherPreferences.PREF_CUSTOM_JAVA_ARGS ?: "") }
        AlertDialog(
            onDismissRequest = { showJvmArgsDialog = false },
            title = { Text(stringResource(R.string.mcl_setting_title_javaargs)) },
            text = {
                OutlinedTextField(
                    value = jvmArgs,
                    onValueChange = { jvmArgs = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.mcl_setting_subtitle_javaargs)) }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    LauncherPreferences.PREF_CUSTOM_JAVA_ARGS = jvmArgs
                    LauncherPreferences.DEFAULT_PREF?.edit { putString("javaArgs", jvmArgs) }
                    showJvmArgsDialog = false
                }) {
                    Text(stringResource(R.string.global_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showJvmArgsDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    LazyColumn(contentPadding = PaddingValues(vertical = 12.dp)) {
        item {
            PreferenceGroup(title = stringResource(R.string.preference_category_java_tweaks)) {
                PreferenceItem(
                    title = stringResource(R.string.multirt_title),
                    summary = stringResource(R.string.multirt_subtitle),
                    onClick = {
                        mDialogScreen.show()
                    }
                )
                
                PreferenceItem(
                    title = stringResource(R.string.mcl_setting_title_javaargs),
                    summary = LauncherPreferences.PREF_CUSTOM_JAVA_ARGS?.ifEmpty { null } ?: stringResource(R.string.mcl_setting_subtitle_javaargs),
                    icon = painterResource(R.drawable.ic_px_java),
                    onClick = {
                        showJvmArgsDialog = true
                    }
                )
                
                var ram by remember { mutableFloatStateOf(LauncherPreferences.PREF_RAM_ALLOCATION.toFloat()) }
                PreferenceSlider(
                    title = stringResource(R.string.mcl_memory_allocation),
                    summary = stringResource(R.string.mcl_memory_allocation_subtitle),
                    value = ram,
                    onValueChange = {
                        ram = it
                        LauncherPreferences.PREF_RAM_ALLOCATION = it.toInt()
                        LauncherPreferences.DEFAULT_PREF?.edit { putInt("allocation", it.toInt()) }
                    },
                    valueRange = 512f..8192f
                )
                
                var javaSandbox by remember { mutableStateOf(LauncherPreferences.PREF_JAVA_SANDBOX) }
                PreferenceSwitch(
                    title = stringResource(R.string.mcl_setting_java_sandbox),
                    summary = stringResource(R.string.mcl_setting_java_sandbox_subtitle),
                    checked = javaSandbox,
                    onCheckedChange = {
                        javaSandbox = it
                        LauncherPreferences.PREF_JAVA_SANDBOX = it
                        LauncherPreferences.DEFAULT_PREF?.edit { putBoolean("java_sandbox", it) }
                    }
                )
            }
        }
    }
}

@Composable
fun MiscSettings() {
    LazyColumn(contentPadding = PaddingValues(vertical = 12.dp)) {
        item {
            PreferenceGroup(title = stringResource(R.string.preference_category_miscellaneous)) {
                var checkFiles by remember { mutableStateOf(LauncherPreferences.PREF_VERIFY_FILES) }
                PreferenceSwitch(
                    title = stringResource(R.string.mcl_setting_check_libraries),
                    summary = stringResource(R.string.mcl_setting_check_libraries_subtitle),
                    icon = painterResource(R.drawable.ic_px_hash),
                    checked = checkFiles,
                    onCheckedChange = {
                        checkFiles = it
                        LauncherPreferences.PREF_VERIFY_FILES = it
                        LauncherPreferences.DEFAULT_PREF?.edit { putBoolean("checkGameFiles", it) }
                    }
                )
                
                var fastStart by remember { mutableStateOf(LauncherPreferences.PREF_RAPID_START) }
                PreferenceSwitch(
                    title = stringResource(R.string.preference_go_vroom_title),
                    summary = stringResource(R.string.preference_go_vroom_description),
                    checked = fastStart,
                    onCheckedChange = {
                        fastStart = it
                        LauncherPreferences.PREF_RAPID_START = it
                        LauncherPreferences.DEFAULT_PREF?.edit { putBoolean("fastStartupCheck", it) }
                    }
                )
                
                var dlSource by remember { mutableStateOf(LauncherPreferences.PREF_DOWNLOAD_SOURCE) }
                PreferenceList(
                    title = stringResource(R.string.preference_download_source_title),
                    entries = stringArrayResource(R.array.download_source_names),
                    entryValues = stringArrayResource(R.array.download_source_values),
                    selectedValue = dlSource,
                    onValueChange = {
                        dlSource = it
                        LauncherPreferences.PREF_DOWNLOAD_SOURCE = it
                        LauncherPreferences.DEFAULT_PREF?.edit { putString("downloadSource", it) }
                    },
                    icon = painterResource(R.drawable.ic_px_download)
                )
                
                var verifyManifest by remember { mutableStateOf(LauncherPreferences.PREF_VERIFY_MANIFEST) }
                PreferenceSwitch(
                    title = stringResource(R.string.preference_verify_manifest_title),
                    summary = stringResource(R.string.preference_verify_manifest_description),
                    checked = verifyManifest,
                    onCheckedChange = {
                        verifyManifest = it
                        LauncherPreferences.PREF_VERIFY_MANIFEST = it
                        LauncherPreferences.DEFAULT_PREF?.edit { putBoolean("verifyManifest", it) }
                    }
                )
                
                var zinkPreferSystem by remember { mutableStateOf(LauncherPreferences.PREF_ZINK_PREFER_SYSTEM_DRIVER) }
                PreferenceSwitch(
                    title = stringResource(R.string.preference_vulkan_driver_system_title),
                    summary = stringResource(R.string.preference_vulkan_driver_system_description),
                    checked = zinkPreferSystem,
                    onCheckedChange = {
                        zinkPreferSystem = it
                        LauncherPreferences.PREF_ZINK_PREFER_SYSTEM_DRIVER = it
                        LauncherPreferences.DEFAULT_PREF?.edit { putBoolean("zinkPreferSystemDriver", it) }
                    }
                )
            }
        }
    }
}

@Composable
fun ExperimentalSettings() {
    LazyColumn(contentPadding = PaddingValues(vertical = 12.dp)) {
        item {
            PreferenceGroup(title = stringResource(R.string.preference_category_experimental_settings)) {
                var dumpShaders by remember { mutableStateOf(LauncherPreferences.PREF_DUMP_SHADERS) }
                PreferenceSwitch(
                    title = stringResource(R.string.preference_shader_dump_title),
                    summary = stringResource(R.string.preference_shader_dump_description),
                    checked = dumpShaders,
                    onCheckedChange = {
                        dumpShaders = it
                        LauncherPreferences.PREF_DUMP_SHADERS = it
                        LauncherPreferences.DEFAULT_PREF?.edit { putBoolean("dump_shaders", it) }
                    }
                )
                
                var bigCore by remember { mutableStateOf(LauncherPreferences.PREF_BIG_CORE_AFFINITY) }
                PreferenceSwitch(
                    title = stringResource(R.string.preference_force_big_core_title),
                    summary = stringResource(R.string.preference_force_big_core_desc),
                    checked = bigCore,
                    onCheckedChange = {
                        bigCore = it
                        LauncherPreferences.PREF_BIG_CORE_AFFINITY = it
                        LauncherPreferences.DEFAULT_PREF?.edit { putBoolean("bigCoreAffinity", it) }
                    }
                )
                
                var enableMipmap by remember { mutableStateOf(LauncherPreferences.PREF_ENABLE_MIPMAP) }
                PreferenceSwitch(
                    title = stringResource(R.string.preference_experimental_mipmap_title),
                    summary = stringResource(R.string.preference_experimental_mipmap_desc),
                    checked = enableMipmap,
                    onCheckedChange = {
                        enableMipmap = it
                        LauncherPreferences.PREF_ENABLE_MIPMAP = it
                        LauncherPreferences.DEFAULT_PREF?.edit { putBoolean("enableMipmap", it) }
                    }
                )
                
                var disableErrorCheck by remember { mutableStateOf(LauncherPreferences.PREF_DISABLE_ERROR_CHECK) }
                PreferenceSwitch(
                    title = stringResource(R.string.preference_experimental_no_error_title),
                    summary = stringResource(R.string.preference_experimental_no_error_desc),
                    checked = disableErrorCheck,
                    onCheckedChange = {
                        disableErrorCheck = it
                        LauncherPreferences.PREF_DISABLE_ERROR_CHECK = it
                        LauncherPreferences.DEFAULT_PREF?.edit { putBoolean("disableErrorCheck", it) }
                    }
                )
                
                var optimizeNetwork by remember { mutableStateOf(LauncherPreferences.PREF_OPTIMIZE_NETWORK) }
                PreferenceSwitch(
                    title = stringResource(R.string.preference_experimental_network_optimization_title),
                    summary = stringResource(R.string.preference_experimental_network_optimization_desc),
                    checked = optimizeNetwork,
                    onCheckedChange = {
                        optimizeNetwork = it
                        LauncherPreferences.PREF_OPTIMIZE_NETWORK = it
                        LauncherPreferences.DEFAULT_PREF?.edit { putBoolean("optimizeNetwork", it) }
                    }
                )
            }
        }
    }
}

@Composable
fun DrawerButtonSettings() {
    LazyColumn(contentPadding = PaddingValues(vertical = 12.dp)) {
        item {
            PreferenceGroup(title = "Drawer Button Style") {
                var buttonSize by remember { mutableFloatStateOf(LauncherPreferences.PREF_DRAWER_BUTTON_SIZE.toFloat()) }
                PreferenceSlider(
                    title = "Button Size (dp)",
                    value = buttonSize,
                    onValueChange = {
                        buttonSize = it
                        LauncherPreferences.PREF_DRAWER_BUTTON_SIZE = it.toInt()
                        LauncherPreferences.DEFAULT_PREF?.edit { putInt("drawerButtonSize", it.toInt()) }
                    },
                    valueRange = 20f..100f
                )
            }
        }
    }
}

@Composable
fun PreferenceGroup(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)
            )
        }
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp).copy(alpha = 0.55f),
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp), content = content)
        }
    }
}

@Composable
fun PreferenceItem(
    title: String,
    summary: String? = null,
    icon: Painter? = null,
    onClick: () -> Unit = {}
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
        supportingContent = summary?.let { { Text(it, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) } },
        leadingContent = icon?.let { { Icon(it, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface) } },
        modifier = Modifier.clickable { onClick() }.padding(horizontal = 4.dp, vertical = 2.dp),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
fun PreferenceSwitch(
    title: String,
    summary: String? = null,
    icon: Painter? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
        supportingContent = summary?.let { { Text(it, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) } },
        leadingContent = icon?.let { { Icon(it, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface) } },
        trailingContent = {
            Switch(
                checked = checked, 
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    uncheckedBorderColor = Color.Transparent
                )
            )
        },
        modifier = Modifier.clickable { onCheckedChange(!checked) }.padding(horizontal = 4.dp, vertical = 2.dp),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
fun PreferenceSlider(
    title: String,
    summary: String? = null,
    icon: Painter? = null,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp).padding(end = 16.dp), tint = MaterialTheme.colorScheme.onSurface)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                summary?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                )
            )
            Text(
                text = value.toInt().toString(),
                modifier = Modifier.padding(start = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun PreferenceList(
    title: String,
    summary: String? = null,
    icon: Painter? = null,
    entries: Array<String>,
    entryValues: Array<String>,
    selectedValue: String,
    onValueChange: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    
    val selectedIndex = entryValues.indexOf(selectedValue).coerceAtLeast(0)
    val displayValue = if (selectedIndex < entries.size) entries[selectedIndex] else selectedValue
    
    PreferenceItem(
        title = title,
        summary = summary ?: displayValue,
        icon = icon,
        onClick = { showDialog = true }
    )
    
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(title, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn {
                    items(entries.zip(entryValues)) { (name, value) ->
                        ListItem(
                            headlineContent = { Text(name, color = MaterialTheme.colorScheme.onSurface) },
                            modifier = Modifier.clickable {
                                onValueChange(value)
                                showDialog = false
                            },
                            trailingContent = {
                                RadioButton(
                                    selected = value == selectedValue,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary,
                                        unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun SettingsScreenPreview() {
    PojavTheme(dynamicColor = true) {
        SettingsScreen(onBack = {})
    }
}
