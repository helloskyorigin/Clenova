package com.clenova.app.feature.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clenova.app.core.designsystem.ClenovaShapes
import com.clenova.app.core.designsystem.ClenovaThemeTokens
import com.clenova.app.domain.model.ThemePreference
import com.clenova.app.domain.model.UserPreferences
import com.example.BuildConfig
import com.example.R

private const val SUPPORT_EMAIL = "hello.skyorigin@gmail.com"

@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    onSelectTheme: (ThemePreference) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    onOpenAutopilot: () -> Unit,
    showUnfinishedRows: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = ClenovaThemeTokens.colors
    val context = LocalContext.current
    val showPlanned = BuildConfig.DEBUG && showUnfinishedRows
    var themeMenu by rememberSaveable { mutableStateOf(false) }
    var actionFailed by rememberSaveable { mutableStateOf(false) }
    val themeLabel = when (preferences.themePreference) {
        ThemePreference.LIGHT -> R.string.settings_theme_light
        ThemePreference.DARK -> R.string.settings_theme_dark
        ThemePreference.SYSTEM -> R.string.settings_theme_system
    }
    fun email(subject: String, body: String = "") {
        val uri = Uri.parse("mailto:" + SUPPORT_EMAIL + "?subject=" + Uri.encode(subject) + "&body=" + Uri.encode(body))
        actionFailed = runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, uri)) }.isFailure
    }

    Box(modifier.fillMaxSize().background(colors.background).windowInsetsPadding(WindowInsets.displayCutout)
        .navigationBarsPadding(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 560.dp).fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp).padding(top = 12.dp, bottom = 28.dp)) {
            IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("settings_back_button")) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.cd_back_button), tint = colors.textPrimary)
            }
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.settings_title), color = colors.textPrimary,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 32.sp, letterSpacing = (-.8).sp))
            Row(Modifier.fillMaxWidth().padding(top = 23.dp, bottom = 23.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                Image(painterResource(R.drawable.clenova_brand_logo), contentDescription = null,
                    modifier = Modifier.size(62.dp).clip(RoundedCornerShape(18.dp)))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(stringResource(R.string.home_brand), color = colors.textPrimary,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-.8).sp))
                    Text(stringResource(R.string.app_tagline), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.settings_version_short, BuildConfig.VERSION_NAME), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
            SettingsSection(R.string.settings_general) {
                Box(Modifier.fillMaxWidth()) {
                    SettingsRow(Icons.Outlined.Brightness6, R.string.settings_theme_label, value = stringResource(themeLabel),
                        dropdown = true, tag = "settings_appearance", onClick = { themeMenu = true })
                    DropdownMenu(expanded = themeMenu, onDismissRequest = { themeMenu = false },
                        modifier = Modifier.widthIn(min = 180.dp).background(colors.surfaceElevated)) {
                        listOf(
                            Triple(ThemePreference.LIGHT, R.string.settings_theme_light, "theme_option_light"),
                            Triple(ThemePreference.DARK, R.string.settings_theme_dark, "theme_option_dark"),
                            Triple(ThemePreference.SYSTEM, R.string.settings_theme_system, "theme_option_system"),
                        ).forEach { (option, label, tag) ->
                            DropdownMenuItem(text = { Text(stringResource(label), color = colors.textPrimary) },
                                onClick = { themeMenu = false; onSelectTheme(option) },
                                modifier = Modifier.testTag(tag),
                                trailingIcon = { if (preferences.themePreference == option) Icon(Icons.Outlined.Check, null, tint = colors.primary) })
                        }
                    }
                }
                if (showPlanned) SettingsRow(Icons.Outlined.Language, R.string.settings_language, value = "English", dropdown = true)
                Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Icon(Icons.Outlined.Vibration, null, Modifier.size(22.dp), tint = colors.textPrimary)
                    Text(stringResource(R.string.settings_haptics_label), Modifier.weight(1f), color = colors.textPrimary, style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = preferences.hapticsEnabled, onCheckedChange = onToggleHaptics,
                        modifier = Modifier.testTag("settings_haptics_switch"),
                        colors = SwitchDefaults.colors(checkedThumbColor = colors.onPrimary, checkedTrackColor = colors.primary))
                }
            }
            SettingsSection(R.string.settings_privacy_control) {
                SettingsRow(Icons.Outlined.Tune, R.string.settings_autopilot, onClick = onOpenAutopilot)
                SettingsRow(Icons.Outlined.VerifiedUser, R.string.settings_permissions, onClick = {
                    actionFailed = runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))) }.isFailure
                })
                if (showPlanned) {
                    SettingsRow(Icons.Outlined.Lock, R.string.settings_data_privacy)
                    SettingsRow(Icons.Outlined.Refresh, R.string.settings_reset)
                }
            }
            SettingsSection(R.string.settings_support) {
                if (showPlanned) SettingsRow(Icons.Outlined.HelpOutline, R.string.settings_help)
                SettingsRow(Icons.Outlined.Email, R.string.settings_contact, onClick = { email("Clenova support") })
                SettingsRow(Icons.Outlined.BugReport, R.string.settings_report, onClick = {
                    email("Clenova problem report", "Describe the problem here:\n\nApp version: " + BuildConfig.VERSION_NAME + "\nAndroid: " + Build.VERSION.RELEASE + "\nDevice: " + Build.MANUFACTURER + " " + Build.MODEL)
                })
                SettingsRow(Icons.Outlined.ChatBubbleOutline, R.string.settings_suggest, onClick = { email("Clenova feature suggestion") })
            }
            if (showPlanned) SettingsSection(R.string.settings_about_section) {
                SettingsRow(Icons.Outlined.Description, R.string.settings_policy)
                SettingsRow(Icons.Outlined.FactCheck, R.string.settings_terms)
                SettingsRow(Icons.Outlined.Code, R.string.settings_licenses)
                SettingsRow(Icons.Outlined.Info, R.string.settings_about_product)
            }
            HorizontalDivider(color = colors.border)
            Column(Modifier.fillMaxWidth().padding(top = 30.dp, bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(stringResource(R.string.settings_by), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                Text("SKYORIGIN", color = colors.textPrimary, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 3.sp))
                Text(SUPPORT_EMAIL, color = colors.textSecondary, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clip(ClenovaShapes.tile).clickable { email("Clenova support") }.padding(4.dp))
                Text(stringResource(R.string.settings_copyright), color = colors.textSecondary, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
    if (actionFailed) AlertDialog(onDismissRequest = { actionFailed = false },
        title = { Text(stringResource(R.string.settings_action_unavailable)) },
        text = { Text(stringResource(R.string.settings_action_unavailable_body, SUPPORT_EMAIL)) },
        confirmButton = { TextButton(onClick = { actionFailed = false }) { Text(stringResource(android.R.string.ok)) } })
}

@Composable
private fun SettingsSection(title: Int, content: @Composable ColumnScope.() -> Unit) {
    val colors = ClenovaThemeTokens.colors
    Column(Modifier.fillMaxWidth()) {
        HorizontalDivider(color = colors.border)
        Text(stringResource(title), Modifier.padding(top = 19.dp, bottom = 7.dp), color = colors.textSecondary,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, letterSpacing = .8.sp))
        Column(Modifier.fillMaxWidth().padding(bottom = 12.dp), content = content)
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, title: Int, value: String? = null, dropdown: Boolean = false,
    tag: String? = null, onClick: (() -> Unit)? = null) {
    val colors = ClenovaThemeTokens.colors
    val enabled = onClick != null
    val tint = if (enabled) colors.textPrimary else colors.textSecondary
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(ClenovaShapes.tile)
        .then(if (tag != null) Modifier.testTag(tag) else Modifier)
        .clickable(enabled = enabled, role = Role.Button, onClick = { onClick?.invoke() }),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, null, Modifier.size(22.dp), tint = tint)
        Text(stringResource(title), Modifier.weight(1f), color = tint, style = MaterialTheme.typography.bodyLarge)
        if (value != null) Text(value, color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
        Icon(if (dropdown) Icons.Outlined.KeyboardArrowDown else Icons.Outlined.ChevronRight, null,
            Modifier.size(19.dp), tint = colors.textSecondary)
    }
}
