package app.shoutsend.modem.sync

/**
 * Operating states of the acoustic synchronization detector.
 */
public enum class SyncState {
    /**
     * Searching for acoustic energy on S0 and S1 sync tones.
     */
    HUNTING,

    /**
     * Alternating S0/S1 preamble pattern detected; tracking candidate symbol boundary.
     */
    PREAMBLE_CANDIDATE,

    /**
     * Preamble confirmed; verifying 8-symbol sync word.
     */
    SYNC_VERIFY,

    /**
     * Exact symbol boundary locked; tracking frame markers.
     */
    LOCKED,

    /**
     * Synchronization lost (missing frame markers or verify failure); entering refractory period.
     */
    LOST
}
