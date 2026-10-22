package app.shoutsend.core.designsystem.components

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import app.shoutsend.core.designsystem.ChirpTheme

/**
 * Standard horizontal divider wrapper using ChirpTheme strokes and outline tokens.
 */
@Composable
fun CdHorizontalDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = ChirpTheme.strokes.thin,
    color: Color = ChirpTheme.colors.outlineVariant
) {
    HorizontalDivider(
        modifier = modifier,
        thickness = thickness,
        color = color
    )
}

/**
 * Standard vertical divider wrapper using ChirpTheme strokes and outline tokens.
 */
@Composable
fun CdVerticalDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = ChirpTheme.strokes.thin,
    color: Color = ChirpTheme.colors.outlineVariant
) {
    VerticalDivider(
        modifier = modifier,
        thickness = thickness,
        color = color
    )
}
