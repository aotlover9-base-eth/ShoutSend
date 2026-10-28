package app.shoutsend

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.shoutsend.core.capability.CapabilityProbe
import app.shoutsend.feature.calibration.CalibrationViewModel
import app.shoutsend.feature.onboarding.OnboardingViewModel
import app.shoutsend.feature.receive.ReceiveViewModel
import app.shoutsend.feature.send.SendViewModel
import app.shoutsend.feature.send.TransmitViewModel
import app.shoutsend.feature.settings.SettingsViewModel
import app.shoutsend.feature.vault.VaultViewModel

/**
 * Centralized ViewModelProvider.Factory implementation pulling dependencies exclusively
 * from the application's [AppContainer] without retaining Activity Context references.
 */
object AppViewModelProvider {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val app = shoutSendApp()
            val container = app.appContainer
            SendViewModel(
                settingsRepository = container.settingsRepository,
                transmitSession = container.transmitSession
            )
        }
        initializer {
            val app = shoutSendApp()
            val container = app.appContainer
            TransmitViewModel(
                transmitSession = container.transmitSession,
                settingsRepository = container.settingsRepository,
                vaultRepository = container.vaultRepository,
                audioRouteMonitor = container.audioRouteMonitor,
                applicationContext = app.applicationContext,
                audioFeedbackPlayer = container.audioFeedbackPlayer,
                tactileFeedback = container.tactileFeedbackController
            )
        }
        initializer {
            val app = shoutSendApp()
            val container = app.appContainer
            ReceiveViewModel(
                acousticReceiver = container.acousticReceiver,
                audioCoordinator = container.audioCoordinator,
                settingsRepository = container.settingsRepository,
                vaultRepository = container.vaultRepository,
                audioFeedbackPlayer = container.audioFeedbackPlayer,
                tactileFeedback = container.tactileFeedbackController
            )
        }
        initializer {
            val app = shoutSendApp()
            val container = app.appContainer
            VaultViewModel(
                vaultRepository = container.vaultRepository
            )
        }
        initializer {
            val app = shoutSendApp()
            val container = app.appContainer
            SettingsViewModel(
                settingsRepository = container.settingsRepository,
                vaultRepository = container.vaultRepository,
                capabilityProbe = CapabilityProbe(app.applicationContext)
            )
        }
        initializer {
            val app = shoutSendApp()
            val container = app.appContainer
            CalibrationViewModel(
                calibrationRunner = container.calibrationRunner,
                settingsRepository = container.settingsRepository,
                audioCoordinator = container.audioCoordinator
            )
        }
        initializer {
            val app = shoutSendApp()
            val container = app.appContainer
            OnboardingViewModel(
                settingsRepository = container.settingsRepository,
                permissionRepository = container.permissionRepository
            )
        }
    }
}

fun CreationExtras.shoutSendApp(): ShoutSendApp =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ShoutSendApp)
