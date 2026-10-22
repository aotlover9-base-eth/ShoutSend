package app.shoutsend.modem.protocol

import app.shoutsend.modem.profile.Scheme
import app.shoutsend.modem.profile.Speed

/**
 * Fundamental constants and bit patterns governing the ShoutSend acoustic physical layer.
 */
public object ProtocolConstants {

    /**
     * Special tone index indicator for Sync Tone 0 (S0).
     */
    public const val SYNC_TONE_0_INDEX: Int = 16

    /**
     * Special tone index indicator for Sync Tone 1 (S1).
     */
    public const val SYNC_TONE_1_INDEX: Int = 17

    /**
     * Length of the preamble in symbols (alternating S0 and S1).
     */
    public const val PREAMBLE_SYMBOL_COUNT: Int = 16

    /**
     * Preamble symbol sequence (16 symbols alternating S0 and S1).
     */
    public val PREAMBLE_PATTERN: IntArray = IntArray(PREAMBLE_SYMBOL_COUNT) { i ->
        if (i % 2 == 0) SYNC_TONE_0_INDEX else SYNC_TONE_1_INDEX
    }

    /**
     * 8-symbol fixed pseudo-random MFSK sync word chosen for zero non-zero autocorrelation side lobes.
     */
    public val SYNC_WORD_PATTERN: IntArray = intArrayOf(3, 11, 7, 14, 1, 9, 5, 12)

    /**
     * 4-symbol fixed frame marker preceding every transmitted Reed-Solomon codeword.
     */
    public val FRAME_MARKER_PATTERN: IntArray = intArrayOf(13, 2, 10, 4)

    /**
     * Mode block symbol count (unrepeated: 2 symbols; repeated: 4 symbols).
     */
    public const val MODE_BLOCK_SYMBOL_COUNT: Int = 2
    public const val MODE_BLOCK_REPEATED_SYMBOL_COUNT: Int = 4

    /**
     * Preamble, sync word, and mode block are always transmitted using MFSK16 at Normal (30ms) speed.
     */
    public val HEADER_SCHEME: Scheme = Scheme.MFSK16
    public val HEADER_SPEED: Speed = Speed.NORMAL

    /**
     * Default frame repeat count on air.
     */
    public const val DEFAULT_FRAME_REPEATS: Int = 2

    /**
     * Fixed PN9 whitening LFSR seed for Protocol v1 transmissions.
     * Constant across all frames so receiver can de-whiten without needing prior knowledge of fragment index.
     */
    public const val WHITEN_SEED_V1: Int = 0xA5

    /**
     * Maximum payload size on air in bytes for Protocol v1.
     */
    public const val MAX_AIR_PAYLOAD_BYTES: Int = 320

    /**
     * Maximum fragments allowed in one transmission.
     */
    public const val MAX_FRAGMENTS: Int = 16

    /**
     * Maximum uncompressed payload bytes per fragment.
     */
    public const val MAX_FRAGMENT_PAYLOAD_BYTES: Int = 42
}
