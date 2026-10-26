package app.shoutsend.core.feedback

import app.shoutsend.core.data.settings.FakeSettingsRepository
import app.shoutsend.core.data.settings.Settings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying Earcon audio synthesis, AudioFeedbackPlayer settings gating,
 * and TactileFeedbackController haptic toggles (Step 47).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FeedbackControllerTest {

    private class TestAudioEarconSink : AudioEarconSink {
        val playedEarcons = mutableListOf<Earcon>()
        var isReleased = false

        override fun play(earcon: Earcon, pcm: ShortArray) {
            playedEarcons.add(earcon)
        }

        override fun release() {
            isReleased = true
        }
    }

    @Test
    fun earconSynthesizerGeneratesValidTransmitStartWaveform() {
        val pcm = EarconSynthesizer.synthesizeTransmitStart(sampleRate = 48000)
        assertTrue(pcm.isNotEmpty())
        assertEquals(4800, pcm.size) // 100 ms at 48 kHz
        assertTrue(pcm.any { it != 0.toShort() })
    }

    @Test
    fun earconSynthesizerGeneratesValidReceiveLockedWaveform() {
        val pcm = EarconSynthesizer.synthesizeReceiveLocked(sampleRate = 48000)
        assertTrue(pcm.isNotEmpty())
        assertEquals(720, pcm.size) // 15 ms at 48 kHz
        assertTrue(pcm.any { it != 0.toShort() })
    }

    @Test
    fun earconSynthesizerGeneratesValidSuccessChordWaveform() {
        val pcm = EarconSynthesizer.synthesizeSuccess(sampleRate = 48000)
        assertTrue(pcm.isNotEmpty())
        assertEquals(11520, pcm.size) // 240 ms at 48 kHz
        assertTrue(pcm.any { it != 0.toShort() })
    }

    @Test
    fun earconSynthesizerGeneratesValidErrorToneWaveform() {
        val pcm = EarconSynthesizer.synthesizeError(sampleRate = 48000)
        assertTrue(pcm.isNotEmpty())
        assertEquals(8640, pcm.size) // 180 ms at 48 kHz
        assertTrue(pcm.any { it != 0.toShort() })
    }

    @Test
    fun audioFeedbackPlayerPlaysWhenSoundEffectsEnabled() = runTest {
        val settingsRepo = FakeSettingsRepository(Settings(soundEffectsEnabled = true))
        val sink = TestAudioEarconSink()

        // Pass null context/audioManager with test sink
        val player = DefaultAudioFeedbackPlayer(
            context = object : android.content.ContextWrapper(null) {},
            settingsRepository = settingsRepo,
            audioManager = null,
            audioSink = sink
        )

        player.playTransmitStart()
        assertEquals(1, sink.playedEarcons.size)
        assertEquals(Earcon.TRANSMIT_START, sink.playedEarcons[0])

        player.playReceiveLocked()
        assertEquals(2, sink.playedEarcons.size)
        assertEquals(Earcon.RECEIVE_LOCKED, sink.playedEarcons[1])

        player.playSuccess()
        assertEquals(3, sink.playedEarcons.size)
        assertEquals(Earcon.SUCCESS, sink.playedEarcons[2])

        player.playError()
        assertEquals(4, sink.playedEarcons.size)
        assertEquals(Earcon.ERROR, sink.playedEarcons[3])

        player.release()
        assertTrue(sink.isReleased)
    }

    @Test
    fun audioFeedbackPlayerSuppressesAudioWhenSoundEffectsDisabled() = runTest {
        val settingsRepo = FakeSettingsRepository(Settings(soundEffectsEnabled = false))
        val sink = TestAudioEarconSink()

        val player = DefaultAudioFeedbackPlayer(
            context = object : android.content.ContextWrapper(null) {},
            settingsRepository = settingsRepo,
            audioManager = null,
            audioSink = sink
        )

        player.playTransmitStart()
        player.playReceiveLocked()
        player.playSuccess()
        player.playError()

        assertTrue(sink.playedEarcons.isEmpty())
    }

    @Test
    fun noOpAudioFeedbackPlayerIsCompletelySafeToCall() {
        NoOpAudioFeedbackPlayer.playTransmitStart()
        NoOpAudioFeedbackPlayer.playReceiveLocked()
        NoOpAudioFeedbackPlayer.playSuccess()
        NoOpAudioFeedbackPlayer.playError()
        NoOpAudioFeedbackPlayer.play(Earcon.SUCCESS)
        NoOpAudioFeedbackPlayer.release()
    }

    @Test
    fun noOpTactileFeedbackControllerIsCompletelySafeToCall() {
        NoOpTactileFeedbackController.click()
        NoOpTactileFeedbackController.tick()
        NoOpTactileFeedbackController.success()
        NoOpTactileFeedbackController.error()
    }
}
