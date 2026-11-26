package com.clenova.app.feature.clean

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.media3.common.util.UnstableApi
import com.clenova.app.core.designsystem.ClenovaShapes
import com.clenova.app.core.designsystem.ClenovaThemeTokens
import com.clenova.app.core.utilities.ByteFormatter
import com.clenova.app.data.storage.*
import com.example.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal fun RealVideoCompressScreen(onBack: () -> Unit, onHaptic: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val colors = ClenovaThemeTokens.colors
    val scope = rememberCoroutineScope()
    var source by remember { mutableStateOf<VideoSource?>(null) }
    var preset by remember { mutableStateOf(VideoPreset.BALANCED) }
    var output by remember { mutableStateOf<EncodedVideo?>(null) }
    var saved by remember { mutableStateOf<Uri?>(null) }
    var phase by remember { mutableIntStateOf(0) }
    var progress by remember { mutableStateOf<Int?>(null) }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var cancelDialog by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    fun clearOutput() { output?.file?.delete(); output = null; saved = null }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            clearOutput(); source = null; failed = false; busy = true
            job = scope.launch {
                try { source = VideoCompression.inspect(context, uri); onHaptic() }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) { failed = true }
                finally { busy = false }
            }
        }
    }
    fun back() {
        if (busy) { if (phase == 2) cancelDialog = true; return }
        if (phase != 0) { clearOutput(); phase = 0; failed = false } else onBack()
    }
    DisposableEffect(Unit) { onDispose { job?.cancel(); output?.file?.delete() } }
    BackHandler(enabled = phase != 0 || busy) { back() }
    val selected = source
    val result = output
    Box(modifier.fillMaxSize().background(colors.background).windowInsetsPadding(WindowInsets.displayCutout), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 560.dp).fillMaxSize().navigationBarsPadding().padding(horizontal = 24.dp)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { back() }, enabled = !busy || phase == 2, modifier = Modifier.testTag("compress_back")) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.cd_back_button), tint = colors.textPrimary)
                }
                Text(stringResource(R.string.clean_tool_video_compress_title), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            }
            HorizontalDivider(color = colors.border)
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when (phase) {
                    0 -> {
                        Text(stringResource(R.string.compress_select_title), style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
                        if (Build.VERSION.SDK_INT < 29) Text(stringResource(R.string.compress_real_unsupported), color = colors.textSecondary)
                        if (selected != null) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(Modifier.size(width = 100.dp, height = 72.dp).clip(ClenovaShapes.tile).background(colors.surface)) { MediaThumbnail(selected.uri) }
                                Column(Modifier.weight(1f)) {
                                    Text(selected.name, maxLines = 2, overflow = TextOverflow.Ellipsis, color = colors.textPrimary)
                                    Text(ByteFormatter.format(selected.bytes), color = colors.textSecondary)
                                }
                            }
                        }
                        OutlinedButton(onClick = { picker.launch(arrayOf("video/*")) }, enabled = !busy && Build.VERSION.SDK_INT >= 29,
                            modifier = Modifier.fillMaxWidth().testTag("compress_pick")) { Text(stringResource(R.string.compress_real_pick)) }
                        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                    1 -> {
                        Text(stringResource(R.string.compress_quality_title), style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
                        Text(stringResource(R.string.compress_quality_body), color = colors.textSecondary)
                        VideoPreset.entries.forEach { option ->
                            val title = when (option) { VideoPreset.HIGH -> R.string.compress_high; VideoPreset.BALANCED -> R.string.compress_balanced; VideoPreset.SMALL -> R.string.compress_small }
                            val desc = when (option) { VideoPreset.HIGH -> R.string.compress_high_body; VideoPreset.BALANCED -> R.string.compress_balanced_body; VideoPreset.SMALL -> R.string.compress_small_body }
                            Surface(color = if (preset == option) colors.softBlue else colors.surface, shape = ClenovaShapes.card) {
                                Row(Modifier.fillMaxWidth().selectable(preset == option, role = Role.RadioButton, onClick = { preset = option; onHaptic() })
                                    .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(stringResource(title), color = colors.textPrimary, style = MaterialTheme.typography.titleMedium)
                                        Text(stringResource(desc), color = colors.textSecondary)
                                        selected?.let { Text("${presetHeight(option, it)}p", color = colors.primary) }
                                    }
                                    RadioButton(preset == option, null)
                                }
                            }
                        }
                        selected?.let { Text(stringResource(R.string.compress_total_original, ByteFormatter.format(it.bytes)), color = colors.textSecondary) }
                    }
                    2 -> {
                        Spacer(Modifier.height(22.dp))
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Box(Modifier.size(166.dp), contentAlignment = Alignment.Center) {
                                val p = progress
                                if (p == null) CircularProgressIndicator(Modifier.fillMaxSize(), color = colors.primary, strokeWidth = 8.dp)
                                else {
                                    CircularProgressIndicator(progress = { p / 100f }, modifier = Modifier.fillMaxSize(), color = colors.primary, strokeWidth = 8.dp)
                                    Text(stringResource(R.string.scan_percent, p), style = MaterialTheme.typography.headlineLarge)
                                }
                            }
                        }
                        Text(stringResource(R.string.compress_real_running), style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
                    }
                    3 -> if (result != null && selected != null) {
                        Icon(Icons.Outlined.CheckCircle, null, Modifier.size(48.dp), tint = colors.success)
                        Text(stringResource(if (saved != null) R.string.compress_real_saved else R.string.compress_real_ready), style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
                        Text(ByteFormatter.format(result.bytes), style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 40.sp), color = colors.textPrimary)
                        Text(stringResource(R.string.compress_total_original, ByteFormatter.format(selected.bytes)), color = colors.textSecondary)
                        Text(stringResource(R.string.compress_real_output, ByteFormatter.format(result.bytes)), color = colors.primary)
                        if (result.bytes >= selected.bytes) Text(stringResource(R.string.compress_real_not_smaller), color = colors.textSecondary)
                        else Text(stringResource(R.string.compress_real_reduction, ByteFormatter.format(selected.bytes - result.bytes)), color = colors.primary)
                        if (saved != null) Text("Movies/Clenova", color = colors.textSecondary)
                    }
                }
                if (failed) Text(stringResource(R.string.compress_real_error), color = colors.error)
                if (phase != 2) Surface(color = colors.surface, shape = ClenovaShapes.card) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.VerifiedUser, null, Modifier.size(24.dp), tint = colors.success)
                        Text(stringResource(R.string.compress_original_safe), color = colors.textSecondary)
                    }
                }
                if (phase == 3) Text(stringResource(R.string.compress_real_space_note), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Column(Modifier.fillMaxWidth().padding(bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (phase == 2) TextButton(onClick = { cancelDialog = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(stringResource(R.string.scan_cancel)) }
                else if (phase == 3 && saved != null) {
                    OutlinedButton(onClick = {
                        val uri = saved ?: return@OutlinedButton
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "video/mp4").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
                            .onFailure { failed = true }
                    }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.compress_real_open)) }
                    Button(onClick = { clearOutput(); onBack() }, modifier = Modifier.fillMaxWidth(), shape = ClenovaShapes.pill) { Text(stringResource(R.string.review_done)) }
                } else Button(onClick = {
                    onHaptic()
                    when (phase) {
                        0 -> { failed = false; phase = 1 }
                        1 -> {
                            val input = source ?: return@Button
                            val chosen = preset
                            failed = false; progress = null; busy = true; phase = 2
                            job = scope.launch {
                                try { output = VideoCompression.encode(context, input, chosen) { progress = it }; phase = 3; onHaptic() }
                                catch (e: CancellationException) { throw e }
                                catch (_: Exception) { failed = true; phase = 1 }
                                finally { busy = false }
                            }
                        }
                        3 -> {
                            val encoded = output ?: return@Button
                            failed = false; busy = true
                            job = scope.launch {
                                try { saved = VideoCompression.save(context, encoded); CleanupHistory(context).record(CleanupKind.COMPRESS, 1, encoded.bytes, source?.bytes ?: 0L); onHaptic() }
                                catch (e: CancellationException) { throw e }
                                catch (_: Exception) { failed = true }
                                finally { busy = false }
                            }
                        }
                    }
                }, enabled = !busy && selected != null && (phase != 3 || (result != null && result.bytes < selected.bytes)),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("compress_primary"), shape = ClenovaShapes.pill) {
                    if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text(stringResource(when (phase) { 0 -> R.string.compress_continue; 1 -> R.string.compress_real_start; else -> R.string.compress_real_save }))
                }
            }
        }
    }
    if (cancelDialog) AlertDialog(onDismissRequest = { cancelDialog = false }, title = { Text(stringResource(R.string.scan_cancel_title)) },
        text = { Text(stringResource(R.string.compress_original_safe)) },
        confirmButton = { TextButton(onClick = { cancelDialog = false; job?.cancel(); clearOutput(); phase = 1; progress = null }) { Text(stringResource(R.string.scan_cancel)) } },
        dismissButton = { TextButton(onClick = { cancelDialog = false }) { Text(stringResource(android.R.string.cancel)) } })
}

private fun presetHeight(preset: VideoPreset, source: VideoSource) = preset.height(source.height)
