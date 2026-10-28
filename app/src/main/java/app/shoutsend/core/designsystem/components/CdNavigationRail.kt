package app.shoutsend.core.designsystem.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.NavigationRailItemColors
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun CdNavigationRail(
    modifier: Modifier = Modifier,
    containerColor: Color = NavigationRailDefaults.ContainerColor,
    contentColor: Color = NavigationRailDefaults.ContainerColor,
    header: (@Composable ColumnScope.() -> Unit)? = null,
    windowInsets: WindowInsets = NavigationRailDefaults.windowInsets,
    content: @Composable ColumnScope.() -> Unit
) = ShoutNavigationRail(
    modifier = modifier,
    containerColor = containerColor,
    contentColor = contentColor,
    header = header,
    windowInsets = windowInsets,
    content = content
)

@Composable
fun CdNavigationRailItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: (@Composable () -> Unit)? = null,
    alwaysShowLabel: Boolean = true,
    colors: NavigationRailItemColors = NavigationRailItemDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) = ShoutNavigationRailItem(
    selected = selected,
    onClick = onClick,
    icon = icon,
    modifier = modifier,
    enabled = enabled,
    label = label,
    alwaysShowLabel = alwaysShowLabel,
    colors = colors,
    interactionSource = interactionSource
)
