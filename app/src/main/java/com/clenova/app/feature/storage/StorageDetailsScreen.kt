package com.clenova.app.feature.storage

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clenova.app.core.designsystem.ClenovaShapes
import com.clenova.app.core.designsystem.ClenovaThemeTokens
import com.clenova.app.core.utilities.ByteFormatter
import com.clenova.app.domain.model.StorageOverview
import com.clenova.app.domain.model.StorageState
import com.example.BuildConfig
import com.example.R
import kotlin.math.roundToInt

@Composable
fun StorageDetailsScreen(
    storageState: StorageState,
    showSampleHome: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenClean: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ClenovaThemeTokens.colors
    val sample = BuildConfig.DEBUG && showSampleHome
    val shownState = if (sample) StorageDesignData.state else storageState
    val context = LocalContext.current
    var settingsUnavailable by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize().background(colors.background).windowInsetsPadding(WindowInsets.displayCutout),
        contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 560.dp).fillMaxSize().navigationBarsPadding().padding(horizontal = 24.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("storage_back_button")) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.cd_back_button), tint = colors.textPrimary)
                }
                Text(stringResource(R.string.storage_details_title), Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                IconButton(onClick = onRefresh, modifier = Modifier.testTag("storage_refresh_button")) {
                    Icon(Icons.Outlined.Refresh, stringResource(R.string.cd_refresh_storage), tint = colors.textSecondary)
                }
            }
            HorizontalDivider(color = colors.border.copy(alpha = .65f))
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)) {
                when (shownState) {
                    StorageState.Loading -> Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = colors.primary)
                        Text(stringResource(R.string.home_storage_loading), color = colors.textSecondary)
                    }
                    StorageState.Unavailable -> {
                        Text(stringResource(R.string.home_storage_unavailable_title), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                        Text(stringResource(R.string.home_storage_unavailable_body), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                    }
                    is StorageState.Ready -> {
                        val overview = shownState.overview
                        val categories = if (sample) StorageDesignData.categories else emptyList()
                        StorageSummary(overview, categories, sample)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(stringResource(R.string.storage_breakdown_title), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                            if (sample) {
                                categories.forEachIndexed { index, category ->
                                    StorageLegendRow(stringResource(category.label), ByteFormatter.format(category.bytes), category.color)
                                    if (index < categories.lastIndex) HorizontalDivider(color = colors.border.copy(alpha = .65f))
                                }
                            } else {
                                StorageLegendRow(stringResource(R.string.storage_used), ByteFormatter.format(overview.usedBytes), colors.primary)
                                HorizontalDivider(color = colors.border.copy(alpha = .65f))
                                Text(stringResource(R.string.storage_breakdown_unavailable), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                            }
                            HorizontalDivider(color = colors.border.copy(alpha = .65f))
                            StorageLegendRow(stringResource(R.string.storage_free), ByteFormatter.format(overview.availableBytes), colors.border)
                        }
                    }
                }
                Surface(shape = ClenovaShapes.sheet, color = colors.surface, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(44.dp).clip(ClenovaShapes.tile).background(colors.softBlue), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.CropFree, null, tint = colors.primary)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.storage_cleanup_title), color = colors.textPrimary, style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.storage_cleanup_body), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = onOpenClean, modifier = Modifier.testTag("storage_open_clean")) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, stringResource(R.string.storage_cleanup_title), tint = colors.primary)
                        }
                    }
                }
                TextButton(onClick = {
                    settingsUnavailable = runCatching {
                        context.startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
                    }.isFailure
                }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.storage_system_settings), color = colors.primary)
                }
                if (settingsUnavailable) Text(stringResource(R.string.storage_system_unavailable), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun StorageSummary(overview: StorageOverview, categories: List<StorageCategory>, sample: Boolean) {
    val colors = ClenovaThemeTokens.colors
    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Text(stringResource(R.string.home_available_storage), color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
        Text(ByteFormatter.format(overview.availableBytes), color = colors.textPrimary,
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 52.sp, lineHeight = 58.sp, letterSpacing = (-2).sp, fontWeight = FontWeight.SemiBold),
            modifier = Modifier.testTag("storage_available_value"))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.home_used_value, if (sample) StorageDesignData.usedLabel else ByteFormatter.format(overview.usedBytes)),
                color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.storage_total_value, ByteFormatter.format(overview.totalBytes)),
                color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
        }
        val segments = if (categories.isNotEmpty()) categories.map { it.bytes to it.color } else listOf(overview.usedBytes to colors.primary)
        val allSegments = (segments + (overview.availableBytes to colors.border)).filter { it.first > 0L }
        Row(Modifier.fillMaxWidth().height(19.dp).clip(ClenovaShapes.pill).background(colors.border)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(overview.usedFraction, 0f..1f) }) {
            allSegments.forEach { (bytes, color) ->
                Box(Modifier.weight(bytes.toFloat()).fillMaxHeight().background(color))
            }
        }
        Text(stringResource(R.string.home_available_percent, ((1f - overview.usedFraction) * 100).roundToInt()),
            color = colors.primary, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun StorageLegendRow(label: String, value: String, swatch: Color) {
    val colors = ClenovaThemeTokens.colors
    Row(Modifier.fillMaxWidth().heightIn(min = 38.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(11.dp).clip(ClenovaShapes.pill).background(swatch))
        Text(label, Modifier.weight(1f), color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
    }
}
