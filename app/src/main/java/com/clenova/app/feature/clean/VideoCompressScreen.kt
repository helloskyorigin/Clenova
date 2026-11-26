package com.clenova.app.feature.clean

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clenova.app.core.designsystem.ClenovaShapes
import com.clenova.app.core.designsystem.ClenovaThemeTokens
import com.example.BuildConfig
import com.example.R
import kotlinx.coroutines.delay

@Composable
fun VideoCompressScreen(sample: Boolean, onBack: () -> Unit, onSelectionHaptic: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ClenovaThemeTokens.colors
    val preview = BuildConfig.DEBUG && sample
    if (!preview) {
        RealVideoCompressScreen(onBack, onSelectionHaptic, modifier)
        return
    }
    var selected by rememberSaveable { mutableStateOf(intArrayOf(0)) }
    var qualityName by rememberSaveable { mutableStateOf(CompressDesignData.Quality.BALANCED.name) }
    var phase by rememberSaveable { mutableIntStateOf(0) }
    var progress by rememberSaveable { mutableIntStateOf(0) }
    var cancelDialog by rememberSaveable { mutableStateOf(false) }
    val quality = CompressDesignData.Quality.valueOf(qualityName)
    val ids = remember(selected) { selected.toSet() }
    val original = CompressDesignData.originalMb(ids)
    val estimated = CompressDesignData.estimateMb(ids, quality)
    val back: () -> Unit = {
        when (phase) {
            2 -> cancelDialog = true
            1, 3 -> { phase = 0; progress = 0 }
            else -> onBack()
        }
    }
    BackHandler(enabled = phase != 0) { back() }
    LaunchedEffect(phase, cancelDialog, preview) {
        if (preview && phase == 2 && !cancelDialog) {
            while (progress < 100) { delay(40); progress = (progress + 1).coerceAtMost(100) }
            phase = 3
        }
    }
    Box(modifier.fillMaxSize().background(colors.background).windowInsetsPadding(WindowInsets.displayCutout), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 560.dp).fillMaxSize().navigationBarsPadding().padding(horizontal = 24.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (phase == 2) cancelDialog = true else if (phase == 1) phase = 0 else back()
                }, modifier = Modifier.testTag("compress_back")) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.cd_back_button), tint = colors.textPrimary)
                }
                Text(stringResource(R.string.clean_tool_video_compress_title), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
            }
            HorizontalDivider(color = colors.border.copy(alpha = .65f))
            if (!preview) {
                Column(Modifier.weight(1f).fillMaxWidth().padding(vertical = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.compress_unavailable_title), style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
                    Text(stringResource(R.string.compress_unavailable_body), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                }
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp), shape = ClenovaShapes.pill) { Text(stringResource(R.string.review_done)) }
            } else {
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                    when (phase) {
                        0 -> {
                            Text(stringResource(R.string.compress_select_title), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                            Text(stringResource(R.string.compress_select_body), color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.compress_selected, ids.size), Modifier.weight(1f), color = colors.primary, style = MaterialTheme.typography.labelLarge)
                                TextButton(onClick = { selected = if (ids.size == CompressDesignData.videos.size) intArrayOf() else CompressDesignData.videos.map { it.id }.toIntArray(); onSelectionHaptic() }) {
                                    Text(stringResource(if (ids.size == CompressDesignData.videos.size) R.string.review_deselect_all else R.string.review_select_all))
                                }
                            }
                            CompressDesignData.videos.forEach { video ->
                                Row(Modifier.fillMaxWidth().clip(ClenovaShapes.tile).testTag("compress_video_${video.id}")
                                    .toggleable(value = video.id in ids, role = Role.Checkbox, onValueChange = { checked ->
                                        selected = (if (checked) ids + video.id else ids - video.id).sorted().toIntArray()
                                        onSelectionHaptic()
                                    }).padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    VideoArtwork(video.id, video.duration)
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(video.name, color = colors.textPrimary, style = MaterialTheme.typography.titleSmall)
                                        Text(video.resolution + " · " + CompressDesignData.label(video.mb), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Checkbox(checked = video.id in ids, onCheckedChange = null)
                                }
                                HorizontalDivider(color = colors.border.copy(alpha = .65f))
                            }
                        }
                        1 -> {
                            Text(stringResource(R.string.compress_quality_title), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                            Text(stringResource(R.string.compress_quality_body), color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                            CompressDesignData.Quality.entries.forEach { option ->
                                val title = when (option) { CompressDesignData.Quality.HIGH -> R.string.compress_high; CompressDesignData.Quality.BALANCED -> R.string.compress_balanced; CompressDesignData.Quality.SMALL -> R.string.compress_small }
                                val description = when (option) { CompressDesignData.Quality.HIGH -> R.string.compress_high_body; CompressDesignData.Quality.BALANCED -> R.string.compress_balanced_body; CompressDesignData.Quality.SMALL -> R.string.compress_small_body }
                                Surface(color = if (quality == option) colors.softBlue else colors.surface, shape = ClenovaShapes.card, modifier = Modifier.fillMaxWidth()) {
                                    Row(Modifier.selectable(selected = quality == option, role = Role.RadioButton, onClick = { qualityName = option.name; onSelectionHaptic() })
                                        .testTag("compress_quality_${option.name}").padding(15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                            Text(stringResource(title), color = colors.textPrimary, style = MaterialTheme.typography.titleMedium)
                                            Text(stringResource(description), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                                        }
                                        RadioButton(selected = quality == option, onClick = null)
                                    }
                                }
                            }
                            EstimatePanel(original, estimated)
                        }
                        2 -> {
                            val animated by animateFloatAsState(progress / 100f, label = "compression_progress")
                            Spacer(Modifier.height(28.dp))
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Box(Modifier.size(166.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(progress = { animated }, modifier = Modifier.fillMaxSize(), color = colors.primary, trackColor = colors.border, strokeWidth = 8.dp)
                                    Text(stringResource(R.string.scan_percent, progress), color = colors.textPrimary, style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.SemiBold))
                                }
                            }
                            Text(stringResource(R.string.compress_running), style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
                            Text(stringResource(R.string.compress_original_safe), color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                        }
                        3 -> {
                            Icon(Icons.Outlined.CheckCircle, null, Modifier.size(48.dp), tint = colors.success)
                            Text(stringResource(R.string.compress_estimate_ready), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                            EstimatePanel(original, estimated)
                            Text(stringResource(R.string.compress_no_output), color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    if (phase != 2) Surface(color = colors.surface, shape = ClenovaShapes.card, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Outlined.VerifiedUser, null, Modifier.size(24.dp), tint = colors.success)
                            Text(stringResource(R.string.compress_original_safe), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (phase == 0) Text(stringResource(R.string.compress_total_original, CompressDesignData.label(original)), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                    if (phase == 2) TextButton(onClick = { cancelDialog = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(stringResource(R.string.scan_cancel)) }
                    else Button(onClick = {
                        onSelectionHaptic()
                        when (phase) { 0 -> phase = 1; 1 -> { progress = 0; phase = 2 }; 3 -> onBack() }
                    }, enabled = ids.isNotEmpty(), modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("compress_primary"), shape = ClenovaShapes.pill) {
                        Text(when (phase) { 0 -> stringResource(R.string.compress_continue); 1 -> stringResource(R.string.compress_start); else -> stringResource(R.string.review_done) })
                    }
                }
            }
        }
    }
    if (cancelDialog) AlertDialog(onDismissRequest = { cancelDialog = false }, title = { Text(stringResource(R.string.compress_cancel_title)) },
        text = { Text(stringResource(R.string.compress_original_safe)) },
        confirmButton = { TextButton(onClick = { cancelDialog = false; phase = 1; progress = 0 }) { Text(stringResource(R.string.scan_cancel)) } },
        dismissButton = { TextButton(onClick = { cancelDialog = false }) { Text(stringResource(android.R.string.cancel)) } })
}

@Composable
private fun EstimatePanel(original: Int, estimated: Int) {
    val colors = ClenovaThemeTokens.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HorizontalDivider(color = colors.border)
        Text(stringResource(R.string.compress_estimated_savings), color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
        Text(CompressDesignData.label(original - estimated), Modifier.testTag("compress_savings"), color = colors.textPrimary, style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 40.sp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.compress_total_original, CompressDesignData.label(original)), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.compress_estimated_output, CompressDesignData.label(estimated)), color = colors.primary, style = MaterialTheme.typography.bodySmall)
        }
        Row(Modifier.fillMaxWidth().height(10.dp).clip(ClenovaShapes.pill).background(colors.border)) {
            if (original > 0 && estimated > 0) Box(Modifier.fillMaxWidth((estimated.toFloat() / original).coerceIn(0f, 1f)).fillMaxHeight().background(colors.primary))
        }
    }
}

@Composable
private fun VideoArtwork(id: Int, duration: String) {
    val palette = listOf(Color(0xFF4B7D96), Color(0xFFB48065), Color(0xFF66896D), Color(0xFF706C9B))
    Box(Modifier.size(width = 91.dp, height = 66.dp).clip(ClenovaShapes.tile), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val color = palette[id % palette.size]
            drawRect(Brush.verticalGradient(listOf(color.copy(alpha = .65f), color)))
            drawCircle(Color(0xFFF4DBA5), size.height * .12f, Offset(size.width * .76f, size.height * .24f))
            drawPath(Path().apply { moveTo(0f, size.height); lineTo(size.width * .32f, size.height * .35f); lineTo(size.width * .65f, size.height * .72f); lineTo(size.width, size.height * .42f); lineTo(size.width, size.height); close() }, Color(0xFF253B47).copy(alpha = .65f))
        }
        Icon(Icons.Outlined.PlayArrow, null, Modifier.size(24.dp), tint = Color.White)
        Text(duration, Modifier.align(Alignment.BottomEnd).padding(4.dp).background(Color.Black.copy(alpha = .65f), ClenovaShapes.pill).padding(horizontal = 4.dp), color = Color.White, fontSize = 10.sp)
    }
}
