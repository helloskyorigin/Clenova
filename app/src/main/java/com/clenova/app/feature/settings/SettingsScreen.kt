package com.clenova.app.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.clenova.app.core.designsystem.ClenovaInfoRow
import com.clenova.app.core.designsystem.ClenovaSectionLabel
import com.clenova.app.core.designsystem.ClenovaShapes
import com.clenova.app.core.designsystem.ClenovaSpacing
import com.clenova.app.core.designsystem.ClenovaSurfaceCard
import com.clenova.app.core.designsystem.ClenovaThemeTokens
import com.clenova.app.domain.model.ThemePreference
import com.clenova.app.domain.model.UserPreferences
import com.example.BuildConfig
import com.example.R

@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    onSelectTheme: (ThemePreference) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ClenovaThemeTokens.colors
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    horizontal = ClenovaSpacing.screenHorizontal,
                    vertical = ClenovaSpacing.md,
                ),
            verticalArrangement = Arrangement.spacedBy(ClenovaSpacing.lg),
        ) {
            // Focused Top App Bar (floating nav hidden here)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ClenovaSpacing.sm),
            ) {
                Surface(
                    shape = CircleShape,
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.border),
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("settings_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back_button),
                            tint = colors.textPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.textPrimary,
                )
            }

            // Preferences Section
            Column(verticalArrangement = Arrangement.spacedBy(ClenovaSpacing.xs)) {
                ClenovaSectionLabel(
                    text = stringResource(R.string.settings_section_preferences),
                    modifier = Modifier.padding(horizontal = ClenovaSpacing.xxs),
                )
                ClenovaSurfaceCard {
                    Text(
                        text = stringResource(R.string.settings_theme_label),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.height(ClenovaSpacing.sm))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ClenovaShapes.pill)
                            .background(colors.background)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        ThemeOptionChip(
                            label = stringResource(R.string.settings_theme_light),
                            selected = preferences.themePreference == ThemePreference.LIGHT,
                            onClick = { onSelectTheme(ThemePreference.LIGHT) },
                            testTag = "theme_option_light",
                            modifier = Modifier.weight(1f),
                        )
                        ThemeOptionChip(
                            label = stringResource(R.string.settings_theme_dark),
                            selected = preferences.themePreference == ThemePreference.DARK,
                            onClick = { onSelectTheme(ThemePreference.DARK) },
                            testTag = "theme_option_dark",
                            modifier = Modifier.weight(1f),
                        )
                        ThemeOptionChip(
                            label = stringResource(R.string.settings_theme_system),
                            selected = preferences.themePreference == ThemePreference.SYSTEM,
                            onClick = { onSelectTheme(ThemePreference.SYSTEM) },
                            testTag = "theme_option_system",
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Spacer(modifier = Modifier.height(ClenovaSpacing.md))
                    HorizontalDivider(thickness = 1.dp, color = colors.border)
                    Spacer(modifier = Modifier.height(ClenovaSpacing.md))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_haptics_label),
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary,
                            )
                            Spacer(modifier = Modifier.height(ClenovaSpacing.xxs))
                            Text(
                                text = stringResource(R.string.settings_haptics_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                            )
                        }
                        Switch(
                            checked = preferences.hapticsEnabled,
                            onCheckedChange = onToggleHaptics,
                            modifier = Modifier.testTag("settings_haptics_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.onPrimary,
                                checkedTrackColor = colors.primary,
                            ),
                        )
                    }
                }
            }

            // Safety & Privacy Section
            Column(verticalArrangement = Arrangement.spacedBy(ClenovaSpacing.xs)) {
                ClenovaSectionLabel(
                    text = stringResource(R.string.settings_section_safety),
                    modifier = Modifier.padding(horizontal = ClenovaSpacing.xxs),
                )
                ClenovaSurfaceCard {
                    Text(
                        text = stringResource(R.string.settings_safety_approval_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.height(ClenovaSpacing.xxs))
                    Text(
                        text = stringResource(R.string.settings_safety_approval_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                    Spacer(modifier = Modifier.height(ClenovaSpacing.md))
                    HorizontalDivider(thickness = 1.dp, color = colors.border)
                    Spacer(modifier = Modifier.height(ClenovaSpacing.md))
                    Text(
                        text = stringResource(R.string.settings_safety_local_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.height(ClenovaSpacing.xxs))
                    Text(
                        text = stringResource(R.string.settings_safety_local_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                }
            }

            // Application Info Section
            Column(verticalArrangement = Arrangement.spacedBy(ClenovaSpacing.xs)) {
                ClenovaSectionLabel(
                    text = stringResource(R.string.settings_section_about),
                    modifier = Modifier.padding(horizontal = ClenovaSpacing.xxs),
                )
                ClenovaSurfaceCard {
                    ClenovaInfoRow(
                        label = stringResource(R.string.settings_about_name_label),
                        value = stringResource(R.string.app_name),
                    )
                    ClenovaInfoRow(
                        label = stringResource(R.string.settings_about_version_label),
                        value = stringResource(
                            R.string.settings_about_version_value,
                            BuildConfig.VERSION_NAME,
                        ),
                    )
                    ClenovaInfoRow(
                        label = stringResource(R.string.settings_about_package_label),
                        value = BuildConfig.APPLICATION_ID,
                        showDivider = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val colors = ClenovaThemeTokens.colors
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(ClenovaShapes.pill)
            .background(if (selected) colors.softBlue else colors.background)
            .testTag(testTag)
            .semantics { this.selected = selected }
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = if (selected) colors.primary else colors.textSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
