package app.shoutsend.core.session

import app.shoutsend.core.audio.receiver.AcousticReceiver
import app.shoutsend.core.audio.receiver.DefaultAcousticReceiver
import app.shoutsend.core.audio.receiver.FakePcmSource
import app.shoutsend.core.audio.receiver.ReceiverDiagnostics
import app.shoutsend.core.audio.transmitter.AcousticTransmitter
import app.shoutsend.core.audio.transmitter.TransmitProgress
import app.shoutsend.core.audio.transmitter.TransmitResult
import app.shoutsend.modem.AudioSpec
import app.shoutsend.modem.channel.AcousticChannelSimulator
import app.shoutsend.modem.channel.ChannelScenario
import app.shoutsend.modem.demod.ReceiveEvent
import app.shoutsend.modem.modulator.AudioStreamSource
import app.shoutsend.modem.packet.MessageFlags
import app.shoutsend.modem.packet.MessageType
import app.shoutsend.modem.packet.ReceivedMessage
import app.shoutsend.modem.payload.Payload
import app.shoutsend.modem.payload.PayloadCodec
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.ModemProfile
import app.shoutsend.modem.profile.Scheme
import app.shoutsend.modem.profile.Speed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionIntegrationTest {

    private class FakeAudioFocusController : AudioFocusController {
        override var isMusicActive: Boolean = false
        var requestCount = 0
        var abandonCount = 0
        var shouldGrant = true
        private var focusListener: ((String) -> Unit)? = null

        override fun requestFocus(onFocusLost: (reason: String) -> Unit): Boolean {
            requestCount++
            focusListener = onFocusLost
            return shouldGrant
        }

        override fun abandonFocus() {
            abandonCount++
            focusListener = null
        }

        fun simulateFocusLoss(reason: String = "AudioFocusLost") {
            focusListener?.invoke(reason)
        }
    }

    private class FakeTransmitter : AcousticTransmitter {
        var transmitCount = 0
        var lastSource: AudioStreamSource? = null
        var onTransmitChunk: ((FloatArray, Int) -> Unit)? = null

        override suspend fun transmit(
            source: AudioStreamSource,
            onProgress: (progress: TransmitProgress) -> Unit
        ): TransmitResult {
            transmitCount++
            lastSource = source

            val total = source.totalSamples.toInt()
            val chunk = 960
            val buf = FloatArray(chunk)
            var cursor = 0

            while (cursor < total) {
                val toRender = minOf(chunk, total - cursor)
                source.render(cursor.toLong(), buf, 0, toRender)
                onTransmitChunk?.invoke(buf, toRender)
                cursor += toRender

                val frac = cursor.toFloat() / total.toFloat()
                onProgress(
                    TransmitProgress(
                        samplesWritten = cursor.toLong(),
                        totalSamples = total.toLong(),
                        fraction = frac,
                        elapsedSeconds = cursor.toDouble() / 48000.0,
                        remainingSeconds = (total - cursor).toDouble() / 48000.0
                    )
                )
            }

            return TransmitResult.Success(
                totalFrames = source.totalSamples,
                durationMs = (source.durationSeconds * 1000).toLong(),
                underrunCount = 0
            )
        }
    }

    private class FakeReceiver : AcousticReceiver {
        private val _events = MutableSharedFlow<ReceiveEvent>(replay = 16)
        override val events: SharedFlow<ReceiveEvent> = _events
        override val diagnostics: StateFlow<ReceiverDiagnostics> = MutableStateFlow(ReceiverDiagnostics(ambientLevelDbfs = -50f))
        override val spectrumRows: ReceiveChannel<FloatArray> = Channel()
        override var rawAudioTap: ((samples: ShortArray, offset: Int, count: Int) -> Unit)? = null
        override var isRunning: Boolean = false
            private set

        var startCount = 0
        var stopCount = 0

        override fun start(band: Band, passphrase: String?) {
            isRunning = true
            startCount++
        }

        override fun stop() {
            isRunning = false
            stopCount++
        }

        fun emitEvent(event: ReceiveEvent) {
            _events.tryEmit(event)
        }
    }

    @Test
    fun testTransmitStateTransitionsAndCompletion() = runBlocking {
        val receiver = FakeReceiver()
        val coordinator = DefaultAudioCoordinator(receiver)
        val focus = FakeAudioFocusController()
        val transmitter = FakeTransmitter()
        val session = DefaultTransmitSession(coordinator, focus, transmitter)

        assertEquals(TransmitState.Idle, session.state.value)

        val payload = Payload.Text("Hello ShoutSend Session!")
        val resultState = session.send(payload)

        assertTrue("State must complete", resultState is TransmitState.Completed)
        assertEquals(1, focus.requestCount)
        assertEquals(1, focus.abandonCount)
        assertEquals(1, transmitter.transmitCount)
        assertEquals(SessionMode.IDLE, coordinator.activeMode.value)
    }

    @Test
    fun testTransmitPayloadTooLargeFailsImmediately() = runBlocking {
        val receiver = FakeReceiver()
        val coordinator = DefaultAudioCoordinator(receiver)
        val focus = FakeAudioFocusController()
        val transmitter = FakeTransmitter()
        val session = DefaultTransmitSession(coordinator, focus, transmitter)

        // Text exceeding 320 bytes
        val largeText = "A".repeat(400)
        val payload = Payload.Text(largeText)
        val result = session.send(payload)

        assertTrue(result is TransmitState.Failed)
        assertEquals("PayloadTooLarge", (result as TransmitState.Failed).reason)
        assertEquals(0, focus.requestCount)
        assertEquals(0, transmitter.transmitCount)
    }

    @Test
    fun testTransmitFocusLossCancelsWithFailed() = runBlocking {
        val receiver = FakeReceiver()
        val coordinator = DefaultAudioCoordinator(receiver)
        val focus = FakeAudioFocusController()
        val transmitter = FakeTransmitter()
        val session = DefaultTransmitSession(coordinator, focus, transmitter)

        // Trigger focus loss during chunk rendering
        transmitter.onTransmitChunk = { _, _ ->
            focus.simulateFocusLoss("AudioFocusLost")
        }

        val result = session.send(Payload.Text("Focus loss test"))
        assertTrue("Must fail on focus loss", result is TransmitState.Failed)
        assertEquals("AudioFocusLost", (result as TransmitState.Failed).reason)
        assertEquals(SessionMode.IDLE, coordinator.activeMode.value)
    }

    @Test
    fun testAudioCoordinatorEnforcesHalfDuplexMutualExclusion() = runBlocking {
        val receiver = FakeReceiver()
        val coordinator = DefaultAudioCoordinator(receiver)
        val focus = FakeAudioFocusController()
        val transmitter = FakeTransmitter()

        val rxSession = DefaultReceiveSession(coordinator, focus, receiver)
        val txSession = DefaultTransmitSession(coordinator, focus, transmitter)

        // 1. Start receiving
        rxSession.start(Band.RETRO)
        assertTrue(receiver.isRunning)
        assertEquals(SessionMode.RECEIVING, coordinator.activeMode.value)
        assertTrue(coordinator.keepScreenOn.value)

        // 2. Start transmitting -> must stop receiver and transition to TRANSMITTING
        val txJob = async {
            txSession.send(Payload.Text("Half duplex test"))
        }

        txJob.await()
        assertFalse("Receiver must be stopped by transmitter lock", receiver.isRunning)
        assertEquals(SessionMode.IDLE, coordinator.activeMode.value)
    }

    @Test
    fun testReceiveStateTransitionsToSuccess() = runBlocking {
        val receiver = FakeReceiver()
        val coordinator = DefaultAudioCoordinator(receiver)
        val focus = FakeAudioFocusController()
        val rxSession = DefaultReceiveSession(coordinator, focus, receiver)

        rxSession.start(Band.RETRO)
        assertTrue(rxSession.state.value is ReceiveState.Listening)

        // Emit demodulation events
        receiver.emitEvent(ReceiveEvent.PreambleDetected)
        delay(5)
        assertTrue(rxSession.state.value is ReceiveState.PreambleDetected)

        receiver.emitEvent(ReceiveEvent.Locked(18.5))
        delay(5)
        assertEquals(ReceiveState.Locked(18.5), rxSession.state.value)

        receiver.emitEvent(ReceiveEvent.FrameDecoded(fragmentIndex = 0, fragmentCount = 1, correctedBytes = 0))
        delay(5)
        assertEquals(ReceiveState.Receiving(1, 1, 18.5), rxSession.state.value)

        val payload = Payload.Text("Demodulated Text")
        val msg = ReceivedMessage(
            type = MessageType.TEXT,
            flags = MessageFlags.NONE,
            messageId = 1,
            payload = PayloadCodec.encode(payload)
        )
        receiver.emitEvent(ReceiveEvent.MessageComplete(message = msg, payload = payload))
        delay(5)

        val state = rxSession.state.value
        assertTrue("State must be Success", state is ReceiveState.Success)
        assertEquals("Demodulated Text", ((state as ReceiveState.Success).payload as Payload.Text).value)

        rxSession.stop()
        assertEquals(ReceiveState.Idle, rxSession.state.value)
    }

    @Test
    fun testReceiveEncryptedMessageWithPassphraseProvider() = runBlocking {
        val receiver = FakeReceiver()
        val coordinator = DefaultAudioCoordinator(receiver)
        val focus = FakeAudioFocusController()
        val rxSession = DefaultReceiveSession(coordinator, focus, receiver)

        val secretText = "TopSecretAcousticData"
        val passphrase = "CorrectPassword123"
        val sealed = app.shoutsend.modem.crypto.SecureBox.seal(
            plainBytes = PayloadCodec.encode(Payload.Text(secretText)),
            type = MessageType.TEXT,
            passphrase = passphrase.toCharArray()
        )

        val encryptedMsg = ReceivedMessage(
            type = MessageType.TEXT,
            flags = MessageFlags(isEncrypted = true, isCompact = false, isReserved = false),
            messageId = 42,
            payload = sealed
        )

        // Start with matching passphrase provider
        rxSession.start(Band.RETRO, passphraseProvider = { passphrase })

        receiver.emitEvent(ReceiveEvent.MessageComplete(message = encryptedMsg, payload = null))
        var waited = 0
        while (waited < 100 && rxSession.state.value !is ReceiveState.Success && rxSession.state.value !is ReceiveState.Failed) {
            delay(20)
            waited++
        }

        val state = rxSession.state.value
        assertTrue("Must successfully decrypt", state is ReceiveState.Success)
        assertEquals(secretText, ((state as ReceiveState.Success).payload as Payload.Text).value)
        assertTrue(state.metadata.isEncrypted)

        rxSession.stop()
    }

    @Test
    fun testSessionLifecycleManagerStopsReceiveAfterGracePeriod() = runBlocking {
        val receiver = FakeReceiver()
        val coordinator = DefaultAudioCoordinator(receiver)
        val focus = FakeAudioFocusController()
        val rxSession = DefaultReceiveSession(coordinator, focus, receiver)
        val lifecycleManager = SessionLifecycleManager(rxSession, defaultGraceSeconds = 1)

        rxSession.start(Band.RETRO)
        assertTrue(receiver.isRunning)

        // App goes to background with 1s grace
        lifecycleManager.onAppBackgrounded(graceSeconds = 1)
        assertTrue(lifecycleManager.isBackgrounded)
        assertTrue("Receiver still running during grace period", receiver.isRunning)

        // Wait past grace period
        delay(1200)
        assertFalse("Receiver must be stopped after grace period expires", receiver.isRunning)
        assertEquals(ReceiveState.Idle, rxSession.state.value)

        // Foreground cancels timer
        lifecycleManager.onAppForegrounded()
        assertFalse(lifecycleManager.isBackgrounded)
    }

    @Test
    fun testEndToEndSecureSendAndReceiveLoopWithStep22ChannelSimulator() = runBlocking {
        val profile = ModemProfile(Band.RETRO, Scheme.MFSK16, Speed.NORMAL, repeats = 1, rsProfileId = 1)
        val messageText = "ShoutSend End-To-End Acoustic Loopback"
        val passphrase = "AcousticPassphrase99"

        val audioSpec = AudioSpec.RATE_48K
        val recordedSamples = mutableListOf<Float>()

        val txTransmitter = FakeTransmitter()
        txTransmitter.onTransmitChunk = { chunk, count ->
            for (i in 0 until count) {
                recordedSamples.add(chunk[i])
            }
        }

        val dummyReceiver = FakeReceiver()
        val coordinator = DefaultAudioCoordinator(dummyReceiver)
        val focus = FakeAudioFocusController()
        val txSession = DefaultTransmitSession(coordinator, focus, txTransmitter, audioSpec)

        // Transmit secure payload
        val txResult = txSession.send(
            payload = Payload.Text(messageText),
            profile = profile,
            securePassphrase = passphrase
        )
        assertTrue("TX completed", txResult is TransmitState.Completed)

        // Impair via ChannelSimulator (CLEAN room)
        val simulator = AcousticChannelSimulator(
            scenario = ChannelScenario.CLEAN,
            audioSpec = audioSpec
        )
        val impairedAudio = simulator.process(recordedSamples.toFloatArray(), seed = 123L)

        // Convert to ShortArray for receiver
        val pcmShorts = ShortArray(impairedAudio.size) { i ->
            (impairedAudio[i] * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
        }

        // Set up production receiver
        val fakeSource = FakePcmSource(pcmShorts, actualSampleRate = 48000)
        val prodReceiver = DefaultAcousticReceiver(
            pcmSourceFactory = { fakeSource }
        )
        val rxCoordinator = DefaultAudioCoordinator(prodReceiver)
        val rxSession = DefaultReceiveSession(rxCoordinator, focus, prodReceiver)

        rxSession.start(band = Band.RETRO, passphraseProvider = { passphrase })

        // Wait up to 5s for decoding
        var waited = 0
        while (waited < 50 && rxSession.state.value !is ReceiveState.Success && rxSession.state.value !is ReceiveState.Failed) {
            delay(100)
            waited++
        }

        val rxState = rxSession.state.value
        assertTrue("RX reached Success, actual state: $rxState", rxState is ReceiveState.Success)
        val decodedText = ((rxState as ReceiveState.Success).payload as Payload.Text).value
        assertEquals(messageText, decodedText)
        assertTrue(rxState.metadata.isEncrypted)

        rxSession.stop()
    }
}
