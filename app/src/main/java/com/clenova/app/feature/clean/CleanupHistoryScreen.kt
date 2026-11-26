package com.clenova.app.feature.clean

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.clenova.app.core.designsystem.ClenovaThemeTokens
import com.clenova.app.core.utilities.ByteFormatter
import com.clenova.app.data.storage.*
import com.example.R
import java.util.Date

@Composable
internal fun CleanupHistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = ClenovaThemeTokens.colors
    val history = remember(context) { CleanupHistory(context) }
    var records by remember(history) { mutableStateOf(history.records()) }
    var confirm by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(colors.background).windowInsetsPadding(WindowInsets.displayCutout), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 560.dp).fillMaxSize().navigationBarsPadding().padding(horizontal = 24.dp)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("history_back")) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.cd_back_button), tint = colors.textPrimary)
                }
                Text(stringResource(R.string.clean_history), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                if (records.isNotEmpty()) TextButton(onClick = { confirm = true }) { Text(stringResource(R.string.history_clear)) }
            }
            HorizontalDivider(color = colors.border)
            if (failed) Text(stringResource(R.string.history_write_failed), color = colors.error)
            Text(stringResource(R.string.history_note), modifier = Modifier.padding(vertical = 16.dp), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            if (records.isEmpty()) {
                Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.History, null, Modifier.size(40.dp), tint = colors.primary)
                    Text(stringResource(R.string.clean_history_empty), style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary, modifier = Modifier.testTag("history_empty"))
                }
            } else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(records) { item ->
                    val title = when (item.kind) {
                        CleanupKind.DUPLICATES -> R.string.clean_tool_duplicates_title
                        CleanupKind.SIMILAR -> R.string.clean_tool_similar_title
                        CleanupKind.SCREENSHOTS -> R.string.clean_tool_screenshots_title
                        CleanupKind.LARGE_FILES -> R.string.clean_tool_large_files_title
                        CleanupKind.LARGE_VIDEOS -> R.string.review_videos
                        CleanupKind.SWIPE -> R.string.clean_tool_swipe_clean_title
                        CleanupKind.COMPRESS -> R.string.clean_tool_video_compress_title
                    }
                    val date = Date(item.timestamp)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(title), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                            Text(stringResource(if (item.kind == CleanupKind.COMPRESS) R.string.compress_real_saved else R.string.history_trashed), color = colors.success, style = MaterialTheme.typography.bodySmall)
                            Text(DateFormat.getDateFormat(context).format(date) + " · " + DateFormat.getTimeFormat(context).format(date), color = colors.textSecondary, style = MaterialTheme.typography.labelSmall)
                            Text(stringResource(R.string.guard_item_count, item.count), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(ByteFormatter.format(item.bytes), color = colors.textPrimary)
                            if (item.kind == CleanupKind.COMPRESS) Text(stringResource(R.string.compress_real_reduction, ByteFormatter.format(item.originalBytes - item.bytes)), style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                        }
                    }
                    HorizontalDivider(color = colors.border)
                }
            }
        }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false },
        title = { Text(stringResource(R.string.history_clear)) },
        text = { Text(stringResource(R.string.history_clear_note)) },
        confirmButton = { TextButton(onClick = { confirm = false; failed = !history.clear(); records = history.records() }) { Text(stringResource(android.R.string.ok)) } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(android.R.string.cancel)) } })
}
