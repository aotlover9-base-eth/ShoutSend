package app.shoutsend.feature.calibration

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.shoutsend.R
import app.shoutsend.core.audio.permissions.MicPermissionState
import app.shoutsend.core.designsystem.ChirpTheme
import app.shoutsend.core.designsystem.components.AcousticStatus
import app.shoutsend.core.designsystem.components.CdButton
import app.shoutsend.core.designsystem.components.CdIconButton
import app.shoutsend.core.designsystem.components.CdOutlinedButton
import app.shoutsend.core.designsystem.components.MicPermissionCard
import app.shoutsend.core.designsystem.components.NeonCard
import app.shoutsend.core.designsystem.components.RadioReadout
import app.shoutsend.core.designsystem.components.ReadoutScale
import app.shoutsend.core.designsystem.components.SignalMeter
import app.shoutsend.core.designsystem.components.StatusPill

/**
 * Screen displaying hardware acoustic calibration diagnostics, noise profiling,
 * and link testing results (Step 45).
 */
@Composable
fun CalibrationScreen(
    viewModel: CalibrationViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    micPermissionState: MicPermissionState = MicPermissionState.Granted,
    onRequestPermission: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = ChirpTheme.colors
    val scrollState = rememberScrollState()

    val isRunning = when (uiState.stage) {
        is CalibrationStage.NoiseSampling,
        is CalibrationStage.TransmittingTones,
        is CalibrationStage.Analyzing -> true
        else -> false
    }

    val (status, statusLabel) = when (val s = uiState.stage) {
        is CalibrationStage.Idle -> AcousticStatus.IDLE to stringResource(R.string.calibration_status_idle)
        is CalibrationStage.NoiseSampling -> AcousticStatus.LISTENING to stringResource(R.string.calibration_status_sampling)
        is CalibrationStage.TransmittingTones -> AcousticStatus.RECEIVING to stringResource(R.string.calibration_status_chirping)
        is CalibrationStage.Analyzing -> AcousticStatus.LOCKED to stringResource(R.string.calibration_status_analyzing)
        is CalibrationStage.Completed -> AcousticStatus.SUCCESS to stringResource(R.string.calibration_status_completed)
        is CalibrationStage.Failed -> AcousticStatus.ERROR to (s.reason.ifBlank { stringResource(R.string.calibration_status_failed) })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(scrollState)
            .padding(ChirpTheme.spacing.md),
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
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = colors.primary
                )
            }
            Text(
                text = stringResource(R.string.calibration_title),
                style = ChirpTheme.typography.titleLarge,
                color = colors.primary
            )
        }

        // Permission Card if needed
        if (micPermissionState != MicPermissionState.Granted) {
            MicPermissionCard(
                state = micPermissionState,
                onRequestPermission = onRequestPermission,
                onOpenSettings = onOpenSettings
            )
        }

        // Overview / Status Card
        NeonCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(ChirpTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.calibration_title),
                        style = ChirpTheme.typography.titleMedium,
                        color = colors.primary
                    )
                    StatusPill(
                        status = status,
                        customLabel = statusLabel
                    )
                }

                Text(
                    text = stringResource(R.string.calibration_desc),
                    style = ChirpTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )

                // Active Progress Indicator
                if (isRunning) {
                    val progress = when (val s = uiState.stage) {
                        is CalibrationStage.TransmittingTones -> s.progress
                        is CalibrationStage.NoiseSampling -> 0.15f
                        is CalibrationStage.Analyzing -> 0.95f
                        else -> 0f
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = ChirpTheme.spacing.xs),
                        color = colors.primary,
                        trackColor = colors.surfaceVariant
                    )
                }
            }
        }

        // Last Calibration Result Card
        val result = uiState.lastResult
        if (result != null) {
            NeonCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(ChirpTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.md)
                ) {
                    Text(
                        text = stringResource(R.string.vault_detail_telemetry),
                        style = ChirpTheme.typography.titleMedium,
                        color = colors.secondary
                    )

                    // Warnings
                    if (result.warnings.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xxs)
                        ) {
                            for (warning in result.warnings) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = colors.warning,
                                        modifier = Modifier.size(ChirpTheme.spacing.md)
                                    )
                                    val warningText = if (warning == "VOLUME_LOW") {
                                        stringResource(R.string.calibration_warning_volume)
                                    } else {
                                        stringResource(R.string.calibration_warning_headset)
                                    }
                                    Text(
                                        text = warningText,
                                        style = ChirpTheme.typography.bodySmall,
                                        color = colors.warning
                                    )
                                }
                            }
                        }
                    }

                    // Recommendations Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm)
                    ) {
                        RadioReadout(
                            label = stringResource(R.string.calibration_recommended_band),
                            value = result.recommendedBand.name,
                            scale = ReadoutScale.MEDIUM,
                            modifier = Modifier.weight(1f)
                        )
                        RadioReadout(
                            label = stringResource(R.string.calibration_recommended_speed),
                            value = result.recommendedSpeed.name,
                            scale = ReadoutScale.MEDIUM,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Band Reports
                    Column(verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)) {
                        Text(
                            text = "Audible Retro Band (${result.retroReport.verdict.name})",
                            style = ChirpTheme.typography.titleSmall,
                            color = colors.onSurface
                        )
                        SignalMeter(
                            snrDb = result.retroReport.minSnrDb,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Min SNR: ${"%.1f".format(result.retroReport.minSnrDb)} dB · Noise Floor: ${"%.1f".format(result.retroReport.noiseFloorDbfs)} dBFS",
                            style = ChirpTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)) {
                        Text(
                            text = "Ultrasonic Stealth Band (${result.stealthReport.verdict.name})",
                            style = ChirpTheme.typography.titleSmall,
                            color = colors.onSurface
                        )
                        SignalMeter(
                            snrDb = result.stealthReport.minSnrDb,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Min SNR: ${"%.1f".format(result.stealthReport.minSnrDb)} dB · Noise Floor: ${"%.1f".format(result.stealthReport.noiseFloorDbfs)} dBFS",
                            style = ChirpTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                    }

                    // Apply Recommendations Button
                    AnimatedVisibility(visible = !uiState.recommendationApplied) {
                        CdOutlinedButton(
                            onClick = { viewModel.applyRecommendations() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.calibration_btn_apply))
                        }
                    }
                    if (uiState.recommendationApplied) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs),
                            modifier = Modifier.padding(top = ChirpTheme.spacing.xs)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = colors.success,
                                modifier = Modifier.size(ChirpTheme.spacing.md)
                            )
                            Text(
                                text = stringResource(R.string.calibration_applied),
                                style = ChirpTheme.typography.bodySmall,
                                color = colors.success
                            )
                        }
                    }
                }
            }
        }

        // Action Buttons
        if (isRunning) {
            CdOutlinedButton(
                onClick = { viewModel.cancelCalibration() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.calibration_btn_cancel))
            }
        } else {
            CdButton(
                onClick = { viewModel.startCalibration() },
                enabled = micPermissionState == MicPermissionState.Granted,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.calibration_btn_start))
            }
        }
    }
}
