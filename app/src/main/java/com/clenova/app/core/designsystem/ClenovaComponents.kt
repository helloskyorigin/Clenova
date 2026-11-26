package com.clenova.app.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Clean bordered surface container following Calm Intelligence spacing and radii.
 */
@Composable
fun ClenovaSurfaceCard(
    modifier: Modifier = Modifier,
    containerColor: Color = ClenovaThemeTokens.colors.surface,
    borderColor: Color = ClenovaThemeTokens.colors.border,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = ClenovaShapes.card
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(shape)
                        .minimumInteractiveComponentSize()
                        .clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = ClenovaElevation.none,
    ) {
        Column(
            modifier = Modifier.padding(ClenovaSpacing.lg),
            content = content,
        )
    }
}

/**
 * Minimal status pill badge with optional leading status dot.
 */
@Composable
fun ClenovaStatusBadge(
    label: String,
    modifier: Modifier = Modifier,
    containerColor: Color = ClenovaThemeTokens.colors.softBlue,
    contentColor: Color = ClenovaThemeTokens.colors.primary,
    dotColor: Color? = null,
) {
    Row(
        modifier = modifier
            .clip(ClenovaShapes.pill)
            .background(containerColor)
            .padding(horizontal = ClenovaSpacing.sm, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (dotColor != null) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Distinctive orbital intelligence icon rendered in Compose Canvas for crisp scaling.
 */
@Composable
fun ClenovaOrbitalIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 24.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.minDimension * 0.095f
        val outerRadius = this.size.minDimension * 0.40f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)

        // Outer orbital path
        drawCircle(
            color = tint.copy(alpha = 0.35f),
            radius = outerRadius,
            center = center,
            style = Stroke(width = strokeWidth),
        )

        // Primary precision orbital arc
        drawArc(
            color = tint,
            startAngle = -55f,
            sweepAngle = 225f,
            useCenter = false,
            topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
            size = Size(outerRadius * 2f, outerRadius * 2f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )

        // Orbiting satellite node on top-right
        val angleRad = Math.toRadians(-40.0)
        val nodeOffset = Offset(
            x = center.x + (outerRadius * kotlin.math.cos(angleRad)).toFloat(),
            y = center.y + (outerRadius * kotlin.math.sin(angleRad)).toFloat(),
        )
        drawCircle(
            color = tint,
            radius = strokeWidth * 1.15f,
            center = nodeOffset,
        )

        // Inner intelligence core
        drawCircle(
            color = tint,
            radius = this.size.minDimension * 0.145f,
            center = center,
        )
    }
}

@Composable
fun ClenovaSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = ClenovaThemeTokens.colors.textSecondary,
        modifier = modifier,
    )
}

@Composable
fun ClenovaScreenHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailingAction: (@Composable RowScope.() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = ClenovaThemeTokens.colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            if (trailingAction != null) {
                Spacer(modifier = Modifier.width(ClenovaSpacing.sm))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    content = trailingAction,
                )
            }
        }
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(ClenovaSpacing.xs))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = ClenovaThemeTokens.colors.textSecondary,
            )
        }
    }
}

@Composable
fun ClenovaInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = ClenovaSpacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = ClenovaThemeTokens.colors.textSecondary,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(ClenovaSpacing.md))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = ClenovaThemeTokens.colors.textPrimary,
            )
        }
        if (showDivider) {
            HorizontalDivider(
                thickness = 1.dp,
                color = ClenovaThemeTokens.colors.border,
            )
        }
    }
}
