package com.clenova.app.feature.guard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clenova.app.core.designsystem.ClenovaShapes
import com.clenova.app.core.designsystem.ClenovaSpacing
import com.clenova.app.core.designsystem.ClenovaThemeTokens
import com.example.BuildConfig
import com.example.R

@Composable
fun GuardScreen(
    sample: Boolean,
    collectionMask: Int,
    onMaskChange: (Int) -> Unit,
    onSelectionHaptic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ClenovaThemeTokens.colors
    val preview = BuildConfig.DEBUG && sample
    val mask = if (preview) collectionMask and GuardDesignData.ALL_MASK else 0
    val count = GuardDesignData.count(mask)
    var adding by rememberSaveable { mutableStateOf(false) }
    var pendingMask by rememberSaveable { mutableIntStateOf(0) }
    var removing by rememberSaveable { mutableIntStateOf(0) }
    val active = GuardDesignData.collections.filter { mask and it.bit != 0 }
    val available = GuardDesignData.collections.filter { mask and it.bit == 0 }

    Box(modifier.fillMaxSize().background(colors.background).windowInsetsPadding(WindowInsets.displayCutout),
        contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 560.dp).fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp).padding(top = 24.dp, bottom = ClenovaSpacing.floatingNavBottomClearance)
            .navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(23.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.guard_title), Modifier.weight(1f), color = colors.textPrimary,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-.7).sp))
                if (preview) IconButton(onClick = { pendingMask = 0; adding = true }, enabled = available.isNotEmpty(),
                    modifier = Modifier.testTag("guard_add_top")) {
                    Icon(Icons.Outlined.Add, stringResource(R.string.guard_add_collection), tint = colors.primary)
                }
            }
            HorizontalDivider(color = colors.border.copy(alpha = .65f))
            if (preview) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        Icon(Icons.Outlined.VerifiedUser, null, Modifier.size(24.dp), tint = colors.success)
                        Text(stringResource(R.string.guard_protection_label), color = colors.success, style = MaterialTheme.typography.labelLarge)
                    }
                    Text(count.toString(), Modifier.testTag("guard_count"), color = colors.textPrimary,
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 58.sp, lineHeight = 63.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-2).sp))
                    Text(stringResource(R.string.guard_protected_label), color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.guard_protected_description), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                }
                Button(onClick = { pendingMask = 0; adding = true }, enabled = available.isNotEmpty(), shape = ClenovaShapes.pill,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("guard_add")) {
                    Icon(Icons.Outlined.Add, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.guard_add_collection))
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.guard_collections), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                        Text(active.size.toString(), color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                    }
                    if (active.isEmpty()) Text(stringResource(R.string.guard_empty_title), color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                    active.forEachIndexed { index, group ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            GuardCollectionIcon(group.type)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(stringResource(group.title), color = colors.textPrimary, style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(R.string.guard_item_count, group.count), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { removing = group.bit }, modifier = Modifier.testTag("guard_remove_${group.bit}")) {
                                Icon(Icons.Outlined.RemoveCircleOutline, stringResource(R.string.guard_remove_collection), tint = colors.textSecondary)
                            }
                        }
                        if (index < active.lastIndex) HorizontalDivider(color = colors.border.copy(alpha = .65f))
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.VerifiedUser, null, Modifier.size(38.dp), tint = colors.primary)
                    Text(stringResource(R.string.guard_empty_title), style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
                    Text(stringResource(R.string.guard_not_ready), color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Surface(shape = ClenovaShapes.card, color = colors.surface, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Lock, null, Modifier.size(25.dp), tint = colors.primary)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(stringResource(R.string.guard_approval_title), color = colors.textPrimary, style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.guard_approval_body), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
    if (preview && adding) AlertDialog(
        onDismissRequest = { adding = false },
        title = { Text(stringResource(R.string.guard_add_collection)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                if (available.isEmpty()) Text(stringResource(R.string.guard_no_more))
                available.forEach { group ->
                    val checked = pendingMask and group.bit != 0
                    Row(Modifier.fillMaxWidth().clip(ClenovaShapes.tile).clickable { pendingMask = pendingMask xor group.bit }
                        .testTag("guard_pick_${group.bit}").padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GuardCollectionIcon(group.type)
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(group.title), color = colors.textPrimary)
                            Text(stringResource(R.string.guard_item_count, group.count), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                        Checkbox(checked = checked, onCheckedChange = null)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onMaskChange(mask or pendingMask); onSelectionHaptic(); adding = false }, enabled = pendingMask != 0,
            modifier = Modifier.testTag("guard_add_confirm")) { Text(stringResource(R.string.guard_protect_action)) } },
        dismissButton = { TextButton(onClick = { adding = false }) { Text(stringResource(android.R.string.cancel)) } },
    )
    if (preview && removing != 0) {
        val group = GuardDesignData.collections.firstOrNull { it.bit == removing }
        if (group != null) AlertDialog(
            onDismissRequest = { removing = 0 },
            title = { Text(stringResource(R.string.guard_remove_collection)) },
            text = { Text(stringResource(R.string.guard_remove_body, stringResource(group.title))) },
            confirmButton = { TextButton(onClick = { onMaskChange(mask and removing.inv()); removing = 0; onSelectionHaptic() },
                modifier = Modifier.testTag("guard_remove_confirm")) { Text(stringResource(R.string.guard_remove_action)) } },
            dismissButton = { TextButton(onClick = { removing = 0 }) { Text(stringResource(android.R.string.cancel)) } },
        )
    }
}

@Composable
private fun GuardCollectionIcon(type: Int) {
    val colors = ClenovaThemeTokens.colors
    val icon = when (type) { 1 -> Icons.Outlined.VideoFile; 2 -> Icons.Outlined.Description; else -> Icons.Outlined.PhotoLibrary }
    Box(Modifier.size(46.dp).clip(ClenovaShapes.tile).background(colors.softBlue), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(23.dp), tint = colors.primary)
    }
}
