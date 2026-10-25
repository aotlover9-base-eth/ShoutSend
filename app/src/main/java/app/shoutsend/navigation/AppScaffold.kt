package app.shoutsend.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import app.shoutsend.core.data.settings.SettingsRepository
import app.shoutsend.core.designsystem.ChirpTheme
import app.shoutsend.core.designsystem.components.AcousticStatus
import app.shoutsend.core.designsystem.components.BrandMark
import app.shoutsend.core.designsystem.components.CdAlertDialog
import app.shoutsend.core.designsystem.components.CdButton
import app.shoutsend.core.designsystem.components.CdIconButton
import app.shoutsend.core.designsystem.components.CdNavigationBar
import app.shoutsend.core.designsystem.components.CdNavigationBarItem
import app.shoutsend.core.designsystem.components.CdNavigationRail
import app.shoutsend.core.designsystem.components.CdNavigationRailItem
import app.shoutsend.core.designsystem.components.CdScaffold
import app.shoutsend.core.designsystem.components.CdSnackbarHost
import app.shoutsend.core.designsystem.components.CdTextButton
import app.shoutsend.core.designsystem.components.CdTopAppBar
import app.shoutsend.core.designsystem.components.StatusPill
import kotlinx.coroutines.launch

/**
 * Root application scaffold adapting between compact phone bottom-bar and wide tablet navigation rail.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    navController: NavHostController,
    settingsRepository: SettingsRepository,
    isSessionActive: Boolean = false,
    sessionStatus: AcousticStatus? = null,
    onCancelSession: () -> Unit = {},
    content: @Composable (Modifier) -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCancelDialog by remember { mutableStateOf(false) }

    // Intercept back button when active acoustic session is running
    BackHandler(enabled = isSessionActive) {
        showCancelDialog = true
    }

    if (showCancelDialog) {
        CdAlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = {
                Text(
                    text = "Cancel Acoustic Session?",
                    style = ChirpTheme.typography.titleMedium,
                    color = ChirpTheme.colors.onSurface
                )
            },
            text = {
                Text(
                    text = "An active sonic transmission or reception is currently running. Leaving will terminate the session.",
                    style = ChirpTheme.typography.bodyMedium,
                    color = ChirpTheme.colors.onSurfaceVariant
                )
            },
            confirmButton = {
                CdButton(
                    onClick = {
                        onCancelSession()
                        showCancelDialog = false
                    }
                ) {
                    Text("Cancel Session")
                }
            },
            dismissButton = {
                CdTextButton(
                    onClick = { showCancelDialog = false }
                ) {
                    Text("Stay")
                }
            }
        )
    }

    val isTopLevelRoute = TopLevelDestination.entries.any { destination ->
        currentDestination?.route?.contains(destination.route::class.simpleName ?: "") == true
    }

    val topBar: @Composable () -> Unit = {
        CdTopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BrandMark(
                        modifier = Modifier.size(ChirpTheme.sizes.iconMd)
                    )
                    Spacer(modifier = Modifier.width(ChirpTheme.spacing.sm))
                    Text(
                        text = "ShoutSend",
                        style = ChirpTheme.typography.titleMedium,
                        color = ChirpTheme.colors.primary
                    )
                }
            },
            navigationIcon = {
                if (!isTopLevelRoute && currentDestination != null) {
                    CdIconButton(
                        onClick = { navController.popBackStack() }
                    ) {
                        Text(
                            text = "←",
                            style = ChirpTheme.typography.titleMedium,
                            color = ChirpTheme.colors.onSurface
                        )
                    }
                }
            },
            actions = {
                if (sessionStatus != null) {
                    StatusPill(
                        status = sessionStatus,
                        modifier = Modifier.padding(end = ChirpTheme.spacing.xs)
                    )
                }
                CdIconButton(
                    onClick = { navController.navigate(CalibrationRoute) }
                ) {
                    Text(
                        text = "🎛",
                        style = ChirpTheme.typography.titleMedium,
                        color = ChirpTheme.colors.onSurfaceVariant
                    )
                }
                CdIconButton(
                    onClick = { navController.navigate(SettingsRoute) }
                ) {
                    Text(
                        text = "⚙",
                        style = ChirpTheme.typography.titleMedium,
                        color = ChirpTheme.colors.onSurfaceVariant
                    )
                }
            }
        )
    }

    val bottomBar: @Composable () -> Unit = {
        if (!isWideScreen && isTopLevelRoute) {
            CdNavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    val selected = currentDestination?.route?.contains(
                        destination.route::class.simpleName ?: ""
                    ) == true

                    CdNavigationBarItem(
                        selected = selected,
                        onClick = {
                            scope.launch {
                                settingsRepository.setLastTab(destination.tabKey)
                            }
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Text(
                                text = destination.iconGlyph,
                                style = ChirpTheme.typography.titleSmall
                            )
                        },
                        label = {
                            Text(
                                text = destination.label,
                                style = ChirpTheme.typography.labelSmall
                            )
                        }
                    )
                }
            }
        }
    }

    if (isWideScreen) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (isTopLevelRoute) {
                CdNavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    header = {
                        Box(modifier = Modifier.padding(ChirpTheme.spacing.sm)) {
                            BrandMark(modifier = Modifier.size(ChirpTheme.sizes.iconLg))
                        }
                    }
                ) {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected = currentDestination?.route?.contains(
                            destination.route::class.simpleName ?: ""
                        ) == true

                        CdNavigationRailItem(
                            selected = selected,
                            onClick = {
                                scope.launch {
                                    settingsRepository.setLastTab(destination.tabKey)
                                }
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Text(
                                    text = destination.iconGlyph,
                                    style = ChirpTheme.typography.titleSmall
                                )
                            },
                            label = {
                                Text(
                                    text = destination.label,
                                    style = ChirpTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }
            }

            CdScaffold(
                modifier = Modifier
                    .weight(1f)
                    .imePadding(),
                topBar = topBar,
                snackbarHost = { CdSnackbarHost(hostState = snackbarHostState) }
            ) { innerPadding ->
                content(Modifier.padding(innerPadding))
            }
        }
    } else {
        CdScaffold(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            topBar = topBar,
            bottomBar = bottomBar,
            snackbarHost = { CdSnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            content(Modifier.padding(innerPadding))
        }
    }
}
