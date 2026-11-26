package com.clenova.app.feature.clean

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BurstMode
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Screenshot
import androidx.compose.material.icons.outlined.Swipe
import androidx.compose.material.icons.outlined.VideoSettings
import androidx.compose.material3.Button
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

private data class CleanToolStub(
    val icon: ImageVector,
    val titleRes: Int,
    val descriptionRes: Int,
)

private val plannedCleanTools = listOf(
    CleanToolStub(Icons.Outlined.ContentCopy, R.string.clean_tool_duplicates_title, R.string.clean_tool_duplicates_desc),
    CleanToolStub(Icons.Outlined.BurstMode, R.string.clean_tool_similar_title, R.string.clean_tool_similar_desc),
    CleanToolStub(Icons.Outlined.Screenshot, R.string.clean_tool_screenshots_title, R.string.clean_tool_screenshots_desc),
    CleanToolStub(Icons.Outlined.FolderZip, R.string.clean_tool_large_files_title, R.string.clean_tool_large_files_desc),
    CleanToolStub(Icons.Outlined.Swipe, R.string.clean_tool_swipe_clean_title, R.string.clean_tool_swipe_clean_desc),
    CleanToolStub(Icons.Outlined.VideoSettings, R.string.clean_tool_video_compress_title, R.string.clean_tool_video_compress_desc),
)

@Composable
fun CleanScreen(
    modifier: Modifier = Modifier,
    onSmartScan: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(ClenovaSpacing.sm),
        ) {
            ClenovaScreenHeader(
                title = stringResource(R.string.clean_title),
                subtitle = stringResource(R.string.clean_subtitle),
            )

            Spacer(modifier = Modifier.height(ClenovaSpacing.xs))

            Button(onClick = onSmartScan, modifier = Modifier.fillMaxWidth(), shape = ClenovaShapes.pill) {
                Text(stringResource(R.string.home_smart_scan))
            }

            plannedCleanTools.forEach { tool ->
                ClenovaSurfaceCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(ClenovaSpacing.md),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(ClenovaShapes.tile)
                                .background(colors.softBlue),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = tool.icon,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(22.dp),
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = stringResource(tool.titleRes),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.textPrimary,
                                )
                                ClenovaStatusBadge(
                                    label = stringResource(R.string.clean_badge_planned),
                                    containerColor = colors.background,
                                    contentColor = colors.textSecondary,
                                )
                            }
                            Spacer(modifier = Modifier.height(ClenovaSpacing.xxs))
                            Text(
                                text = stringResource(tool.descriptionRes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(ClenovaSpacing.xs))

            Text(
                text = stringResource(R.string.clean_footer_honesty),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = ClenovaSpacing.xxs),
            )
        }
    }
}
