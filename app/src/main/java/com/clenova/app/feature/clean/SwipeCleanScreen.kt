package com.clenova.app.feature.clean

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.clenova.app.core.designsystem.ClenovaShapes
import com.clenova.app.core.designsystem.ClenovaThemeTokens
import com.clenova.app.core.utilities.ByteFormatter
import com.clenova.app.data.storage.*
import com.example.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun SwipeCleanScreen(result: MediaScanResult?, owner: LifecycleOwner, onBack: () -> Unit,
    onScan: () -> Unit, onInvalidate: () -> Unit, onHaptic: () -> Unit) {
    val context = LocalContext.current
    val colors = ClenovaThemeTokens.colors
    val scope = rememberCoroutineScope()
    val photos = remember(result) { result?.photos.orEmpty().distinctBy { it.uri } }
    val allowed = remember(result) { result?.let(SwipeTrash::eligible).orEmpty() }
    var session by remember(result) { mutableStateOf(SwipeSession()) }
    var reviewing by remember(result) { mutableStateOf(false) }
    val current = photos.getOrNull(session.index)
    val chosen = session.selected.intersect(allowed)
    val selectedItems = photos.filter { it.uri in chosen }
    val supported = Build.VERSION.SDK_INT >= 30
    var checking by remember { mutableStateOf(false) }
    var pending by rememberSaveable { mutableStateOf(false) }
    var pendingCount by rememberSaveable { mutableIntStateOf(0) }
    var pendingBytes by rememberSaveable { mutableLongStateOf(0L) }
    var confirming by remember { mutableStateOf(false) }
    var outcome by rememberSaveable { mutableStateOf<Int?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }
    val busy = checking || pending
    val decoded by produceState<Pair<Uri, Bitmap>?>(null, current?.uri, reviewing) {
        value = null
        val uri = current?.uri
        if (!reviewing && uri != null && Build.VERSION.SDK_INT >= 28) {
            value = withContext(Dispatchers.IO) {
                try {
                    val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        val max = maxOf(info.size.width, info.size.height)
                        decoder.setTargetSampleSize(maxOf(1, (max + 1079) / 1080))
                    }
                    uri to bitmap
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) { null }
            }
        }
    }
    val preview = decoded?.takeIf { it.first == current?.uri && !reviewing }?.second
    val canMark = supported && !busy && current != null && current.uri in allowed &&
        chosen.size < SwipeTrash.MAX_BATCH && preview != null
    fun decide(mark: Boolean) {
        val media = current ?: return
        if (busy || reviewing || (mark && !canMark)) return
        session = session.decide(media.uri, mark, allowed)
        onHaptic()
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        if (pending && it.resultCode == Activity.RESULT_OK) CleanupHistory(context).record(CleanupKind.SWIPE, pendingCount, pendingBytes)
        pending = false; pendingCount = 0; pendingBytes = 0L
        session = SwipeSession()
        reviewing = false
        onInvalidate()
        outcome = if (it.resultCode == Activity.RESULT_OK) R.string.duplicates_trash_success else R.string.duplicates_trash_cancelled
        onHaptic()
    }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && !pending) {
                job?.cancel(); confirming = false; reviewing = false; session = SwipeSession()
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); job?.cancel() }
    }
    BackHandler(enabled = busy || reviewing) { if (!busy) reviewing = false }
    val threshold = with(LocalDensity.current) { 84.dp.toPx() }
    Box(Modifier.fillMaxSize().background(colors.background).windowInsetsPadding(WindowInsets.displayCutout),
        contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 560.dp).fillMaxSize().navigationBarsPadding().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (reviewing) reviewing = false else onBack() }, enabled = !busy) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.cd_back_button), tint = colors.textPrimary)
                }
                Text(stringResource(R.string.clean_tool_swipe_clean_title), Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                if (result != null) TextButton(onClick = onScan, enabled = !busy) { Text(stringResource(R.string.scan_rescan)) }
            }
            HorizontalDivider(color = colors.border)
            if (result == null || photos.isEmpty()) {
                Column(Modifier.weight(1f).padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Image, null, Modifier.size(38.dp), tint = colors.primary)
                    Text(stringResource(if (result == null) R.string.review_empty_title else R.string.swipe_empty),
                        style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
                    Text(stringResource(R.string.swipe_rule), color = colors.textSecondary)
                    if (result != null && (!result.access.full || result.incomplete)) Text(stringResource(R.string.scan_partial), color = colors.textSecondary)
                }
                Button(onClick = onScan, enabled = !busy, shape = ClenovaShapes.pill,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) { Text(stringResource(R.string.home_smart_scan)) }
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${session.index} / ${photos.size}", Modifier.weight(1f), color = colors.textPrimary)
                    Text(ByteFormatter.format(selectedItems.sumOf { it.bytes }), color = colors.primary)
                }
                LinearProgressIndicator(progress = { session.index.toFloat() / photos.size }, modifier = Modifier.fillMaxWidth(), color = colors.primary)
                if (!result.access.full || result.incomplete) Text(stringResource(R.string.scan_partial), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                if (!supported) Text(stringResource(R.string.duplicates_trash_unsupported), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                if (reviewing) {
                    Text(stringResource(R.string.swipe_review), style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
                    Text(stringResource(R.string.guard_item_count, chosen.size), color = colors.primary)
                    Text(stringResource(R.string.duplicates_guard_note), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(selectedItems, key = { it.uri.toString() }) { item ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(Modifier.size(62.dp).clip(RoundedCornerShape(8.dp))) { MediaThumbnail(item.uri) }
                                Column(Modifier.weight(1f)) {
                                    Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.textPrimary)
                                    Text(ByteFormatter.format(item.bytes), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                                }
                                TextButton(onClick = { session = session.unmark(item.uri); onHaptic() }, enabled = !busy) { Text(stringResource(R.string.duplicates_keep)) }
                            }
                        }
                    }
                    Button(onClick = { confirming = true }, enabled = supported && !busy && chosen.isNotEmpty(), shape = ClenovaShapes.pill,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).testTag("swipe_approve")) {
                        if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Text(stringResource(R.string.duplicates_move_trash, chosen.size))
                    }
                } else {
                    Text(stringResource(R.string.swipe_rule), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    if (current != null) {
                        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.surface)
                            .pointerInput(current.uri, canMark, busy) {
                                var drag = 0f
                                detectHorizontalDragGestures(onDragStart = { drag = 0f }, onDragCancel = { drag = 0f },
                                    onDragEnd = {
                                        if (drag > threshold) decide(false)
                                        else if (drag < -threshold && canMark) decide(true)
                                        drag = 0f
                                    }) { change, amount -> change.consume(); drag += amount }
                            }.testTag("swipe_photo"), contentAlignment = Alignment.Center) {
                            val bitmap = preview
                            if (bitmap != null) Image(bitmap.asImageBitmap(), contentDescription = current.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                            else Text(stringResource(R.string.swipe_preview_unavailable), color = colors.textSecondary, modifier = Modifier.padding(16.dp))
                        }
                        Text(current.name, maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.textPrimary)
                        Text(ByteFormatter.format(current.bytes), color = colors.textSecondary)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { decide(true) }, enabled = canMark, modifier = Modifier.weight(1f).testTag("swipe_mark")) { Text(stringResource(R.string.swipe_mark)) }
                            Button(onClick = { decide(false) }, enabled = !busy, modifier = Modifier.weight(1f).testTag("swipe_keep"), shape = ClenovaShapes.pill) { Text(stringResource(R.string.duplicates_keep)) }
                        }
                    } else {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.swipe_finished), style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { session = session.undo(); onHaptic() }, enabled = !busy && session.index > 0) { Text(stringResource(R.string.swipe_undo)) }
                        Button(onClick = { reviewing = true }, enabled = !busy && chosen.isNotEmpty(),
                            modifier = Modifier.weight(1f).padding(bottom = 12.dp).testTag("swipe_review"), shape = ClenovaShapes.pill) {
                            Text(stringResource(R.string.swipe_review) + " (" + chosen.size + ")")
                        }
                    }
                }
            }
        }
    }
    if (confirming && result != null) AlertDialog(onDismissRequest = { confirming = false },
        title = { Text(stringResource(R.string.duplicates_move_trash, chosen.size)) },
        text = { Text(stringResource(R.string.large_media_confirm)) },
        confirmButton = { TextButton(onClick = {
            confirming = false; checking = true
            job = scope.launch {
                try {
                    val request = SwipeTrash.request(context, result, chosen)
                    if (!owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return@launch
                    pendingCount = chosen.size; pendingBytes = selectedItems.sumOf { it.bytes }; pending = true
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
