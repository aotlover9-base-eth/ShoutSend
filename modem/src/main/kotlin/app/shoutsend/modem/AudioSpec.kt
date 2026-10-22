package app.shoutsend.modem

public data class AudioSpec(
    public val sampleRateHz: Int
) {
    init {
        require(sampleRateHz == 48000 || sampleRateHz == 44100) {
            "Unsupported sample rate: $sampleRateHz. Allowed rates: 48000, 44100."
        }
    }

    public companion object {
        public val RATE_48K: AudioSpec = AudioSpec(48000)
        public val RATE_44K: AudioSpec = AudioSpec(44100)
    }
}
