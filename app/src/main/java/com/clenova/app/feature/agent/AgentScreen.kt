package com.clenova.app.feature.agent

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
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Tune
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
import com.clenova.app.core.designsystem.ClenovaOrbitalIcon
import com.clenova.app.core.designsystem.ClenovaScreenHeader
import com.clenova.app.core.designsystem.ClenovaShapes
import com.clenova.app.core.designsystem.ClenovaSpacing
import com.clenova.app.core.designsystem.ClenovaStatusBadge
import com.clenova.app.core.designsystem.ClenovaSurfaceCard
import com.clenova.app.core.designsystem.ClenovaThemeTokens
import com.example.R

private data class AgentPillar(
    val icon: ImageVector,
    val titleRes: Int,
    val descriptionRes: Int,
)

private val agentPillars = listOf(
    AgentPillar(Icons.Outlined.Tune, R.string.agent_pillar_goals_title, R.string.agent_pillar_goals_desc),
    AgentPillar(Icons.Outlined.FactCheck, R.string.agent_pillar_planning_title, R.string.agent_pillar_planning_desc),
    AgentPillar(Icons.Outlined.History, R.string.agent_pillar_history_title, R.string.agent_pillar_history_desc),
)

/**
 * Dedicated Autopilot configuration and management workspace.
 * Explicitly not a chatbot or generic scanning screen.
 */
@Composable
fun AgentScreen(
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
                title = stringResource(R.string.agent_title),
                subtitle = stringResource(R.string.agent_subtitle),
            )

            ClenovaSurfaceCard(
                containerColor = colors.surfaceElevated,
            ) {
                ClenovaStatusBadge(
                    label = stringResource(R.string.agent_status_inactive),
                    containerColor = colors.softBlue,
                    contentColor = colors.primary,
                    dotColor = colors.textSecondary,
                )

                Spacer(modifier = Modifier.height(ClenovaSpacing.md))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ClenovaSpacing.md),
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(colors.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        ClenovaOrbitalIcon(
                            tint = colors.onPrimary,
                            size = 26.dp,
                        )
                    }
                    Text(
                        text = stringResource(R.string.agent_workspace_banner_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary,
                    )
                }

                Spacer(modifier = Modifier.height(ClenovaSpacing.sm))

                Text(
                    text = stringResource(R.string.agent_workspace_banner_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }

            agentPillars.forEach { pillar ->
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
