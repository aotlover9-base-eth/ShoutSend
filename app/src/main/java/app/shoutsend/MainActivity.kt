package app.shoutsend

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import app.shoutsend.core.audio.permissions.createAppSettingsIntent
import app.shoutsend.core.designsystem.ChirpTheme
import app.shoutsend.feature.calibration.CalibrationViewModel
import app.shoutsend.feature.onboarding.OnboardingViewModel
import app.shoutsend.feature.receive.ReceiveViewModel
import app.shoutsend.feature.send.ParsedSharedContent
import app.shoutsend.feature.send.SendViewModel
import app.shoutsend.feature.send.SharedTextParser
import app.shoutsend.feature.send.TransmitViewModel
import app.shoutsend.feature.settings.SettingsViewModel
import app.shoutsend.feature.vault.VaultViewModel
import app.shoutsend.navigation.AppScaffold
import app.shoutsend.navigation.OnboardingRoute
import app.shoutsend.navigation.SendRoute
import app.shoutsend.navigation.ShoutSendNavHost
import app.shoutsend.navigation.TopLevelDestination
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var onShareReceived: ((ParsedSharedContent) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as ShoutSendApp
        val themeController = app.appContainer.themeController
        val settingsRepository = app.appContainer.settingsRepository
        val permissionRepository = app.appContainer.permissionRepository
        val transmitSession = app.appContainer.transmitSession
        val acousticReceiver = app.appContainer.acousticReceiver
        val audioCoordinator = app.appContainer.audioCoordinator
        val vaultRepository = app.appContainer.vaultRepository
        val audioRouteMonitor = app.appContainer.audioRouteMonitor
        val devTools = app.appContainer.devTools

        setContent {
            val tokens by themeController.tokens.collectAsStateWithLifecycle()
            val settings by settingsRepository.settings.collectAsStateWithLifecycle()
            val micState by permissionRepository.micState.collectAsStateWithLifecycle()
            val scope = rememberCoroutineScope()

            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    permissionRepository.refresh()
                }
            }

            val micPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { _ ->
                scope.launch {
                    settingsRepository.setMicPermissionRequested(true)
                    permissionRepository.refresh()
                }
            }

            ChirpTheme(tokens = tokens) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    val sendViewModel = remember {
                        SendViewModel(settingsRepository, transmitSession)
                    }
                    val transmitViewModel = remember {
                        TransmitViewModel(
                            transmitSession = transmitSession,
                            settingsRepository = settingsRepository,
                            vaultRepository = vaultRepository,
                            audioRouteMonitor = audioRouteMonitor,
                            context = this@MainActivity
                        )
                    }
                    val receiveViewModel = remember {
                        ReceiveViewModel(
                            acousticReceiver = acousticReceiver,
                            audioCoordinator = audioCoordinator,
                            settingsRepository = settingsRepository,
                            vaultRepository = vaultRepository
                        )
                    }
                    val vaultViewModel = remember { VaultViewModel(vaultRepository) }
                    val settingsViewModel = remember { SettingsViewModel(settingsRepository) }
                    val calibrationViewModel = remember { CalibrationViewModel() }
                    val onboardingViewModel = remember {
                        OnboardingViewModel(settingsRepository, permissionRepository)
                    }

                    DisposableEffect(navController) {
                        onShareReceived = { parsed ->
                            navController.navigate(
                                SendRoute(
                                    sharedText = parsed.content,
                                    sharedType = parsed.type.name
                                )
                            ) {
                                launchSingleTop = true
                            }
                        }
                        handleShareIntent(intent)
                        onDispose {
                            onShareReceived = null
                        }
                    }

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
                            transmitViewModel = transmitViewModel,
                            receiveViewModel = receiveViewModel,
                            vaultViewModel = vaultViewModel,
                            settingsViewModel = settingsViewModel,
                            calibrationViewModel = calibrationViewModel,
                            onboardingViewModel = onboardingViewModel,
                            micPermissionState = micState,
                            onRequestPermission = {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            onOpenSettings = {
                                startActivity(createAppSettingsIntent(this@MainActivity))
                            },
                            modifier = contentModifier
                        )
                    }

                    // DevTools overlay (rendered in dev flavor, zero-overhead no-op in user flavor)
                    devTools.entryPoint?.invoke()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                val parsed = SharedTextParser.parse(sharedText)
                onShareReceived?.invoke(parsed)
            }
        }
    }
}
