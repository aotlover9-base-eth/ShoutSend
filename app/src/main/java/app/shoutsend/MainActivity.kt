package app.shoutsend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import app.shoutsend.core.designsystem.ChirpTheme
import app.shoutsend.feature.calibration.CalibrationViewModel
import app.shoutsend.feature.onboarding.OnboardingViewModel
import app.shoutsend.feature.receive.ReceiveViewModel
import app.shoutsend.feature.send.SendViewModel
import app.shoutsend.feature.settings.SettingsViewModel
import app.shoutsend.feature.vault.VaultViewModel
import app.shoutsend.navigation.AppScaffold
import app.shoutsend.navigation.OnboardingRoute
import app.shoutsend.navigation.ShoutSendNavHost
import app.shoutsend.navigation.TopLevelDestination

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as ShoutSendApp
        val themeController = app.appContainer.themeController
        val settingsRepository = app.appContainer.settingsRepository
        val devTools = app.appContainer.devTools

        setContent {
            val tokens by themeController.tokens.collectAsStateWithLifecycle()
            val settings by settingsRepository.settings.collectAsStateWithLifecycle()

            ChirpTheme(tokens = tokens) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    val sendViewModel = remember { SendViewModel() }
                    val receiveViewModel = remember { ReceiveViewModel() }
                    val vaultViewModel = remember { VaultViewModel() }
                    val settingsViewModel = remember { SettingsViewModel(settingsRepository) }
                    val calibrationViewModel = remember { CalibrationViewModel() }
                    val onboardingViewModel = remember { OnboardingViewModel(settingsRepository) }

                    val startDestination: Any = remember(settings.onboardingCompleted, settings.lastTab) {
                        if (!settings.onboardingCompleted) {
                            OnboardingRoute
                        } else {
                            TopLevelDestination.fromTabKey(settings.lastTab).route
                        }
                    }

                    AppScaffold(
                        navController = navController,
                        settingsRepository = settingsRepository
                    ) { contentModifier ->
                        ShoutSendNavHost(
                            navController = navController,
                            startDestination = startDestination,
                            settingsRepository = settingsRepository,
                            sendViewModel = sendViewModel,
                            receiveViewModel = receiveViewModel,
                            vaultViewModel = vaultViewModel,
                            settingsViewModel = settingsViewModel,
                            calibrationViewModel = calibrationViewModel,
                            onboardingViewModel = onboardingViewModel,
                            modifier = contentModifier
                        )
                    }

                    // DevTools overlay (rendered in dev flavor, zero-overhead no-op in user flavor)
                    devTools.entryPoint?.invoke()
                }
            }
        }
    }
}
