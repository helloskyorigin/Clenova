package com.clenova.app.feature.clean

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

@Composable
internal fun SmartScanScreen(
    onBack: () -> Unit,
    onSelectionHaptic: () -> Unit,
    onResult: (MediaScanResult?) -> Unit,
    activityLifecycleOwner: LifecycleOwner,
    onDuplicates: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val owner = activityLifecycleOwner
    val colors = ClenovaThemeTokens.colors
    val scope = rememberCoroutineScope()
    val scanner = remember(context) { MediaScanner(context.applicationContext) }
    var access by remember { mutableStateOf(mediaAccess(context)) }
    var result by remember { mutableStateOf<MediaScanResult?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }
    var running by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var stage by remember { mutableIntStateOf(0) }
    var done by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }
    var cancelPrompt by rememberSaveable { mutableStateOf(false) }
    var permissionAsked by rememberSaveable { mutableStateOf(false) }
    fun start() {
        if (running || !mediaAccess(context).any) return
        result = null; onResult(null); failed = false; stage = 0; done = 0; total = 0; running = true
        job = scope.launch {
            try {
                val scanned = scanner.scan { phase, completed, count ->
                    withContext(Dispatchers.Main) { stage = phase; done = completed; total = count }
                }
                result = scanned
                onResult(scanned)
                onSelectionHaptic()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                failed = true
                access = mediaAccess(context)
            } finally {
                running = false
            }
        }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        access = mediaAccess(context)
        if (access.any) start()
    }
    DisposableEffect(owner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                job?.cancel()
                result = null
                onResult(null)
            }
            if (event == Lifecycle.Event.ON_RESUME) {
                access = mediaAccess(context)
                if (!access.any || result?.access?.let { it != access } == true) {
                    job?.cancel(); result = null; onResult(null)
                }
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); job?.cancel() }
    }
    LaunchedEffect(Unit) { if (access.any) start() }
    val requestBack = { if (running) cancelPrompt = true else onBack() }
    BackHandler(enabled = running) { cancelPrompt = true }

    Box(modifier.fillMaxSize().background(colors.background).windowInsetsPadding(WindowInsets.displayCutout), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 560.dp).fillMaxSize().navigationBarsPadding().padding(horizontal = 24.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = requestBack, modifier = Modifier.testTag("scan_back")) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.cd_back_button), tint = colors.textPrimary)
                }
                Text(stringResource(R.string.home_smart_scan), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                if (result != null && !running) TextButton(onClick = { start() }, modifier = Modifier.testTag("scan_rescan")) { Text(stringResource(R.string.scan_rescan)) }
            }
            HorizontalDivider(color = colors.border)
            Crossfade(targetState = running, modifier = Modifier.weight(1f), animationSpec = tween(250), label = "real_scan") { scanning ->
                val snapshot = result
                when {
                    scanning -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        Box(Modifier.size(176.dp), contentAlignment = Alignment.Center) {
                            if (stage == 1 && total > 0) CircularProgressIndicator(progress = { done.toFloat() / total }, modifier = Modifier.fillMaxSize(), strokeWidth = 7.dp, color = colors.primary)
                            else CircularProgressIndicator(Modifier.fillMaxSize(), color = colors.primary, strokeWidth = 7.dp)
                            Icon(Icons.Outlined.CropFree, null, Modifier.size(40.dp), tint = colors.primary)
                        }
                        Text(stringResource(R.string.scan_running_title), style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
                        Text(stringResource(when(stage) { 0 -> R.string.scan_stage_inventory; 1 -> R.string.scan_stage_duplicates; else -> R.string.scan_stage_plan }), color = colors.textSecondary)
                        if (stage == 1 && total > 0) Text("$done / $total", color = colors.textSecondary)
                    }
                    snapshot != null -> ActualResults(snapshot, onDuplicates)
                    else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Outlined.VerifiedUser, null, Modifier.size(40.dp), tint = colors.primary)
                        Text(stringResource(if (failed) R.string.scan_read_error else R.string.scan_access_title), style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
                        Text(stringResource(R.string.scan_access_body), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                        if (!access.any) {
                            Button(onClick = { permissionAsked = true; launcher.launch(mediaPermissions()) }, shape = ClenovaShapes.pill, modifier = Modifier.fillMaxWidth().testTag("scan_allow")) { Text(stringResource(R.string.scan_allow_access)) }
                            if (permissionAsked) OutlinedButton(onClick = {
                                failed = runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))) }.isFailure
                            }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.permissions_manage)) }
                        } else Button(onClick = { start() }, modifier = Modifier.fillMaxWidth(), shape = ClenovaShapes.pill) { Text(stringResource(R.string.home_smart_scan)) }
                    }
                }
            }
            if (running) TextButton(onClick = { cancelPrompt = true }, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 12.dp)) { Text(stringResource(R.string.scan_cancel)) }
            else if (result != null) Button(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp).heightIn(min = 52.dp), shape = ClenovaShapes.pill) { Text(stringResource(R.string.review_done)) }
        }
    }
    if (cancelPrompt) AlertDialog(onDismissRequest = { cancelPrompt = false },
        title = { Text(stringResource(R.string.scan_cancel_title)) },
        text = { Text(stringResource(R.string.scan_cancel_body)) },
        confirmButton = { TextButton(onClick = { cancelPrompt = false; job?.cancel(); onBack() }) { Text(stringResource(R.string.scan_cancel)) } },
        dismissButton = { TextButton(onClick = { cancelPrompt = false }) { Text(stringResource(R.string.scan_continue)) } })
}

@Composable
private fun ActualResults(result: MediaScanResult, onDuplicates: () -> Unit) {
    val colors = ClenovaThemeTokens.colors
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(stringResource(R.string.scan_complete), style = MaterialTheme.typography.labelMedium, color = colors.success)
        Text(ByteFormatter.format(result.reviewBytes), Modifier.testTag("scan_result_total"), style = MaterialTheme.typography.displaySmall.copy(fontSize = 48.sp, lineHeight = 54.sp, fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
        Text(stringResource(R.string.scan_review_space), color = colors.textSecondary)
        Text(stringResource(R.string.scan_accessible_count, result.scanned), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
        if (!result.access.full || result.incomplete) Text(stringResource(R.string.scan_partial), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
        HorizontalDivider(color = colors.border)
        listOf(
            Triple(R.string.clean_tool_duplicates_title, result.extras.size, result.duplicateBytes),
            Triple(R.string.clean_tool_screenshots_title, result.screenshots.size, result.screenshots.sumOf { it.bytes }),
            Triple(R.string.review_videos, result.largeVideos.size, result.largeVideos.sumOf { it.bytes }),
        ).forEach { (title, count, bytes) ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(stringResource(title), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    Text(stringResource(R.string.guard_item_count, count), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                }
                Text(ByteFormatter.format(bytes), style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
            }
            HorizontalDivider(color = colors.border)
        }
        if (result.duplicateGroups.isNotEmpty()) OutlinedButton(onClick = onDuplicates, modifier = Modifier.fillMaxWidth().testTag("scan_duplicates")) { Text(stringResource(R.string.clean_tool_duplicates_title)) }
        Text(stringResource(R.string.scan_read_only), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
    }
}
