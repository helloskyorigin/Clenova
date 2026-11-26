package com.clenova.app.feature.guard

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Search
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.CancellationException
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
    var albums by remember(store) { mutableStateOf(runCatching { store.albums() }.getOrNull()) }
    var inventory by remember { mutableStateOf<MediaScanner.Inventory?>(null) }
    var loading by remember { mutableStateOf(false) }
    var refreshId by remember { mutableIntStateOf(0) }
    var albumMode by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var albumChoice by remember { mutableStateOf<String?>(null) }
    var albumRemove by remember { mutableStateOf<String?>(null) }
    val owner = LocalLifecycleOwner.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { refreshId++ }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshId++
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(refreshId) {
        inventory = null
        if (mediaAccess(context).any) {
            loading = true
            try { inventory = MediaScanner(context.applicationContext).inventory() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { failed = true }
            finally { loading = false }
        }
    }
    var picking by remember { mutableStateOf(false) }
    var selected by remember(inventory) { mutableStateOf(emptySet<Uri>()) }
    var removing by remember { mutableStateOf<Uri?>(null) }
    val saved = records
    val media = inventory?.items.orEmpty()
    val protected = remember(media, saved, albums) {
        if (saved == null || albums == null) emptySet() else saved.keys + media.filter { MemoryGuardStore.albumKey(it) in albums.orEmpty() }.map { it.uri }
    }
    val available = remember(media, protected) { media.filter { it.uri !in protected } }
    val grouped = remember(media) { media.mapNotNull { item -> MemoryGuardStore.albumKey(item)?.let { it to item } }.groupBy({ it.first }, { it.second }) }
    val shown = remember(available, search) { available.filter { it.name.contains(search, true) || it.folder.contains(search, true) } }
    val visibleCount = media.count { it.uri in protected }
    val canEdit = saved != null && albums != null
    val chosen = available.filter { it.uri in selected }
    fun refresh() {
        records = runCatching { store.records() }.getOrNull()
        albums = runCatching { store.albums() }.getOrNull()
        runCatching { store.protectedUris(result?.allMedia.orEmpty()) }.onSuccess(onChanged)
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
                if (!picking) IconButton(onClick = { picking = true }, enabled = canEdit) {
                    Icon(Icons.Outlined.Add, stringResource(R.string.guard_add_collection), tint = colors.primary)
                }
            }
            HorizontalDivider(color = colors.border)
            if (!canEdit || failed) Text(stringResource(R.string.guard_real_error), color = colors.error)
            if (picking) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !albumMode, onClick = { albumMode = false; search = "" }, label = { Text(stringResource(R.string.guard_real_items)) })
                    FilterChip(selected = albumMode, onClick = { albumMode = true; search = "" }, label = { Text(stringResource(R.string.guard_real_albums)) })
                }
                if (loading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Spacer(Modifier.weight(1f))
                } else if (inventory == null) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.guard_real_pick), color = colors.textSecondary)
                        Button(onClick = { if (mediaAccess(context).any) refreshId++ else permission.launch(mediaPermissions()) }, shape = ClenovaShapes.pill) { Text(stringResource(R.string.guard_real_browse)) }
                    }
                } else if (albumMode) {
                    OutlinedTextField(value = search, onValueChange = { search = it }, singleLine = true, modifier = Modifier.fillMaxWidth(), placeholder = { Text(stringResource(R.string.guard_real_search)) }, leadingIcon = { Icon(Icons.Outlined.Search, null) })
                    Text(stringResource(R.string.guard_real_album_scope), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(grouped.entries.filter { it.key !in albums.orEmpty() && MemoryGuardStore.albumLabel(it.key).contains(search, true) }.sortedByDescending { it.value.size }, key = { it.key }) { group ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Outlined.FolderOpen, null, tint = colors.primary)
                                Column(Modifier.weight(1f)) {
                                    Text(MemoryGuardStore.albumLabel(group.key), color = colors.textPrimary)
                                    Text(group.key.substringAfter('|'), maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.textSecondary, style = MaterialTheme.typography.labelSmall)
                                    Text(stringResource(R.string.guard_item_count, group.value.size), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                                }
                                TextButton(onClick = { albumChoice = group.key }, enabled = canEdit && albums.orEmpty().size < 200) { Text(stringResource(R.string.guard_protect_action)) }
                            }
                            HorizontalDivider(color = colors.border)
                        }
                    }
                } else {
                    OutlinedTextField(value = search, onValueChange = { search = it }, singleLine = true, modifier = Modifier.fillMaxWidth(), placeholder = { Text(stringResource(R.string.guard_real_search)) }, leadingIcon = { Icon(Icons.Outlined.Search, null) })
                    if (inventory?.incomplete == true || inventory?.access?.full != true) Text(stringResource(R.string.scan_partial), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.guard_item_count, chosen.size), color = colors.primary)
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(shown, key = { it.uri.toString() }) { item ->
                            val checked = item.uri in selected
                            Row(Modifier.fillMaxWidth().clip(ClenovaShapes.tile).toggleable(checked, role = Role.Checkbox,
                                enabled = canEdit && saved != null && (checked || selected.size + saved.size < MemoryGuardStore.LIMIT),
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
                    }, enabled = chosen.isNotEmpty() && canEdit, modifier = Modifier.fillMaxWidth().testTag("guard_real_protect"), shape = ClenovaShapes.pill) {
                        Text(stringResource(R.string.guard_protect_action))
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.VerifiedUser, null, Modifier.size(24.dp), tint = colors.primary)
                    Text(stringResource(R.string.guard_protection_label), color = colors.primary)
                }
                Text(if (canEdit && inventory != null) visibleCount.toString() else "—", Modifier.testTag("guard_count"), color = colors.textPrimary,
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 58.sp, fontWeight = FontWeight.SemiBold))
                Text(stringResource(R.string.guard_real_visible_count), color = colors.textSecondary)
                if (inventory?.incomplete == true || inventory?.access?.full == false) Text(stringResource(R.string.scan_partial), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                Text(stringResource(R.string.guard_real_count) + " · " + (saved.orEmpty().size + albums.orEmpty().size), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.guard_real_scope), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                Button(onClick = { selected = emptySet(); picking = true }, enabled = canEdit,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("guard_add"), shape = ClenovaShapes.pill) {
                    Icon(Icons.Outlined.Add, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.guard_add_collection))
                }
                if (saved.isNullOrEmpty() && albums.isNullOrEmpty()) Text(stringResource(R.string.guard_real_empty), color = colors.textSecondary)
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(albums.orEmpty().toList().sorted(), key = { "album:" + it }) { key ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Outlined.FolderOpen, null, tint = colors.primary)
                            Column(Modifier.weight(1f)) {
                                Text(MemoryGuardStore.albumLabel(key), color = colors.textPrimary)
                                Text(stringResource(R.string.guard_real_albums), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { albumRemove = key }) { Icon(Icons.Outlined.RemoveCircleOutline, stringResource(R.string.guard_remove_action)) }
                        }
                        HorizontalDivider(color = colors.border)
                    }
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
    albumChoice?.let { key ->
        AlertDialog(onDismissRequest = { albumChoice = null }, title = { Text(MemoryGuardStore.albumLabel(key)) },
            text = { Text(stringResource(R.string.guard_real_album_scope)) },
            confirmButton = { TextButton(onClick = {
                failed = !runCatching { store.addAlbum(key) }.getOrDefault(false)
                if (!failed) { refresh(); onHaptic() }
                albumChoice = null
            }) { Text(stringResource(R.string.guard_protect_action)) } },
            dismissButton = { TextButton(onClick = { albumChoice = null }) { Text(stringResource(android.R.string.cancel)) } })
    }
    albumRemove?.let { key ->
        AlertDialog(onDismissRequest = { albumRemove = null }, title = { Text(MemoryGuardStore.albumLabel(key)) },
            text = { Text(stringResource(R.string.guard_real_remove)) },
            confirmButton = { TextButton(onClick = {
                failed = !runCatching { store.removeAlbum(key) }.getOrDefault(false)
                if (!failed) { refresh(); onHaptic() }
                albumRemove = null
            }) { Text(stringResource(R.string.guard_remove_action)) } },
            dismissButton = { TextButton(onClick = { albumRemove = null }) { Text(stringResource(android.R.string.cancel)) } })
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
