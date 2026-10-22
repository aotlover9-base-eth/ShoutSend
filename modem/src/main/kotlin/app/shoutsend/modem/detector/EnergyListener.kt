package app.shoutsend.modem.detector

/**
 * Listener interface for receiving evaluated acoustic tone energy blocks.
 * Designed for zero allocations on the reception hot path.
 */
public fun interface EnergyListener {

    /**
     * Called when a block of audio samples has been evaluated by [BlockDetector].
     *
     * @param sampleIndex Absolute sample index corresponding to the start of this analysis block.
     * @param energies Preallocated float array of size [GoertzelBank.filterCount] holding normalized tone powers.
     *                 The array is owned and reused by the detector; listeners must copy if retaining data.
     */
    public fun onBlockEnergies(sampleIndex: Long, energies: FloatArray)
}
