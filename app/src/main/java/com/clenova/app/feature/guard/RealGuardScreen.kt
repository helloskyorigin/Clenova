package com.clenova.app.feature.guard

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.RemoveCircleOutline
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
import com.clenova.app.core.designsystem.ClenovaShapes
import com.clenova.app.core.designsystem.ClenovaSpacing
import com.clenova.app.core.designsystem.ClenovaThemeTokens
import com.clenova.app.core.utilities.ByteFormatter
import com.clenova.app.data.storage.*
import com.clenova.app.feature.clean.MediaThumbnail
import com.example.R

@Composable
internal fun RealGuardScreen(result: MediaScanResult?, onScan: () -> Unit,
    onChanged: (Set<Uri>) -> Unit, onHaptic: () -> Unit) {
    val context = LocalContext.current
    val colors = ClenovaThemeTokens.colors
    val store = remember(context) { MemoryGuardStore(context) }
    var records by remember(store) { mutableStateOf(runCatching { store.records() }.getOrNull()) }
    var failed by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    var selected by remember(result) { mutableStateOf(emptySet<Uri>()) }
    var removing by remember { mutableStateOf<Uri?>(null) }
    val saved = records
    val available = remember(result, saved) { result?.allMedia.orEmpty().filter { it.uri !in saved.orEmpty() } }
    val chosen = available.filter { it.uri in selected }
    fun refresh() {
        records = runCatching { store.records() }.getOrNull()
        records?.let { onChanged(it.keys) }
    }
    BackHandler(enabled = picking) { picking = false; selected = emptySet() }
    Box(Modifier.fillMaxSize().background(colors.background).windowInsetsPadding(WindowInsets.displayCutout), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 560.dp).fillMaxSize().navigationBarsPadding()
            .padding(horizontal = 24.dp).padding(top = 24.dp, bottom = ClenovaSpacing.floatingNavBottomClearance),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (picking) IconButton(onClick = { picking = false; selected = emptySet() }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.cd_back_button), tint = colors.textPrimary)
                }
                Text(stringResource(if (picking) R.string.guard_add_collection else R.string.guard_title),
                    Modifier.weight(1f), color = colors.textPrimary, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold))
                if (!picking) IconButton(onClick = { picking = true }, enabled = saved != null && saved.size < MemoryGuardStore.LIMIT) {
                    Icon(Icons.Outlined.Add, stringResource(R.string.guard_add_collection), tint = colors.primary)
                }
            }
            HorizontalDivider(color = colors.border)
            if (saved == null || failed) Text(stringResource(R.string.guard_real_error), color = colors.error)
            if (picking) {
                Text(stringResource(R.string.guard_real_pick), color = colors.textSecondary)
                if (result == null || available.isEmpty()) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(if (result == null) R.string.review_empty_title else R.string.guard_no_more), color = colors.textSecondary)
                        Button(onClick = onScan, shape = ClenovaShapes.pill) { Text(stringResource(R.string.home_smart_scan)) }
                    }
                } else {
                    if (result.incomplete || !result.access.full) Text(stringResource(R.string.scan_partial), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.guard_item_count, chosen.size), color = colors.primary)
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(available, key = { it.uri.toString() }) { item ->
                            val checked = item.uri in selected
                            Row(Modifier.fillMaxWidth().clip(ClenovaShapes.tile).toggleable(checked, role = Role.Checkbox,
                                enabled = saved != null && (checked || selected.size + saved.size < MemoryGuardStore.LIMIT),
                                onValueChange = { selected = if (it) selected + item.uri else selected - item.uri; onHaptic() }).padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(Modifier.size(52.dp).clip(ClenovaShapes.tile).background(colors.surface)) { MediaThumbnail(item.uri) }
                                Column(Modifier.weight(1f)) {
                                    Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.textPrimary)
                                    Text(ByteFormatter.format(item.bytes), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                                }
                                Checkbox(checked, onCheckedChange = null)
                            }
                        }
                    }
                    Button(onClick = {
                        failed = !runCatching { store.add(chosen) }.getOrDefault(false)
                        if (!failed) { refresh(); selected = emptySet(); picking = false; onHaptic() }
                    }, enabled = chosen.isNotEmpty() && saved != null, modifier = Modifier.fillMaxWidth().testTag("guard_real_protect"), shape = ClenovaShapes.pill) {
                        Text(stringResource(R.string.guard_protect_action))
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.VerifiedUser, null, Modifier.size(24.dp), tint = colors.primary)
                    Text(stringResource(R.string.guard_protection_label), color = colors.primary)
                }
                Text(saved?.size?.toString() ?: "—", Modifier.testTag("guard_count"), color = colors.textPrimary,
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 58.sp, fontWeight = FontWeight.SemiBold))
                Text(stringResource(R.string.guard_real_count), color = colors.textSecondary)
                Text(stringResource(R.string.guard_real_scope), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                Button(onClick = { selected = emptySet(); picking = true }, enabled = saved != null && saved.size < MemoryGuardStore.LIMIT,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("guard_add"), shape = ClenovaShapes.pill) {
                    Icon(Icons.Outlined.Add, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.guard_add_collection))
                }
                if (saved.isNullOrEmpty()) Text(stringResource(R.string.guard_real_empty), color = colors.textSecondary)
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(saved.orEmpty().entries.toList(), key = { it.key.toString() }) { entry ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Outlined.Lock, null, tint = colors.primary)
                            Text(entry.value, Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis, color = colors.textPrimary)
                            IconButton(onClick = { removing = entry.key }) {
                                Icon(Icons.Outlined.RemoveCircleOutline, stringResource(R.string.guard_remove_action), tint = colors.textSecondary)
                            }
                        }
                        HorizontalDivider(color = colors.border)
                    }
                }
                Surface(shape = ClenovaShapes.card, color = colors.surface) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.Lock, null, tint = colors.primary)
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.guard_approval_title), color = colors.textPrimary)
                            Text(stringResource(R.string.guard_approval_body), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                    }
                }
            }
        }
    }
    removing?.let { uri ->
        AlertDialog(onDismissRequest = { removing = null }, title = { Text(stringResource(R.string.guard_remove_collection)) },
            text = { Text(stringResource(R.string.guard_real_remove)) },
            confirmButton = { TextButton(onClick = {
                failed = !runCatching { store.remove(uri) }.getOrDefault(false)
                if (!failed) { refresh(); onHaptic() }
                removing = null
            }) { Text(stringResource(R.string.guard_remove_action)) } },
            dismissButton = { TextButton(onClick = { removing = null }) { Text(stringResource(android.R.string.cancel)) } })
    }
}
