package app.shoutsend.feature.send

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.shoutsend.R
import app.shoutsend.core.designsystem.ChirpTheme
import app.shoutsend.core.designsystem.components.AcousticStatus
import app.shoutsend.core.designsystem.components.CdButton
import app.shoutsend.core.designsystem.components.CdOutlinedButton
import app.shoutsend.core.designsystem.components.CdScaffold
import app.shoutsend.core.designsystem.components.ShoutCard
import app.shoutsend.core.designsystem.components.StatusPill
import java.util.Locale

/**
 * Cyber-radio acoustic transmission screen featuring real-time soundwave ripple rings,
 * circular countdown arc, hardware volume alerts, and history vault archiving.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransmitScreen(
    viewModel: TransmitViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val view = LocalView.current
    val density = LocalDensity.current
    var showCancelConfirmDialog by remember { mutableStateOf(false) }

    // Keep screen on while preparing or actively transmitting soundwaves
    DisposableEffect(uiState.isTransmitting) {
        view.keepScreenOn = uiState.isTransmitting
        onDispose {
            view.keepScreenOn = false
        }
    }

    // Intercept back gesture during active transmission with confirmation dialog (Task 7)
    BackHandler(enabled = uiState.isTransmitting) {
        showCancelConfirmDialog = true
    }

    if (showCancelConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.transmit_stop_confirm_title),
                    style = ChirpTheme.typography.titleMedium,
                    color = ChirpTheme.colors.onSurface
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.transmit_stop_confirm_body),
                    style = ChirpTheme.typography.bodyMedium,
                    color = ChirpTheme.colors.onSurfaceVariant
                )
            },
            confirmButton = {
                CdButton(
                    onClick = {
                        showCancelConfirmDialog = false
                        viewModel.cancelTransmission()
                        onNavigateBack()
                    }
                ) {
                    Text(stringResource(R.string.transmit_stop_confirm_action))
                }
            },
            dismissButton = {
                CdOutlinedButton(
                    onClick = { showCancelConfirmDialog = false }
                ) {
                    Text(stringResource(R.string.transmit_stop_keep_playing))
                }
            }
        )
    }

    CdScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.transmit_screen_title),
                        style = ChirpTheme.typography.titleMedium,
                        color = ChirpTheme.colors.onSurface
                    )
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Layer 1: Animated Soundwave Ripple Rings
            RingsCanvas(
                isTransmitting = uiState.isTransmitting,
                activeRms = uiState.activeRms,
                activeToneIndex = uiState.activeToneIndex,
                modifier = Modifier.fillMaxSize()
            )

            // Layer 2: Main Layout (Warnings Top, Center Progress Arc, Action Bar Bottom)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(ChirpTheme.spacing.md),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Banners: Low Volume Warning & Peripheral Warnings
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)
                ) {
                    AnimatedVisibility(
                        visible = uiState.isLowVolume && uiState.isTransmitting,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        ShoutCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(ChirpTheme.spacing.sm),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.transmit_volume_warning_title),
                                        style = ChirpTheme.typography.titleSmall,
                                        color = ChirpTheme.colors.warning
                                    )
                                    Text(
                                        text = stringResource(R.string.transmit_volume_warning_body),
                                        style = ChirpTheme.typography.bodySmall,
                                        color = ChirpTheme.colors.onSurfaceVariant
                                    )
                                }
                                CdButton(
                                    onClick = { viewModel.openVolumePanel() }
                                ) {
                                    Text(stringResource(R.string.transmit_volume_warning_action))
                                }
                            }
                        }
                    }

                    if (uiState.routeWarning != null && uiState.isTransmitting) {
                        ShoutCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = uiState.routeWarning!!,
                                style = ChirpTheme.typography.bodySmall,
                                color = ChirpTheme.colors.warning,
                                modifier = Modifier.padding(ChirpTheme.spacing.sm)
                            )
                        }
                    }
                }

                // Center Display: Circular Progress Arc, Countdown, Status
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val trackColor = ChirpTheme.colors.surfaceContainerHighest
                    val progressColor = if (uiState.isCompleted) ChirpTheme.colors.success else ChirpTheme.colors.primary
                    val trackStrokePx = with(density) { ChirpTheme.strokes.thin.toPx() }
                    val progressStrokePx = with(density) { ChirpTheme.strokes.regular.toPx() }

                    // Circular Progress Ring
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(
                            color = trackColor,
                            startAngle = 0.0f,
                            sweepAngle = 360.0f,
                            useCenter = false,
                            style = Stroke(width = trackStrokePx)
                        )
                        drawArc(
                            color = progressColor,
                            startAngle = -90.0f,
                            sweepAngle = (uiState.progress * 360.0f).coerceIn(0.0f, 360.0f),
                            useCenter = false,
                            style = Stroke(width = progressStrokePx, cap = StrokeCap.Round)
                        )
                    }

                    // Content inside Circular Progress Arc
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)
                    ) {
                        when {
                            uiState.isCompleted -> {
                                Text(
                                    text = "✓",
                                    style = ChirpTheme.typography.displayLarge,
                                    color = ChirpTheme.colors.success
                                )
                                Text(
                                    text = stringResource(R.string.transmit_status_sent),
                                    style = ChirpTheme.typography.titleLarge,
                                    color = ChirpTheme.colors.onSurface
                                )
                                if (uiState.isSecure) {
                                    StatusPill(
                                        status = AcousticStatus.LOCKED,
                                        customLabel = stringResource(R.string.transmit_sealed_badge)
                                    )
                                }
                                if (uiState.isSavedToVault) {
                                    Text(
                                        text = stringResource(R.string.transmit_status_saved_to_vault),
                                        style = ChirpTheme.typography.bodySmall,
                                        color = ChirpTheme.colors.secondary
                                    )
                                }
                            }
                            uiState.isFailed -> {
                                StatusPill(
                                    status = AcousticStatus.ERROR,
                                    customLabel = stringResource(R.string.transmit_screen_failed)
                                )
                                if (uiState.errorMessage != null) {
                                    Text(
                                        text = uiState.errorMessage!!,
                                        style = ChirpTheme.typography.bodySmall,
                                        color = ChirpTheme.colors.error,
                                        modifier = Modifier.padding(horizontal = ChirpTheme.spacing.sm)
                                    )
                                }
                            }
                            uiState.isCancelled -> {
                                StatusPill(
                                    status = AcousticStatus.IDLE,
                                    customLabel = stringResource(R.string.transmit_screen_cancelled)
                                )
                            }
                            else -> {
                                // Active Countdown Timer
                                val remainingFormatted = String.format(Locale.US, "%.1f", uiState.remainingSeconds)
                                Text(
                                    text = remainingFormatted,
                                    style = ChirpTheme.typography.displaySmall,
                                    color = ChirpTheme.colors.primary
                                )
                                Text(
                                    text = "seconds",
                                    style = ChirpTheme.typography.labelSmall,
                                    color = ChirpTheme.colors.onSurfaceVariant
                                )

                                // Frame and Repetition Counter
                                val frameText = if (uiState.repeats > 1) {
                                    String.format(
                                        Locale.US,
                                        stringResource(R.string.transmit_frame_repeat),
                                        uiState.currentFrame,
                                        uiState.totalFrames,
                                        uiState.currentRepeat,
                                        uiState.repeats
                                    )
                                } else {
                                    String.format(
                                        Locale.US,
                                        stringResource(R.string.transmit_frame_only),
                                        uiState.currentFrame,
                                        uiState.totalFrames
                                    )
                                }

                                Text(
                                    text = frameText,
                                    style = ChirpTheme.typography.bodySmall,
                                    color = ChirpTheme.colors.onSurfaceVariant
                                )

                                // Band and Speed Pill
                                StatusPill(
                                    status = AcousticStatus.LOCKED,
                                    customLabel = "${uiState.band.name} · ${uiState.speed.name}"
                                )

                                if (uiState.isSecure) {
                                    StatusPill(
                                        status = AcousticStatus.LOCKED,
                                        customLabel = stringResource(R.string.transmit_sealed_badge)
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when {
                        uiState.isCompleted -> {
                            CdButton(
                                onClick = onNavigateBack,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.transmit_btn_send_another))
                            }
                            CdOutlinedButton(
                                onClick = onNavigateBack,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.transmit_btn_done))
                            }
                        }
                        uiState.isFailed || uiState.isCancelled -> {
                            CdButton(
                                onClick = { viewModel.retryTransmission() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.transmit_btn_retry))
                            }
                            CdOutlinedButton(
                                onClick = onNavigateBack,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.transmit_btn_back))
                            }
                        }
                        else -> {
                            // Active Transmission: Single-Tap Stop Button
                            CdOutlinedButton(
                                onClick = { viewModel.cancelTransmission() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.transmit_btn_stop))
                            }
                        }
                    }
                }
            }
        }
    }
}
