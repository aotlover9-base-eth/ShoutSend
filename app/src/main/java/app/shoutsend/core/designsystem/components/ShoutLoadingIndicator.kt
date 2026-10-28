package app.shoutsend.core.designsystem.components

import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LoadingIndicatorDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.graphics.shapes.RoundedPolygon
import app.shoutsend.core.designsystem.ShoutSendTheme

/**
 * Material 3 Expressive morphing loading indicators.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ShoutLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    polygons: List<RoundedPolygon> = LoadingIndicatorDefaults.IndeterminateIndicatorPolygons
) {
    LoadingIndicator(
        modifier = modifier,
        color = color,
        polygons = polygons
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ShoutContainedLoadingIndicator(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    indicatorColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    containerShape: Shape = ShoutSendTheme.shapes.card,
    polygons: List<RoundedPolygon> = LoadingIndicatorDefaults.IndeterminateIndicatorPolygons
) {
    ContainedLoadingIndicator(
        modifier = modifier,
        containerColor = containerColor,
        indicatorColor = indicatorColor,
        containerShape = containerShape,
        polygons = polygons
    )
}
