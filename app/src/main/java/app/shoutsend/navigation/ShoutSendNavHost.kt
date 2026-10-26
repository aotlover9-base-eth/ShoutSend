package app.shoutsend.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
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
    sendViewModel: SendViewModel,
    transmitViewModel: TransmitViewModel,
    receiveViewModel: ReceiveViewModel,
    vaultViewModel: VaultViewModel,
    settingsViewModel: SettingsViewModel,
    calibrationViewModel: CalibrationViewModel,
    onboardingViewModel: OnboardingViewModel,
    micPermissionState: MicPermissionState = MicPermissionState.Granted,
    onRequestPermission: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Capture motion tokens in composable scope for non-composable transition lambdas
    val durShort = ChirpTheme.motion.durationShort2
    val durMed = ChirpTheme.motion.durationMedium1
    val durLong = ChirpTheme.motion.durationMedium2
    val easeDecel = ChirpTheme.motion.easingStandardDecelerate
    val easeAccel = ChirpTheme.motion.easingStandardAccelerate

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // --- Onboarding Destination ---
        composable<OnboardingRoute>(
            enterTransition = { fadeIn(tween(durLong, easing = easeDecel)) },
            exitTransition = { fadeOut(tween(durMed, easing = easeAccel)) }
        ) {
            OnboardingScreen(
                viewModel = onboardingViewModel,
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
            enterTransition = {
                fadeIn(tween(durMed, easing = easeDecel)) +
                    slideInVertically(tween(durMed, easing = easeDecel)) { it / 20 }
            },
            exitTransition = {
                fadeOut(tween(durShort, easing = easeAccel)) +
                    slideOutVertically(tween(durShort, easing = easeAccel)) { -it / 20 }
            }
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<SendRoute>()
            androidx.compose.runtime.LaunchedEffect(route.sharedText, route.sharedType) {
                if (!route.sharedText.isNullOrEmpty()) {
                    sendViewModel.consumeSharedRouteArgs(route.sharedText, route.sharedType)
                }
            }
            SendScreen(
                viewModel = sendViewModel,
                onNavigateToTransmit = {
                    navController.navigate(TransmitRoute)
                }
            )
        }

        // --- Active Transmission Destination ---
        composable<TransmitRoute>(
            enterTransition = {
                slideInVertically(tween(durMed, easing = easeDecel)) { it } +
                    fadeIn(tween(durShort))
            },
            exitTransition = {
                slideOutVertically(tween(durMed, easing = easeAccel)) { it } +
                    fadeOut(tween(durShort))
            }
        ) {
            TransmitScreen(
                viewModel = transmitViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // --- Top-Level Tab: Receive ---
        composable<ReceiveRoute>(
            enterTransition = {
                fadeIn(tween(durMed, easing = easeDecel)) +
                    slideInVertically(tween(durMed, easing = easeDecel)) { it / 20 }
            },
            exitTransition = {
                fadeOut(tween(durShort, easing = easeAccel)) +
                    slideOutVertically(tween(durShort, easing = easeAccel)) { -it / 20 }
            }
        ) {
            ReceiveScreen(
                viewModel = receiveViewModel,
                micPermissionState = micPermissionState,
                onRequestPermission = onRequestPermission,
                onOpenSettings = onOpenSettings
            )
        }

        // --- Top-Level Tab: Vault ---
        composable<VaultRoute>(
            enterTransition = {
                fadeIn(tween(durMed, easing = easeDecel)) +
                    slideInVertically(tween(durMed, easing = easeDecel)) { it / 20 }
            },
            exitTransition = {
                fadeOut(tween(durShort, easing = easeAccel)) +
                    slideOutVertically(tween(durShort, easing = easeAccel)) { -it / 20 }
            }
        ) {
            VaultScreen(
                viewModel = vaultViewModel,
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
            enterTransition = {
                slideInHorizontally(tween(durLong, easing = easeDecel)) { it } +
                    fadeIn(tween(durShort))
            },
            exitTransition = {
                slideOutHorizontally(tween(durLong, easing = easeAccel)) { -it / 3 } +
                    fadeOut(tween(durShort))
            },
            popEnterTransition = {
                slideInHorizontally(tween(durLong, easing = easeDecel)) { -it / 3 } +
                    fadeIn(tween(durShort))
            },
            popExitTransition = {
                slideOutHorizontally(tween(durLong, easing = easeAccel)) { it } +
                    fadeOut(tween(durShort))
            }
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<VaultDetailRoute>()
            VaultDetailScreen(
                id = route.id,
                viewModel = vaultViewModel,
                onNavigateBack = { navController.popBackStack() },
                onRetransmit = { payload, isSecure ->
                    sendViewModel.loadPayloadForRetransmit(payload, isSecure)
                    sendViewModel.startTransmission {
                        navController.navigate(TransmitRoute)
                    }
                }
            )
        }

        // --- Deeper Screen: Settings ---
        composable<SettingsRoute>(
            enterTransition = {
                slideInHorizontally(tween(durLong, easing = easeDecel)) { it } +
                    fadeIn(tween(durShort))
            },
            exitTransition = {
                slideOutHorizontally(tween(durLong, easing = easeAccel)) { -it / 3 } +
                    fadeOut(tween(durShort))
            },
            popEnterTransition = {
                slideInHorizontally(tween(durLong, easing = easeDecel)) { -it / 3 } +
                    fadeIn(tween(durShort))
            },
            popExitTransition = {
                slideOutHorizontally(tween(durLong, easing = easeAccel)) { it } +
                    fadeOut(tween(durShort))
            }
        ) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCalibration = { navController.navigate(CalibrationRoute) }
            )
        }

        // --- Deeper Screen: Calibration ---
        composable<CalibrationRoute>(
            enterTransition = {
                slideInHorizontally(tween(durLong, easing = easeDecel)) { it } +
                    fadeIn(tween(durShort))
            },
            exitTransition = {
                slideOutHorizontally(tween(durLong, easing = easeAccel)) { -it / 3 } +
                    fadeOut(tween(durShort))
            },
            popEnterTransition = {
                slideInHorizontally(tween(durLong, easing = easeDecel)) { -it / 3 } +
                    fadeIn(tween(durShort))
            },
            popExitTransition = {
                slideOutHorizontally(tween(durLong, easing = easeAccel)) { it } +
                    fadeOut(tween(durShort))
            }
        ) {
            CalibrationScreen(
                viewModel = calibrationViewModel,
                onNavigateBack = { navController.popBackStack() },
                micPermissionState = micPermissionState,
                onRequestPermission = onRequestPermission,
                onOpenSettings = onOpenSettings
            )
        }

        // --- Deeper Screen: Link Test ---
        composable<LinkTestRoute>(
            enterTransition = {
                slideInHorizontally(tween(durLong, easing = easeDecel)) { it } +
                    fadeIn(tween(durShort))
            },
            exitTransition = {
                slideOutHorizontally(tween(durLong, easing = easeAccel)) { -it / 3 } +
                    fadeOut(tween(durShort))
            },
            popEnterTransition = {
                slideInHorizontally(tween(durLong, easing = easeDecel)) { -it / 3 } +
                    fadeIn(tween(durShort))
            },
            popExitTransition = {
                slideOutHorizontally(tween(durLong, easing = easeAccel)) { it } +
                    fadeOut(tween(durShort))
            }
        ) {
            LinkTestScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
