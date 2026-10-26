package app.shoutsend.core.devtools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import app.shoutsend.core.designsystem.ChirpTheme
import app.shoutsend.core.designsystem.components.CdButton
import app.shoutsend.core.designsystem.components.CdCard
import app.shoutsend.core.designsystem.components.CdFilterChip
import app.shoutsend.core.designsystem.components.CdOutlinedButton
import app.shoutsend.core.designsystem.components.CdTextButton
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.Speed

/**
 * Developer DSP Lab screen enabling direct hardware/modem inspection:
 * - 16-bar MFSK energy meter inspecting live Goertzel bin powers
 * - Synthetic calibration tone injector
 * - Real-time SNR and noise floor probe
 * - Additive noise simulation toggle
 */
@Composable
fun DspLabContent(
    viewModel: DspLabViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = ChirpTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.md)
    ) {
        // 1. Band & Speed Selector Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)) {
                Text(
                    text = "Acoustic Band & Speed Profile",
                    style = ChirpTheme.typography.labelMedium,
                    color = ChirpTheme.colors.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)
                ) {
                    Band.entries.forEach { b ->
                        CdFilterChip(
                            selected = uiState.band == b,
                            onClick = { viewModel.selectBand(b) },
                            label = { Text(b.name) }
                        )
                    }
                    Spacer(modifier = Modifier.width(ChirpTheme.spacing.sm))
                    Speed.entries.forEach { s ->
                        CdFilterChip(
                            selected = uiState.speed == s,
                            onClick = { viewModel.selectSpeed(s) },
                            label = { Text(s.name) }
                        )
                    }
                }
            }
        }

        // 2. 16-Bar Goertzel Power Visualizer
        item {
            CdCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(ChirpTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm)
                ) {
                    Text(
                        text = "16-Tone MFSK Power Spectrum (Goertzel Filters)",
                        style = ChirpTheme.typography.titleSmall,
                        color = ChirpTheme.colors.primary
                    )

                    // 16 vertical power bars
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ChirpTheme.spacing.xxl * 2)
                            .background(ChirpTheme.colors.surfaceContainerLowest)
                            .padding(ChirpTheme.spacing.xs),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        for (i in 0 until 16) {
                            val power = if (i < uiState.binPowers.size) uiState.binPowers[i] else 0.0f
                            // Normalize power: 0.0f to 0.64f peak for 0.8 amplitude sine wave
                            val fraction = (power / 0.64f).coerceIn(0.02f, 1.0f)
                            val isDetected = i == uiState.detectedToneIndex
                            val barColor = when {
                                isDetected -> ChirpTheme.colors.signalPeak
                                i == uiState.activeToneIndex -> ChirpTheme.colors.signalActive
                                else -> ChirpTheme.colors.signalIdle.copy(alpha = ChirpTheme.opacity.subtle)
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1.0f, fill = false)
                                        .fillMaxHeight(fraction)
                                        .width(ChirpTheme.spacing.sm)
                                        .clip(ChirpTheme.shapes.extraSmall)
                                        .background(barColor)
                                )
                                Spacer(modifier = Modifier.height(ChirpTheme.spacing.xxs))
                                Text(
                                    text = "$i",
                                    style = ChirpTheme.typography.radioReadoutSmall,
                                    color = if (isDetected) ChirpTheme.colors.signalPeak else ChirpTheme.colors.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Telemetry & Probe Readout
        item {
            CdCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(ChirpTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)
                ) {
                    Text(
                        text = "Real-Time Demodulator Telemetry",
                        style = ChirpTheme.typography.titleSmall,
                        color = ChirpTheme.colors.secondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Peak Frequency:",
                            style = ChirpTheme.typography.bodySmall,
                            color = ChirpTheme.colors.onSurfaceVariant
                        )
                        Text(
                            text = if (uiState.peakFrequencyHz > 0) "${uiState.peakFrequencyHz.toInt()} Hz" else "---",
                            style = ChirpTheme.typography.radioReadoutSmall,
                            color = ChirpTheme.colors.onSurface
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Estimated SNR:",
                            style = ChirpTheme.typography.bodySmall,
                            color = ChirpTheme.colors.onSurfaceVariant
                        )
                        Text(
                            text = "%.1f dB".format(uiState.snrDb),
                            style = ChirpTheme.typography.radioReadoutSmall,
                            color = if (uiState.snrDb >= 12.0) ChirpTheme.colors.success else ChirpTheme.colors.warning
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Status:",
                            style = ChirpTheme.typography.bodySmall,
                            color = ChirpTheme.colors.onSurfaceVariant
                        )
                        Text(
                            text = uiState.statusMessage,
                            style = ChirpTheme.typography.radioReadoutSmall,
                            color = ChirpTheme.colors.primary
                        )
                    }
                }
            }
        }

        // 4. Synthetic Tone Injection Keypad
        item {
            Column(verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)) {
                Text(
                    text = "Inject Synthetic Test Tone (0..15)",
                    style = ChirpTheme.typography.labelMedium,
                    color = ChirpTheme.colors.onSurfaceVariant
                )

                // 4x4 Grid of Tone Buttons
                for (row in 0 until 4) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)
                    ) {
                        for (col in 0 until 4) {
                            val toneIdx = row * 4 + col
                            val isSelected = toneIdx == uiState.activeToneIndex
                            CdOutlinedButton(
                                onClick = { viewModel.injectTone(toneIdx) },
                                modifier = Modifier.weight(1.0f)
                            ) {
                                Text(
                                    text = "T$toneIdx",
                                    style = ChirpTheme.typography.labelMedium,
                                    color = if (isSelected) ChirpTheme.colors.primary else ChirpTheme.colors.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(ChirpTheme.spacing.xs))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)
                ) {
                    CdButton(
                        onClick = { viewModel.toggleNoiseInjection() },
                        modifier = Modifier.weight(1.0f)
                    ) {
                        Text(
                            text = if (uiState.isInjectingNoise) "Disable Noise" else "Add Noise (+12dB)",
                            style = ChirpTheme.typography.labelSmall
                        )
                    }
                    CdTextButton(
                        onClick = { viewModel.clear() },
                        modifier = Modifier.weight(0.5f)
                    ) {
                        Text("Reset", style = ChirpTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
