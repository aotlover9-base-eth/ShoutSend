package app.shoutsend.core.designsystem.previews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.shoutsend.core.designsystem.ChirpTheme
import app.shoutsend.core.designsystem.DesignTokens
import app.shoutsend.core.designsystem.ThemeMode
import app.shoutsend.core.designsystem.components.CdButton
import app.shoutsend.core.designsystem.components.CdFilledIconButton
import app.shoutsend.core.designsystem.components.CdIconButton
import app.shoutsend.core.designsystem.components.CdOutlinedButton
import app.shoutsend.core.designsystem.components.CdOutlinedIconButton
import app.shoutsend.core.designsystem.components.CdTextButton
import app.shoutsend.core.designsystem.components.CdTonalButton
import app.shoutsend.core.designsystem.components.CdTonalIconButton

@Composable
private fun ButtonsPreviewContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChirpTheme.colors.background)
            .padding(ChirpTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.md)
    ) {
        Text(
            text = "Action Buttons",
            style = ChirpTheme.typography.titleMedium,
            color = ChirpTheme.colors.onBackground
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CdButton(onClick = {}) { Text("Transmit") }
            CdTonalButton(onClick = {}) { Text("Tune") }
            CdOutlinedButton(onClick = {}) { Text("Mute") }
            CdTextButton(onClick = {}) { Text("Cancel") }
        }

        Text(
            text = "Disabled State",
            style = ChirpTheme.typography.titleSmall,
            color = ChirpTheme.colors.onBackground
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CdButton(onClick = {}, enabled = false) { Text("Transmit") }
            CdTonalButton(onClick = {}, enabled = false) { Text("Tune") }
            CdOutlinedButton(onClick = {}, enabled = false) { Text("Mute") }
        }

        Text(
            text = "Icon Buttons",
            style = ChirpTheme.typography.titleSmall,
            color = ChirpTheme.colors.onBackground
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CdFilledIconButton(onClick = {}) { Text("▶") }
            CdTonalIconButton(onClick = {}) { Text("⚙") }
            CdOutlinedIconButton(onClick = {}) { Text("⏹") }
            CdIconButton(onClick = {}) { Text("✕") }
        }
    }
}

@Preview(name = "Buttons - Cyber Radio Dark")
@Composable
fun ButtonsPreviewDark() {
    ChirpTheme(tokens = DesignTokens.defaultDark()) {
        ButtonsPreviewContent()
    }
}

@Preview(name = "Buttons - Cyber Radio Light")
@Composable
fun ButtonsPreviewLight() {
    ChirpTheme(tokens = DesignTokens.defaultLight()) {
        ButtonsPreviewContent()
    }
}
