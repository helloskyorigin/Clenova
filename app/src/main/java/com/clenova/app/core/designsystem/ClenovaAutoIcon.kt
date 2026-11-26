package com.clenova.app.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp

/** Two coordinated paths, representing automatic workflows rather than a speedometer. */
@Composable
fun ClenovaAutoIcon(tint: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        scale(this.size.width / 28f, this.size.height / 28f, pivot = androidx.compose.ui.geometry.Offset.Zero) {
            val path = Path().apply {
                moveTo(2f, 5f); lineTo(8f, 5f); quadraticTo(14f, 5f, 14f, 11f)
                lineTo(14f, 17f); quadraticTo(14f, 23f, 20f, 23f); lineTo(26f, 23f)
                moveTo(21f, 18f); lineTo(26f, 23f); lineTo(21f, 28f)
                moveTo(2f, 23f); lineTo(8f, 23f); quadraticTo(10f, 23f, 11f, 21f)
                moveTo(20f, 5f); lineTo(26f, 5f)
                moveTo(21f, 0f); lineTo(26f, 5f); lineTo(21f, 10f)
            }
            // Keep arrow tips within the icon's drawing bounds.
            scale(.85f, .85f, pivot = androidx.compose.ui.geometry.Offset(14f, 14f)) {
                drawPath(path, tint, style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}
