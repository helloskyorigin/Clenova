package com.clenova.app.feature.clean

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ChevronRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clenova.app.core.designsystem.ClenovaShapes
import com.clenova.app.core.designsystem.ClenovaSpacing
import com.clenova.app.core.designsystem.ClenovaThemeTokens
import com.clenova.app.feature.agent.CleanupDesignPlan
import com.example.BuildConfig
import com.clenova.app.data.storage.MediaScanResult
import com.clenova.app.core.utilities.ByteFormatter
import com.example.R

@Composable
internal fun CleanScreen(
    modifier: Modifier = Modifier,
    sample: Boolean = false,
    result: MediaScanResult? = null,
    onSmartScan: () -> Unit,
    onDuplicates: () -> Unit,
    onCompress: () -> Unit,
) {
    val colors = ClenovaThemeTokens.colors
    val preview = BuildConfig.DEBUG && sample
    var dialog by rememberSaveable { mutableStateOf<Int?>(null) }
    Box(modifier.fillMaxSize().background(colors.background).windowInsetsPadding(WindowInsets.statusBars),
        contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 560.dp).fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = ClenovaSpacing.floatingNavBottomClearance),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.clean_title), Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 32.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-.8).sp),
                    color = colors.textPrimary)
                IconButton(onClick = { dialog = R.string.clean_history }, modifier = Modifier.testTag("clean_history")) {
                    Icon(Icons.Outlined.History, stringResource(R.string.clean_history), tint = colors.textPrimary)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(result?.let { ByteFormatter.format(it.reviewBytes) } ?: if (preview) CleanupDesignPlan.spaceLabel(CleanupDesignPlan.ALL) else "—",
                        modifier = Modifier.testTag("clean_recoverable"),
                        style = MaterialTheme.typography.displaySmall.copy(fontSize = 39.sp, lineHeight = 45.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1.4).sp),
                        color = colors.textPrimary)
                    Text(stringResource(if (result != null) R.string.scan_review_space else R.string.review_estimated_space), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    if (result != null && (!result.access.full || result.incomplete)) Text(stringResource(R.string.scan_partial), style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                    if (preview) Text(stringResource(R.string.clean_preview), style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                }
                Button(onClick = onSmartScan, shape = ClenovaShapes.pill,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("clean_review_all"),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)) {
                    Text(stringResource(if (preview) R.string.clean_review_all else R.string.home_smart_scan))
                }
            }
            HorizontalDivider(color = colors.border)
            PhotoStrip(
                title = R.string.clean_tool_duplicates_title,
                count = result?.let { stringResource(R.string.guard_item_count, it.extras.size) } ?: if (preview) stringResource(R.string.guard_item_count, DuplicateDesignData.removableIds.size) else null,
                preview = preview, similar = false, tag = "clean_duplicates", onClick = onDuplicates,
            )
            if (result != null) Text(ByteFormatter.format(result.duplicateBytes), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            if (preview) Text(CleanupDesignPlan.spaceLabel(1) + " · " + stringResource(R.string.review_duplicates_detail),
                style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            PhotoStrip(
                title = R.string.clean_tool_similar_title,
                count = if (preview) stringResource(R.string.scan_similar_count) else null,
                preview = preview, similar = true, tag = "clean_similar",
                onClick = { dialog = R.string.clean_tool_similar_title },
            )
            HorizontalDivider(color = colors.border)
            Column {
                CleanFileRow(Icons.Outlined.Screenshot, R.string.clean_tool_screenshots_title,
                    result?.let { stringResource(R.string.guard_item_count, it.screenshots.size) } ?: if (preview) stringResource(R.string.guard_item_count, 84) else null, "clean_screenshots") { dialog = R.string.clean_tool_screenshots_title }
                HorizontalDivider(color = colors.border)
                CleanFileRow(Icons.Outlined.VideoFile, R.string.review_videos,
                    result?.let { ByteFormatter.format(it.largeVideos.sumOf { video -> video.bytes }) } ?: if (preview) CleanupDesignPlan.spaceLabel(2) else null, "clean_large_videos") { dialog = R.string.review_videos }
                HorizontalDivider(color = colors.border)
                CleanFileRow(Icons.Outlined.FolderOpen, R.string.clean_tool_large_files_title,
                    if (preview) stringResource(R.string.guard_item_count, 8) else null, "clean_large_files") { dialog = R.string.clean_tool_large_files_title }
                HorizontalDivider(color = colors.border)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CleanQuickTool(Icons.Outlined.Swipe, R.string.clean_tool_swipe_clean_title, "clean_swipe", Modifier.weight(1f)) { dialog = R.string.clean_tool_swipe_clean_title }
                CleanQuickTool(Icons.Outlined.VideoSettings, R.string.clean_tool_video_compress_title, "clean_compress", Modifier.weight(1f), onCompress)
            }
        }
    }
    dialog?.let { title ->
        AlertDialog(onDismissRequest = { dialog = null },
            title = { Text(stringResource(title)) },
            text = { Text(stringResource(if (title == R.string.clean_history) R.string.clean_history_empty else R.string.home_tool_unavailable)) },
            confirmButton = { TextButton(onClick = { dialog = null }, modifier = Modifier.testTag("clean_dialog_done")) { Text(stringResource(R.string.review_done)) } })
    }
}

@Composable
private fun PhotoStrip(title: Int, count: String?, preview: Boolean, similar: Boolean, tag: String, onClick: () -> Unit) {
    val colors = ClenovaThemeTokens.colors
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).testTag(tag)
        .clickable(role = Role.Button, onClick = onClick), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 38.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(title), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
            if (count != null) Text(count, style = MaterialTheme.typography.bodySmall, color = colors.primary)
            Icon(Icons.AutoMirrored.Outlined.ChevronRight, null, Modifier.size(18.dp), tint = colors.primary)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(3) { index ->
                Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(8.dp)).background(colors.surface),
                    contentAlignment = Alignment.Center) {
                    if (preview) DuplicateArtwork(if (similar) 1 else 0, Modifier.fillMaxSize(), variant = if (similar) index else 0)
                    else Icon(Icons.Outlined.Image, null, Modifier.size(25.dp), tint = colors.textSecondary.copy(alpha = .5f))
                }
            }
        }
    }
}

@Composable
private fun CleanFileRow(icon: ImageVector, title: Int, value: String?, tag: String, onClick: () -> Unit) {
    val colors = ClenovaThemeTokens.colors
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(8.dp)).testTag(tag)
        .clickable(role = Role.Button, onClick = onClick), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, Modifier.size(24.dp), tint = colors.textPrimary)
        Text(stringResource(title), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
        if (value != null) Text(value, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        Icon(Icons.AutoMirrored.Outlined.ChevronRight, null, Modifier.size(18.dp), tint = colors.textSecondary)
    }
}

@Composable
private fun CleanQuickTool(icon: ImageVector, title: Int, tag: String, modifier: Modifier, onClick: () -> Unit) {
    val colors = ClenovaThemeTokens.colors
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(colors.surface).testTag(tag)
        .clickable(role = Role.Button, onClick = onClick).padding(15.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, Modifier.size(26.dp), tint = colors.primary)
        Text(stringResource(title), style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
    }
}
