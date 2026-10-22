package app.shoutsend.core.designsystem.previews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import app.shoutsend.core.designsystem.components.CdAssistChip
import app.shoutsend.core.designsystem.components.CdCard
import app.shoutsend.core.designsystem.components.CdElevatedCard
import app.shoutsend.core.designsystem.components.CdFilterChip
import app.shoutsend.core.designsystem.components.CdInputChip
import app.shoutsend.core.designsystem.components.CdOutlinedCard
import app.shoutsend.core.designsystem.components.CdOutlinedTextField
import app.shoutsend.core.designsystem.components.CdSuggestionChip
import app.shoutsend.core.designsystem.components.CdSwitch
import app.shoutsend.core.designsystem.components.CdTextField

@Composable
private fun CardsAndFieldsPreviewContent() {
    var textVal by remember { mutableStateOf("18500 Hz carrier") }
    var switchVal by remember { mutableStateOf(true) }
    var chipSelected by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChirpTheme.colors.background)
            .padding(ChirpTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.md)
    ) {
        Text(
            text = "Cards & Surfaces",
            style = ChirpTheme.typography.titleMedium,
            color = ChirpTheme.colors.onBackground
        )

        CdCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(ChirpTheme.spacing.md)) {
                Text(
                    text = "Standard Card (SurfaceContainer)",
                    style = ChirpTheme.typography.titleSmall,
                    color = ChirpTheme.colors.onSurface
                )
                Text(
                    text = "MFSK16 48kHz Acoustic Channel",
                    style = ChirpTheme.typography.bodyMedium,
                    color = ChirpTheme.colors.onSurfaceVariant
                )
            }
        }

        CdOutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(ChirpTheme.spacing.md)) {
                Text(
                    text = "Outlined Card (Border Token)",
                    style = ChirpTheme.typography.titleSmall,
                    color = ChirpTheme.colors.onSurface
                )
                Text(
                    text = "Stealth Mode: 17,600 - 19,850 Hz",
                    style = ChirpTheme.typography.radioReadoutMedium,
                    color = ChirpTheme.colors.primary
                )
            }
        }

        CdElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(ChirpTheme.spacing.md)) {
                Text(
                    text = "Elevated Card",
                    style = ChirpTheme.typography.titleSmall,
                    color = ChirpTheme.colors.onSurface
                )
            }
        }

        Text(
            text = "Input Fields",
            style = ChirpTheme.typography.titleMedium,
            color = ChirpTheme.colors.onBackground
        )

        CdTextField(
            value = textVal,
            onValueChange = { textVal = it },
            label = { Text("Payload") },
            modifier = Modifier.fillMaxWidth()
        )

        CdOutlinedTextField(
            value = textVal,
            onValueChange = { textVal = it },
            label = { Text("Outlined Payload") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Acoustic Carrier Active",
                style = ChirpTheme.typography.bodyLarge,
                color = ChirpTheme.colors.onBackground
            )
            CdSwitch(
                checked = switchVal,
                onCheckedChange = { switchVal = it }
            )
        }

        Text(
            text = "Chips",
            style = ChirpTheme.typography.titleSmall,
            color = ChirpTheme.colors.onBackground
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)
        ) {
            CdFilterChip(
                selected = chipSelected,
                onClick = { chipSelected = !chipSelected },
                label = { Text("Stealth") }
            )
            CdAssistChip(
                onClick = {},
                label = { Text("Retro") }
            )
            CdSuggestionChip(
                onClick = {},
                label = { Text("Robust") }
            )
        }
    }
}

@Preview(name = "Cards & Fields - Dark")
@Composable
fun CardsAndFieldsPreviewDark() {
    ChirpTheme(tokens = DesignTokens.defaultDark()) {
        CardsAndFieldsPreviewContent()
    }
}

@Preview(name = "Cards & Fields - Light")
@Composable
fun CardsAndFieldsPreviewLight() {
    ChirpTheme(tokens = DesignTokens.defaultLight()) {
        CardsAndFieldsPreviewContent()
    }
}
