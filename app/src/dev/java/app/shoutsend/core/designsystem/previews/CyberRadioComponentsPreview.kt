package app.shoutsend.core.designsystem.previews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.shoutsend.core.designsystem.ChirpTheme
import app.shoutsend.core.designsystem.DesignTokens
import app.shoutsend.core.designsystem.components.AcousticStatus
import app.shoutsend.core.designsystem.components.BrandMark
import app.shoutsend.core.designsystem.components.ByteMeter
import app.shoutsend.core.designsystem.components.LevelMeter
import app.shoutsend.core.designsystem.components.ModeSelector
import app.shoutsend.core.designsystem.components.NeonCard
import app.shoutsend.core.designsystem.components.RadioReadout
import app.shoutsend.core.designsystem.components.ReadoutScale
import app.shoutsend.core.designsystem.components.ScanlineOverlay
import app.shoutsend.core.designsystem.components.SignalMeter
import app.shoutsend.core.designsystem.components.SpeedSelector
import app.shoutsend.core.designsystem.components.StatusPill
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.Speed

@Composable
private fun CyberRadioComponentsPreviewContent() {
    var selectedBand by remember { mutableStateOf(Band.RETRO) }
    var selectedSpeed by remember { mutableStateOf(Speed.NORMAL) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChirpTheme.colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(ChirpTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.md)
        ) {
            Text(
                text = "Cyber Radio Components",
                style = ChirpTheme.typography.titleLarge,
                color = ChirpTheme.colors.primary
            )

            // Brand Mark
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.md)
            ) {
                BrandMark(
                    modifier = Modifier.size(ChirpTheme.sizes.iconXl)
                )
                Text(
                    text = "Acoustic Transmission Engine",
                    style = ChirpTheme.typography.titleSmall,
                    color = ChirpTheme.colors.onBackground
                )
            }

            // Status Pills
            Text(
                text = "Status Indicators",
                style = ChirpTheme.typography.titleSmall,
                color = ChirpTheme.colors.onBackground
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)
            ) {
                StatusPill(status = AcousticStatus.IDLE)
                StatusPill(status = AcousticStatus.LISTENING)
                StatusPill(status = AcousticStatus.LOCKED)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)
            ) {
                StatusPill(status = AcousticStatus.RECEIVING)
                StatusPill(status = AcousticStatus.SUCCESS)
                StatusPill(status = AcousticStatus.ERROR)
            }

            // Neon Card with Radio Readouts
            NeonCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(ChirpTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm)
                ) {
                    Text(
                        text = "Carrier Frequency Readouts",
                        style = ChirpTheme.typography.titleSmall,
                        color = ChirpTheme.colors.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        RadioReadout(
                            label = "Center Freq",
                            value = "17,600 Hz",
                            scale = ReadoutScale.MEDIUM
                        )
                        RadioReadout(
                            label = "Bandwidth",
                            value = "2,200 Hz",
                            scale = ReadoutScale.MEDIUM
                        )
                        RadioReadout(
                            label = "Tones",
                            value = "16 MFSK",
                            scale = ReadoutScale.MEDIUM
                        )
                    }
                }
            }

            // Meters
            Text(
                text = "Acoustic Signal Meters",
                style = ChirpTheme.typography.titleSmall,
                color = ChirpTheme.colors.onBackground
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SignalMeter(snrDb = 4.0)
                SignalMeter(snrDb = 14.5)
                SignalMeter(snrDb = 22.0)
            }

            LevelMeter(
                levelDbfs = -18.5f,
                peakDbfs = -9.2f
            )

            LevelMeter(
                levelDbfs = -1.5f,
                peakDbfs = 0.0f
            )

            // Byte Budget Meter
            Text(
                text = "Frame Byte Meter",
                style = ChirpTheme.typography.titleSmall,
                color = ChirpTheme.colors.onBackground
            )
            ByteMeter(
                bytesUsed = 120,
                maxBytes = 320,
                estimatedDurationSeconds = 4.8
            )
            ByteMeter(
                bytesUsed = 310,
                maxBytes = 320,
                estimatedDurationSeconds = 12.4
            )

            // Selectors
            Text(
                text = "Mode & Speed Selectors",
                style = ChirpTheme.typography.titleSmall,
                color = ChirpTheme.colors.onBackground
            )
            ModeSelector(
                selectedBand = selectedBand,
                onBandSelected = { selectedBand = it }
            )
            SpeedSelector(
                selectedSpeed = selectedSpeed,
                onSpeedSelected = { selectedSpeed = it }
            )
        }

        ScanlineOverlay()
    }
}

@Preview(name = "Cyber Radio Components - Dark")
@Composable
fun CyberRadioComponentsPreviewDark() {
    ChirpTheme(tokens = DesignTokens.defaultDark()) {
        CyberRadioComponentsPreviewContent()
    }
}

@Preview(name = "Cyber Radio Components - Light")
@Composable
fun CyberRadioComponentsPreviewLight() {
    ChirpTheme(tokens = DesignTokens.defaultLight()) {
        CyberRadioComponentsPreviewContent()
    }
}
