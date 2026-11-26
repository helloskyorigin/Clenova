package com.clenova.app.feature.guard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.clenova.app.core.designsystem.ClenovaScreenHeader
import com.clenova.app.core.designsystem.ClenovaShapes
import com.clenova.app.core.designsystem.ClenovaSpacing
import com.clenova.app.core.designsystem.ClenovaStatusBadge
import com.clenova.app.core.designsystem.ClenovaSurfaceCard
import com.clenova.app.core.designsystem.ClenovaThemeTokens
import com.example.R

private data class GuardPillar(
    val icon: ImageVector,
    val titleRes: Int,
    val descriptionRes: Int,
)

private val guardPillars = listOf(
    GuardPillar(Icons.Outlined.PhotoLibrary, R.string.guard_pillar_exclusions_title, R.string.guard_pillar_exclusions_desc),
    GuardPillar(Icons.Outlined.Lock, R.string.guard_pillar_uncertain_title, R.string.guard_pillar_uncertain_desc),
    GuardPillar(Icons.Outlined.VerifiedUser, R.string.guard_pillar_cross_tool_title, R.string.guard_pillar_cross_tool_desc),
)

@Composable
fun GuardScreen(
    modifier: Modifier = Modifier,
) {
    val colors = ClenovaThemeTokens.colors
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.statusBars),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    start = ClenovaSpacing.screenHorizontal,
                    end = ClenovaSpacing.screenHorizontal,
                    top = ClenovaSpacing.md,
                    bottom = ClenovaSpacing.floatingNavBottomClearance,
                ),
            verticalArrangement = Arrangement.spacedBy(ClenovaSpacing.md),
        ) {
            ClenovaScreenHeader(
                title = stringResource(R.string.guard_title),
                subtitle = stringResource(R.string.guard_subtitle),
            )

            // Honest empty / unconfigured protection state
            ClenovaSurfaceCard(
                containerColor = colors.surfaceElevated,
            ) {
                ClenovaStatusBadge(
                    label = stringResource(R.string.guard_status_unconfigured),
                    containerColor = colors.surface,
                    contentColor = colors.textSecondary,
                    dotColor = colors.textSecondary,
                )
                Spacer(modifier = Modifier.height(ClenovaSpacing.md))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ClenovaSpacing.md),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(colors.softBlue),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Text(
                        text = stringResource(R.string.guard_empty_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary,
                    )
                }
                Spacer(modifier = Modifier.height(ClenovaSpacing.sm))
                Text(
                    text = stringResource(R.string.guard_empty_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }

            guardPillars.forEach { pillar ->
                ClenovaSurfaceCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(ClenovaSpacing.md),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(ClenovaShapes.tile)
                                .background(colors.background),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = pillar.icon,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(pillar.titleRes),
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary,
                            )
                            Spacer(modifier = Modifier.height(ClenovaSpacing.xxs))
                            Text(
                                text = stringResource(pillar.descriptionRes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}
