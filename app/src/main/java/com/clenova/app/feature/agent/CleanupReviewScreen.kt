package com.clenova.app.feature.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material.icons.outlined.Screenshot
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.clenova.app.core.designsystem.ClenovaShapes
import com.clenova.app.core.designsystem.ClenovaThemeTokens
import com.example.BuildConfig
import com.example.R

@Composable
fun CleanupReviewScreen(
    sample: Boolean,
    approvedMask: Int,
    onBack: () -> Unit,
    onApprove: (Int) -> Unit,
    onRevise: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ClenovaThemeTokens.colors
    val hasPlan = BuildConfig.DEBUG && sample
    val approved = hasPlan && approvedMask in 1..CleanupDesignPlan.ALL
    var selection by rememberSaveable { mutableIntStateOf(if (approved) approvedMask else CleanupDesignPlan.ALL) }
    var confirm by rememberSaveable { mutableStateOf(false) }
    val mask = if (approved) approvedMask else selection
    val total = if (hasPlan) CleanupDesignPlan.spaceLabel(mask) else ""
    Box(modifier.fillMaxSize().background(colors.background).windowInsetsPadding(WindowInsets.displayCutout),
        contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 560.dp).fillMaxSize().navigationBarsPadding().padding(horizontal = 24.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("review_back")) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.cd_back_button), tint = colors.textPrimary)
                }
                Text(stringResource(R.string.review_title), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
            }
            HorizontalDivider(color = colors.border.copy(alpha = .65f))
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)) {
                if (!hasPlan) {
                    Text(stringResource(R.string.review_empty_title), style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
                    Text(stringResource(R.string.review_empty_body), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(7.dp).clip(ClenovaShapes.pill).background(if (approved) colors.success else colors.primary))
                            Text(stringResource(if (approved) R.string.review_approved else R.string.review_prepared), color = if (approved) colors.success else colors.primary, style = MaterialTheme.typography.labelMedium)
                        }
                        Text(total, modifier = Modifier.testTag("review_total"), style = MaterialTheme.typography.displayLarge.copy(fontSize = 56.sp, lineHeight = 62.sp, letterSpacing = (-2).sp, fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                        Text(stringResource(if (approved) R.string.review_approved_estimate else R.string.review_estimated_space), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                        Row(Modifier.fillMaxWidth().height(9.dp).clip(ClenovaShapes.pill).background(colors.border)) {
                            val fraction = CleanupDesignPlan.selectedMb(mask) / 4800f
                            if (fraction > 0f) Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().background(colors.primary))
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.review_recommendations), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                            if (!approved) TextButton(onClick = { selection = if (selection == CleanupDesignPlan.ALL) 0 else CleanupDesignPlan.ALL }, modifier = Modifier.testTag("review_select_all")) {
                                Text(stringResource(if (selection == CleanupDesignPlan.ALL) R.string.review_deselect_all else R.string.review_select_all))
                            }
                        }
                        CleanupDesignPlan.groups.forEachIndexed { index, group ->
                            val checked = mask and group.bit != 0
                            val icon = when (group.bit) { 1 -> Icons.Outlined.ContentCopy; 2 -> Icons.Outlined.VideoFile; else -> Icons.Outlined.Screenshot }
                            Row(Modifier.fillMaxWidth().clip(ClenovaShapes.tile)
                                .clickable(enabled = !approved, role = Role.Checkbox) { selection = selection xor group.bit }
                                .testTag("review_group_${group.bit}").padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(43.dp).clip(ClenovaShapes.tile).background(colors.softBlue), contentAlignment = Alignment.Center) {
                                    Icon(icon, null, Modifier.size(23.dp), tint = colors.primary)
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(stringResource(group.title), style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                                    Text(stringResource(group.detail), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                }
                                Text(CleanupDesignPlan.spaceLabel(group.bit), style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                                Checkbox(checked = checked, onCheckedChange = null, enabled = !approved, colors = CheckboxDefaults.colors(checkedColor = colors.primary))
                            }
                            if (index != CleanupDesignPlan.groups.lastIndex) HorizontalDivider(color = colors.border.copy(alpha = .65f))
                        }
                    }
                    Surface(shape = ClenovaShapes.card, color = colors.surface, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Outlined.VerifiedUser, null, Modifier.size(28.dp), tint = colors.success)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(stringResource(R.string.review_protected), color = colors.textPrimary, style = MaterialTheme.typography.titleSmall)
                                Text(stringResource(R.string.review_safety), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    if (approved) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.CheckCircle, null, tint = colors.success)
                        Text(stringResource(R.string.review_approval_only), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                    }
                }
            }
            Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = { if (approved || !hasPlan) onBack() else confirm = true },
                    enabled = !hasPlan || approved || mask != 0,
                    shape = ClenovaShapes.pill,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("review_approve")) {
                    Text(if (approved || !hasPlan) stringResource(R.string.review_done) else stringResource(R.string.review_approve_space, total))
                }
                if (approved) TextButton(onClick = { selection = approvedMask; onRevise() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.review_revise))
                } else if (hasPlan) TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.review_not_now))
                }
            }
        }
    }
    if (confirm && hasPlan && !approved && mask != 0) {
        AlertDialog(onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.review_confirm_title)) },
            text = { Text(stringResource(R.string.review_confirm_body, total)) },
            confirmButton = { TextButton(onClick = { confirm = false; onApprove(mask) }, modifier = Modifier.testTag("review_confirm")) { Text(stringResource(R.string.review_confirm_action)) } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(android.R.string.cancel)) } },
        )
    }
}
