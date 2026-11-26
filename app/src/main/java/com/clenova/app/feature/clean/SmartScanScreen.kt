package com.clenova.app.feature.clean

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Screenshot
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clenova.app.core.designsystem.ClenovaShapes
import com.clenova.app.core.designsystem.ClenovaThemeTokens
import com.clenova.app.feature.agent.CleanupDesignPlan
import com.clenova.app.feature.agent.CleanupReviewScreen
import com.example.BuildConfig
import com.example.R
import kotlinx.coroutines.delay

private val scanStages = listOf(
    R.string.scan_stage_inventory,
    R.string.scan_stage_duplicates,
    R.string.scan_stage_screenshots,
    R.string.scan_stage_videos,
    R.string.scan_stage_plan,
)

/**
 * The development fixture is a visual-only walkthrough. It never reads or modifies media.
 * The release path will consume real scan results when the media engine is integrated.
 */
@Composable
fun SmartScanScreen(
    sample: Boolean,
    onBack: () -> Unit,
    onSelectionHaptic: () -> Unit,
    onDuplicates: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ClenovaThemeTokens.colors
    val preview = BuildConfig.DEBUG && sample
    var progress by rememberSaveable { mutableIntStateOf(0) }
    var reviewing by rememberSaveable { mutableStateOf(false) }
    var approvedMask by rememberSaveable { mutableIntStateOf(0) }
    var cancelPrompt by rememberSaveable { mutableStateOf(false) }
    val scanning = preview && progress < 100
    LaunchedEffect(preview, cancelPrompt) {
        if (preview && !cancelPrompt) {
            while (progress < 100) {
                delay(42)
                progress = (progress + 1).coerceAtMost(100)
            }
        }
    }
    val requestBack: () -> Unit = {
        if (scanning) cancelPrompt = true else onBack()
    }
    BackHandler(enabled = reviewing || scanning) {
        if (reviewing) reviewing = false else cancelPrompt = true
    }
    if (reviewing && preview && !scanning) {
        CleanupReviewScreen(
            sample = true,
            approvedMask = approvedMask,
            onBack = { reviewing = false; onSelectionHaptic() },
            onApprove = { mask ->
                if (mask in 1..CleanupDesignPlan.ALL) {
                    approvedMask = mask
                    onSelectionHaptic()
                }
            },
            onRevise = { approvedMask = 0; onSelectionHaptic() },
            modifier = modifier,
        )
    } else {
        Box(modifier.fillMaxSize().background(colors.background).windowInsetsPadding(WindowInsets.displayCutout),
            contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 560.dp).fillMaxSize().navigationBarsPadding().padding(horizontal = 24.dp)) {
                Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = requestBack, modifier = Modifier.testTag("scan_back")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.cd_back_button), tint = colors.textPrimary)
                    }
                    Text(stringResource(R.string.home_smart_scan), Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                    if (preview && !scanning) TextButton(onClick = {
                        progress = 0
                        approvedMask = 0
                        onSelectionHaptic()
                    }, modifier = Modifier.testTag("scan_rescan")) {
                        Text(stringResource(R.string.scan_rescan))
                    }
                }
                HorizontalDivider(color = colors.border.copy(alpha = .65f))
                Crossfade(targetState = scanning, modifier = Modifier.weight(1f), animationSpec = tween(300), label = "scan_to_results") { running ->
                    when {
                        !preview -> Column(Modifier.fillMaxSize().padding(vertical = 30.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(stringResource(R.string.scan_unavailable_title), style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
                            Text(stringResource(R.string.scan_unavailable_body), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                        }
                        running -> ScanProgress(progress)
                        else -> ScanResults(approvedMask, onReview = { reviewing = true; onSelectionHaptic() }, onDuplicates = onDuplicates)
                    }
                }
                if (scanning) TextButton(onClick = { cancelPrompt = true }, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 14.dp)) {
                    Text(stringResource(R.string.scan_cancel))
                } else if (!preview) Button(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp), shape = ClenovaShapes.pill) {
                    Text(stringResource(R.string.review_done))
                }
            }
        }
    }
    if (cancelPrompt) {
        AlertDialog(onDismissRequest = { cancelPrompt = false },
            title = { Text(stringResource(R.string.scan_cancel_title)) },
            text = { Text(stringResource(R.string.scan_cancel_body)) },
            confirmButton = { TextButton(onClick = { cancelPrompt = false; onBack() }) { Text(stringResource(R.string.scan_cancel)) } },
            dismissButton = { TextButton(onClick = { cancelPrompt = false }) { Text(stringResource(R.string.scan_continue)) } },
        )
    }
}

@Composable
private fun ScanProgress(progress: Int) {
    val colors = ClenovaThemeTokens.colors
    val animated by animateFloatAsState(progress / 100f, tween(90), label = "scan_progress")
    val stage = (progress / 20).coerceAtMost(scanStages.lastIndex)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Spacer(Modifier.height(20.dp))
        Box(Modifier.size(184.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().padding(8.dp)) {
                val stroke = 8.dp.toPx()
                drawCircle(colors.border, radius = size.minDimension / 2 - stroke / 2, style = Stroke(stroke))
                drawArc(colors.primary, startAngle = -90f, sweepAngle = animated.coerceIn(0f, 1f) * 360f,
                    useCenter = false, topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                    size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                    style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.CropFree, null, Modifier.size(30.dp), tint = colors.primary)
                Text(stringResource(R.string.scan_percent, progress), style = MaterialTheme.typography.headlineLarge.copy(fontSize = 39.sp, fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(stringResource(R.string.scan_running_title), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
            Text(stringResource(scanStages[stage]), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            scanStages.forEachIndexed { index, label ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    if (index < stage) Icon(Icons.Outlined.CheckCircle, null, Modifier.size(21.dp), tint = colors.success)
                    else if (index == stage) CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp, color = colors.primary)
                    else Box(Modifier.size(19.dp).clip(ClenovaShapes.pill).background(colors.border))
                    Text(stringResource(label), style = MaterialTheme.typography.bodyMedium,
                        color = if (index <= stage) colors.textPrimary else colors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun ScanResults(approvedMask: Int, onReview: () -> Unit, onDuplicates: () -> Unit) {
    val colors = ClenovaThemeTokens.colors
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.CheckCircle, null, Modifier.size(18.dp), tint = colors.success)
                    Text(stringResource(R.string.scan_complete), color = colors.success, style = MaterialTheme.typography.labelMedium)
                }
                Text(CleanupDesignPlan.spaceLabel(CleanupDesignPlan.ALL), Modifier.testTag("scan_result_total"),
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 56.sp, lineHeight = 62.sp, letterSpacing = (-2).sp, fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary)
                Text(stringResource(R.string.scan_potential), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                Row(Modifier.fillMaxWidth().height(10.dp).clip(ClenovaShapes.pill)) {
                    listOf(colors.primary, colors.primary.copy(alpha = .68f), colors.primary.copy(alpha = .34f)).forEachIndexed { i, color ->
                        Box(Modifier.weight(CleanupDesignPlan.groups[i].mb.toFloat()).fillMaxHeight().background(color))
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.scan_findings), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                CleanupDesignPlan.groups.forEachIndexed { index, group ->
                    val icon = when(group.bit) { 1 -> Icons.Outlined.ContentCopy; 2 -> Icons.Outlined.VideoFile; else -> Icons.Outlined.Screenshot }
                    ScanFinding(icon, stringResource(group.title), stringResource(group.detail), CleanupDesignPlan.spaceLabel(group.bit), if (group.bit == 1) onDuplicates else null)
                    if (index < CleanupDesignPlan.groups.lastIndex) HorizontalDivider(color = colors.border.copy(alpha = .65f))
                }
                HorizontalDivider(color = colors.border.copy(alpha = .65f))
                ScanFinding(Icons.Outlined.Image, stringResource(R.string.clean_tool_similar_title), stringResource(R.string.scan_similar_detail), stringResource(R.string.scan_similar_count))
            }
            Surface(shape = ClenovaShapes.card, color = colors.surface, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.VerifiedUser, null, Modifier.size(27.dp), tint = colors.success)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.review_protected), color = colors.textPrimary, style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.review_safety), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (approvedMask != 0) Text(stringResource(R.string.review_approval_only), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Button(onClick = onReview, modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 14.dp).heightIn(min = 52.dp).testTag("scan_review"),
            shape = ClenovaShapes.pill, colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary)) {
            Text(stringResource(if (approvedMask != 0) R.string.review_view_plan else R.string.home_auto_review), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ScanFinding(icon: ImageVector, title: String, detail: String, value: String, onClick: (() -> Unit)? = null) {
    val colors = ClenovaThemeTokens.colors
    Row(Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clip(ClenovaShapes.tile).clickable(onClick = onClick) else Modifier).padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(44.dp).clip(ClenovaShapes.tile).background(colors.softBlue), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(23.dp), tint = colors.primary)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = colors.textPrimary, style = MaterialTheme.typography.titleSmall)
            Text(detail, color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Text(value, color = colors.textPrimary, style = MaterialTheme.typography.labelLarge)
    }
}
