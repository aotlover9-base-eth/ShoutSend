package app.shoutsend.modem.packet

import app.shoutsend.modem.profile.ModemProfile

/**
 * Pure function utility for estimating physical on-air transmission time.
 */
public object AirTime {

    /** Preamble symbol count (16 symbols alternating S0 and S1). */
    public const val PREAMBLE_SYMBOLS: Int = 16

    /** Sync word symbol count (8 fixed pseudo-random MFSK symbols). */
    public const val SYNC_WORD_SYMBOLS: Int = 8

    /** Mode block symbol count (2 symbols repeated once = 4 symbols). */
    public const val MODE_BLOCK_SYMBOLS: Int = 4

    /** Frame marker symbol count preceding each frame codeword. */
    public const val FRAME_MARKER_SYMBOLS: Int = 4

    /** Duration in milliseconds of preamble, sync, and mode block symbols (Normal MFSK16 timing). */
    public const val HEADER_SYMBOL_DURATION_MS: Int = 30

    /** Default lead-in silence in milliseconds. */
    public const val DEFAULT_LEAD_IN_SILENCE_MS: Int = 250

    /** Default tail silence in milliseconds. */
    public const val DEFAULT_TAIL_SILENCE_MS: Int = 150

    /**
     * Estimates total on-air transmission time in seconds.
     *
     * Total transmission includes:
     * - Optional lead-in and tail silence.
     * - Fixed acquisition header: 16 (preamble) + 8 (sync word) + 4 (mode block) = 28 symbols at 30 ms (0.84 s).
     * - For each frame: 4 (frame marker) + RS codeword symbols at the active timing preset.
     * - Multiplied by the number of transmission [repeats].
     *
     * @param profile The configured modem profile.
     * @param onAirBytes Total payload bytes to transmit (1..320).
     * @param repeats Number of frame list repetitions (defaults to [profile.repeats], 1..3).
     * @param leadInSilenceMs Hardware warm-up lead-in silence in milliseconds.
     * @param tailSilenceMs Hardware cooldown tail silence in milliseconds.
     * @return Transmission duration in seconds.
     */
    public fun estimate(
        profile: ModemProfile,
        onAirBytes: Int,
        repeats: Int = profile.repeats,
        leadInSilenceMs: Int = 0,
        tailSilenceMs: Int = 0
    ): Double {
        require(repeats in 1..3) { "Repeats must be between 1 and 3, got $repeats" }
        require(onAirBytes >= 0) { "onAirBytes cannot be negative, got $onAirBytes" }
        require(leadInSilenceMs >= 0) { "leadInSilenceMs cannot be negative, got $leadInSilenceMs" }
        require(tailSilenceMs >= 0) { "tailSilenceMs cannot be negative, got $tailSilenceMs" }

        val silenceDurationSec = (leadInSilenceMs + tailSilenceMs) / 1000.0
        val totalHeaderSymbols = PREAMBLE_SYMBOLS + SYNC_WORD_SYMBOLS + MODE_BLOCK_SYMBOLS
        val headerDurationSec = (totalHeaderSymbols * HEADER_SYMBOL_DURATION_MS) / 1000.0

        if (onAirBytes == 0) {
            return silenceDurationSec + headerDurationSec
        }

        val rs = profile.rsProfile
        val payloadPerFrame = rs.k - 6
        val frameCount = (onAirBytes + payloadPerFrame - 1) / payloadPerFrame

        val codewordBits = rs.n * 8
        val codewordSymbols = codewordBits / profile.scheme.bitsPerSymbol
        val symbolsPerFrame = FRAME_MARKER_SYMBOLS + codewordSymbols

        val frameDurationSec = (symbolsPerFrame * profile.speed.durationMs) / 1000.0
        val totalFramesDurationSec = frameCount * repeats * frameDurationSec

        return silenceDurationSec + headerDurationSec + totalFramesDurationSec
    }
}
