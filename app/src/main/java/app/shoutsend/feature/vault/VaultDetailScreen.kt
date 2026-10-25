package app.shoutsend.feature.vault

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.shoutsend.core.designsystem.ChirpTheme
import app.shoutsend.core.designsystem.components.CdButton
import app.shoutsend.core.designsystem.components.NeonCard
import app.shoutsend.core.designsystem.components.RadioReadout
import app.shoutsend.core.designsystem.components.ReadoutScale

@Composable
fun VaultDetailScreen(
    id: Long,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                    text = "Transmission Detail",
                    style = ChirpTheme.typography.titleLarge,
                    color = ChirpTheme.colors.primary
                )
                RadioReadout(
                    label = "Transmission ID",
                    value = "#$id",
                    scale = ReadoutScale.LARGE
                )
                Text(
                    text = "Acoustic payload decrypted from offline room audio.",
                    style = ChirpTheme.typography.bodyMedium,
                    color = ChirpTheme.colors.onSurfaceVariant
                )
            }
        }

        CdButton(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Vault")
        }
    }
}
