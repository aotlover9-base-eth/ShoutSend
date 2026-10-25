package app.shoutsend.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.shoutsend.core.designsystem.ChirpTheme
import app.shoutsend.core.designsystem.components.CdButton
import app.shoutsend.core.designsystem.components.CdOutlinedButton
import app.shoutsend.core.designsystem.components.NeonCard
import app.shoutsend.core.designsystem.components.RadioReadout
import app.shoutsend.core.designsystem.components.ReadoutScale

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCalibration: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

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
                    text = "Application Settings",
                    style = ChirpTheme.typography.titleLarge,
                    color = ChirpTheme.colors.primary
                )
                Text(
                    text = "Acoustic modem profiles, haptic feedback, and offline vault retention.",
                    style = ChirpTheme.typography.bodyMedium,
                    color = ChirpTheme.colors.onSurfaceVariant
                )
                RadioReadout(
                    label = "Default Acoustic Band",
                    value = settings.band.name,
                    scale = ReadoutScale.MEDIUM
                )
                RadioReadout(
                    label = "Default Transmission Speed",
                    value = settings.speed.name,
                    scale = ReadoutScale.MEDIUM
                )
            }
        }

        CdOutlinedButton(
            onClick = onNavigateToCalibration,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Hardware Calibration Engine")
        }

        CdButton(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}
