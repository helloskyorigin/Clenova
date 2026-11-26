package com.clenova.app.feature.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PersistableBundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.clenova.app.core.designsystem.ClenovaShapes
import com.clenova.app.core.designsystem.ClenovaThemeTokens
import com.example.BuildConfig
import com.example.R

internal object SupportMail {
    fun intent(subject: String, body: String): Intent = Intent(
        Intent.ACTION_SENDTO,
        Uri.parse("mailto:" + SUPPORT_EMAIL + "?subject=" + Uri.encode(subject) + "&body=" + Uri.encode(body)),
    )
    fun copy(context: Context, text: String): Boolean = runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Clenova", text)
        if (Build.VERSION.SDK_INT >= 24) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean("android.content.extra.IS_SENSITIVE", true)
            }
        }
        clipboard.setPrimaryClip(clip)
    }.isSuccess
}

@Composable
internal fun HelpCenterDialog(onDismiss: () -> Unit, onContact: () -> Unit) {
    val colors = ClenovaThemeTokens.colors
    var query by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableIntStateOf(-1) }
    val topics = listOf(
        R.string.support_faq_storage to R.string.support_faq_storage_body,
        R.string.settings_autopilot to R.string.support_faq_auto_body,
        R.string.guard_title to R.string.privacy_files_body,
        R.string.settings_permissions to R.string.permissions_intro,
        R.string.settings_data_privacy to R.string.privacy_local_body,
        R.string.settings_reset to R.string.settings_reset_confirm_body,
        R.string.settings_language to R.string.support_faq_language_body,
    )
    val visible = topics.mapIndexed { i, pair ->
        Triple(i, stringResource(pair.first), stringResource(pair.second))
    }.filter { (_, title, body) -> title.contains(query.trim(), ignoreCase = true) || body.contains(query.trim(), ignoreCase = true) }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_help)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = query, onValueChange = { query = it; expanded = -1 },
                    singleLine = true, placeholder = { Text(stringResource(R.string.support_search)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) }, shape = ClenovaShapes.tile,
                    modifier = Modifier.fillMaxWidth().testTag("help_search"))
                Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    if (visible.isEmpty()) Text(stringResource(R.string.support_no_results), color = colors.textSecondary)
                    visible.forEach { (index, title, body) ->
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { expanded = if (expanded == index) -1 else index }
                            .testTag("help_topic_$index"), verticalAlignment = Alignment.CenterVertically) {
                            Text(title, Modifier.weight(1f), color = colors.textPrimary, style = MaterialTheme.typography.titleSmall)
                            Icon(if (expanded == index) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
                        }
                        if (expanded == index) Text(body, Modifier.padding(bottom = 12.dp), color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                        HorizontalDivider(color = colors.border)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onContact, modifier = Modifier.testTag("help_contact")) { Text(stringResource(R.string.settings_contact)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.review_done)) } })
}

@Composable
internal fun SupportComposeDialog(kind: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val colors = ClenovaThemeTokens.colors
    val report = kind == "report"
    val title = when (kind) {
        "report" -> R.string.settings_report
        "suggest" -> R.string.settings_suggest
        else -> R.string.settings_contact
    }
    val subject = stringResource(when (kind) {
        "report" -> R.string.settings_report_subject
        "suggest" -> R.string.settings_suggest_subject
        else -> R.string.settings_support_subject
    })
    var message by remember(kind) { mutableStateOf("") }
    var includeDetails by rememberSaveable(kind) { mutableStateOf(false) }
    var failed by rememberSaveable(kind) { mutableStateOf(false) }
    var copied by rememberSaveable(kind) { mutableStateOf(false) }
    val diagnostics = listOf(
        stringResource(R.string.settings_version_short, BuildConfig.VERSION_NAME),
        stringResource(R.string.settings_report_android, Build.VERSION.RELEASE),
        stringResource(R.string.settings_report_device, Build.MANUFACTURER + " " + Build.MODEL),
    ).joinToString("\n")
    val body = if (report && includeDetails) message.trim() + "\n\n" + diagnostics else message.trim()
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(SUPPORT_EMAIL, color = colors.primary, style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.privacy_support_body), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = message, onValueChange = { message = it.take(4000); copied = false; failed = false },
                    label = { Text(stringResource(R.string.support_message)) }, minLines = 4, maxLines = 6,
                    modifier = Modifier.fillMaxWidth().testTag("support_message"), shape = ClenovaShapes.tile)
                if (report) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = includeDetails, onCheckedChange = { includeDetails = it; copied = false }, modifier = Modifier.testTag("support_diagnostics"))
                        Text(stringResource(R.string.support_include_details), Modifier.weight(1f))
                    }
                    if (includeDetails) Text(diagnostics, color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                }
                if (failed) Text(stringResource(R.string.support_email_unavailable), color = colors.error, style = MaterialTheme.typography.bodySmall)
                if (copied) Text(stringResource(R.string.support_copied), color = colors.success, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { copied = SupportMail.copy(context, SUPPORT_EMAIL + "\n" + subject + "\n\n" + body); failed = !copied },
                    enabled = message.isNotBlank(), modifier = Modifier.testTag("support_copy")) {
                    Text(stringResource(R.string.support_copy))
                }
            }
        },
        confirmButton = { TextButton(enabled = message.isNotBlank(), modifier = Modifier.testTag("support_open_email"), onClick = {
            failed = runCatching { context.startActivity(SupportMail.intent(subject, body)) }.isFailure
            if (!failed) onDismiss()
        }) { Text(stringResource(R.string.support_open_email)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } })
}
