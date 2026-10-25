package app.shoutsend.feature.receive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.shoutsend.core.designsystem.ChirpTheme
import app.shoutsend.core.designsystem.components.LevelMeter
import app.shoutsend.core.designsystem.components.NeonCard
import app.shoutsend.core.designsystem.components.RadioReadout
import app.shoutsend.core.designsystem.components.ReadoutScale
import app.shoutsend.core.designsystem.components.SignalMeter
import app.shoutsend.core.designsystem.components.StatusPill

@Composable
fun ReceiveScreen(
    viewModel: ReceiveViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(ChirpTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.md)
    ) {
        NeonCard(
            modifier = Modifier.fillMaxWidth()
        ) {
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
                        text = "Acoustic Receiver",
                        style = ChirpTheme.typography.titleLarge,
                        color = ChirpTheme.colors.primary
                    )
                    StatusPill(status = uiState.status)
                }

                Text(
                    text = "Awaiting ultrasonic or audible MFSK data frames via microphone.",
                    style = ChirpTheme.typography.bodyMedium,
                    color = ChirpTheme.colors.onSurfaceVariant
                )

                RadioReadout(
                    label = "Acoustic Detection",
                    value = "GOERTZEL ACTIVE",
                    scale = ReadoutScale.MEDIUM
                )
            }
        }

        Text(
            text = "Carrier Signal Strength",
            style = ChirpTheme.typography.titleSmall,
            color = ChirpTheme.colors.onBackground
        )

        SignalMeter(
            snrDb = uiState.snrDb,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Microphone Input Level",
            style = ChirpTheme.typography.titleSmall,
            color = ChirpTheme.colors.onBackground
        )

        LevelMeter(
            levelDbfs = uiState.levelDbfs,
            peakDbfs = uiState.peakDbfs,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
