package app.shoutsend.core.designsystem.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemColors
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.shoutsend.core.designsystem.ChirpTheme

/**
 * NavigationRail wrapper utilizing ChirpTheme tokens for wide screen layouts.
 */
@Composable
fun CdNavigationRail(
    modifier: Modifier = Modifier,
    containerColor: Color = ChirpTheme.colors.surfaceContainer,
    contentColor: Color = ChirpTheme.colors.onSurface,
    header: @Composable (ColumnScope.() -> Unit)? = null,
    windowInsets: WindowInsets = NavigationRailDefaults.windowInsets,
    content: @Composable ColumnScope.() -> Unit
) {
    NavigationRail(
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor,
        header = header,
        windowInsets = windowInsets,
        content = content
    )
}

/**
 * NavigationRailItem wrapper with tokenized active/inactive colors.
 */
@Composable
fun CdNavigationRailItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
    alwaysShowLabel: Boolean = true,
    colors: NavigationRailItemColors = NavigationRailItemDefaults.colors(
        selectedIconColor = ChirpTheme.colors.primary,
        selectedTextColor = ChirpTheme.colors.primary,
        indicatorColor = ChirpTheme.colors.primaryContainer,
        unselectedIconColor = ChirpTheme.colors.onSurfaceVariant,
        unselectedTextColor = ChirpTheme.colors.onSurfaceVariant
    )
) {
    NavigationRailItem(
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
