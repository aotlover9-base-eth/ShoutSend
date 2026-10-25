package app.shoutsend.feature.vault

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
fun VaultScreen(
    viewModel: VaultViewModel,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToLinkTest: () -> Unit,
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
                Text(
                    text = "Acoustic Message Vault",
                    style = ChirpTheme.typography.titleLarge,
                    color = ChirpTheme.colors.primary
                )
                Text(
                    text = "Local history of received encrypted and unencrypted acoustic transmissions.",
                    style = ChirpTheme.typography.bodyMedium,
                    color = ChirpTheme.colors.onSurfaceVariant
                )
                RadioReadout(
                    label = "Stored Transmissions",
                    value = "${uiState.entryCount} FRAMES",
                    scale = ReadoutScale.MEDIUM
                )
            }
        }

        CdButton(
            onClick = { onNavigateToDetail(1L) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Inspect Sample Transmission (#1)")
        }

        CdOutlinedButton(
            onClick = onNavigateToLinkTest,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Launch Link Diagnostic Test")
        }
    }
}
