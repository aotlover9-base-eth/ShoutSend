package app.shoutsend.core.designsystem.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import app.shoutsend.core.designsystem.ChirpTheme

/**
 * NavigationBar wrapper utilizing ChirpTheme tokens for container color and tonal elevation.
 */
@Composable
fun CdNavigationBar(
    modifier: Modifier = Modifier,
    containerColor: Color = ChirpTheme.colors.surfaceContainer,
    contentColor: Color = ChirpTheme.colors.onSurface,
    tonalElevation: Dp = ChirpTheme.elevation.level0,
    windowInsets: WindowInsets = NavigationBarDefaults.windowInsets,
    content: @Composable RowScope.() -> Unit
) {
    NavigationBar(
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        windowInsets = windowInsets,
        content = content
    )
}

/**
 * NavigationBarItem wrapper with tokenized active/inactive colors.
 */
@Composable
fun RowScope.CdNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
    alwaysShowLabel: Boolean = true,
    colors: NavigationBarItemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = ChirpTheme.colors.primary,
        selectedTextColor = ChirpTheme.colors.primary,
        indicatorColor = ChirpTheme.colors.primaryContainer,
        unselectedIconColor = ChirpTheme.colors.onSurfaceVariant,
        unselectedTextColor = ChirpTheme.colors.onSurfaceVariant
    )
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = icon,
        modifier = modifier,
        enabled = enabled,
        label = label,
        alwaysShowLabel = alwaysShowLabel,
        colors = colors
    )
}
