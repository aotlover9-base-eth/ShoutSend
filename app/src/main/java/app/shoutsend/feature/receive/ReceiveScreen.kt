package app.shoutsend.feature.receive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.shoutsend.R
import app.shoutsend.core.audio.permissions.MicPermissionState
import app.shoutsend.core.designsystem.ChirpTheme
import app.shoutsend.core.designsystem.components.CdFilterChip
import app.shoutsend.core.designsystem.components.LevelMeter
import app.shoutsend.core.designsystem.components.MicPermissionCard
import app.shoutsend.core.designsystem.components.NeonCard
import app.shoutsend.core.designsystem.components.RadioReadout
import app.shoutsend.core.designsystem.components.ReadoutScale
import app.shoutsend.core.designsystem.components.SignalMeter
import app.shoutsend.core.designsystem.components.StatusPill
import app.shoutsend.modem.profile.Band
import java.util.Locale

/**
 * Cyber-radio acoustic reception screen featuring a live 256-bin spectrogram waterfall,
 * carrier lock highlight, signal and volume meters, and hardware diagnostics.
 *
 * Conforms to:
 * - Rule 6 & 7: 100% tokenized design system (zero raw hex or measurement literals).
 * - Rule 22: Zero-allocation audio and waterfall rendering pipeline.
 * - Rule 24: Permission gates gracefully handle microphone state without crash.
 */
@Composable
fun ReceiveScreen(
    viewModel: ReceiveViewModel,
    modifier: Modifier = Modifier,
    micPermissionState: MicPermissionState = MicPermissionState.Granted,
    onRequestPermission: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val frameClock by viewModel.frameClock.collectAsStateWithLifecycle()
    val view = LocalView.current
    val scrollState = rememberScrollState()

    // Manage microphone capture lifecycle & keep screen powered during active reception
    DisposableEffect(micPermissionState) {
        if (micPermissionState == MicPermissionState.Granted) {
            viewModel.startListening()
            view.keepScreenOn = true
        }
        onDispose {
            viewModel.stopListening()
            view.keepScreenOn = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(ChirpTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.md)
    ) {
        // --- 1. Microphone Permission Banner Gate ---
        if (micPermissionState != MicPermissionState.Granted) {
            MicPermissionCard(
                state = micPermissionState,
                onRequestPermission = onRequestPermission,
                onOpenSettings = onOpenSettings,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // --- 2. Top Header Strip: Band Selector Chips and Active Status Pill ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CdFilterChip(
                    selected = uiState.band == Band.RETRO,
                    onClick = { viewModel.setBand(Band.RETRO) },
                    label = { Text(stringResource(R.string.receive_band_retro)) }
                )
                CdFilterChip(
                    selected = uiState.band == Band.STEALTH,
                    onClick = { viewModel.setBand(Band.STEALTH) },
                    label = { Text(stringResource(R.string.receive_band_stealth)) }
                )
            }

            StatusPill(status = uiState.status)
        }

        // --- 3. Hero Waterfall Card ---
        NeonCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(ChirpTheme.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)
            ) {
                // Waterfall Canvas (Height: navBarHeight * 3 = 240 dp equivalent)
                WaterfallCanvas(
                    buffer = viewModel.waterfallBuffer,
                    band = uiState.band,
                    isPreambleLocked = uiState.isPreambleLocked,
                    activeToneIndex = uiState.activeToneIndex,
                    frameClock = frameClock,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ChirpTheme.sizes.navBarHeight * 3)
                        .clip(ChirpTheme.shapes.medium)
                )

                // Subtitle readout describing reception state
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ChirpTheme.spacing.xs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusText = when {
                        uiState.isPreambleLocked -> String.format(
                            Locale.US,
                            stringResource(R.string.receive_hero_locked),
                            String.format(Locale.US, "%.1f", uiState.snrDb)
                        )
                        uiState.currentFragment > 0 -> String.format(
                            Locale.US,
                            stringResource(R.string.receive_hero_receiving),
                            uiState.currentFragment,
                            uiState.totalFragments
                        )
                        uiState.isListening -> String.format(
                            Locale.US,
                            stringResource(R.string.receive_hero_listening),
                            uiState.band.name
                        )
                        else -> stringResource(R.string.receive_screen_title)
                    }

                    Text(
                        text = statusText,
                        style = ChirpTheme.typography.bodySmall,
                        color = if (uiState.isPreambleLocked) ChirpTheme.colors.syncMarker else ChirpTheme.colors.onSurfaceVariant
                    )

                    RadioReadout(
                        value = "${uiState.sampleRateHz / 1000} kHz",
                        scale = ReadoutScale.SMALL,
                        color = ChirpTheme.colors.primary
                    )
                }
            }
        }

        // --- 4. Signal Meters Card ---
        NeonCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(ChirpTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm)
            ) {
                Text(
                    text = stringResource(R.string.receive_meter_carrier_signal),
                    style = ChirpTheme.typography.titleSmall,
                    color = ChirpTheme.colors.onSurface
                )
                SignalMeter(
                    snrDb = uiState.snrDb,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(ChirpTheme.spacing.xs))

                Text(
                    text = stringResource(R.string.receive_meter_mic_input),
                    style = ChirpTheme.typography.titleSmall,
                    color = ChirpTheme.colors.onSurface
                )
                LevelMeter(
                    levelDbfs = uiState.levelDbfs,
                    peakDbfs = uiState.peakDbfs,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // --- 5. Diagnostic Warnings (Clipping or Digital Silence) ---
        AnimatedVisibility(
            visible = uiState.isClipping,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = ChirpTheme.colors.errorContainer,
                        shape = ChirpTheme.shapes.medium
                    )
                    .border(
                        width = ChirpTheme.strokes.thin,
                        color = ChirpTheme.colors.error,
                        shape = ChirpTheme.shapes.medium
                    )
                    .padding(ChirpTheme.spacing.sm)
            ) {
                Text(
                    text = stringResource(R.string.receive_clipping_alert),
                    style = ChirpTheme.typography.bodySmall,
                    color = ChirpTheme.colors.onErrorContainer
                )
            }
        }

        AnimatedVisibility(
            visible = uiState.isSilent && uiState.isListening,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = ChirpTheme.colors.surfaceContainerHigh,
                        shape = ChirpTheme.shapes.medium
                    )
                    .border(
                        width = ChirpTheme.strokes.thin,
                        color = ChirpTheme.colors.warning,
                        shape = ChirpTheme.shapes.medium
                    )
                    .padding(ChirpTheme.spacing.sm)
            ) {
                Text(
                    text = stringResource(R.string.receive_silence_alert),
                    style = ChirpTheme.typography.bodySmall,
                    color = ChirpTheme.colors.onSurfaceVariant
                )
            }
        }

        // --- 6. Hardware Info Strip ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ChirpTheme.spacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SOURCE: ${uiState.audioSource}",
                style = ChirpTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = ChirpTheme.colors.onSurfaceVariant
            )

            Text(
                text = stringResource(R.string.receive_guidance_tip),
                style = ChirpTheme.typography.labelSmall,
                color = ChirpTheme.colors.onSurfaceVariant
            )
        }
    }
}
