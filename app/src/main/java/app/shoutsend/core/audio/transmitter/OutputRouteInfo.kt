package app.shoutsend.core.audio.transmitter

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build

/**
 * Snapshot of current audio output routing and media volume.
 */
data class OutputRouteInfo(
    val hasHeadset: Boolean,
    val hasBluetooth: Boolean,
    val hasUsb: Boolean,
    val volumeFraction: Float,
    val nativeSampleRateHz: Int
)

/**
 * Pure inspection helper reading output device and volume information from [AudioManager].
 * Does not modify device volume or settings.
 */
fun getOutputRouteInfo(context: Context): OutputRouteInfo {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    val nativeRate = AudioTrackSink.selectSampleRate()

    if (audioManager == null) {
        return OutputRouteInfo(
            hasHeadset = false,
            hasBluetooth = false,
            hasUsb = false,
            volumeFraction = 1.0f,
            nativeSampleRateHz = nativeRate
        )
    }

    val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    val volumeFraction = (currentVol.toFloat() / maxVol.toFloat()).coerceIn(0.0f, 1.0f)

    var hasHeadset = false
    var hasBluetooth = false
    var hasUsb = false

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        for (dev in devices) {
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
    } else {
        @Suppress("DEPRECATION")
        hasHeadset = audioManager.isWiredHeadsetOn
        @Suppress("DEPRECATION")
        hasBluetooth = audioManager.isBluetoothA2dpOn || audioManager.isBluetoothScoOn
    }

    return OutputRouteInfo(
        hasHeadset = hasHeadset,
        hasBluetooth = hasBluetooth,
        hasUsb = hasUsb,
        volumeFraction = volumeFraction,
        nativeSampleRateHz = nativeRate
    )
}
