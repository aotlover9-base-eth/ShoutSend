package app.shoutsend.core.designsystem.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.shoutsend.core.designsystem.ChirpTheme

/**
 * Standard TopAppBar wrapper using ChirpTheme tokens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CdTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = ChirpTheme.colors.surface,
        scrolledContainerColor = ChirpTheme.colors.surfaceContainer,
        navigationIconContentColor = ChirpTheme.colors.onSurface,
        titleContentColor = ChirpTheme.colors.onSurface,
        actionIconContentColor = ChirpTheme.colors.onSurfaceVariant
    ),
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    TopAppBar(
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        windowInsets = windowInsets,
        colors = colors,
        scrollBehavior = scrollBehavior
    )
}

/**
 * Center-aligned TopAppBar wrapper.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CdCenterAlignedTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.centerAlignedTopAppBarColors(
        containerColor = ChirpTheme.colors.surface,
        scrolledContainerColor = ChirpTheme.colors.surfaceContainer,
        navigationIconContentColor = ChirpTheme.colors.onSurface,
        titleContentColor = ChirpTheme.colors.onSurface,
        actionIconContentColor = ChirpTheme.colors.onSurfaceVariant
    ),
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    CenterAlignedTopAppBar(
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        windowInsets = windowInsets,
        colors = colors,
        scrollBehavior = scrollBehavior
    )
}

/**
 * Large collapsible TopAppBar wrapper.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CdLargeTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.largeTopAppBarColors(
        containerColor = ChirpTheme.colors.surface,
        scrolledContainerColor = ChirpTheme.colors.surfaceContainer,
        navigationIconContentColor = ChirpTheme.colors.onSurface,
        titleContentColor = ChirpTheme.colors.onSurface,
        actionIconContentColor = ChirpTheme.colors.onSurfaceVariant
    ),
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    LargeTopAppBar(
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        windowInsets = windowInsets,
        colors = colors,
        scrollBehavior = scrollBehavior
    )
}
