package com.clenova.app.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Calm Intelligence color palette tokens for CLENOVA.
 * Light mode is the default; dark mode tokens are structured for parity.
 */
object ClenovaPalette {
    val ElectricBlue = Color(0xFF0057FF)
    val ElectricBlueDark = Color(0xFF4D8BFF)
    val BackgroundWhite = Color(0xFFFFFFFF)
    val PrimaryText = Color(0xFF101318)
    val SecondaryText = Color(0xFF747C89)
    val SurfaceLight = Color(0xFFF4F6FA)
    val SoftBlueLight = Color(0xFFEAF2FF)
    val SuccessLight = Color(0xFF15803D)
    val SuccessSoftLight = Color(0xFFECFDF3)
    val BorderLight = Color(0xFFE3E8EF)

    // Dark theme counterparts
    val BackgroundDark = Color(0xFF0B0E14)
    val SurfaceDark = Color(0xFF131822)
    val SurfaceElevatedDark = Color(0xFF1A2130)
    val PrimaryTextDark = Color(0xFFF4F6FA)
    val SecondaryTextDark = Color(0xFF94A3B8)
    val SoftBlueDark = Color(0xFF152544)
    val SuccessDark = Color(0xFF22C55E)
    val SuccessSoftDark = Color(0xFF0F291A)
    val BorderDark = Color(0xFF232B3A)
    val ErrorLight = Color(0xFFDC2626)
    val ErrorDark = Color(0xFFF87171)
}

@Immutable
data class ClenovaColors(
    val primary: Color,
    val onPrimary: Color,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val softBlue: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val border: Color,
    val success: Color,
    val successSoft: Color,
    val error: Color,
    val navPillBackground: Color,
    val isDark: Boolean,
)

val LightClenovaColors = ClenovaColors(
    primary = ClenovaPalette.ElectricBlue,
    onPrimary = Color.White,
    background = ClenovaPalette.BackgroundWhite,
    surface = ClenovaPalette.SurfaceLight,
    surfaceElevated = ClenovaPalette.BackgroundWhite,
    softBlue = ClenovaPalette.SoftBlueLight,
    textPrimary = ClenovaPalette.PrimaryText,
    textSecondary = ClenovaPalette.SecondaryText,
    border = ClenovaPalette.BorderLight,
    success = ClenovaPalette.SuccessLight,
    successSoft = ClenovaPalette.SuccessSoftLight,
    error = ClenovaPalette.ErrorLight,
    navPillBackground = ClenovaPalette.BackgroundWhite,
    isDark = false,
)

val DarkClenovaColors = ClenovaColors(
    primary = ClenovaPalette.ElectricBlueDark,
    onPrimary = Color.White,
    background = ClenovaPalette.BackgroundDark,
    surface = ClenovaPalette.SurfaceDark,
    surfaceElevated = ClenovaPalette.SurfaceElevatedDark,
    softBlue = ClenovaPalette.SoftBlueDark,
    textPrimary = ClenovaPalette.PrimaryTextDark,
    textSecondary = ClenovaPalette.SecondaryTextDark,
    border = ClenovaPalette.BorderDark,
    success = ClenovaPalette.SuccessDark,
    successSoft = ClenovaPalette.SuccessSoftDark,
    error = ClenovaPalette.ErrorDark,
    navPillBackground = ClenovaPalette.SurfaceElevatedDark,
    isDark = true,
)

val LocalClenovaColors = staticCompositionLocalOf { LightClenovaColors }
