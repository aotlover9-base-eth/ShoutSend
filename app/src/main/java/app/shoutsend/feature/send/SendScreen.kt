package app.shoutsend.feature.send

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.shoutsend.core.designsystem.ChirpTheme
import app.shoutsend.core.designsystem.components.CdButton
import app.shoutsend.core.designsystem.components.ModeSelector
import app.shoutsend.core.designsystem.components.NeonCard
import app.shoutsend.core.designsystem.components.RadioReadout
import app.shoutsend.core.designsystem.components.ReadoutScale
import app.shoutsend.core.designsystem.components.SpeedSelector
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.Speed

@Composable
fun SendScreen(
    viewModel: SendViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedBand by remember { mutableStateOf(Band.RETRO) }
    var selectedSpeed by remember { mutableStateOf(Speed.NORMAL) }

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
                Text(
                    text = "Acoustic Transmitter",
                    style = ChirpTheme.typography.titleLarge,
                    color = ChirpTheme.colors.primary
                )
                Text(
                    text = "Transmit arbitrary payloads offline via sound wave synthesis.",
                    style = ChirpTheme.typography.bodyMedium,
                    color = ChirpTheme.colors.onSurfaceVariant
                )
                RadioReadout(
                    label = "Modulation State",
                    value = uiState.statusMessage,
                    scale = ReadoutScale.MEDIUM
                )
            }
        }

        ModeSelector(
            selectedBand = selectedBand,
            onBandSelected = { selectedBand = it }
        )

        SpeedSelector(
            selectedSpeed = selectedSpeed,
            onSpeedSelected = { selectedSpeed = it }
        )

        CdButton(
            onClick = {},
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Emit Soundwave")
        }
    }
}
