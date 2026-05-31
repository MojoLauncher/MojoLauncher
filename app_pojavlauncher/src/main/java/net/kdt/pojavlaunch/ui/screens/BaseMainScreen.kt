package net.kdt.pojavlaunch.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import android.widget.ListView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.kdt.LoggerView
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.BaseActivity
import net.kdt.pojavlaunch.MinecraftGLSurface
import net.kdt.pojavlaunch.customcontrols.ControlLayout
import net.kdt.pojavlaunch.customcontrols.handleview.DrawerPullButton
import net.kdt.pojavlaunch.customcontrols.keyboard.TouchCharInput
import net.kdt.pojavlaunch.customcontrols.mouse.HotbarView
import net.kdt.pojavlaunch.customcontrols.mouse.Touchpad
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.extra.ExtraListener
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.ui.theme.PojavTheme

@Composable
fun BaseMainScreen(
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    onControlLayoutBound: (ControlLayout) -> Unit = {},
    onGlSurfaceBound: (MinecraftGLSurface) -> Unit = {},
    onTouchpadBound: (Touchpad) -> Unit = {},
    onCharInputBound: (TouchCharInput) -> Unit = {},
    onPullButtonBound: (DrawerPullButton) -> Unit = {},
    onHotbarBound: (HotbarView) -> Unit = {},
    onLoggerBound: (LoggerView) -> Unit = {},
    onNavListBound: (ListView) -> Unit = {},
    drawerContent: @Composable (isExpanded: Boolean) -> Unit = {},
    loadingVisible: Boolean = true,
    onLoadingClick: () -> Unit = {},
    onDismissMenu: () -> Unit = {}
) {
    val isPreview = LocalInspectionMode.current
    val ignoreNotch = if (isPreview) true else LauncherPreferences.PREF_IGNORE_NOTCH

    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val scrimColor = MaterialTheme.colorScheme.scrim
    val surfaceContainerHighColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    var isRailExpanded by remember { mutableStateOf(false) }
    val railWidth by animateDpAsState(
        targetValue = if (isRailExpanded) 200.dp else 80.dp,
        label = "railWidth"
    )

    // ✅ Background State
    var backgroundBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var blurEnabled by remember { mutableStateOf(false) }
    var blurIntensity by remember { mutableFloatStateOf(0f) }
    var overlayEnabled by remember { mutableStateOf(false) }
    var overlayOpacity by remember { mutableFloatStateOf(0f) }

    fun updateBackgroundState() {
        if (isPreview) return
        val path = LauncherPreferences.PREF_BACKGROUND_PATH
        backgroundBitmap = if (path != null) BitmapFactory.decodeFile(path) else null
        blurEnabled = LauncherPreferences.PREF_BACKGROUND_BLUR
        blurIntensity = LauncherPreferences.PREF_BACKGROUND_BLUR_INTENSITY.toFloat()
        overlayEnabled = LauncherPreferences.PREF_BACKGROUND_IMAGE_OVERLAY_ENABLED
        overlayOpacity = LauncherPreferences.PREF_BACKGROUND_IMAGE_OVERLAY_ALPHA
    }

    val backgroundRefreshListener = remember {
        ExtraListener<Any?> { _, _ ->
            updateBackgroundState()
            false
        }
    }

    LaunchedEffect(Unit) {
        updateBackgroundState()
        ExtraCore.addExtraListener(ExtraConstants.REFRESH_BACKGROUND, backgroundRefreshListener)
    }

    val hasBackground = backgroundBitmap != null

    val layoutModifier = if (ignoreNotch) {
        Modifier.fillMaxSize()
    } else {
        Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
    }

    Box(modifier = layoutModifier) {
        // Shared Background Logic
        if (backgroundBitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = backgroundBitmap!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .run {
                        if (blurEnabled && blurIntensity > 0) blur((blurIntensity / 2.5f).dp) else this
                    },
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            )
        }

        // Overlay Logic - Use MaterialTheme surface color
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = if (hasBackground && overlayEnabled) overlayOpacity else 0f))
        )

        // Tint Overlay (reduced alpha to avoid washing out dynamic colors)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(primaryContainerColor.copy(alpha = 0.08f))
        )

        AndroidView(
            factory = { ctx ->
                val contentFrame = FrameLayout(ctx).apply { id = R.id.content_frame }

                val controlLayout = ControlLayout(ctx).apply {
                    id = R.id.main_control_layout
                    layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                }

                val glSurface = MinecraftGLSurface(ctx).apply {
                    id = R.id.main_game_render_view
                    layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                }

                val touchpad = Touchpad(ctx).apply {
                    id = R.id.main_touchpad
                    layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    visibility = View.GONE
                    translationZ = resources.displayMetrics.density * 1f
                }

                val charInput = TouchCharInput(ctx).apply {
                    id = R.id.mainTouchCharInput
                    layoutParams = FrameLayout.LayoutParams(1, 1)
                }

                val pullButton = DrawerPullButton(ctx).apply {
                    id = R.id.drawer_button
                    val size = (2 * ctx.resources.displayMetrics.density).toInt()
                    layoutParams = FrameLayout.LayoutParams(size, size)
                    visibility = View.GONE
                    elevation = resources.displayMetrics.density * 10f
                }

                val hotbarView = HotbarView(ctx).apply {
                    id = R.id.hotbar_view
                    layoutParams = FrameLayout.LayoutParams(0, 0)
                }

                controlLayout.addView(glSurface)
                controlLayout.addView(touchpad)
                controlLayout.addView(charInput)
                controlLayout.addView(pullButton)
                controlLayout.addView(hotbarView)

                contentFrame.addView(controlLayout)

                val loggerView = LoggerView(ctx).apply {
                    id = R.id.mainLoggerView
                    layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    visibility = View.GONE
                }
                contentFrame.addView(loggerView)

                onControlLayoutBound(controlLayout)
                onGlSurfaceBound(glSurface)
                onTouchpadBound(touchpad)
                onCharInputBound(charInput)
                onPullButtonBound(pullButton)
                onHotbarBound(hotbarView)
                onLoggerBound(loggerView)

                contentFrame
            },
            modifier = Modifier.fillMaxSize()
        )

        if (drawerState.isOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrimColor.copy(alpha = 0.32f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            isRailExpanded = false
                            onDismissMenu()
                        }
                    )
            )
        }

        AnimatedVisibility(
            visible = drawerState.isOpen,
            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            NavigationRail(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(railWidth),
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                header = {
                    IconButton(onClick = { isRailExpanded = !isRailExpanded }) {
                        Icon(
                            imageVector = if (isRailExpanded)
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft
                            else
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Expand menu",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    drawerContent(isRailExpanded)
                }
            }
        }

        AnimatedVisibility(
            visible = loadingVisible,
            enter = fadeIn(),
            exit = fadeOut(animationSpec = tween(durationMillis = 800))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrimColor.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onLoadingClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = surfaceContainerHighColor,
                    tonalElevation = 8.dp,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = primaryColor,
                            trackColor = primaryColor.copy(alpha = 0.2f),
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Launching game...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Please wait",
                            style = MaterialTheme.typography.bodySmall,
                            color = onSurfaceVariantColor,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=1280dp,height=800dp,orientation=landscape")
@Composable
fun BaseMainScreenPreview() {
    PojavTheme(dynamicColor = false) {
        BaseMainScreen(
            loadingVisible = true
        )
    }
}

@Preview(showBackground = true, device = "spec:width=1280dp,height=800dp,orientation=landscape")
@Composable
fun BaseMainScreenRailPreview() {
    PojavTheme(dynamicColor = false) {
        BaseMainScreen(
            drawerState = rememberDrawerState(initialValue = DrawerValue.Open),
            loadingVisible = false,
            drawerContent = { expanded ->
                NavigationRailItem(
                    selected = false,
                    onClick = {},
                    icon = { Icon(Icons.Default.Settings, null) },
                    label = { Text("Settings") },
                    alwaysShowLabel = expanded
                )
            }
        )
    }
}
