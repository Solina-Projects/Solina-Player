package app.solina.player.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import app.solina.player.data.preferences.ThemePreset
import app.solina.player.presentation.viewmodel.ColorSchemePair
import androidx.core.graphics.ColorUtils

val LocalSolinaPlayerDarkTheme = staticCompositionLocalOf { false }
val LocalSolinaPlayerAmoledTheme = staticCompositionLocalOf { false }
val LocalShowScrollbar = staticCompositionLocalOf { true }

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Suppress("DEPRECATION")
@Composable
fun SolinaPlayerStatusBarStyle(
    color: Color,
    useDarkIcons: Boolean = ColorUtils.calculateLuminance(color.toArgb()) > 0.55,
    navigationColor: Color? = null,
    useDarkNavigationIcons: Boolean = navigationColor
        ?.let { ColorUtils.calculateLuminance(it.toArgb()) > 0.55 }
        ?: useDarkIcons
) {
    val view = LocalView.current
    if (view.isInEditMode) return

    val updateNavigationBar = navigationColor != null
    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
        }

        WindowCompat.getInsetsController(window, view).run {
            isAppearanceLightStatusBars = useDarkIcons

            if (updateNavigationBar) {
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
                isAppearanceLightNavigationBars = useDarkNavigationIcons
            }
        }
    }
}

val DarkColorScheme = darkColorScheme(
    primary = SolinaPlayerPurplePrimary,
    secondary = SolinaPlayerPink,
    tertiary = SolinaPlayerOrange,
    background = SolinaPlayerPurpleDark,
    surface = SolinaPlayerSurface,
    onPrimary = SolinaPlayerWhite,
    onSecondary = SolinaPlayerWhite,
    onTertiary = SolinaPlayerWhite,
    onBackground = SolinaPlayerWhite,
    onSurface = SolinaPlayerLightPurple,
    error = Color(0xFFFF5252),
    onError = SolinaPlayerWhite
)

val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = SolinaPlayerWhite,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = SolinaPlayerPink,
    onSecondary = SolinaPlayerWhite,
    secondaryContainer = SolinaPlayerPink.copy(alpha = 0.15f),
    onSecondaryContainer = SolinaPlayerPink.copy(alpha = 0.85f),
    tertiary = SolinaPlayerOrange,
    onTertiary = SolinaPlayerBlack,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutline.copy(alpha = 0.6f),
    surfaceTint = LightPrimary,
    error = Color(0xFFD32F2F),
    onError = SolinaPlayerWhite
)

fun isAmoledThemePreset(preset: String): Boolean = preset == ThemePreset.AMOLED

/** Keep the source palette's accents and content colors, replacing only its surface hierarchy. */
fun ColorScheme.withAmoledSurfaces(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceBright = Color(0xFF242424),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF080808),
    surfaceContainer = Color(0xFF101010),
    surfaceContainerHigh = Color(0xFF181818),
    surfaceContainerHighest = Color(0xFF242424),
    surfaceVariant = Color(0xFF242424),
)

/**
 * AMOLED always uses a dark palette, even when the app is set to light mode. The accents come
 * from [colorSchemePairOverride] (the app-wide now-playing palette) when the user enabled it,
 * otherwise from the wallpaper/dynamic dark scheme.
 */
internal fun resolveAmoledColorScheme(
    colorSchemePairOverride: ColorSchemePair?,
    wallpaperDarkScheme: () -> ColorScheme,
): ColorScheme = (colorSchemePairOverride?.dark ?: wallpaperDarkScheme()).withAmoledSurfaces()

fun getAmoledColorScheme(
    context: Context,
    colorSchemePairOverride: ColorSchemePair? = null,
): ColorScheme = resolveAmoledColorScheme(colorSchemePairOverride) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        try {
            dynamicDarkColorScheme(context)
        } catch (e: Exception) {
            DarkColorScheme
        }
    } else {
        DarkColorScheme
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SolinaPlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorSchemePairOverride: ColorSchemePair? = null,
    themePreset: String = ThemePreset.DEFAULT,
    // Appearance customisation (bug-fix round 2026-11-17, item 6): the
    // font family chosen in Settings; defaults to the app's Rounded face.
    fontFamily: FontFamily = RoundedSans,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isAmoled = isAmoledThemePreset(themePreset)
    val finalColorScheme = when {
        isAmoled -> remember(context, colorSchemePairOverride) {
            getAmoledColorScheme(context, colorSchemePairOverride)
        }
        colorSchemePairOverride == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            try {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } catch (e: Exception) {
                if (darkTheme) DarkColorScheme else LightColorScheme
            }
        }
        colorSchemePairOverride != null -> {
            if (darkTheme) colorSchemePairOverride.dark else colorSchemePairOverride.light
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    SolinaPlayerStatusBarStyle(
        color = finalColorScheme.background,
        navigationColor = finalColorScheme.background
    )

    CompositionLocalProvider(
        LocalSolinaPlayerDarkTheme provides (darkTheme || isAmoled),
        LocalSolinaPlayerAmoledTheme provides isAmoled
    ) {
        MaterialTheme(
            colorScheme = finalColorScheme,
            motionScheme = MotionScheme.expressive(),
            typography = typographyFor(fontFamily),
            shapes = Shapes,
            content = content
        )
    }
}
