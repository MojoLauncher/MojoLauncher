package net.kdt.pojavlaunch.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// Colors updated to match colors.xml exactly
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF000000),
    secondary = Color(0xFFB8B8B8),
    background = Color(0xFF0F0F0F),
    surface = Color(0xFF1A1A1A),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF2B2B2B),
    onSurfaceVariant = Color(0xFFB8B8B8),
    errorContainer = Color(0xFF410002),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF000000),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF666666),
    background = Color(0xFFF5F5F5), // Matches @color/background_app
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFEEEEEE),
    onSurfaceVariant = Color(0xFF444444)
)

// Fix: Use RoundedCornerShape instead of CircleShape to avoid circular lists/dialogs
private val PojavShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun PojavTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = PojavShapes,
        content = content
    )
}
