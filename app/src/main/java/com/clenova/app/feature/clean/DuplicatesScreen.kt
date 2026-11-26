package com.clenova.app.feature.clean

import android.app.Activity
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
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
internal fun DuplicatesScreen(
    result: MediaScanResult?,
    activityLifecycleOwner: LifecycleOwner,
    onBack: () -> Unit,
    onScan: () -> Unit,
    onInvalidate: () -> Unit,
    onSelectionHaptic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = ClenovaThemeTokens.colors
    val scope = rememberCoroutineScope()
    val groups = result?.duplicateGroups.orEmpty()
    val allowed = remember(result) { DuplicateTrash.allowed(groups) }
    var selected by remember(result) { mutableStateOf(emptySet<Uri>()) }
    val actualSelection = selected.intersect(allowed)
    val selectedBytes = result?.extras.orEmpty().filter { it.uri in actualSelection }.sumOf { it.bytes }
    var confirming by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    var pending by rememberSaveable { mutableStateOf(false) }
    var outcome by rememberSaveable { mutableStateOf<Int?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }
    val supported = Build.VERSION.SDK_INT >= 30
    val busy = checking || pending
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { response ->
        pending = false
        selected = emptySet()
        onInvalidate()
        outcome = if (response.resultCode == Activity.RESULT_OK) R.string.duplicates_trash_success else R.string.duplicates_trash_cancelled
        onSelectionHaptic()
    }
    DisposableEffect(activityLifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && !pending) {
                job?.cancel()
                confirming = false
                selected = emptySet()
            }
        }
        activityLifecycleOwner.lifecycle.addObserver(observer)
        onDispose { activityLifecycleOwner.lifecycle.removeObserver(observer); job?.cancel() }
    }
    BackHandler(enabled = busy) {}
    Box(modifier.fillMaxSize().background(colors.background).windowInsetsPadding(WindowInsets.displayCutout), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 560.dp).fillMaxSize().navigationBarsPadding().padding(horizontal = 24.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, enabled = !busy, modifier = Modifier.testTag("duplicates_back")) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.cd_back_button), tint = colors.textPrimary)
                }
                Text(stringResource(R.string.clean_tool_duplicates_title), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                if (result != null) TextButton(onClick = onScan, enabled = !busy) { Text(stringResource(R.string.scan_rescan)) }
            }
            HorizontalDivider(color = colors.border)
            if (result == null || groups.isEmpty()) {
                Column(Modifier.weight(1f).fillMaxWidth().padding(vertical = 30.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (busy) CircularProgressIndicator(color = colors.primary)
                    else {
                        Icon(Icons.Outlined.ContentCopy, null, Modifier.size(38.dp), tint = colors.primary)
                        Text(stringResource(if (result == null) R.string.review_empty_title else R.string.duplicates_no_matches), style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
                        Text(stringResource(if (result == null) R.string.review_empty_body else R.string.scan_read_only), color = colors.textSecondary)
                        if (result != null && (!result.access.full || result.incomplete)) Text(stringResource(R.string.scan_partial), color = colors.textSecondary)
                    }
                }
                Button(onClick = onScan, enabled = !busy, modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp).heightIn(min = 52.dp).testTag("duplicates_scan"), shape = ClenovaShapes.pill) { Text(stringResource(R.string.home_smart_scan)) }
            } else {
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    item {
                        Text(ByteFormatter.format(selectedBytes), Modifier.testTag("duplicates_total"), style = MaterialTheme.typography.displaySmall.copy(fontSize = 48.sp, lineHeight = 54.sp, fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                        Text(stringResource(R.string.scan_review_space), color = colors.textSecondary)
                        Text(stringResource(R.string.duplicates_summary, actualSelection.size, groups.size), color = colors.primary, style = MaterialTheme.typography.bodySmall)
                        if (!result.access.full || result.incomplete) Text(stringResource(R.string.scan_partial), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    item {
                        Text(stringResource(R.string.duplicates_keep_rule), color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(R.string.duplicates_batch_note), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                        Text(stringResource(R.string.guard_not_ready), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.duplicates_groups), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            TextButton(enabled = !busy && supported && allowed.isNotEmpty(), onClick = {
                                selected = if (actualSelection.isNotEmpty()) emptySet() else allowed.take(DuplicateTrash.MAX_BATCH).toSet()
                                onSelectionHaptic()
                            }, modifier = Modifier.testTag("duplicates_select_all")) {
                                Text(stringResource(if (actualSelection.isNotEmpty()) R.string.review_deselect_all else R.string.duplicates_select_batch))
                            }
                        }
                    }
                    itemsIndexed(groups, key = { _, group -> group.first().uri.toString() }) { index, group ->
                        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            Text(stringResource(R.string.duplicates_group_title, index + 1), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                            Text(stringResource(R.string.guard_item_count, group.size) + " · " + ByteFormatter.format(group.first().bytes), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                itemsIndexed(group, key = { _, media -> media.uri.toString() }) { i, media ->
                                    val keep = i == 0 || media.favorite
                                    val checked = media.uri in actualSelection
                                    Column(Modifier.width(112.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(colors.surface)
                                            .then(if (keep) Modifier else Modifier.toggleable(value = checked,
                                                enabled = supported && !busy && (checked || actualSelection.size < DuplicateTrash.MAX_BATCH),
                                                role = Role.Checkbox, onValueChange = { value ->
                                                    selected = if (value) actualSelection + media.uri else actualSelection - media.uri
                                                    onSelectionHaptic()
                                                })), contentAlignment = Alignment.Center) {
                                            MediaThumbnail(media.uri)
                                            Surface(color = if (checked) colors.primary else colors.background, shape = ClenovaShapes.pill,
                                                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(26.dp)) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(if (keep) Icons.Outlined.Lock else if (checked) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, null,
                                                        Modifier.size(18.dp), tint = if (checked) colors.onPrimary else colors.textSecondary)
                                                }
                                            }
                                        }
                                        Text(media.name, maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.textPrimary, style = MaterialTheme.typography.bodySmall)
                                        Text(stringResource(if (keep) R.string.duplicates_keep else R.string.duplicates_copy, i + 1), color = colors.textSecondary, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                            HorizontalDivider(color = colors.border)
                        }
                    }
                }
                if (!supported) Text(stringResource(R.string.duplicates_trash_unsupported), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                Button(enabled = supported && !busy && actualSelection.isNotEmpty(), onClick = { confirming = true },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).heightIn(min = 52.dp).testTag("duplicates_approve"), shape = ClenovaShapes.pill) {
                    if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text(stringResource(R.string.duplicates_move_trash, actualSelection.size))
                }
            }
        }
    }
    if (confirming && result != null) AlertDialog(onDismissRequest = { confirming = false },
        title = { Text(stringResource(R.string.duplicates_move_trash, actualSelection.size)) },
        text = { Text(stringResource(R.string.duplicates_trash_confirm)) },
        confirmButton = { TextButton(onClick = {
            confirming = false
            val snapshot = result
            val chosen = actualSelection
            checking = true
            job = scope.launch {
                try {
                    val request = DuplicateTrash.request(context, snapshot, chosen)
                    if (!activityLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return@launch
                    pending = true
                    launcher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                    onInvalidate()
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) { pending = false; onInvalidate(); outcome = R.string.duplicates_trash_failed }
                finally { checking = false }
            }
        }, modifier = Modifier.testTag("duplicates_confirm")) { Text(stringResource(android.R.string.ok)) } },
        dismissButton = { TextButton(onClick = { confirming = false }) { Text(stringResource(android.R.string.cancel)) } })
    outcome?.let { message ->
        AlertDialog(onDismissRequest = { outcome = null }, title = { Text(stringResource(message)) },
            confirmButton = { TextButton(onClick = { outcome = null; onScan() }) { Text(stringResource(R.string.scan_rescan)) } },
            dismissButton = { TextButton(onClick = { outcome = null }) { Text(stringResource(R.string.review_done)) } })
    }
}

/** Deterministic local artwork used only for visual layout review, not device photo thumbnails. */
@Composable
internal fun DuplicateArtwork(group: Int, modifier: Modifier, variant: Int = 0) {
    val palettes = listOf(
        listOf(Color(0xFFB8DCE9), Color(0xFF689E92), Color(0xFF285F6D)),
        listOf(Color(0xFFF3D3AD), Color(0xFFBD8673), Color(0xFF795865)),
        listOf(Color(0xFFC0D4F0), Color(0xFF8799BA), Color(0xFF4D6581)),
        listOf(Color(0xFFD0E5C8), Color(0xFF83A881), Color(0xFF4D715C)),
    )
    val p = palettes[group % palettes.size]
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(listOf(p[0], p[1])))
        drawCircle(Color(0xFFFDF3D5), radius = size.width * .14f, center = Offset(size.width * (.72f - variant * .06f), size.height * (.22f + variant * .025f)))
        drawPath(Path().apply {
            moveTo(0f, size.height * .68f); lineTo(size.width * .29f, size.height * .32f)
            lineTo(size.width * .52f, size.height * .6f); lineTo(size.width * .76f, size.height * .43f)
            lineTo(size.width, size.height * .68f); lineTo(size.width, size.height); lineTo(0f, size.height); close()
        }, p[1])
        drawPath(Path().apply {
            moveTo(0f, size.height * .82f); lineTo(size.width * .4f, size.height * .57f)
            lineTo(size.width * .7f, size.height * .8f); lineTo(size.width, size.height * .65f)
            lineTo(size.width, size.height); lineTo(0f, size.height); close()
        }, p[2])
    }
}
