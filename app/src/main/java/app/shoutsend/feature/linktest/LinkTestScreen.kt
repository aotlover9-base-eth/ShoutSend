package app.shoutsend.feature.linktest

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.shoutsend.R
import app.shoutsend.core.designsystem.ChirpTheme
import app.shoutsend.core.designsystem.components.CdButton
import app.shoutsend.core.designsystem.components.CdIconButton
import app.shoutsend.core.designsystem.components.NeonCard
import app.shoutsend.core.designsystem.components.RadioReadout
import app.shoutsend.core.designsystem.components.ReadoutScale

@Composable
fun LinkTestScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ChirpTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(ChirpTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.md)
    ) {
        // Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm)
        ) {
            val backDesc = stringResource(R.string.vault_action_back_desc)
            CdIconButton(
                onClick = onNavigateBack,
                modifier = Modifier.semantics { contentDescription = backDesc }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = colors.primary
                )
            }
            Text(
                text = stringResource(R.string.link_test_title),
                style = ChirpTheme.typography.titleLarge,
                color = colors.primary
            )
        }

        NeonCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(ChirpTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm)
            ) {
                Text(
                    text = stringResource(R.string.link_test_title),
                    style = ChirpTheme.typography.titleMedium,
                    color = colors.primary
                )
                Text(
                    text = stringResource(R.string.link_test_desc),
                    style = ChirpTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
                RadioReadout(
                    label = "Acoustic Channel State",
                    value = "MONITORING",
                    scale = ReadoutScale.MEDIUM
                )
            }
        }

        CdButton(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.vault_clear_dialog_cancel))
        }
    }
}
