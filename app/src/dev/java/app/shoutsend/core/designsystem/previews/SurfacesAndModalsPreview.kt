package app.shoutsend.core.designsystem.previews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.shoutsend.core.designsystem.ChirpTheme
import app.shoutsend.core.designsystem.DesignTokens
import app.shoutsend.core.designsystem.components.CdButton
import app.shoutsend.core.designsystem.components.CdCenterAlignedTopAppBar
import app.shoutsend.core.designsystem.components.CdHorizontalDivider
import app.shoutsend.core.designsystem.components.CdTextButton
import app.shoutsend.core.designsystem.components.CdTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SurfacesPreviewContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChirpTheme.colors.background)
            .padding(ChirpTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.md)
    ) {
        Text(
            text = "Top Bars",
            style = ChirpTheme.typography.titleMedium,
            color = ChirpTheme.colors.onBackground
        )

        CdTopAppBar(
            title = { Text("ShoutSend Dev") },
            actions = {
                CdTextButton(onClick = {}) { Text("Help") }
            }
        )

        CdCenterAlignedTopAppBar(
            title = { Text("Acoustic Receiver") }
        )

        CdHorizontalDivider()

        Text(
            text = "Dialog Layout Preview",
            style = ChirpTheme.typography.titleMedium,
            color = ChirpTheme.colors.onBackground
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = ChirpTheme.colors.surfaceContainerHigh,
                    shape = ChirpTheme.shapes.dialog
                )
                .padding(ChirpTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.md)
        ) {
            Text(
                text = "Calibrate Microphone",
                style = ChirpTheme.typography.headlineSmall,
                color = ChirpTheme.colors.onSurface
            )
            Text(
                text = "Near-ultrasound response requires a quiet environment for test chirp sweep.",
                style = ChirpTheme.typography.bodyMedium,
                color = ChirpTheme.colors.onSurfaceVariant
            )
            CdButton(
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Begin Calibration")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Surfaces - Dark")
@Composable
fun SurfacesPreviewDark() {
    ChirpTheme(tokens = DesignTokens.defaultDark()) {
        SurfacesPreviewContent()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Surfaces - Light")
@Composable
fun SurfacesPreviewLight() {
    ChirpTheme(tokens = DesignTokens.defaultLight()) {
        SurfacesPreviewContent()
    }
}
