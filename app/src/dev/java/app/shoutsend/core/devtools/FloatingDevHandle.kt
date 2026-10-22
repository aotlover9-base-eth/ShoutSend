package app.shoutsend.core.devtools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import app.shoutsend.core.designsystem.ChirpTheme
import app.shoutsend.core.designsystem.ThemeController
import kotlin.math.roundToInt

/**
 * Floating draggable handle on the screen edge that opens the developer panel when tapped.
 */
@Composable
fun FloatingDevHandle(
    themeController: ThemeController,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val viewModel = remember(themeController) { DevPanelViewModel(themeController, scope) }
    var showPanel by remember { mutableStateOf(false) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Floating pill on right edge
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset { IntOffset(0, offsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, dragAmount ->
                        change.consume()
                        offsetY += dragAmount
                    }
                }
                .background(
                    color = ChirpTheme.colors.surfaceContainerHigh,
                    shape = ChirpTheme.shapes.button
                )
                .border(
                    width = ChirpTheme.strokes.thin,
                    color = ChirpTheme.colors.primary,
                    shape = ChirpTheme.shapes.button
                )
                .clickable { showPanel = true }
                .padding(
                    horizontal = ChirpTheme.spacing.sm,
                    vertical = ChirpTheme.spacing.xs
                )
        ) {
            Text(
                text = "DEV",
                style = ChirpTheme.typography.radioReadoutSmall,
                color = ChirpTheme.colors.primary
            )
        }

        if (showPanel) {
            DeveloperPanel(
                viewModel = viewModel,
                onDismiss = { showPanel = false }
            )
        }
    }
}
