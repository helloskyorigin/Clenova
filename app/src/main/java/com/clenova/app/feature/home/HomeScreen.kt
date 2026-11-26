package com.clenova.app.feature.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clenova.app.core.designsystem.ClenovaMotion
import com.clenova.app.core.designsystem.ClenovaOrbitalIcon
import com.clenova.app.core.designsystem.ClenovaSectionLabel
import com.clenova.app.core.designsystem.ClenovaShapes
import com.clenova.app.core.designsystem.ClenovaSpacing
import com.clenova.app.core.designsystem.ClenovaStatusBadge
import com.clenova.app.core.designsystem.ClenovaSurfaceCard
import com.clenova.app.core.designsystem.ClenovaThemeTokens
import com.clenova.app.core.utilities.ByteFormatter
import com.clenova.app.domain.model.StorageState
import com.example.R

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onRefreshStorage: () -> Unit,
    onOpenAgent: () -> Unit,
    onOpenSettings: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(ClenovaSpacing.lg),
        ) {
            // Minimal Brand Header + Settings Entry
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.app_tagline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.border),
                ) {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("settings_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.cd_settings_button),
                            tint = colors.textPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }

            // Real Device Storage Card
            ClenovaSurfaceCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    ClenovaSectionLabel(
                        text = stringResource(R.string.home_storage_section_label),
                    )
                    IconButton(
                        onClick = onRefreshStorage,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("refresh_storage_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = stringResource(R.string.cd_refresh_storage),
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(ClenovaSpacing.xs))

                when (val storage = uiState.storageState) {
                    StorageState.Loading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(ClenovaSpacing.sm),
                            modifier = Modifier.padding(vertical = ClenovaSpacing.sm),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = colors.primary,
                            )
                            Text(
                                text = stringResource(R.string.home_storage_loading),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary,
                            )
                        }
                    }

                    is StorageState.Ready -> {
                        val overview = storage.overview
                        val animatedFraction by animateFloatAsState(
                            targetValue = overview.usedFraction,
                            animationSpec = ClenovaMotion.gentleSpring(),
                            label = "storage_used_fraction",
                        )

                        Text(
                            text = stringResource(
                                R.string.home_storage_available_value,
                                ByteFormatter.format(overview.availableBytes),
                            ),
                            style = MaterialTheme.typography.displaySmall,
                            color = colors.textPrimary,
                        )
                        Spacer(modifier = Modifier.height(ClenovaSpacing.xxs))
                        Text(
                            text = stringResource(
                                R.string.home_storage_used_of_total,
                                ByteFormatter.format(overview.usedBytes),
                                ByteFormatter.format(overview.totalBytes),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                        )

                        Spacer(modifier = Modifier.height(ClenovaSpacing.md))

                        // Minimal Storage Progress Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(ClenovaShapes.pill)
                                .background(colors.border),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = animatedFraction.coerceAtLeast(0.02f))
                                    .height(8.dp)
                                    .clip(ClenovaShapes.pill)
                                    .background(colors.primary),
                            )
                        }
                    }

                    StorageState.Unavailable -> {
                        Text(
                            text = stringResource(R.string.home_storage_unavailable_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                        )
                        Spacer(modifier = Modifier.height(ClenovaSpacing.xxs))
                        Text(
                            text = stringResource(R.string.home_storage_unavailable_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                        )
                    }
                }
            }

            // Honest Autopilot Agent Status Card (Not configured / inactive)
            ClenovaSurfaceCard(
                containerColor = colors.surfaceElevated,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    ClenovaSectionLabel(
                        text = stringResource(R.string.home_agent_section_label),
                    )
                    ClenovaStatusBadge(
                        label = stringResource(R.string.home_status_not_configured),
                        containerColor = colors.surface,
                        contentColor = colors.textSecondary,
                        dotColor = colors.textSecondary,
                    )
                }

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
                        ClenovaOrbitalIcon(
                            tint = colors.primary,
                            size = 24.dp,
                        )
                    }
                    Text(
                        text = stringResource(R.string.home_agent_inactive_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary,
                    )
                }

                Spacer(modifier = Modifier.height(ClenovaSpacing.sm))

                Text(
                    text = stringResource(R.string.home_agent_inactive_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )

                Spacer(modifier = Modifier.height(ClenovaSpacing.lg))

                Button(
                    onClick = onOpenAgent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("open_agent_workspace_button"),
                    shape = ClenovaShapes.pill,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary,
                    ),
                    contentPadding = PaddingValues(horizontal = ClenovaSpacing.lg),
                ) {
                    Text(
                        text = stringResource(R.string.home_agent_open_workspace),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(modifier = Modifier.width(ClenovaSpacing.xs))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Text(
                text = stringResource(R.string.home_foundation_note),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = ClenovaSpacing.xxs),
            )
        }
    }
}
