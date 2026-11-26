package com.clenova.app.feature.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.clenova.app.core.designsystem.ClenovaThemeTokens
import com.example.R

internal data class RuntimePermission(val name: String, val label: String, val granted: Boolean)

@Suppress("DEPRECATION")
internal fun readRuntimePermissions(context: Context): Result<List<RuntimePermission>> = runCatching {
    val pm = context.packageManager
    val info = pm.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
    info.requestedPermissions.orEmpty().distinct().mapNotNull { name ->
        val permission = try { pm.getPermissionInfo(name, 0) } catch (_: PackageManager.NameNotFoundException) { return@mapNotNull null }
        if ((permission.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE) != PermissionInfo.PROTECTION_DANGEROUS) return@mapNotNull null
        RuntimePermission(name, permission.loadLabel(pm).toString(), context.checkSelfPermission(name) == PackageManager.PERMISSION_GRANTED)
    }.sortedBy { it.label }
}

@Composable
internal fun PermissionsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val colors = ClenovaThemeTokens.colors
    var permissions by remember(context) { mutableStateOf(readRuntimePermissions(context)) }
    var failed by remember { mutableStateOf(false) }
    DisposableEffect(owner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissions = readRuntimePermissions(context)
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_permissions)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.permissions_intro))
                val entries = permissions.getOrNull()
                when {
                    entries == null -> Text(stringResource(R.string.permissions_read_failed), color = colors.error)
                    entries.isEmpty() -> Text(stringResource(R.string.permissions_none), Modifier.testTag("permissions_empty"), color = colors.textSecondary)
                    else -> entries.forEach { entry ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(entry.label, Modifier.weight(1f))
                            Text(stringResource(if (entry.granted) R.string.permissions_granted else R.string.permissions_denied),
                                color = if (entry.granted) colors.success else colors.textSecondary)
                        }
                        HorizontalDivider(color = colors.border)
                    }
                }
                if (failed) Text(stringResource(R.string.settings_action_unavailable), color = colors.error)
            }
        },
        confirmButton = { TextButton(onClick = {
            failed = runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))) }.isFailure
        }, modifier = Modifier.testTag("permissions_manage")) { Text(stringResource(R.string.permissions_manage)) } },
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.testTag("permissions_back")) { Text(stringResource(android.R.string.cancel)) } })
}

@Composable
internal fun DataPrivacyDialog(onDismiss: () -> Unit) {
    val colors = ClenovaThemeTokens.colors
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_data_privacy)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                Text(stringResource(R.string.privacy_current_build), style = MaterialTheme.typography.titleMedium)
                listOf(
                    R.string.privacy_local_title to R.string.privacy_local_body,
                    R.string.privacy_files_title to R.string.privacy_files_body,
                    R.string.privacy_support_title to R.string.privacy_support_body,
                    R.string.settings_safety_approval_title to R.string.settings_safety_approval_desc,
                    R.string.settings_reset to R.string.settings_reset_confirm_body,
                ).forEach { (title, body) ->
                    Text(stringResource(title), style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                    Text(stringResource(body), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    HorizontalDivider(color = colors.border)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss, modifier = Modifier.testTag("privacy_back")) { Text(stringResource(R.string.review_done)) } })
}
