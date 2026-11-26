package com.clenova.app.feature.agent

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clenova.app.core.designsystem.*
import com.clenova.app.core.utilities.ByteFormatter
import com.clenova.app.data.agent.AgentReport
import com.clenova.app.domain.model.*
import com.clenova.app.feature.home.BrandHeader
import com.example.R
import java.text.DateFormat
import java.util.Date

@Composable
internal fun AgentScreen(
    preferences: UserPreferences,
    report: AgentReport?,
    checking: Boolean,
    onConfigure: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onCheck: () -> Unit,
    onReview: () -> Unit,
    onReviewCategory: (AgentAction) -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onGuard: () -> Unit,
    onPermission: () -> Unit,
    error: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = ClenovaThemeTokens.colors
    val configured = preferences.autopilotStatus != AutopilotStatus.NotConfigured
    val active = preferences.autopilotStatus == AutopilotStatus.Active || preferences.autopilotStatus == AutopilotStatus.AwaitingApproval
    var pro by remember { mutableStateOf(false) }
    Box(modifier.fillMaxSize().background(colors.background).windowInsetsPadding(WindowInsets.displayCutout), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 560.dp).fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp).padding(top = 24.dp, bottom = ClenovaSpacing.floatingNavBottomClearance).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(15.dp)) {
            BrandHeader(onOpenSettings = onSettings, onOpenPro = { pro = true })
            HorizontalDivider(color = colors.border.copy(alpha = .65f))
            if (!configured) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    AgentNetwork()
                    Text(stringResource(R.string.agent_title), style = MaterialTheme.typography.labelMedium, color = colors.primary)
                    Text(stringResource(R.string.agent_real_welcome), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-.7).sp), color = colors.textPrimary)
                    Text(stringResource(R.string.agent_real_intro), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                }
                AgentFeature(Icons.Outlined.Search, R.string.agent_real_proactive, R.string.agent_real_proactive_body)
                AgentFeature(Icons.Outlined.Storage, R.string.agent_real_storage, R.string.agent_real_storage_body)
                AgentFeature(Icons.Outlined.VerifiedUser, R.string.guard_protection_label, R.string.agent_real_guard_body)
                AgentFeature(Icons.Outlined.FactCheck, R.string.auto_safety_title, R.string.guard_approval_body)
                AgentFeature(Icons.Outlined.History, R.string.agent_real_transparency, R.string.agent_real_transparency_body)
                Button(onClick = onConfigure, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("agent_setup"), shape = ClenovaShapes.pill) {
                    Text(stringResource(R.string.agent_real_setup)); Spacer(Modifier.width(8.dp)); Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(18.dp))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Lock, null, Modifier.size(15.dp), tint = colors.textSecondary)
                    Spacer(Modifier.width(5.dp)); Text(stringResource(R.string.agent_real_private), style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                }
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.agent_real_your), Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
                    IconButton(onClick = onHistory) { Icon(Icons.Outlined.History, stringResource(R.string.clean_history), tint = colors.textPrimary) }
                    IconButton(onClick = onConfigure) { Icon(Icons.Outlined.Tune, stringResource(R.string.auto_configure), tint = colors.textPrimary) }
                }
                Surface(shape = ClenovaShapes.card, color = colors.softBlue, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(42.dp).clip(ClenovaShapes.tile).background(colors.primary), contentAlignment = Alignment.Center) { ClenovaAutoIcon(tint = colors.onPrimary, size = 24.dp) }
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.home_autopilot), color = colors.textPrimary, style = MaterialTheme.typography.titleLarge)
                                Text(stringResource(if (active) R.string.agent_real_active else R.string.home_auto_paused), color = if (active) colors.success else colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                            }
                            Switch(checked = active, onCheckedChange = onToggle, modifier = Modifier.testTag("agent_toggle"))
                        }
                        HorizontalDivider(color = colors.border)
                        if (checking) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                            Text(stringResource(R.string.agent_real_checking), color = colors.textPrimary)
                        } else if (report == null) Text(stringResource(R.string.agent_real_first), color = colors.textPrimary)
                        else if (report.problem.isNotEmpty()) {
                            Text(stringResource(if (report.problem == "permission") R.string.agent_real_permission else R.string.agent_real_error), color = colors.textPrimary)
                            if (report.problem == "permission") TextButton(onClick = onPermission) { Text(stringResource(R.string.guard_real_browse)) }
                        } else {
                            Text(stringResource(R.string.review_estimated_space), style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                            Text(ByteFormatter.format(report.reviewBytes), style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary, modifier = Modifier.testTag("agent_review_bytes"))
                            Text(stringResource(R.string.agent_real_ready), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                            if (report.partial) Text(stringResource(R.string.scan_partial), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                        HorizontalDivider(color = colors.border)
                        if (report != null && report.problem.isEmpty()) {
                            AgentDetail(R.string.agent_real_last, DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(report.checkedAt)))
                            AgentDetail(R.string.auto_goal_title, stringResource(R.string.auto_goal_value, preferences.storageGoalGb))
                            AgentDetail(R.string.home_available_storage, ByteFormatter.format(report.availableBytes))
                        }
                        AgentDetail(R.string.auto_frequency_title, stringResource(when(preferences.checkIntervalDays) { 3 -> R.string.auto_every_three_days; 7 -> R.string.auto_weekly; else -> R.string.auto_daily }))
                        if (active) Text(stringResource(R.string.agent_real_schedule_note), color = colors.textSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (error) Text(stringResource(R.string.auto_save_failed), color = colors.error)
                Text(stringResource(R.string.agent_real_attention), style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                if (!checking && report != null && report.problem.isEmpty() && report.reviewBytes > 0) {
                    if (report.duplicateCount > 0) AgentOpportunity(Icons.Outlined.ContentCopy, R.string.clean_tool_duplicates_title, report.duplicateCount, report.duplicateBytes) { onReviewCategory(AgentAction.DUPLICATES) }
                    if (report.screenshotCount > 0) AgentOpportunity(Icons.Outlined.Image, R.string.clean_tool_screenshots_title, report.screenshotCount, report.screenshotBytes) { onReviewCategory(AgentAction.SCREENSHOTS) }
                    if (report.videoCount > 0) AgentOpportunity(Icons.Outlined.VideoFile, R.string.review_videos, report.videoCount, report.videoBytes) { onReviewCategory(AgentAction.VIDEOS) }
                    Button(onClick = onReview, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("agent_review"), shape = ClenovaShapes.pill) { Text(stringResource(R.string.agent_real_review)) }
                } else Text(stringResource(if (checking) R.string.agent_real_checking else R.string.review_empty_title), color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f).clickable(onClick = onGuard), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(Icons.Outlined.VerifiedUser, null, tint = colors.primary, modifier = Modifier.size(20.dp))
                        Text(stringResource(R.string.guard_title), color = colors.textPrimary, style = MaterialTheme.typography.labelMedium)
                    }
                    TextButton(onClick = onCheck, enabled = active && !checking, modifier = Modifier.testTag("agent_check")) { Text(stringResource(R.string.agent_real_check_now)) }
                }
            }
        }
    }
    if (pro) AlertDialog(onDismissRequest = { pro = false }, title = { Text(stringResource(R.string.home_pro)) },
        text = { Text(stringResource(R.string.home_pro_unavailable)) }, confirmButton = { TextButton(onClick = { pro = false }) { Text(stringResource(android.R.string.ok)) } })
}

@Composable private fun AgentNetwork() {
    val colors = ClenovaThemeTokens.colors
    Box(Modifier.width(210.dp).height(116.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val endpoints = listOf(Offset(size.width * .12f, size.height * .25f), Offset(size.width * .88f, size.height * .75f), Offset(size.width * .74f, size.height * .08f), Offset(size.width * .28f, size.height * .92f))
            endpoints.forEach { p ->
                val bend = Offset(p.x, c.y)
                drawLine(colors.primary.copy(alpha = .25f), c, bend, 1.5.dp.toPx(), StrokeCap.Round)
                drawLine(colors.primary.copy(alpha = .25f), bend, p, 1.5.dp.toPx(), StrokeCap.Round)
                drawCircle(colors.primary, 3.dp.toPx(), p)
            }
        }
        Box(Modifier.size(88.dp).clip(ClenovaShapes.card).background(colors.softBlue), contentAlignment = Alignment.Center) {
            Box(Modifier.size(64.dp).clip(ClenovaShapes.tile).background(colors.primary), contentAlignment = Alignment.Center) { ClenovaAutoIcon(tint = colors.onPrimary, size = 36.dp) }
        }
    }
}

@Composable private fun AgentFeature(icon: ImageVector, title: Int, body: Int) {
    val colors = ClenovaThemeTokens.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
        Icon(icon, null, Modifier.size(25.dp), tint = colors.primary)
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), color = colors.textPrimary, style = MaterialTheme.typography.titleSmall)
            Text(stringResource(body), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable private fun AgentDetail(label: Int, value: String) {
    val colors = ClenovaThemeTokens.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(label), color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
        Text(value, color = colors.textPrimary, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable private fun AgentOpportunity(icon: ImageVector, title: Int, count: Int, bytes: Long, onClick: () -> Unit) {
    val colors = ClenovaThemeTokens.colors
    Row(Modifier.fillMaxWidth().clip(ClenovaShapes.tile).clickable(onClick = onClick).padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(ClenovaShapes.tile).background(colors.softBlue), contentAlignment = Alignment.Center) { Icon(icon, null, tint = colors.primary) }
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), color = colors.textPrimary, style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.guard_item_count, count), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Text(ByteFormatter.format(bytes), color = colors.textPrimary, style = MaterialTheme.typography.labelLarge)
        Icon(Icons.Outlined.ChevronRight, null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
    }
    HorizontalDivider(color = colors.border)
}
