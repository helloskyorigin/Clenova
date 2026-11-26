package com.clenova.app.core.designsystem

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object ClenovaSpacing {
    val xxs: Dp = 4.dp
    val xs: Dp = 8.dp
    val sm: Dp = 12.dp
    val md: Dp = 16.dp
    val lg: Dp = 24.dp
    val xl: Dp = 32.dp
    val xxl: Dp = 48.dp
    val screenHorizontal: Dp = 20.dp
    val floatingNavBottomClearance: Dp = 112.dp
}

object ClenovaShapes {
    val tile = RoundedCornerShape(16.dp)
    val card = RoundedCornerShape(22.dp)
    val sheet = RoundedCornerShape(28.dp)
    val badge = RoundedCornerShape(10.dp)
    val pill = RoundedCornerShape(999.dp)

    val materialShapes = Shapes(
        extraSmall = badge,
        small = tile,
        medium = card,
        large = sheet,
        extraLarge = pill,
    )
}

object ClenovaElevation {
    val none: Dp = 0.dp
    val subtle: Dp = 2.dp
    val floatingNav: Dp = 10.dp
}

object ClenovaMotion {
    const val quickFadeMs = 200
    const val navTransitionMs = 240

    fun <T> gentleSpring() = spring<T>(
        dampingRatio = 0.80f,
        stiffness = Spring.StiffnessMediumLow,
    )

    fun <T> snappySpring() = spring<T>(
        dampingRatio = 0.78f,
        stiffness = 460f,
    )

    fun <T> quickTween() = tween<T>(durationMillis = quickFadeMs)
}
