package com.clenova.app.feature.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.window.PopupProperties
import com.clenova.app.core.localization.AppLanguages
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

internal const val SUPPORT_EMAIL = "hello.skyorigin@gmail.com"

@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    onSelectTheme: (ThemePreference) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    onOpenAutopilot: () -> Unit,
    onResetPreferences: suspend () -> Unit,
    showUnfinishedRows: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = ClenovaThemeTokens.colors
    val context = LocalContext.current
    var themeMenu by remember { mutableStateOf(false) }
    var languageMenu by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    val configuration = LocalConfiguration.current
    val localeTag = remember(context, configuration) { AppLanguages.current(context) }
    val languageLabel = AppLanguages.supported.firstOrNull { it.tag == localeTag }?.nativeName
        ?: stringResource(R.string.settings_language_system)
    var languageFailed by remember { mutableStateOf(false) }
    var actionFailed by rememberSaveable { mutableStateOf(false) }
    var detail by rememberSaveable { mutableStateOf("") }
    var resetting by remember { mutableStateOf(false) }
    var resetResult by rememberSaveable { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()
    BackHandler(enabled = resetting) { }
    val themeLabel = when (preferences.themePreference) {
        ThemePreference.LIGHT -> R.string.settings_theme_light
        ThemePreference.DARK -> R.string.settings_theme_dark
        ThemePreference.SYSTEM -> R.string.settings_theme_system
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
                        modifier = Modifier.widthIn(min = 196.dp, max = 260.dp).background(colors.surfaceElevated),
                        properties = PopupProperties(focusable = true)) {
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
                Box(Modifier.fillMaxWidth()) {
                    SettingsRow(Icons.Outlined.Language, R.string.settings_language, value = languageLabel,
                        dropdown = true, tag = "settings_language", onClick = { search = ""; languageMenu = true })
                    DropdownMenu(expanded = languageMenu, onDismissRequest = { languageMenu = false },
                        modifier = Modifier.widthIn(min = 250.dp, max = 320.dp).heightIn(max = 420.dp).background(colors.surfaceElevated),
                        properties = PopupProperties(focusable = true)) {
                        OutlinedTextField(value = search, onValueChange = { search = it }, singleLine = true,
                            placeholder = { Text(stringResource(R.string.settings_language_search)) },
                            leadingIcon = { Icon(Icons.Outlined.Search, null) },
                            shape = ClenovaShapes.tile,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).testTag("language_search"))
                        if (search.isBlank()) DropdownMenuItem(
                            text = { Text(stringResource(R.string.settings_language_system), color = colors.textPrimary) },
                            onClick = { languageMenu = false; languageFailed = !AppLanguages.select(context, "") },
                            trailingIcon = { if (localeTag.isEmpty()) Icon(Icons.Outlined.Check, null, tint = colors.primary) },
                            modifier = Modifier.testTag("language_system"))
                        val matches = AppLanguages.supported.filter {
                            it.nativeName.contains(search.trim(), ignoreCase = true) || it.englishName.contains(search.trim(), ignoreCase = true)
                        }
                        matches.forEach { language ->
                            DropdownMenuItem(
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(language.nativeName, color = colors.textPrimary, style = MaterialTheme.typography.bodyLarge)
                                        if (language.nativeName != language.englishName) Text(language.englishName, color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                                    }
                                },
                                trailingIcon = { if (localeTag == language.tag) Icon(Icons.Outlined.Check, null, tint = colors.primary) },
                                onClick = { languageMenu = false; languageFailed = !AppLanguages.select(context, language.tag) },
                                modifier = Modifier.testTag("language_${language.tag}"))
                        }
                        if (matches.isEmpty()) Text(stringResource(R.string.settings_language_no_results),
                            color = colors.textSecondary, modifier = Modifier.padding(16.dp))
                    }
                }
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
                SettingsRow(Icons.Outlined.VerifiedUser, R.string.settings_permissions, tag = "settings_permissions", onClick = { detail = "permissions" })
                SettingsRow(Icons.Outlined.Lock, R.string.settings_data_privacy, tag = "settings_privacy", onClick = { detail = "privacy" })
                SettingsRow(Icons.Outlined.Refresh, R.string.settings_reset, tag = "settings_reset", onClick = { detail = "reset" })
            }
            SettingsSection(R.string.settings_support) {
                SettingsRow(Icons.Outlined.HelpOutline, R.string.settings_help, tag = "settings_help", onClick = { detail = "help" })
                SettingsRow(Icons.Outlined.Email, R.string.settings_contact, tag = "settings_contact", onClick = { detail = "contact" })
                SettingsRow(Icons.Outlined.BugReport, R.string.settings_report, tag = "settings_report", onClick = { detail = "report" })
                SettingsRow(Icons.Outlined.ChatBubbleOutline, R.string.settings_suggest, tag = "settings_suggest", onClick = { detail = "suggest" })
            }
            SettingsSection(R.string.settings_about_section) {
                SettingsRow(Icons.Outlined.Description, R.string.settings_policy, tag = "settings_policy", onClick = { detail = "policy" })
                SettingsRow(Icons.Outlined.FactCheck, R.string.settings_terms, tag = "settings_terms", onClick = { detail = "terms" })
                SettingsRow(Icons.Outlined.Code, R.string.settings_licenses, tag = "settings_licenses", onClick = { detail = "licenses" })
                SettingsRow(Icons.Outlined.Info, R.string.settings_about_product, tag = "settings_about", onClick = { detail = "about" })
            }
            HorizontalDivider(color = colors.border)
            Column(Modifier.fillMaxWidth().padding(top = 30.dp, bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(stringResource(R.string.settings_by), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                Text("SKYORIGIN", color = colors.textPrimary, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 3.sp))
                Text(SUPPORT_EMAIL, color = colors.textSecondary, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clip(ClenovaShapes.tile).clickable { detail = "contact" }.padding(4.dp))
                Text(stringResource(R.string.settings_copyright), color = colors.textSecondary, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
    if (detail in listOf("policy", "terms", "licenses", "about")) LegalAboutDialog(
        kind = detail, onDismiss = { detail = "" }, onContact = { detail = "contact" },
    )
    if (detail == "help") HelpCenterDialog(onDismiss = { detail = "" }, onContact = { detail = "contact" })
    if (detail in listOf("contact", "report", "suggest")) SupportComposeDialog(kind = detail, onDismiss = { detail = "" })
    if (detail == "permissions") PermissionsDialog(onDismiss = { detail = "" })
    if (detail == "privacy") DataPrivacyDialog(onDismiss = { detail = "" })
    if (detail == "reset") AlertDialog(
        onDismissRequest = { if (!resetting) detail = "" },
        title = { Text(stringResource(R.string.settings_reset_confirm_title)) },
        text = { Text(stringResource(R.string.settings_reset_confirm_body)) },
        confirmButton = {
            TextButton(enabled = !resetting, modifier = Modifier.testTag("settings_reset_confirm"), onClick = {
                if (!resetting) {
                    resetting = true
                    scope.launch {
                        try {
                            onResetPreferences()
                            detail = ""
                            resetResult = R.string.settings_reset_success
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            detail = ""
                            resetResult = R.string.settings_reset_failure
                        } finally { resetting = false }
                    }
                }
            }) {
                if (resetting) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text(stringResource(R.string.settings_reset))
            }
        },
        dismissButton = { TextButton(enabled = !resetting, onClick = { detail = "" }) { Text(stringResource(android.R.string.cancel)) } },
    )
    resetResult?.let { result ->
        AlertDialog(onDismissRequest = { resetResult = null },
            title = { Text(stringResource(result)) },
            confirmButton = { TextButton(onClick = { resetResult = null }, modifier = Modifier.testTag("settings_reset_done")) { Text(stringResource(android.R.string.ok)) } })
    }
    if (languageFailed) AlertDialog(onDismissRequest = { languageFailed = false },
        title = { Text(stringResource(R.string.settings_language_failed)) },
        confirmButton = { TextButton(onClick = { languageFailed = false }) { Text(stringResource(android.R.string.ok)) } })
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
