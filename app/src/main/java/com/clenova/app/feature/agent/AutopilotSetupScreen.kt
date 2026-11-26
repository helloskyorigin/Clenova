package com.clenova.app.feature.agent

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clenova.app.core.designsystem.ClenovaAutoIcon
import com.clenova.app.core.designsystem.ClenovaShapes
import com.clenova.app.core.designsystem.ClenovaThemeTokens
import com.clenova.app.domain.model.UserPreferences
import com.example.R

@Composable
fun AutopilotSetupScreen(
    preferences: UserPreferences,
    sample: Boolean,
    saving: Boolean,
    failed: Boolean,
    onBack: () -> Unit,
    onSave: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ClenovaThemeTokens.colors
    var goalOverride by rememberSaveable { mutableStateOf<Int?>(null) }
    var intervalOverride by rememberSaveable { mutableStateOf<Int?>(null) }
    val goal = goalOverride ?: preferences.storageGoalGb
    val interval = intervalOverride ?: preferences.checkIntervalDays
    BackHandler(enabled = saving) { }

    Box(modifier.fillMaxSize().background(colors.background).windowInsetsPadding(WindowInsets.displayCutout),
        contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 560.dp).fillMaxSize().navigationBarsPadding().padding(horizontal = 24.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, enabled = !saving, modifier = Modifier.testTag("auto_setup_back")) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.cd_back_button), tint = colors.textPrimary)
                }
                Text(stringResource(R.string.home_autopilot), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
            }
            HorizontalDivider(color = colors.border.copy(alpha = .65f))
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(23.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(colors.softBlue),
                        contentAlignment = Alignment.Center) {
                        ClenovaAutoIcon(tint = colors.primary, size = 26.dp)
                    }
                    Text(stringResource(R.string.auto_setup_headline), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-.8).sp), color = colors.textPrimary)
                    Text(stringResource(R.string.auto_setup_intro), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.auto_goal_title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                    Text(stringResource(R.string.auto_goal_subtitle), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5, 10, 20, 30).forEach { amount ->
                            val selected = amount == goal
                            Surface(
                                modifier = Modifier.weight(1f).heightIn(min = 49.dp).clip(ClenovaShapes.tile)
                                    .semantics { this.selected = selected }
                                    .testTag("auto_goal_$amount").clickable(enabled = !saving, role = Role.RadioButton) { goalOverride = amount },
                                color = if (selected) colors.primary else colors.surface,
                                shape = ClenovaShapes.tile,
                            ) {
                                Box(Modifier.padding(vertical = 13.dp, horizontal = 3.dp), contentAlignment = Alignment.Center) {
                                    Text(stringResource(R.string.auto_goal_value, amount), color = if (selected) colors.onPrimary else colors.textPrimary,
                                        style = MaterialTheme.typography.labelLarge, maxLines = 1)
                                }
                            }
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(stringResource(R.string.auto_frequency_title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                    listOf(1 to R.string.auto_daily, 3 to R.string.auto_every_three_days, 7 to R.string.auto_weekly).forEach { (days, label) ->
                        val selected = interval == days
                        Row(Modifier.fillMaxWidth().clip(ClenovaShapes.tile)
                            .semantics { this.selected = selected }.testTag("auto_interval_$days")
                            .clickable(enabled = !saving, role = Role.RadioButton) { intervalOverride = days }
                            .padding(vertical = 8.dp, horizontal = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(label), Modifier.weight(1f), color = colors.textPrimary, style = MaterialTheme.typography.bodyLarge)
                            RadioButton(selected = selected, onClick = null, enabled = !saving, colors = RadioButtonDefaults.colors(selectedColor = colors.primary))
                        }
                        if (days != 7) HorizontalDivider(color = colors.border.copy(alpha = .65f))
                    }
                }
                Surface(shape = ClenovaShapes.card, color = colors.surface, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(15.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.VerifiedUser, null, Modifier.size(27.dp), tint = colors.success)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.auto_safety_title), style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                            Text(stringResource(R.string.auto_safety_body), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                    }
                }
            }
            Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (failed) Text(stringResource(R.string.auto_save_failed), color = colors.error, style = MaterialTheme.typography.bodySmall)
                Button(onClick = { onSave(goal, interval) }, enabled = !saving,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("auto_setup_save"),
                    shape = ClenovaShapes.pill, colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary)) {
                    if (saving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text(stringResource(if (sample) R.string.home_sample_activate else R.string.auto_save_preferences), style = MaterialTheme.typography.labelLarge)
                }
                if (!sample) Text(stringResource(R.string.auto_monitoring_unavailable), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
