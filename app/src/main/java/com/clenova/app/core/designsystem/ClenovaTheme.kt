package com.clenova.app.core.designsystem

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.clenova.app.domain.model.ThemePreference

private val ClenovaLightColorScheme = lightColorScheme(
    primary = ClenovaPalette.ElectricBlue,
    onPrimary = ClenovaPalette.BackgroundWhite,
    primaryContainer = ClenovaPalette.SoftBlueLight,
    onPrimaryContainer = ClenovaPalette.ElectricBlue,
    secondary = ClenovaPalette.SecondaryText,
    onSecondary = ClenovaPalette.BackgroundWhite,
    secondaryContainer = ClenovaPalette.SurfaceLight,
    onSecondaryContainer = ClenovaPalette.PrimaryText,
    background = ClenovaPalette.BackgroundWhite,
    onBackground = ClenovaPalette.PrimaryText,
    surface = ClenovaPalette.SurfaceLight,
    onSurface = ClenovaPalette.PrimaryText,
    surfaceVariant = ClenovaPalette.SurfaceLight,
    onSurfaceVariant = ClenovaPalette.SecondaryText,
    outline = ClenovaPalette.BorderLight,
    outlineVariant = ClenovaPalette.BorderLight,
    error = ClenovaPalette.ErrorLight,
)

private val ClenovaDarkColorScheme = darkColorScheme(
    primary = ClenovaPalette.ElectricBlueDark,
    onPrimary = ClenovaPalette.BackgroundWhite,
    primaryContainer = ClenovaPalette.SoftBlueDark,
    onPrimaryContainer = ClenovaPalette.ElectricBlueDark,
    secondary = ClenovaPalette.SecondaryTextDark,
    onSecondary = ClenovaPalette.BackgroundDark,
    secondaryContainer = ClenovaPalette.SurfaceDark,
    onSecondaryContainer = ClenovaPalette.PrimaryTextDark,
    background = ClenovaPalette.BackgroundDark,
    onBackground = ClenovaPalette.PrimaryTextDark,
    surface = ClenovaPalette.SurfaceDark,
    onSurface = ClenovaPalette.PrimaryTextDark,
    surfaceVariant = ClenovaPalette.SurfaceElevatedDark,
    onSurfaceVariant = ClenovaPalette.SecondaryTextDark,
    outline = ClenovaPalette.BorderDark,
    outlineVariant = ClenovaPalette.BorderDark,
    error = ClenovaPalette.ErrorDark,
)

object ClenovaThemeTokens {
    val colors: ClenovaColors
        @Composable
        @ReadOnlyComposable
        get() = LocalClenovaColors.current
}

@Composable
fun ClenovaTheme(
    themePreference: ThemePreference = ThemePreference.LIGHT,
    content: @Composable () -> Unit,
) {
    val useDarkTheme = when (themePreference) {
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
    }

    val clenovaColors = if (useDarkTheme) DarkClenovaColors else LightClenovaColors
    val materialScheme = if (useDarkTheme) ClenovaDarkColorScheme else ClenovaLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !useDarkTheme
            insetsController.isAppearanceLightNavigationBars = !useDarkTheme
        }
    }

    CompositionLocalProvider(LocalClenovaColors provides clenovaColors) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = ClenovaTypography,
            shapes = ClenovaShapes.materialShapes,
            content = content,
        )
    }
}
