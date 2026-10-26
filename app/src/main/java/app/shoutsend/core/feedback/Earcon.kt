package app.shoutsend.core.feedback

/**
 * Acoustic earcon identifiers for distinctive UI and modem state chimes.
 */
enum class Earcon {
    /** Upward chirp frequency sweep emitted upon transmission initiation. */
    TRANSMIT_START,

    /** Crisp acoustic click emitted when preamble sync and symbol timing lock. */
    RECEIVE_LOCKED,

    /** Ascending harmonic chord chime emitted on frame delivery or reception completion. */
    SUCCESS,

    /** Subtle low tone emitted on transmission or packet decode failure. */
    ERROR
}
