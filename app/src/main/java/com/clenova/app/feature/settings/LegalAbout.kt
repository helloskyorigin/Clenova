package com.clenova.app.feature.settings

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clenova.app.core.designsystem.ClenovaThemeTokens
import com.example.BuildConfig
import com.example.R
import com.google.android.gms.oss.licenses.v2.OssLicensesMenuActivity

@Composable
internal fun LegalAboutDialog(kind: String, onDismiss: () -> Unit, onContact: () -> Unit) {
    val colors = ClenovaThemeTokens.colors
    val context = LocalContext.current
    var fontExpanded by rememberSaveable(kind) { mutableStateOf(false) }
    var failed by rememberSaveable(kind) { mutableStateOf(false) }
    val title = when (kind) {
        "policy" -> R.string.settings_policy
        "terms" -> R.string.settings_terms
        "licenses" -> R.string.settings_licenses
        else -> R.string.settings_about_product
    }
    val fontLicense = remember(context, fontExpanded) {
        if (fontExpanded) runCatching { context.resources.openRawResource(R.raw.manrope_ofl).bufferedReader().use { it.readText() } }.getOrNull() else null
    }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).testTag("legal_content"), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                when (kind) {
                    "policy" -> {
                        Text(stringResource(R.string.legal_revision), color = colors.textSecondary, style = MaterialTheme.typography.labelSmall)
                        Text(stringResource(R.string.legal_policy_intro))
                        LegalParagraph(R.string.privacy_local_title, R.string.privacy_local_body)
                        LegalParagraph(R.string.privacy_files_title, R.string.privacy_files_body)
                        LegalParagraph(R.string.privacy_support_title, R.string.legal_support_processing)
                        LegalParagraph(R.string.legal_retention_title, R.string.legal_retention_body)
                        LegalParagraph(R.string.settings_reset, R.string.settings_reset_confirm_body)
                        Text(SUPPORT_EMAIL, color = colors.primary)
                    }
                    "terms" -> {
                        Text(stringResource(R.string.legal_revision), color = colors.textSecondary, style = MaterialTheme.typography.labelSmall)
                        Text(stringResource(R.string.legal_terms_intro))
                        LegalParagraph(R.string.legal_use_title, R.string.legal_use_body)
                        LegalParagraph(R.string.settings_safety_approval_title, R.string.legal_cleanup_body)
                        LegalParagraph(R.string.legal_availability_title, R.string.legal_availability_body)
                        Text(stringResource(R.string.legal_rights_body))
                        Text(SUPPORT_EMAIL, color = colors.primary)
                    }
                    "licenses" -> {
                        Text(stringResource(R.string.legal_licenses_intro))
                        OutlinedButton(onClick = {
                            failed = runCatching {
                                OssLicensesMenuActivity.setActivityTitle(context.getString(R.string.settings_licenses))
                                context.startActivity(Intent(context, OssLicensesMenuActivity::class.java))
                            }.isFailure
                        }, modifier = Modifier.fillMaxWidth().testTag("licenses_dependencies")) {
                            Text(stringResource(R.string.legal_dependency_notices))
                        }
                        if (failed) Text(stringResource(R.string.settings_action_unavailable), color = colors.error)
                        HorizontalDivider(color = colors.border)
                        Text("Manrope", fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        Text("SIL Open Font License 1.1", color = colors.textSecondary)
                        TextButton(onClick = { fontExpanded = !fontExpanded }, modifier = Modifier.testTag("licenses_font")) {
                            Text(stringResource(if (fontExpanded) R.string.review_done else R.string.legal_read_license))
                        }
                        if (fontExpanded) Text(fontLicense ?: stringResource(R.string.settings_action_unavailable), style = MaterialTheme.typography.bodySmall)
                    }
                    else -> {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Image(painterResource(R.drawable.clenova_brand_logo), null, Modifier.size(72.dp))
                            Text(stringResource(R.string.home_brand), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.app_tagline), color = colors.textSecondary)
                            Text(stringResource(R.string.settings_version_short, BuildConfig.VERSION_NAME))
                            Text(BuildConfig.APPLICATION_ID, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                            Text(stringResource(R.string.settings_by), color = colors.textSecondary)
                            Text("SKYORIGIN", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            Text(SUPPORT_EMAIL, color = colors.primary, style = MaterialTheme.typography.bodySmall)
                            Text(stringResource(R.string.settings_copyright), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(stringResource(R.string.privacy_files_body), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss, modifier = Modifier.testTag("legal_done")) { Text(stringResource(R.string.review_done)) } },
        dismissButton = { if (kind != "licenses") TextButton(onClick = onContact) { Text(stringResource(R.string.settings_contact)) } })
}

@Composable
private fun LegalParagraph(title: Int, body: Int) {
    val colors = ClenovaThemeTokens.colors
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
        Text(stringResource(body), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
    }
}
