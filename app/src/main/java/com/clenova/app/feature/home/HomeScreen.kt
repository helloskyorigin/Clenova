package com.clenova.app.feature.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clenova.app.core.designsystem.ClenovaMotion
import com.clenova.app.core.designsystem.ClenovaShapes
import com.clenova.app.core.designsystem.ClenovaSpacing
import com.clenova.app.core.designsystem.ClenovaThemeTokens
import com.clenova.app.core.utilities.ByteFormatter
import com.clenova.app.domain.model.AutopilotStatus
import com.clenova.app.domain.model.StorageState
import com.clenova.app.feature.storage.StorageDesignData
import com.clenova.app.feature.agent.CleanupDesignPlan
import com.example.BuildConfig
import com.example.R
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onRefreshStorage: () -> Unit,
    onOpenAgent: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenClean: () -> Unit,
    onOpenGuard: () -> Unit,
    onOpenStorage: () -> Unit,
    onOpenAutopilot: () -> Unit,
    onOpenSmartScan: () -> Unit,
    sampleAutoOn: Boolean = true,
    sampleApprovedMask: Int = 0,
    onSampleAutoChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    showSampleHome: Boolean = false,
) {
    val colors = ClenovaThemeTokens.colors
    var dialogTitle by rememberSaveable { mutableStateOf<Int?>(null) }
    // Local design review only. Never feed these values into storage, protection or deletion logic.
    val sample = BuildConfig.DEBUG && showSampleHome
    val shownStorage = if (sample) StorageDesignData.state else uiState.storageState

    Box(
        modifier = modifier.fillMaxSize().background(colors.background)
            .windowInsetsPadding(WindowInsets.displayCutout),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 560.dp).fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = ClenovaSpacing.floatingNavBottomClearance)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                BrandHeader(onOpenSettings = onOpenSettings, onOpenPro = { dialogTitle = R.string.home_pro })
                HorizontalDivider(color = colors.border.copy(alpha = .65f))
            }
            StorageOverviewSection(shownStorage, onRefreshStorage, sample, onOpenStorage)
            AutopilotPanel(uiState.autopilotStatus, onOpenAgent, onOpenAutopilot, sample, sampleAutoOn, sampleApprovedMask, onSampleAutoChange)
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.home_quick_actions), Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
                        color = colors.textPrimary)
                    TextButton(onClick = onOpenClean, modifier = Modifier.testTag("home_view_all")) {
                        Text(stringResource(R.string.home_view_all), color = colors.primary)
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.padding(start = 4.dp).size(14.dp), tint = colors.primary)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickAction(Icons.Outlined.CropFree, R.string.home_smart_scan, onOpenSmartScan, Modifier.weight(1f))
                    QuickAction(Icons.Outlined.ContentCopy, R.string.clean_tool_duplicates_title, { dialogTitle = R.string.clean_tool_duplicates_title }, Modifier.weight(1f))
                    QuickAction(Icons.Outlined.Compress, R.string.home_compress, { dialogTitle = R.string.home_compress }, Modifier.weight(1f))
                }
            }
            Column {
                HorizontalDivider(color = colors.border.copy(alpha = .65f))
                Row(
                    modifier = Modifier.fillMaxWidth().clip(ClenovaShapes.tile)
                        .clickable(onClick = onOpenGuard).testTag("home_memory_guard").padding(vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    Icon(Icons.Outlined.VerifiedUser, null, Modifier.size(29.dp), tint = colors.success)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(stringResource(R.string.guard_title), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                        Text(if (sample) stringResource(R.string.home_sample_protected, 248) else stringResource(R.string.home_guard_status), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(18.dp), tint = colors.textSecondary)
                }
            }
        }
    }

    dialogTitle?.let { title ->
        AlertDialog(
            onDismissRequest = { dialogTitle = null },
            title = { Text(stringResource(title)) },
            text = { Text(stringResource(if (title == R.string.home_pro) R.string.home_pro_unavailable else R.string.home_tool_unavailable)) },
            confirmButton = { TextButton(onClick = { dialogTitle = null; if (title != R.string.home_pro) onOpenClean() }) {
                Text(stringResource(if (title == R.string.home_pro) android.R.string.ok else R.string.home_view_all))
            } },
            dismissButton = { if (title != R.string.home_pro) TextButton(onClick = { dialogTitle = null }) { Text(stringResource(android.R.string.cancel)) } },
        )
    }
}

@Composable
private fun BrandHeader(onOpenSettings: () -> Unit, onOpenPro: () -> Unit) {
    val colors = ClenovaThemeTokens.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.clenova_brand_logo), contentDescription = null,
            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.home_brand), color = colors.textPrimary,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(stringResource(R.string.app_tagline), color = colors.textSecondary,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 14.sp),
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        TextButton(onClick = onOpenPro, modifier = Modifier.heightIn(min = 48.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)) {
            Text(stringResource(R.string.home_pro), modifier = Modifier.clip(ClenovaShapes.pill)
                .background(colors.softBlue).padding(horizontal = 10.dp, vertical = 7.dp),
                color = colors.primary, style = MaterialTheme.typography.labelMedium)
        }
        IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("settings_button")) {
            Icon(Icons.Outlined.Settings, stringResource(R.string.cd_settings_button), Modifier.size(24.dp), tint = colors.textPrimary)
        }
    }
}

@Composable
private fun StorageOverviewSection(state: StorageState, onRefresh: () -> Unit, sample: Boolean, onOpenStorage: () -> Unit) {
    val colors = ClenovaThemeTokens.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).clip(ClenovaShapes.tile).clickable(onClick = onOpenStorage)
                .testTag("home_storage_details").heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.home_available_storage),
                    style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(14.dp), tint = colors.textSecondary)
            }
            IconButton(onClick = onRefresh, modifier = Modifier.size(48.dp).testTag("refresh_storage_button")) {
                Icon(Icons.Outlined.Refresh, stringResource(R.string.cd_refresh_storage), Modifier.size(21.dp), tint = colors.textSecondary)
            }
        }
        when (state) {
            StorageState.Loading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = colors.primary)
                Text(stringResource(R.string.home_storage_loading), color = colors.textSecondary)
            }
            StorageState.Unavailable -> {
                Text(stringResource(R.string.home_storage_unavailable_title), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Text(stringResource(R.string.home_storage_unavailable_body), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
            is StorageState.Ready -> {
                val overview = state.overview
                val formatted = ByteFormatter.format(overview.availableBytes)
                val usedFraction by animateFloatAsState(overview.usedFraction, ClenovaMotion.gentleSpring(), label = "storage_fraction")
                Column(Modifier.fillMaxWidth().clip(ClenovaShapes.tile).clickable(onClick = onOpenStorage),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(buildAnnotatedString {
                        append(formatted.substringBeforeLast(" "))
                        withStyle(SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Normal, color = colors.textSecondary, letterSpacing = 0.sp)) {
                            append(" " + formatted.substringAfterLast(" "))
                        }
                    }, Modifier.weight(1f).testTag("home_storage_value"),
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 56.sp, lineHeight = 62.sp, letterSpacing = (-2.5).sp, fontWeight = FontWeight.SemiBold),
                        color = colors.textPrimary, maxLines = 2)
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(bottom = 5.dp)) {
                        Text(stringResource(R.string.home_capacity), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        Text(ByteFormatter.format(overview.totalBytes), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    }
                }
                Box(Modifier.fillMaxWidth().height(10.dp).clip(ClenovaShapes.pill).background(colors.border)
                    .semantics { progressBarRangeInfo = ProgressBarRangeInfo(overview.usedFraction, 0f..1f) }) {
                    if (usedFraction > 0f) Box(Modifier.fillMaxWidth(usedFraction.coerceIn(0f, 1f)).fillMaxHeight()
                        .clip(ClenovaShapes.pill).background(colors.primary))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.home_used_value, if (sample) StorageDesignData.usedLabel else ByteFormatter.format(overview.usedBytes)), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    Text(stringResource(R.string.home_available_percent, ((1f - overview.usedFraction) * 100).roundToInt()), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                }
                }
            }
        }
    }
}

@Composable
private fun AutopilotPanel(status: AutopilotStatus, onOpenAgent: () -> Unit, onOpenAutopilot: () -> Unit, sample: Boolean, sampleAutoOn: Boolean, sampleApprovedMask: Int, onSampleToggle: (Boolean) -> Unit) {
    val colors = ClenovaThemeTokens.colors
    val awaiting = if (sample) sampleAutoOn else status == AutopilotStatus.AwaitingApproval
    val statusLabel = if (sample && sampleAutoOn) R.string.home_sample_monitoring else if (sample) R.string.home_auto_off else when (status) {
        AutopilotStatus.NotConfigured -> R.string.home_auto_off
        AutopilotStatus.Paused -> R.string.home_auto_paused
        AutopilotStatus.AwaitingApproval -> R.string.home_auto_pending
    }
    Surface(shape = RoundedCornerShape(28.dp), color = colors.surface, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.home_autopilot), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(7.dp).clip(ClenovaShapes.pill).background(if (sample && sampleAutoOn) colors.success else if (awaiting) colors.primary else colors.textSecondary))
                        Text(stringResource(statusLabel), style = MaterialTheme.typography.labelMedium, color = if (sample && sampleAutoOn) colors.success else colors.textSecondary)
                    }
                }
                // The workspace owns activation; a tap must not claim monitoring has started.
                Switch(checked = awaiting, onCheckedChange = { if (sample && !it) onSampleToggle(false) else onOpenAutopilot() },
                    modifier = Modifier.testTag("home_autopilot_switch"))
            }
            HorizontalDivider(color = colors.border)
            if (sample && sampleAutoOn) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(CleanupDesignPlan.spaceLabel(if (sampleApprovedMask != 0) sampleApprovedMask else CleanupDesignPlan.ALL), style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 40.sp, lineHeight = 45.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1.5).sp),
                        color = colors.textPrimary)
                    Text(stringResource(if (sampleApprovedMask != 0) R.string.review_approved else R.string.home_sample_cleanup), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                }
            } else {
                Text(stringResource(if (awaiting) R.string.home_auto_review_title else R.string.home_auto_title),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-.6).sp),
                    color = colors.textPrimary)
                Text(stringResource(if (awaiting) R.string.home_auto_review_body else R.string.home_auto_body),
                    style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
            Button(onClick = { if (awaiting) onOpenAgent() else onOpenAutopilot() }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("open_agent_workspace_button"),
                shape = ClenovaShapes.pill, colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary)) {
                Text(stringResource(if (awaiting && sample && sampleApprovedMask != 0) R.string.review_view_plan else if (awaiting) R.string.home_auto_review else if (sample) R.string.home_sample_activate else R.string.home_auto_setup), Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ClenovaThemeTokens.colors
    Column(modifier.clip(ClenovaShapes.tile).clickable(onClick = onClick).then(if (label == R.string.home_smart_scan) Modifier.testTag("home_smart_scan") else Modifier).padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(Modifier.size(58.dp).clip(RoundedCornerShape(19.dp)).background(colors.softBlue), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(28.dp), tint = colors.primary)
        }
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = colors.textPrimary,
            maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
