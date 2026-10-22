package app.shoutsend.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import app.shoutsend.core.designsystem.ChirpTheme

/**
 * Standard filled/surface container card wrapper using ChirpTheme tokens.
 */
@Composable
fun CdCard(
    modifier: Modifier = Modifier,
    shape: Shape = ChirpTheme.shapes.card,
    colors: CardColors = CardDefaults.cardColors(
        containerColor = ChirpTheme.colors.surfaceContainer,
        contentColor = ChirpTheme.colors.onSurface
    ),
    elevation: CardElevation = CardDefaults.cardElevation(
        defaultElevation = ChirpTheme.elevation.level0
    ),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        content = content
    )
}

/**
 * Clickable standard card wrapper.
 */
@Composable
fun CdCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ChirpTheme.shapes.card,
    colors: CardColors = CardDefaults.cardColors(
        containerColor = ChirpTheme.colors.surfaceContainer,
        contentColor = ChirpTheme.colors.onSurface
    ),
    elevation: CardElevation = CardDefaults.cardElevation(
        defaultElevation = ChirpTheme.elevation.level0
    ),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        content = content
    )
}

/**
 * Outlined card wrapper with theme border stroke.
 */
@Composable
fun CdOutlinedCard(
    modifier: Modifier = Modifier,
    shape: Shape = ChirpTheme.shapes.card,
    colors: CardColors = CardDefaults.outlinedCardColors(
        containerColor = ChirpTheme.colors.surface,
        contentColor = ChirpTheme.colors.onSurface
    ),
    elevation: CardElevation = CardDefaults.outlinedCardElevation(),
    border: BorderStroke = BorderStroke(
        width = ChirpTheme.strokes.thin,
        color = ChirpTheme.colors.outlineVariant
    ),
    content: @Composable ColumnScope.() -> Unit
) {
    OutlinedCard(
        modifier = modifier,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        content = content
    )
}

/**
 * Clickable outlined card wrapper.
 */
@Composable
fun CdOutlinedCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ChirpTheme.shapes.card,
    colors: CardColors = CardDefaults.outlinedCardColors(
        containerColor = ChirpTheme.colors.surface,
        contentColor = ChirpTheme.colors.onSurface
    ),
    elevation: CardElevation = CardDefaults.outlinedCardElevation(),
    border: BorderStroke = BorderStroke(
        width = ChirpTheme.strokes.thin,
        color = ChirpTheme.colors.outlineVariant
    ),
    content: @Composable ColumnScope.() -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        content = content
    )
}

/**
 * Elevated card wrapper with subtle shadow token elevation.
 */
@Composable
fun CdElevatedCard(
    modifier: Modifier = Modifier,
    shape: Shape = ChirpTheme.shapes.card,
    colors: CardColors = CardDefaults.elevatedCardColors(
        containerColor = ChirpTheme.colors.surfaceContainerLow,
        contentColor = ChirpTheme.colors.onSurface
    ),
    elevation: CardElevation = CardDefaults.elevatedCardElevation(
        defaultElevation = ChirpTheme.elevation.level1
    ),
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = modifier,
        shape = shape,
        colors = colors,
        elevation = elevation,
        content = content
    )
}
