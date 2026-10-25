package app.shoutsend

import android.content.Context
import app.shoutsend.core.audio.permissions.AndroidPermissionRepository
import app.shoutsend.core.audio.permissions.PermissionRepository
import app.shoutsend.core.audio.receiver.AcousticReceiver
import app.shoutsend.core.audio.receiver.AudioRecordSource
import app.shoutsend.core.audio.receiver.DefaultAcousticReceiver
import app.shoutsend.core.audio.transmitter.AcousticTransmitter
import app.shoutsend.core.audio.transmitter.DefaultAcousticTransmitter
import app.shoutsend.core.data.settings.DataStoreMicPermissionRequestedProvider
import app.shoutsend.core.data.settings.SettingsRepository
import app.shoutsend.core.data.settings.bindToThemeController
import app.shoutsend.core.data.settings.createSettingsRepository
import app.shoutsend.core.designsystem.ThemeController
import app.shoutsend.core.designsystem.createTokenOverridesRepository
import app.shoutsend.core.devtools.DevTools
import app.shoutsend.core.devtools.createDevTools
import app.shoutsend.core.modem.ModemConfigProvider
import app.shoutsend.core.modem.createModemConfigProvider
import app.shoutsend.core.session.AudioCoordinator
import app.shoutsend.core.session.AudioFocusController
import app.shoutsend.core.session.DefaultAudioCoordinator
import app.shoutsend.core.session.DefaultAudioFocusController
import app.shoutsend.core.session.DefaultTransmitSession
import app.shoutsend.core.session.TransmitSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(val context: Context) {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val tokenOverridesRepository = createTokenOverridesRepository(context, applicationScope)
    val settingsRepository: SettingsRepository = createSettingsRepository(context, applicationScope)

    val micPermissionRequestedProvider = DataStoreMicPermissionRequestedProvider(settingsRepository)
    val permissionRepository: PermissionRepository = AndroidPermissionRepository(
        context = context,
        provider = micPermissionRequestedProvider
    )

    val themeController: ThemeController = ThemeController(
        repository = tokenOverridesRepository,
        scope = applicationScope
    )

    val modemConfigProvider: ModemConfigProvider = createModemConfigProvider(
        settingsRepository = settingsRepository,
        scope = applicationScope
    )

    val acousticReceiver: AcousticReceiver = DefaultAcousticReceiver(
        pcmSourceFactory = { AudioRecordSource(context) },
        scope = applicationScope,
        configProvider = modemConfigProvider
    )

    val audioCoordinator: AudioCoordinator = DefaultAudioCoordinator(
        receiver = acousticReceiver
    )
    val audioFocusController: AudioFocusController = DefaultAudioFocusController(context)
    val acousticTransmitter: AcousticTransmitter = DefaultAcousticTransmitter()

    val transmitSession: TransmitSession = DefaultTransmitSession(
        coordinator = audioCoordinator,
        focusController = audioFocusController,
        transmitter = acousticTransmitter,
        configProvider = modemConfigProvider
    )

    init {
        settingsRepository.bindToThemeController(themeController, applicationScope)
    }

    val devTools: DevTools = createDevTools(context, themeController)
}
