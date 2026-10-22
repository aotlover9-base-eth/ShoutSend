# Listening to ShoutSend Audio Samples (OWNER RUN)

Step 17 generated standard 16-bit 48 kHz mono RIFF/WAVE files of modulated acoustic transmissions for manual listening:

## 1. Generated Samples
- **Retro Band Sample**: `modem/build/outputs/shoutsend_retro_sample.wav`
  - Modulation: MFSK16 at Normal (30ms) speed, audible frequency range $1.5\text{ kHz} - 3.5\text{ kHz}$.
  - Sounds like a classic 90s acoustic telephone coupler/modem or dial-up chirp sequence.
- **Stealth Band Sample**: `modem/build/outputs/shoutsend_stealth_sample.wav`
  - Modulation: DTFSK at Fast (20ms) speed, ultrasonic boundary $17.6\text{ kHz} - 20.0\text{ kHz}$.
  - Near-inaudible to human ears; sounds like faint high-frequency clicks on laptop speakers.

## 2. How to Re-generate Samples
Run the dedicated test target:
```bash
./gradlew :modem:test --tests "app.shoutsend.modem.modulator.ModulatorTest.testWavGenerationProducesValidAudioFiles"
```

## 3. How to Play on Host Machine (Linux)
Using `aplay` (ALSA), `paplay` (PulseAudio), or `ffplay`:
```bash
# Play retro modem chirp
aplay modem/build/outputs/shoutsend_retro_sample.wav

# Or using ffplay
ffplay -nodisp -autoexit modem/build/outputs/shoutsend_retro_sample.wav
```
Confirm that the Retro sample sounds like a crisp, musical frequency-shift keying modem sequence with clear preamble, sync word, mode declaration, and frame chirps.
