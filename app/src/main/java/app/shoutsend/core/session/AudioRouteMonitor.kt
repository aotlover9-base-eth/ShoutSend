package app.shoutsend.core.session

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Route classification indicating acoustic fidelity and suitability for soundwave transfer.
 */
enum class RouteClassification {
    /** Built-in phone loudspeaker and microphone. Optimal acoustic path. */
    SPEAKER_OK,

    /** Wired headset connected. Transmission through speaker may be diverted. */
    HEADSET_WARNING,

    /** Bluetooth device connected. High-frequency and ultrasound tones are heavily band-limited. */
    BLUETOOTH_WARNING,

    /** USB audio peripheral connected. */
    USB_WARNING
}

/**
 * Comprehensive snapshot of audio routing status and active peripheral warnings.
 */
data class AudioRouteState(
    val classification: RouteClassification = RouteClassification.SPEAKER_OK,
    val hasHeadset: Boolean = false,
    val hasBluetooth: Boolean = false,
    val hasUsb: Boolean = false,
    val hasBluetoothInput: Boolean = false,
    val transmitWarning: String? = null,
    val receiveWarning: String? = null
)

/**
 * Interface contract for observing hardware audio routing changes.
 */
interface AudioRouteMonitor {
    val routeState: StateFlow<AudioRouteState>
}

/**
 * Android implementation observing audio hardware device connections via [AudioDeviceCallback].
 */
class DefaultAudioRouteMonitor(
    private val context: Context,
    private val audioManager: AudioManager? = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
) : AudioRouteMonitor {

    private val _routeState = MutableStateFlow(evaluateCurrentRoute())
    override val routeState: StateFlow<AudioRouteState> = _routeState.asStateFlow()

    private var deviceCallback: AudioDeviceCallback? = null

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioManager != null) {
            val callback = object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                    _routeState.value = evaluateCurrentRoute()
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                    _routeState.value = evaluateCurrentRoute()
                }
            }
            deviceCallback = callback
            audioManager.registerAudioDeviceCallback(callback, null)
        }
    }

    fun destroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && deviceCallback != null) {
            audioManager?.unregisterAudioDeviceCallback(deviceCallback)
            deviceCallback = null
        }
    }

    private fun evaluateCurrentRoute(): AudioRouteState {
        if (audioManager == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return AudioRouteState()
        }

        val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val inputs = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)

        var hasHeadset = false
        var hasBluetooth = false
        var hasUsb = false
        var hasBluetoothInput = false

        for (dev in outputs) {
            when (dev.type) {
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> hasHeadset = true
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                AudioDeviceInfo.TYPE_BLE_HEADSET,
                AudioDeviceInfo.TYPE_BLE_SPEAKER -> hasBluetooth = true
                AudioDeviceInfo.TYPE_USB_DEVICE,
                AudioDeviceInfo.TYPE_USB_ACCESSORY,
                AudioDeviceInfo.TYPE_USB_HEADSET -> hasUsb = true
            }
        }

        for (dev in inputs) {
            when (dev.type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                AudioDeviceInfo.TYPE_BLE_HEADSET -> hasBluetoothInput = true
            }
        }

        val classification = when {
            hasBluetooth -> RouteClassification.BLUETOOTH_WARNING
            hasHeadset -> RouteClassification.HEADSET_WARNING
            hasUsb -> RouteClassification.USB_WARNING
            else -> RouteClassification.SPEAKER_OK
        }

        val transmitWarning = when (classification) {
            RouteClassification.BLUETOOTH_WARNING -> "Bluetooth audio connected; acoustic chirps may be compressed or filtered."
            RouteClassification.HEADSET_WARNING -> "Headphones plugged in; acoustic transmission may play into earpieces."
            RouteClassification.USB_WARNING -> "USB audio connected; acoustic signals may be rerouted."
            RouteClassification.SPEAKER_OK -> null
        }

        val receiveWarning = if (hasBluetoothInput) {
            "Bluetooth microphone active; near-ultrasound Stealth frequencies are unavailable."
        } else null

        return AudioRouteState(
            classification = classification,
            hasHeadset = hasHeadset,
            hasBluetooth = hasBluetooth,
            hasUsb = hasUsb,
            hasBluetoothInput = hasBluetoothInput,
            transmitWarning = transmitWarning,
            receiveWarning = receiveWarning
        )
    }
}
