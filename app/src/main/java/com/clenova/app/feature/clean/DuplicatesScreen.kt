package com.clenova.app.feature.clean

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.VerifiedUser
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clenova.app.core.designsystem.ClenovaShapes
import com.clenova.app.core.designsystem.ClenovaThemeTokens
import com.example.BuildConfig
import com.example.R

@Composable
fun DuplicatesScreen(
    sample: Boolean,
    onBack: () -> Unit,
    onSelectionHaptic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ClenovaThemeTokens.colors
    val preview = BuildConfig.DEBUG && sample
    var selected by rememberSaveable { mutableStateOf(DuplicateDesignData.removableIds.toIntArray()) }
    var approved by rememberSaveable { mutableStateOf(false) }
    var confirm by rememberSaveable { mutableStateOf(false) }
    val selectedSet = remember(selected) { selected.toSet() }
    val amount = DuplicateDesignData.label(DuplicateDesignData.selectedBytes(selectedSet))
    val allSelected = selectedSet.size == DuplicateDesignData.removableIds.size

    Box(modifier.fillMaxSize().background(colors.background).windowInsetsPadding(WindowInsets.displayCutout),
        contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 560.dp).fillMaxSize().navigationBarsPadding().padding(horizontal = 24.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("duplicates_back")) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.cd_back_button), tint = colors.textPrimary)
                }
                Text(stringResource(R.string.clean_tool_duplicates_title), Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
            }
            HorizontalDivider(color = colors.border.copy(alpha = .65f))
            if (!preview) {
                Column(Modifier.weight(1f).fillMaxWidth().padding(vertical = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.duplicates_unavailable_title), style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
                    Text(stringResource(R.string.duplicates_unavailable_body), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                }
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp), shape = ClenovaShapes.pill) {
                    Text(stringResource(R.string.review_done))
                }
            } else {
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(amount, Modifier.testTag("duplicates_total"), style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 53.sp, lineHeight = 59.sp, letterSpacing = (-2).sp, fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                            Text(stringResource(if (approved) R.string.review_approved_estimate else R.string.duplicates_selected_space), color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                            Text(stringResource(R.string.duplicates_summary, selectedSet.size, DuplicateDesignData.groups.size), color = colors.primary, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Outlined.VerifiedUser, null, Modifier.size(24.dp), tint = colors.success)
                            Text(stringResource(R.string.duplicates_keep_rule), Modifier.weight(1f), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                        HorizontalDivider(Modifier.padding(top = 15.dp), color = colors.border)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.duplicates_groups), Modifier.weight(1f), color = colors.textPrimary, style = MaterialTheme.typography.titleLarge)
                            if (!approved) TextButton(onClick = {
                                selected = if (allSelected) intArrayOf() else DuplicateDesignData.removableIds.toIntArray()
                                onSelectionHaptic()
                            }, modifier = Modifier.testTag("duplicates_select_all")) {
                                Text(stringResource(if (allSelected) R.string.review_deselect_all else R.string.review_select_all))
                            }
                        }
                    }
                    items(DuplicateDesignData.groups, key = { it.id }) { group ->
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(stringResource(R.string.duplicates_group_title, group.id + 1), color = colors.textPrimary, style = MaterialTheme.typography.titleMedium)
                                    Text(stringResource(R.string.duplicates_group_detail, group.copies, DuplicateDesignData.label(group.bytesPerCopy)), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                                }
                                Text(DuplicateDesignData.label(group.bytesPerCopy * group.removableIds.count { it in selectedSet }), color = colors.primary, style = MaterialTheme.typography.labelLarge)
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(group.copies) { copy ->
                                    val id = group.id * 10 + copy
                                    val keep = copy == 0
                                    DuplicateTile(group.id, copy, keep, id in selectedSet, approved) {
                                        if (!keep && !approved) {
                                            selected = (if (id in selectedSet) selectedSet - id else selectedSet + id).sorted().toIntArray()
                                            onSelectionHaptic()
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(color = colors.border.copy(alpha = .65f))
                        }
                    }
                    if (approved) item {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.CheckCircle, null, tint = colors.success)
                            Text(stringResource(R.string.review_approval_only), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (approved) Text(stringResource(R.string.review_approved), color = colors.success, style = MaterialTheme.typography.labelMedium)
                    Button(onClick = { if (approved) onBack() else confirm = true }, enabled = approved || selectedSet.isNotEmpty(),
                        shape = ClenovaShapes.pill, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("duplicates_approve"),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary)) {
                        Text(if (approved) stringResource(R.string.review_done) else stringResource(R.string.review_approve_space, amount))
                    }
                    if (approved) TextButton(onClick = { approved = false; onSelectionHaptic() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text(stringResource(R.string.review_revise))
                    }
                }
            }
        }
    }
    if (preview && confirm && selectedSet.isNotEmpty() && !approved) {
        AlertDialog(onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.review_confirm_title)) },
            text = { Text(stringResource(R.string.review_confirm_body, amount)) },
            confirmButton = { TextButton(onClick = { confirm = false; approved = true; onSelectionHaptic() }, modifier = Modifier.testTag("duplicates_confirm")) { Text(stringResource(R.string.review_confirm_action)) } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(android.R.string.cancel)) } })
    }
}

@Composable
private fun DuplicateTile(group: Int, copy: Int, keep: Boolean, selected: Boolean, approved: Boolean, onToggle: () -> Unit) {
    val colors = ClenovaThemeTokens.colors
    val label = stringResource(if (keep) R.string.duplicates_keep else R.string.duplicates_copy, copy)
    Column(Modifier.width(105.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.fillMaxWidth().height(103.dp).clip(RoundedCornerShape(14.dp))
            .then(if (!keep) Modifier.testTag("duplicate_${group * 10 + copy}").toggleable(value = selected, enabled = !approved, role = Role.Checkbox, onValueChange = { onToggle() }) else Modifier)
            .semantics { contentDescription = label }) {
            DuplicateArtwork(group, Modifier.fillMaxSize())
            if (keep) {
                Surface(color = colors.background, shape = ClenovaShapes.pill, modifier = Modifier.align(Alignment.TopStart).padding(6.dp)) {
                    Row(Modifier.padding(horizontal = 7.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Outlined.Lock, null, Modifier.size(12.dp), tint = colors.success)
                        Text(stringResource(R.string.duplicates_keep), style = MaterialTheme.typography.labelSmall, color = colors.success)
                    }
                }
            } else {
                Surface(shape = ClenovaShapes.pill, color = if (selected) colors.primary else colors.background,
                    modifier = Modifier.align(Alignment.TopEnd).padding(7.dp).size(25.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        if (selected) Icon(Icons.Outlined.CheckCircle, null, Modifier.size(20.dp), tint = colors.onPrimary)
                    }
                }
            }
        }
        Text(label, color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
    }
}

/** Deterministic local artwork used only for visual layout review, not device photo thumbnails. */
@Composable
private fun DuplicateArtwork(group: Int, modifier: Modifier) {
    val palettes = listOf(
        listOf(Color(0xFFB8DCE9), Color(0xFF689E92), Color(0xFF285F6D)),
        listOf(Color(0xFFF3D3AD), Color(0xFFBD8673), Color(0xFF795865)),
        listOf(Color(0xFFC0D4F0), Color(0xFF8799BA), Color(0xFF4D6581)),
        listOf(Color(0xFFD0E5C8), Color(0xFF83A881), Color(0xFF4D715C)),
    )
    val p = palettes[group % palettes.size]
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(listOf(p[0], p[1])))
        drawCircle(Color(0xFFFDF3D5), radius = size.width * .14f, center = Offset(size.width * .72f, size.height * .22f))
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
