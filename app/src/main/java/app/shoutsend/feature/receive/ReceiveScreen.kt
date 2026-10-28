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
import androidx.compose.material3.FilterChip
import app.shoutsend.R
import app.shoutsend.core.audio.permissions.MicPermissionState
import app.shoutsend.core.designsystem.ChirpTheme
import app.shoutsend.core.designsystem.components.AcousticStatus
import app.shoutsend.core.designsystem.components.LevelMeter
import app.shoutsend.core.designsystem.components.MicPermissionCard
import app.shoutsend.core.designsystem.components.ShoutCard
import app.shoutsend.core.designsystem.components.SignalMeter
import app.shoutsend.core.designsystem.components.StatusPill
import app.shoutsend.modem.profile.Band
import java.util.Locale

/**
 * Cyber-radio acoustic reception screen featuring a live 256-bin spectrogram waterfall,
 * segmented reassembly progress, error-correction indicator, inline received payload card,
 * and automatic encrypted vault archiving.
 *
 * Conforms to UI Gate specifications:
 * - Inline animated card right below the waterfall keeping the live spectrogram in view.
 * - Segmented progress bar showing "Locked, reading header…" before block count is known,
 *   and block segments + remaining seconds once header is decoded.
 * - Honest error correction reporting ("Noise detected · Auto-repaired") only when RS corrected bytes.
 * - Auto-saves to encrypted vault with a one-tap star button.
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
                FilterChip(
                    selected = uiState.band == Band.RETRO,
                    onClick = { viewModel.setBand(Band.RETRO) },
                    label = { Text(stringResource(R.string.receive_band_retro)) }
                )
                FilterChip(
                    selected = uiState.band == Band.STEALTH,
                    onClick = { viewModel.setBand(Band.STEALTH) },
                    label = { Text(stringResource(R.string.receive_band_stealth)) }
                )
            }

            StatusPill(status = uiState.status)
        }

        // --- 3. Hero Waterfall Card ---
        ShoutCard(
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
                        uiState.status == AcousticStatus.SUCCESS -> stringResource(R.string.receive_hero_success)
                        uiState.isPreambleLocked -> String.format(
                            Locale.US,
                            stringResource(R.string.receive_hero_locked),
                            String.format(Locale.US, "%.1f", uiState.snrDb)
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
                        color = if (uiState.isPreambleLocked) ChirpTheme.colors.tertiary else ChirpTheme.colors.onSurfaceVariant
                    )

                    Text(
                        text = "${uiState.sampleRateHz / 1000} kHz",
                        style = ChirpTheme.typography.labelSmall,
                        color = ChirpTheme.colors.primary
                    )
                }
            }
        }

        // --- 4. In-Flight Progress or Success Card (UI Gate Option 1: Inline Card) ---
        if (uiState.status == AcousticStatus.SUCCESS && uiState.lastReceivedPayload != null) {
            ReceivedPayloadCard(
                payload = uiState.lastReceivedPayload!!,
                snrDb = uiState.snrDb,
                profileSummary = uiState.activeProfileSummary,
                isStarred = uiState.isStarred,
                isSavedToVault = uiState.isSavedToVault,
                hasAutoRepairedNoise = uiState.hasAutoRepairedNoise,
                onToggleStar = { viewModel.toggleStar() },
                onReceiveAnother = { viewModel.resetReception() },
                modifier = Modifier.fillMaxWidth()
            )
        } else if (uiState.isPreambleLocked || uiState.status == AcousticStatus.RECEIVING || uiState.isReadingHeader) {
            ShoutCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(ChirpTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)
                ) {
                    SegmentedProgressBar(
                        receivedBlocks = uiState.currentFragment,
                        totalBlocks = uiState.totalFragments,
                        estimatedRemainingSeconds = uiState.estimatedRemainingSeconds,
                        hasAutoRepairedNoise = uiState.hasAutoRepairedNoise,
                        isReadingHeader = uiState.isReadingHeader,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // --- 5. Retry Hint Banner on Failed Reception (UI Gate A3) ---
        AnimatedVisibility(
            visible = uiState.retryHint != null,
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
                    .padding(ChirpTheme.spacing.md)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)
                ) {
                    Text(
                        text = "ℹ",
                        style = ChirpTheme.typography.titleMedium,
                        color = ChirpTheme.colors.warning
                    )
                    Text(
                        text = uiState.retryHint ?: stringResource(R.string.receive_retry_hint),
                        style = ChirpTheme.typography.bodyMedium,
                        color = ChirpTheme.colors.onSurface
                    )
                }
            }
        }

        // --- 6. Signal & Volume Input Meters ---
        ShoutCard(
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

        // --- 7. Hardware Alerts (Clipping or Silence) ---
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

        // --- 8. Hardware Source Info Strip ---
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
