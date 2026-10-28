package app.shoutsend.navigation

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import app.shoutsend.AppViewModelProvider
import app.shoutsend.core.audio.permissions.MicPermissionState
import app.shoutsend.core.data.settings.SettingsRepository
import app.shoutsend.core.designsystem.ChirpTheme
import app.shoutsend.feature.calibration.CalibrationScreen
import app.shoutsend.feature.calibration.CalibrationViewModel
import app.shoutsend.feature.linktest.LinkTestScreen
import app.shoutsend.feature.onboarding.OnboardingScreen
import app.shoutsend.feature.onboarding.OnboardingViewModel
import app.shoutsend.feature.receive.ReceiveScreen
import app.shoutsend.feature.receive.ReceiveViewModel
import app.shoutsend.feature.send.SendScreen
import app.shoutsend.feature.send.SendViewModel
import app.shoutsend.feature.send.TransmitScreen
import app.shoutsend.feature.send.TransmitViewModel
import app.shoutsend.feature.settings.SettingsScreen
import app.shoutsend.feature.settings.SettingsViewModel
import app.shoutsend.feature.vault.VaultDetailScreen
import app.shoutsend.feature.vault.VaultScreen
import app.shoutsend.feature.vault.VaultViewModel

/**
 * Single navigation host defining all application destinations, transitions, and arguments.
 */
@Composable
fun ShoutSendNavHost(
    navController: NavHostController,
    startDestination: Any,
    settingsRepository: SettingsRepository,
    micPermissionState: MicPermissionState = MicPermissionState.Granted,
    onRequestPermission: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    sendViewModel: SendViewModel? = null,
    transmitViewModel: TransmitViewModel? = null,
    receiveViewModel: ReceiveViewModel? = null,
    vaultViewModel: VaultViewModel? = null,
    settingsViewModel: SettingsViewModel? = null,
    calibrationViewModel: CalibrationViewModel? = null,
    onboardingViewModel: OnboardingViewModel? = null,
    modifier: Modifier = Modifier
) {
    val isReducedMotion = ChirpTheme.isReducedMotion

    // Capture motion tokens in composable scope for non-composable transition lambdas
    val durShort = ChirpTheme.motion.durationShort2
    val durMed = ChirpTheme.motion.durationMedium1
    val durLong = ChirpTheme.motion.durationMedium2
    val easeDecel = ChirpTheme.motion.easingStandardDecelerate
    val easeAccel = ChirpTheme.motion.easingStandardAccelerate

    // Top-level tab transition lambdas with reduced-motion support
    val tabEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (isReducedMotion) {
            EnterTransition.None
        } else {
            fadeIn(tween(durMed, easing = easeDecel)) +
                slideInVertically(tween(durMed, easing = easeDecel)) { it / 20 }
        }
    }

    val tabExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (isReducedMotion) {
            ExitTransition.None
        } else {
            fadeOut(tween(durShort, easing = easeAccel)) +
                slideOutVertically(tween(durShort, easing = easeAccel)) { -it / 20 }
        }
    }

    val tabPopEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (isReducedMotion) {
            EnterTransition.None
        } else {
            fadeIn(tween(durMed, easing = easeDecel)) +
                slideInVertically(tween(durMed, easing = easeDecel)) { it / 20 }
        }
    }

    val tabPopExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (isReducedMotion) {
            ExitTransition.None
        } else {
            fadeOut(tween(durShort, easing = easeAccel)) +
                slideOutVertically(tween(durShort, easing = easeAccel)) { it / 20 }
        }
    }

    // Modal/Active transmission transition lambdas
    val transmitEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (isReducedMotion) {
            EnterTransition.None
        } else {
            slideInVertically(tween(durMed, easing = easeDecel)) { it } +
                fadeIn(tween(durShort, easing = easeDecel))
        }
    }

    val transmitExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (isReducedMotion) {
            ExitTransition.None
        } else {
            slideOutVertically(tween(durMed, easing = easeAccel)) { it } +
                fadeOut(tween(durShort, easing = easeAccel))
        }
    }

    val transmitPopEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (isReducedMotion) {
            EnterTransition.None
        } else {
            fadeIn(tween(durShort, easing = easeDecel))
        }
    }

    val transmitPopExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (isReducedMotion) {
            ExitTransition.None
        } else {
            slideOutVertically(tween(durMed, easing = easeAccel)) { it } +
                fadeOut(tween(durShort, easing = easeAccel))
        }
    }

    // Deep destination slide-horizontal transition lambdas (Predictive Back compatible)
    val deepEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (isReducedMotion) {
            EnterTransition.None
        } else {
            slideInHorizontally(tween(durLong, easing = easeDecel)) { it } +
                fadeIn(tween(durShort, easing = easeDecel))
        }
    }

    val deepExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (isReducedMotion) {
            ExitTransition.None
        } else {
            slideOutHorizontally(tween(durLong, easing = easeAccel)) { -it / 3 } +
                fadeOut(tween(durShort, easing = easeAccel))
        }
    }

    val deepPopEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (isReducedMotion) {
            EnterTransition.None
        } else {
            slideInHorizontally(tween(durLong, easing = easeDecel)) { -it / 3 } +
                fadeIn(tween(durShort, easing = easeDecel))
        }
    }

    val deepPopExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (isReducedMotion) {
            ExitTransition.None
        } else {
            slideOutHorizontally(tween(durLong, easing = easeAccel)) { it } +
                fadeOut(tween(durShort, easing = easeAccel))
        }
    }

    val onboardingEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (isReducedMotion) {
            EnterTransition.None
        } else {
            fadeIn(tween(durLong, easing = easeDecel))
        }
    }

    val onboardingExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (isReducedMotion) {
            ExitTransition.None
        } else {
            fadeOut(tween(durMed, easing = easeAccel))
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = tabEnterTransition,
        exitTransition = tabExitTransition,
        popEnterTransition = tabPopEnterTransition,
        popExitTransition = tabPopExitTransition
    ) {
        // --- Onboarding Destination ---
        composable<OnboardingRoute>(
            enterTransition = onboardingEnterTransition,
            exitTransition = onboardingExitTransition,
            popEnterTransition = onboardingEnterTransition,
            popExitTransition = onboardingExitTransition
        ) {
            val vm = onboardingViewModel ?: viewModel(factory = AppViewModelProvider.Factory)
            OnboardingScreen(
                viewModel = vm,
                onFinished = {
                    navController.navigate(ReceiveRoute) {
                        popUpTo(OnboardingRoute) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // --- Top-Level Tab: Send ---
        composable<SendRoute>(
            enterTransition = tabEnterTransition,
            exitTransition = tabExitTransition,
            popEnterTransition = tabPopEnterTransition,
            popExitTransition = tabPopExitTransition
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<SendRoute>()
            val activity = LocalContext.current as ComponentActivity
            val vm = sendViewModel ?: viewModel(viewModelStoreOwner = activity, factory = AppViewModelProvider.Factory)
            androidx.compose.runtime.LaunchedEffect(route.sharedText, route.sharedType) {
                if (!route.sharedText.isNullOrEmpty()) {
                    vm.consumeSharedRouteArgs(route.sharedText, route.sharedType)
                }
            }
            SendScreen(
                viewModel = vm,
                onNavigateToTransmit = {
                    navController.navigate(TransmitRoute)
                }
            )
        }

        // --- Active Transmission Destination ---
        composable<TransmitRoute>(
            enterTransition = transmitEnterTransition,
            exitTransition = transmitExitTransition,
            popEnterTransition = transmitPopEnterTransition,
            popExitTransition = transmitPopExitTransition
        ) {
            val activity = LocalContext.current as ComponentActivity
            val vm = transmitViewModel ?: viewModel(viewModelStoreOwner = activity, factory = AppViewModelProvider.Factory)
            TransmitScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // --- Top-Level Tab: Receive ---
        composable<ReceiveRoute>(
            enterTransition = tabEnterTransition,
            exitTransition = tabExitTransition,
            popEnterTransition = tabPopEnterTransition,
            popExitTransition = tabPopExitTransition
        ) {
            val vm = receiveViewModel ?: viewModel(factory = AppViewModelProvider.Factory)
            ReceiveScreen(
                viewModel = vm,
                micPermissionState = micPermissionState,
                onRequestPermission = onRequestPermission,
                onOpenSettings = onOpenSettings
            )
        }

        // --- Top-Level Tab: Vault ---
        composable<VaultRoute>(
            enterTransition = tabEnterTransition,
            exitTransition = tabExitTransition,
            popEnterTransition = tabPopEnterTransition,
            popExitTransition = tabPopExitTransition
        ) {
            val vm = vaultViewModel ?: viewModel(factory = AppViewModelProvider.Factory)
            VaultScreen(
                viewModel = vm,
                onNavigateToDetail = { id ->
                    navController.navigate(VaultDetailRoute(id))
                },
                onNavigateToLinkTest = {
                    navController.navigate(LinkTestRoute)
                }
            )
        }

        // --- Deeper Screen: Vault Detail ---
        composable<VaultDetailRoute>(
            enterTransition = deepEnterTransition,
            exitTransition = deepExitTransition,
            popEnterTransition = deepPopEnterTransition,
            popExitTransition = deepPopExitTransition
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<VaultDetailRoute>()
            val vm = vaultViewModel ?: viewModel(factory = AppViewModelProvider.Factory)
            val activity = LocalContext.current as ComponentActivity
            val sendVm = sendViewModel ?: viewModel(viewModelStoreOwner = activity, factory = AppViewModelProvider.Factory)
            VaultDetailScreen(
                id = route.id,
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onRetransmit = { payload, isSecure ->
                    sendVm.loadPayloadForRetransmit(payload, isSecure)
                    sendVm.startTransmission {
                        navController.navigate(TransmitRoute)
                    }
                }
            )
        }

        // --- Deeper Screen: Settings ---
        composable<SettingsRoute>(
            enterTransition = deepEnterTransition,
            exitTransition = deepExitTransition,
            popEnterTransition = deepPopEnterTransition,
            popExitTransition = deepPopExitTransition
        ) {
            val vm = settingsViewModel ?: viewModel(factory = AppViewModelProvider.Factory)
            SettingsScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCalibration = { navController.navigate(CalibrationRoute) },
                onNavigateToOnboarding = { navController.navigate(OnboardingRoute) }
            )
        }

        // --- Deeper Screen: Calibration ---
        composable<CalibrationRoute>(
            enterTransition = deepEnterTransition,
            exitTransition = deepExitTransition,
            popEnterTransition = deepPopEnterTransition,
            popExitTransition = deepPopExitTransition
        ) {
            val vm = calibrationViewModel ?: viewModel(factory = AppViewModelProvider.Factory)
            CalibrationScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                micPermissionState = micPermissionState,
                onRequestPermission = onRequestPermission,
                onOpenSettings = onOpenSettings
            )
        }

        // --- Deeper Screen: Link Test ---
        composable<LinkTestRoute>(
            enterTransition = deepEnterTransition,
            exitTransition = deepExitTransition,
            popEnterTransition = deepPopEnterTransition,
            popExitTransition = deepPopExitTransition
        ) {
            LinkTestScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

/**
 * Encapsulates motion policies and reduced-motion bypass logic for route navigation.
 */
object NavMotionPolicy {
    fun shouldBypassAnimations(isReducedMotion: Boolean): Boolean = isReducedMotion
    fun resolveTransitionDuration(isReducedMotion: Boolean, durationMs: Int): Int =
        if (isReducedMotion) 0 else durationMs
}
