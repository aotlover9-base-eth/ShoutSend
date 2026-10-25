package app.shoutsend.core.capability

import app.shoutsend.core.audio.receiver.AudioEffectsState
import app.shoutsend.core.audio.transmitter.OutputRouteInfo

/**
 * Diagnostic snapshot of device audio hardware capabilities and system features.
 *
 * Privacy Guard (Rule 25): [deviceModel] is used solely for on-device diagnostics and
 * settings screens. It is NEVER transmitted acoustically, persisted in the Vault, or logged.
 */
data class CapabilityReport(
    val nativeOutputSampleRateHz: Int,
    val outputFramesPerBuffer: Int,
    val supportedInputSampleRates: List<Int>,
    val isUnprocessedSupported: Boolean,
    val hasLowLatencyAudio: Boolean,
    val hasProAudio: Boolean,
    val deviceModel: String,
    val effects: AudioEffectsState,
    val outputRoute: OutputRouteInfo
) {
    /**
     * Formatted technical summary for developer diagnostics and capability card.
     */
    fun toSummaryString(): String = buildString {
        appendLine("Device: $deviceModel")
        appendLine("Output: $nativeOutputSampleRateHz Hz (${outputFramesPerBuffer} frames/buf)")
        appendLine("Input Rates: ${supportedInputSampleRates.joinToString()} Hz")
        appendLine("Unprocessed Mic: ${if (isUnprocessedSupported) "YES" else "NO"}")
        appendLine("Low Latency: ${if (hasLowLatencyAudio) "YES" else "NO"}, Pro Audio: ${if (hasProAudio) "YES" else "NO"}")
        appendLine("Effects: AEC=${effects.aecAvailable}, NS=${effects.nsAvailable}, AGC=${effects.agcAvailable}")
        appendLine("Route: Headset=${outputRoute.hasHeadset}, BT=${outputRoute.hasBluetooth}, USB=${outputRoute.hasUsb}, Vol=${(outputRoute.volumeFraction * 100).toInt()}%")
    }
}
