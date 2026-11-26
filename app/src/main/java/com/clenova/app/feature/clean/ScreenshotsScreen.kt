package com.clenova.app.feature.clean

import android.app.Activity
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.clenova.app.core.designsystem.ClenovaShapes
import com.clenova.app.core.designsystem.ClenovaThemeTokens
import com.clenova.app.core.utilities.ByteFormatter
import com.clenova.app.data.storage.*
import com.example.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
internal fun ScreenshotsScreen(result: MediaScanResult?, owner: LifecycleOwner, onBack: () -> Unit,
    onScan: () -> Unit, onInvalidate: () -> Unit, onHaptic: () -> Unit, largeMedia: Boolean = false, videosOnly: Boolean = false) {
    val context = LocalContext.current
    val colors = ClenovaThemeTokens.colors
    val scope = rememberCoroutineScope()
    val title = if (!largeMedia) R.string.clean_tool_screenshots_title else if (videosOnly) R.string.review_videos else R.string.clean_tool_large_files_title
    val items = (if (!largeMedia) result?.screenshots else if (videosOnly) result?.largeVideos else result?.largeFiles).orEmpty()
    val eligible = remember(result, largeMedia, videosOnly) { result?.let { snapshot ->
        (if (largeMedia) LargeMediaTrash.eligible(snapshot, videosOnly) else ScreenshotTrash.eligible(snapshot)).map { m -> m.uri }.toSet()
    }.orEmpty() }
    var selected by remember(result) { mutableStateOf(emptySet<Uri>()) }
    val chosen = selected.intersect(eligible)
    val bytes = items.filter { it.uri in chosen }.sumOf { it.bytes }
    val supported = Build.VERSION.SDK_INT >= 30
    val valid = result != null && runCatching { if (largeMedia) LargeMediaTrash.plan(result, chosen, videosOnly) else ScreenshotTrash.plan(result, chosen) }.isSuccess
    var confirming by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    var pending by rememberSaveable { mutableStateOf(false) }
    var pendingCount by rememberSaveable { mutableIntStateOf(0) }
    var pendingBytes by rememberSaveable { mutableLongStateOf(0L) }
    var outcome by rememberSaveable { mutableStateOf<Int?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }
    val busy = checking || pending
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        if (pending && it.resultCode == Activity.RESULT_OK) CleanupHistory(context).record(if (!largeMedia) CleanupKind.SCREENSHOTS else if (videosOnly) CleanupKind.LARGE_VIDEOS else CleanupKind.LARGE_FILES, pendingCount, pendingBytes)
        pending = false; pendingCount = 0; pendingBytes = 0L
        selected = emptySet()
        onInvalidate()
        outcome = if (it.resultCode == Activity.RESULT_OK) R.string.duplicates_trash_success else R.string.duplicates_trash_cancelled
        onHaptic()
    }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && !pending) {
                job?.cancel(); confirming = false; selected = emptySet()
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); job?.cancel() }
    }
    BackHandler(enabled = busy) {}
    Box(Modifier.fillMaxSize().background(colors.background).windowInsetsPadding(WindowInsets.displayCutout),
        contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 560.dp).fillMaxSize().navigationBarsPadding().padding(horizontal = 24.dp)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, enabled = !busy) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.cd_back_button), tint = colors.textPrimary)
                }
                Text(stringResource(title), Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                if (result != null) TextButton(onClick = onScan, enabled = !busy) { Text(stringResource(R.string.scan_rescan)) }
            }
            HorizontalDivider(color = colors.border)
            if (result == null || items.isEmpty()) {
                Column(Modifier.weight(1f).padding(vertical = 30.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(if (largeMedia) Icons.Outlined.FolderOpen else Icons.Outlined.Screenshot, null, Modifier.size(38.dp), tint = colors.primary)
                    Text(stringResource(if (result == null) R.string.review_empty_title else if (largeMedia) R.string.large_media_empty else R.string.screenshots_no_matches),
                        style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
                    Text(stringResource(if (result == null) R.string.review_empty_body else R.string.scan_read_only), color = colors.textSecondary)
                    if (result != null && (!result.access.full || result.incomplete)) Text(stringResource(R.string.scan_partial), color = colors.textSecondary)
                }
                Button(onClick = onScan, enabled = !busy, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    .testTag("screenshots_scan"), shape = ClenovaShapes.pill) { Text(stringResource(R.string.home_smart_scan)) }
            } else {
                Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(ByteFormatter.format(bytes), Modifier.testTag("screenshots_total"),
                        style = MaterialTheme.typography.displaySmall.copy(fontSize = 48.sp, fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                    Text(stringResource(R.string.duplicates_selected_space), color = colors.textSecondary)
                    Text(stringResource(R.string.guard_item_count, chosen.size), color = colors.primary)
                    Text(stringResource(if (largeMedia) R.string.large_media_rule else R.string.screenshots_manual_rule), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    Text(stringResource(R.string.duplicates_guard_note), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    if (!result.access.full || result.incomplete) Text(stringResource(R.string.scan_partial), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.guard_item_count, items.size), Modifier.weight(1f), color = colors.textPrimary)
                        TextButton(onClick = { selected = emptySet(); onHaptic() }, enabled = !busy && chosen.isNotEmpty()) {
                            Text(stringResource(R.string.review_deselect_all))
                        }
                    }
                }
                LazyVerticalGrid(columns = GridCells.Adaptive(if (largeMedia) 144.dp else 104.dp), modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(items, key = { it.uri.toString() }) { media ->
                        val locked = media.uri !in eligible
                        var previewReady by remember(media.uri) { mutableStateOf(false) }
                        val checked = media.uri in chosen
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.fillMaxWidth().aspectRatio(if (largeMedia) 1.25f else .78f).clip(RoundedCornerShape(10.dp)).background(colors.surface)
                                .then(if (locked) Modifier else Modifier.toggleable(checked,
                                    enabled = supported && !busy && (checked || (previewReady && chosen.size < (if (largeMedia) LargeMediaTrash.MAX_BATCH else ScreenshotTrash.MAX_BATCH))),
                                    role = Role.Checkbox, onValueChange = {
                                        selected = if (it) chosen + media.uri else chosen - media.uri
                                        onHaptic()
                                    })), contentAlignment = Alignment.Center) {
                                MediaThumbnail(media.uri, onReady = { previewReady = it }, fit = androidx.compose.ui.layout.ContentScale.Fit)
                                Surface(shape = ClenovaShapes.pill, color = if (checked) colors.primary else colors.background,
                                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(26.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(if (locked) Icons.Outlined.Lock else if (checked) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                            stringResource(if (locked) R.string.duplicates_keep else title),
                                            Modifier.size(18.dp), tint = if (checked) colors.onPrimary else colors.textSecondary)
                                    }
                                }
                            }
                            Text(media.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = colors.textPrimary)
                            Text(ByteFormatter.format(media.bytes), style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                        }
                    }
                }
                if (!supported) Text(stringResource(R.string.duplicates_trash_unsupported), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                if (!largeMedia && chosen.isNotEmpty() && !valid) Text(stringResource(R.string.duplicates_verify_limit), color = colors.error, style = MaterialTheme.typography.bodySmall)
                Button(onClick = { confirming = true }, enabled = supported && !busy && valid,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).heightIn(min = 52.dp).testTag("screenshots_approve"), shape = ClenovaShapes.pill) {
                    if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text(stringResource(R.string.duplicates_move_trash, chosen.size))
                }
            }
        }
    }
    if (confirming && result != null) AlertDialog(onDismissRequest = { confirming = false },
        title = { Text(stringResource(R.string.duplicates_move_trash, chosen.size)) },
        text = { Text(stringResource(if (largeMedia) R.string.large_media_confirm else R.string.similar_confirm)) },
        confirmButton = { TextButton(onClick = {
            confirming = false
            checking = true
            job = scope.launch {
                try {
                    val request = if (largeMedia) LargeMediaTrash.request(context, result, chosen, videosOnly) else ScreenshotTrash.request(context, result, chosen)
                    if (!owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return@launch
                    pendingCount = chosen.size; pendingBytes = bytes; pending = true
                    launcher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                    onInvalidate()
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) { pending = false; onInvalidate(); outcome = R.string.duplicates_trash_failed }
                finally { checking = false }
            }
        }) { Text(stringResource(android.R.string.ok)) } },
        dismissButton = { TextButton(onClick = { confirming = false }) { Text(stringResource(android.R.string.cancel)) } })
    outcome?.let { message ->
        AlertDialog(onDismissRequest = { outcome = null }, title = { Text(stringResource(message)) },
            confirmButton = { TextButton(onClick = { outcome = null; onScan() }) { Text(stringResource(R.string.scan_rescan)) } },
            dismissButton = { TextButton(onClick = { outcome = null }) { Text(stringResource(R.string.review_done)) } })
    }
}
