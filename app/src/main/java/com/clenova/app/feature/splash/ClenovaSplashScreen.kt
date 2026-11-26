package com.clenova.app.feature.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clenova.app.core.designsystem.ManropeFontFamily
import com.example.R
import kotlin.math.PI
import kotlin.math.sin

internal const val SplashMotionMillis = 1150

/**
 * Native layout, original brand asset and lightweight vector motion.
 * No storage scan or network request is implied by the animated dots.
 */
@Composable
fun ClenovaSplashScreen() {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(SplashMotionMillis))
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().testTag("clenova_splash"),
    ) {
        val logoSize = minOf(maxWidth * 0.54f, maxHeight * 0.31f, 240.dp)
        val compact = maxHeight < 480.dp
        val logoCenter = if (compact) 0.30f else 0.385f
        val brandTop = if (compact) 0.53f else 0.59f

        SplashWaves(progress = { progress.value })

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = maxHeight * logoCenter - logoSize / 2)
                .size(logoSize)
                .graphicsLayer {
                    val entrance = FastOutSlowInEasing.transform((progress.value * 2.5f).coerceIn(0f, 1f))
                    alpha = entrance
                    scaleX = 0.88f + entrance * 0.12f
                    scaleY = scaleX
                    translationY = (1f - entrance) * 18.dp.toPx()
                },
        ) {
            val shape = RoundedCornerShape(27)
            Image(
                painter = painterResource(R.drawable.clenova_brand_logo),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
                    .shadow(18.dp, shape, ambientColor = Color(0xFF009BEB), spotColor = Color(0xFF0065DC))
                    .clip(shape),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = maxHeight * brandTop)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .graphicsLayer {
                    val entrance = FastOutSlowInEasing.transform(((progress.value - 0.12f) * 2.6f).coerceIn(0f, 1f))
                    alpha = entrance
                    translationY = (1f - entrance) * 12.dp.toPx()
                },
        ) {
            Text(
                text = stringResource(R.string.app_name),
                color = Color(0xFF123457),
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = if (maxWidth < 350.dp) 34.sp else 42.sp,
                lineHeight = 48.sp,
                letterSpacing = (-1.2).sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.app_tagline),
                color = Color(0xFF294A70),
                fontFamily = ManropeFontFamily,
                fontSize = if (maxWidth < 350.dp) 15.sp else 17.sp,
                textAlign = TextAlign.Center,
            )
        }

        Row(
            modifier = Modifier.align(Alignment.TopCenter).offset(y = maxHeight * 0.81f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 5.dp)
                        .size(9.dp)
                        .graphicsLayer {
                            val phase = (progress.value * 2.2f - index / 3f) * 2f * PI.toFloat()
                            val pulse = ((sin(phase) + 1f) / 2f).coerceIn(0f, 1f)
                            alpha = 0.24f + pulse * 0.76f
                            scaleX = 0.85f + pulse * 0.30f
                            scaleY = scaleX
                        }
                        .background(Color(0xFF0097F4), CircleShape),
                )
            }
        }
    }
}

@Composable
private fun SplashWaves(progress: () -> Float) {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val drift = sin(progress() * PI.toFloat()) * h * 0.012f
        drawRect(
            Brush.verticalGradient(listOf(Color(0xFFE6F2FF), Color(0xFFFAFCFF), Color(0xFFEAF5FF))),
        )
        drawRect(
            Brush.radialGradient(
                colors = listOf(Color.White, Color.White.copy(alpha = 0f)),
                center = Offset(w * 0.50f, h * 0.49f),
                radius = h * 0.52f,
            ),
        )
        fun topWave(edge: Float, depth: Float): Path = Path().apply {
            moveTo(0f, 0f)
            lineTo(w * edge, 0f)
            cubicTo(w * 0.33f, h * 0.16f + drift, w * 0.11f, h * 0.07f, 0f, h * depth + drift)
            close()
        }
        drawPath(topWave(0.78f, 0.26f), Brush.linearGradient(
            listOf(Color(0xFFBCECFB), Color(0xFFA8D9FB).copy(alpha = 0.15f)),
            start = Offset.Zero, end = Offset(w, h * 0.25f),
        ))
        drawPath(topWave(0.50f, 0.13f), Brush.linearGradient(
            listOf(Color(0xFFDEF6FF), Color(0xFF7AC6FA).copy(alpha = 0.65f)),
            start = Offset.Zero, end = Offset(w * 0.50f, h * 0.18f),
        ))
        val bottom = Path().apply {
            moveTo(w, h * 0.73f - drift)
            cubicTo(w * 0.77f, h * 0.91f, w * 0.45f, h * 0.86f, w * 0.08f, h)
            lineTo(w, h)
            close()
        }
        drawPath(bottom, Brush.linearGradient(
            listOf(Color(0xFFDDF5FF), Color(0xFF85C8FB).copy(alpha = 0.65f)),
            start = Offset(w * 0.15f, h * 0.81f), end = Offset(w, h),
        ))
        val lower = Path().apply {
            moveTo(w, h * 0.85f - drift)
            cubicTo(w * 0.64f, h * 0.86f, w * 0.70f, h * 0.98f, w * 0.30f, h)
            lineTo(w, h)
            close()
        }
        drawPath(lower, Brush.linearGradient(
            listOf(Color(0xFF42B9F5).copy(alpha = 0.45f), Color(0xFFD4F4FF)),
            start = Offset(w, h * 0.8f), end = Offset(w * 0.3f, h),
        ))
    }
}
