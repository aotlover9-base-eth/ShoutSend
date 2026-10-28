package app.shoutsend.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.shoutsend.R
import app.shoutsend.core.designsystem.ChirpTheme
import app.shoutsend.core.designsystem.ThemeMode
import app.shoutsend.core.designsystem.components.CdButton
import app.shoutsend.core.designsystem.components.CdIconButton
import app.shoutsend.core.designsystem.components.CdOutlinedButton
import app.shoutsend.core.designsystem.components.CdSwitch
import app.shoutsend.core.designsystem.components.NeonCard
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.Speed

/**
 * Settings screen presenting card-based grouped sections for acoustic parameters,
 * vault retention, audio power, theme appearance, diagnostics, and privacy (Step 44).
 *
 * Conforms to UI Gate specifications:
 * 1. Card-based grouped sections: Transmission Defaults, Transmission Vault,
 *    Audio/Haptics/Power, Appearance, Diagnostics & Calibration, About & Privacy.
 * 2. Defaults propagate to Send/Receive compose screens without locking overrides.
 * 3. Near-ultrasound Stealth disclosure dialog required if not previously accepted.
 * 4. Vault limit chips (100, 250, 500, 1000, 2000) with prune confirmation for unstarred items.
 * 5. "Delete all app data" permanently wipes encrypted vault, resets settings, and clears onboarding.
 * 6. Minimum 48dp touch targets, TalkBack labels, and design tokens only (Rule 6, Rule 7).
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCalibration: () -> Unit,
    onNavigateToOnboarding: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = ChirpTheme.colors
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.settings
    val scrollState = rememberScrollState()

    // 1. Stealth Mode Disclosure Dialog
    if (uiState.showStealthWarningDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissStealthWarning() },
            title = {
                Text(
                    text = stringResource(R.string.send_stealth_dialog_title),
                    style = ChirpTheme.typography.titleMedium,
                    color = colors.warning
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.send_stealth_dialog_body),
                    style = ChirpTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            },
            confirmButton = {
                CdButton(onClick = { viewModel.onAcceptStealthWarning() }) {
                    Text(stringResource(R.string.send_stealth_dialog_confirm))
                }
            },
            dismissButton = {
                CdOutlinedButton(onClick = { viewModel.onDismissStealthWarning() }) {
                    Text(stringResource(R.string.send_stealth_dialog_cancel))
                }
            }
        )
    }

    // 2. Vault Prune Confirmation Dialog
    if (uiState.showPruneConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPruneDialog() },
            title = {
                Text(
                    text = stringResource(R.string.settings_vault_prune_title),
                    style = ChirpTheme.typography.titleMedium,
                    color = colors.warning
                )
            },
            text = {
                val message = String.format(
                    stringResource(R.string.settings_vault_prune_message),
                    uiState.pendingMaxEntries ?: 0,
                    uiState.pendingPruneCount
                )
                Text(
                    text = message,
                    style = ChirpTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            },
            confirmButton = {
                CdButton(onClick = { viewModel.confirmPrune() }) {
                    Text(stringResource(R.string.settings_vault_prune_confirm))
                }
            },
            dismissButton = {
                CdOutlinedButton(onClick = { viewModel.dismissPruneDialog() }) {
                    Text(stringResource(R.string.vault_clear_dialog_cancel))
                }
            }
        )
    }

    // 3. Delete All App Data Confirmation Dialog
    if (uiState.showDeleteAllDataDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteAllDataDialog() },
            title = {
                Text(
                    text = stringResource(R.string.settings_delete_dialog_title),
                    style = ChirpTheme.typography.titleMedium,
                    color = colors.signalError
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.settings_delete_dialog_message),
                    style = ChirpTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            },
            confirmButton = {
                CdButton(
                    onClick = { viewModel.confirmDeleteAllData() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.signalError,
                        contentColor = colors.onError
                    )
                ) {
                    Text(stringResource(R.string.settings_delete_dialog_confirm))
                }
            },
            dismissButton = {
                CdOutlinedButton(onClick = { viewModel.dismissDeleteAllDataDialog() }) {
                    Text(stringResource(R.string.vault_clear_dialog_cancel))
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = ChirpTheme.spacing.md, vertical = ChirpTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.md)
    ) {
        // Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm)
        ) {
            val backDesc = stringResource(R.string.vault_action_back_desc)
            CdIconButton(
                onClick = onNavigateBack,
                modifier = Modifier.semantics { contentDescription = backDesc }
            ) {
                Text(
                    text = "←",
                    style = ChirpTheme.typography.titleLarge,
                    color = colors.primary
                )
            }

            Text(
                text = stringResource(R.string.settings_screen_title),
                style = ChirpTheme.typography.titleLarge,
                color = colors.onSurface
            )
        }

        // Section 1: Transmission Defaults
        NeonCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(ChirpTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm)
            ) {
                Text(
                    text = stringResource(R.string.settings_section_transmission),
                    style = ChirpTheme.typography.titleMedium,
                    color = colors.primary
                )

                // Band Selector
                Column(verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xxs)) {
                    Text(
                        text = stringResource(R.string.settings_band_label),
                        style = ChirpTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)
                    ) {
                        FilterChip(
                            selected = settings.band == Band.RETRO,
                            onClick = { viewModel.setBand(Band.RETRO) },
                            label = { Text(stringResource(R.string.settings_band_retro)) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = settings.band == Band.STEALTH,
                            onClick = { viewModel.setBand(Band.STEALTH) },
                            label = { Text(stringResource(R.string.settings_band_stealth)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Speed Selector
                Column(verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xxs)) {
                    Text(
                        text = stringResource(R.string.settings_speed_label),
                        style = ChirpTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)
                    ) {
                        FilterChip(
                            selected = settings.speed == Speed.FAST,
                            onClick = { viewModel.setSpeed(Speed.FAST) },
                            label = { Text(stringResource(R.string.settings_speed_fast)) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = settings.speed == Speed.NORMAL,
                            onClick = { viewModel.setSpeed(Speed.NORMAL) },
                            label = { Text(stringResource(R.string.settings_speed_normal)) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = settings.speed == Speed.ROBUST,
                            onClick = { viewModel.setSpeed(Speed.ROBUST) },
                            label = { Text(stringResource(R.string.settings_speed_robust)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Repeats Selector
                Column(verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xxs)) {
                    Text(
                        text = stringResource(R.string.settings_repeats_label),
                        style = ChirpTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)
                    ) {
                        FilterChip(
                            selected = settings.repeats == 1,
                            onClick = { viewModel.setRepeats(1) },
                            label = { Text("1x") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = settings.repeats == 2,
                            onClick = { viewModel.setRepeats(2) },
                            label = { Text("2x") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = settings.repeats == 3,
                            onClick = { viewModel.setRepeats(3) },
                            label = { Text("3x") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Section 2: Transmission Vault
        NeonCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(ChirpTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm)
            ) {
                Text(
                    text = stringResource(R.string.settings_section_vault),
                    style = ChirpTheme.typography.titleMedium,
                    color = colors.primary
                )

                // History Enable Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_vault_enable_label),
                            style = ChirpTheme.typography.bodyMedium,
                            color = colors.onSurface
                        )
                        Text(
                            text = stringResource(R.string.settings_vault_enable_desc),
                            style = ChirpTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    CdSwitch(
                        checked = settings.historyEnabled,
                        onCheckedChange = { viewModel.setHistoryEnabled(it) }
                    )
                }

                // History Limit Picker Chips
                if (settings.historyEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xxs)) {
                        Text(
                            text = stringResource(R.string.settings_vault_limit_label),
                            style = ChirpTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)
                        ) {
                            listOf(100, 250, 500, 1000, 2000).forEach { limit ->
                                item {
                                    FilterChip(
                                        selected = settings.historyMaxEntries == limit,
                                        onClick = { viewModel.requestHistoryMaxEntries(limit) },
                                        label = { Text("$limit") }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Audio, Haptics & Power
        NeonCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(ChirpTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm)
            ) {
                Text(
                    text = stringResource(R.string.settings_section_audio_power),
                    style = ChirpTheme.typography.titleMedium,
                    color = colors.primary
                )

                // Haptic Feedback Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_haptics_label),
                            style = ChirpTheme.typography.bodyMedium,
                            color = colors.onSurface
                        )
                        Text(
                            text = stringResource(R.string.settings_haptics_desc),
                            style = ChirpTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    CdSwitch(
                        checked = settings.hapticsEnabled,
                        onCheckedChange = { viewModel.setHapticsEnabled(it) }
                    )
                }

                // Audio Chimes Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_sound_effects_label),
                            style = ChirpTheme.typography.bodyMedium,
                            color = colors.onSurface
                        )
                        Text(
                            text = stringResource(R.string.settings_sound_effects_desc),
                            style = ChirpTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    CdSwitch(
                        checked = settings.soundEffectsEnabled,
                        onCheckedChange = { viewModel.setSoundEffectsEnabled(it) }
                    )
                }

                // Keep Screen On Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_keep_screen_on_label),
                            style = ChirpTheme.typography.bodyMedium,
                            color = colors.onSurface
                        )
                        Text(
                            text = stringResource(R.string.settings_keep_screen_on_desc),
                            style = ChirpTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    CdSwitch(
                        checked = settings.keepScreenOnDuringSessions,
                        onCheckedChange = { viewModel.setKeepScreenOnDuringSessions(it) }
                    )
                }

                // Background Grace Timer
                Column(verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xxs)) {
                    Text(
                        text = stringResource(R.string.settings_grace_label),
                        style = ChirpTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.settings_grace_desc),
                        style = ChirpTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)
                    ) {
                        listOf(0 to "0s", 5 to "5s", 15 to "15s", 30 to "30s").forEach { (sec, label) ->
                            FilterChip(
                                selected = settings.backgroundGraceSeconds == sec,
                                onClick = { viewModel.setBackgroundGraceSeconds(sec) },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Section 4: Appearance
        NeonCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(ChirpTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm)
            ) {
                Text(
                    text = stringResource(R.string.settings_section_appearance),
                    style = ChirpTheme.typography.titleMedium,
                    color = colors.primary
                )

                // Theme Mode Selector
                Column(verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xxs)) {
                    Text(
                        text = stringResource(R.string.settings_theme_label),
                        style = ChirpTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)
                    ) {
                        FilterChip(
                            selected = settings.themeMode == ThemeMode.DARK,
                            onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                            label = { Text(stringResource(R.string.settings_theme_dark)) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = settings.themeMode == ThemeMode.LIGHT,
                            onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                            label = { Text(stringResource(R.string.settings_theme_light)) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = settings.themeMode == ThemeMode.SYSTEM,
                            onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                            label = { Text(stringResource(R.string.settings_theme_system)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Dynamic Color Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_dynamic_color_label),
                            style = ChirpTheme.typography.bodyMedium,
                            color = colors.onSurface
                        )
                        Text(
                            text = stringResource(R.string.settings_dynamic_color_desc),
                            style = ChirpTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    CdSwitch(
                        checked = settings.dynamicColor,
                        onCheckedChange = { viewModel.setDynamicColor(it) }
                    )
                }
            }
        }

        // Section 5: Diagnostics & Calibration
        NeonCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(ChirpTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm)
            ) {
                Text(
                    text = stringResource(R.string.settings_section_diagnostics),
                    style = ChirpTheme.typography.titleMedium,
                    color = colors.primary
                )
                Text(
                    text = stringResource(R.string.settings_diagnostics_desc),
                    style = ChirpTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )

                Text(
                    text = uiState.capabilitySummary,
                    style = ChirpTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = colors.onSurface
                )

                CdButton(
                    onClick = onNavigateToCalibration,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = ChirpTheme.sizes.touchTargetMin)
                ) {
                    Text(stringResource(R.string.settings_btn_open_calibration))
                }
            }
        }

        // Section 6: About & Privacy
        NeonCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(ChirpTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm)
            ) {
                Text(
                    text = stringResource(R.string.settings_section_about),
                    style = ChirpTheme.typography.titleMedium,
                    color = colors.primary
                )

                Text(
                    text = stringResource(R.string.settings_version_label),
                    style = ChirpTheme.typography.titleSmall,
                    color = colors.onSurface
                )

                Text(
                    text = stringResource(R.string.settings_offline_pledge),
                    style = ChirpTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )

                CdOutlinedButton(
                    onClick = { viewModel.replayOnboarding(onNavigateToOnboarding) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = ChirpTheme.sizes.touchTargetMin)
                ) {
                    Text(stringResource(R.string.settings_btn_replay_onboarding))
                }

                CdOutlinedButton(
                    onClick = { viewModel.requestDeleteAllData() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = ChirpTheme.sizes.touchTargetMin),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.signalError
                    )
                ) {
                    Text(stringResource(R.string.settings_btn_delete_all_data))
                }
            }
        }

        Spacer(modifier = Modifier.height(ChirpTheme.spacing.md))
    }
}
