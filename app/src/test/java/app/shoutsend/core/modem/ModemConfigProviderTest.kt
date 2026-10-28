package app.shoutsend.core.modem

import app.shoutsend.core.audio.receiver.AcousticReceiver
import app.shoutsend.core.audio.receiver.ReceiverDiagnostics
import app.shoutsend.core.audio.transmitter.AcousticTransmitter
import app.shoutsend.core.audio.transmitter.TransmitProgress
import app.shoutsend.core.audio.transmitter.TransmitResult
import app.shoutsend.core.session.AudioCoordinator
import app.shoutsend.core.session.AudioFocusController
import app.shoutsend.core.session.DefaultTransmitSession
import app.shoutsend.core.session.SessionMode
import app.shoutsend.modem.AudioSpec
import app.shoutsend.modem.demod.ReceiveEvent
import app.shoutsend.modem.modulator.AudioStreamSource
import app.shoutsend.modem.payload.Payload
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.Speed
import app.shoutsend.modem.tuning.ModemTuning
import app.shoutsend.modem.tuning.ModemTuningValidationResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ModemConfigProviderTest {

    private class TestModemConfigProvider(
        initialConfig: ModemConfig = ModemConfig()
    ) : ModemConfigProvider {
        private val _config = MutableStateFlow(initialConfig)
        override val config: StateFlow<ModemConfig> = _config.asStateFlow()

        fun update(newConfig: ModemConfig) {
            _config.value = newConfig
        }
    }

    private class TestAudioCoordinator : AudioCoordinator {
        override val activeMode: StateFlow<SessionMode> = MutableStateFlow(SessionMode.IDLE)
        override val keepScreenOn: StateFlow<Boolean> = MutableStateFlow(false)
        override suspend fun acquireTransmitLock(): Boolean = true
        override fun releaseTransmitLock() {}
        override suspend fun acquireReceiveLock(): Boolean = true
        override fun releaseReceiveLock() {}
        override suspend fun acquireCalibrationLock(): Boolean = true
        override fun releaseCalibrationLock() {}
    }

    private class TestAudioFocusController : AudioFocusController {
        override var isMusicActive: Boolean = false
        override fun requestFocus(onFocusLost: (reason: String) -> Unit): Boolean = true
        override fun abandonFocus() {}
    }

    private class TestAcousticTransmitter : AcousticTransmitter {
        var lastSource: AudioStreamSource? = null

        override suspend fun transmit(
            source: AudioStreamSource,
            onProgress: (progress: TransmitProgress) -> Unit
        ): TransmitResult {
            lastSource = source
            return TransmitResult.Success(
                totalFrames = source.totalSamples,
                durationMs = (source.durationSeconds * 1000).toLong(),
                underrunCount = 0
            )
        }
    }

    private class TestAcousticReceiver : AcousticReceiver {
        private val _events = MutableSharedFlow<ReceiveEvent>(replay = 16)
        override val events: SharedFlow<ReceiveEvent> = _events
        override val diagnostics: StateFlow<ReceiverDiagnostics> = MutableStateFlow(ReceiverDiagnostics())
        override val spectrumRows: ReceiveChannel<FloatArray> = Channel()
        override var rawAudioTap: ((samples: ShortArray, offset: Int, count: Int) -> Unit)? = null
        override var isRunning: Boolean = false
            private set

        var lastBand: Band? = null
        var lastPassphrase: String? = null
        var lastTuning: ModemTuning? = null

        override fun start(band: Band, passphrase: String?, tuning: ModemTuning) {
            isRunning = true
            lastBand = band
            lastPassphrase = passphrase
            lastTuning = tuning
        }

        override fun stop() {
            isRunning = false
        }
    }

    @Test
    fun testProviderEmitsUpdates() {
        val provider = TestModemConfigProvider()
        assertEquals(Band.RETRO, provider.config.value.band)
        assertEquals(ModemTuning.DEFAULT, provider.config.value.tuning)

        val customTuning = ModemTuning(leadInSilenceMs = 300, outputLevel = 0.5f)
        val updated = ModemConfig(
            band = Band.STEALTH,
            speed = Speed.FAST,
            repeats = 1,
            tuning = customTuning
        )
        provider.update(updated)

        assertEquals(Band.STEALTH, provider.config.value.band)
        assertEquals(Speed.FAST, provider.config.value.speed)
        assertEquals(1, provider.config.value.repeats)
        assertEquals(customTuning, provider.config.value.tuning)
    }

    @Test
    fun testTransmitSessionPicksUpConfigAtStart() = runBlocking {
        val coordinator = TestAudioCoordinator()
        val focus = TestAudioFocusController()
        val transmitter = TestAcousticTransmitter()
        val provider = TestModemConfigProvider()

        val session = DefaultTransmitSession(
            coordinator = coordinator,
            focusController = focus,
            transmitter = transmitter,
            configProvider = provider
        )

        val payload = Payload.Text("Dynamic Tuning Config Test")

        // First send: default config
        session.send(payload)
        val initialSource = transmitter.lastSource
        assertNotNull(initialSource)

        // Update provider with custom leadInSilenceMs and tailSilenceMs
        val customTuning = ModemTuning(leadInSilenceMs = 600, tailSilenceMs = 300)
        provider.update(ModemConfig(speed = Speed.FAST, repeats = 1, tuning = customTuning))

        // Second send: must use updated profile and tuning
        session.send(payload)
        val updatedSource = transmitter.lastSource
        assertNotNull(updatedSource)

        // Confirm updated configuration produced different transmission waveform length
        assertTrue(
            "Custom lead-in/tail silence and speed must affect total transmission samples",
            initialSource!!.totalSamples != updatedSource!!.totalSamples
        )
    }

    @Test
    fun testModemTuningValidationRejectsDangerousValues() {
        // Output level > 1.0 (clipping)
        val overAmplified = ModemTuning(outputLevel = 1.1f)
        assertTrue(overAmplified.validate() is ModemTuningValidationResult.Invalid)

        // Output level <= 0.0 (silent)
        val silent = ModemTuning(outputLevel = 0.0f)
        assertTrue(silent.validate() is ModemTuningValidationResult.Invalid)

        // Non-power-of-2 FFT
        val badFft = ModemTuning(fftSize = 500)
        assertTrue(badFft.validate() is ModemTuningValidationResult.Invalid)

        // Negative silence duration
        val negativeLeadIn = ModemTuning(leadInSilenceMs = -10)
        assertTrue(negativeLeadIn.validate() is ModemTuningValidationResult.Invalid)

        // Symbol duration too short (< 10 ms)
        val tooFastSymbol = ModemTuning(fastDurationMs = 5)
        assertTrue(tooFastSymbol.validate() is ModemTuningValidationResult.Invalid)

        // Default tuning must be strictly valid
        assertTrue(ModemTuning.DEFAULT.validate() is ModemTuningValidationResult.Valid)
    }
}
