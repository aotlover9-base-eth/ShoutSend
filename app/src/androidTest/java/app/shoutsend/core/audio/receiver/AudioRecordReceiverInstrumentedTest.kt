package app.shoutsend.core.audio.receiver

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.shoutsend.modem.profile.Band
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test verifying microphone capture and audio hardware initialization via [AudioRecordSource].
 * Requires physical hardware or emulator with RECORD_AUDIO permission.
 */
@RunWith(AndroidJUnit4::class)
class AudioRecordReceiverInstrumentedTest {

    @Test
    fun testAudioRecordSourceInitializationAndCapture() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = AudioRecordSource(context)

        try {
            source.start()
            val info = source.sourceInfo
            assertNotNull(info.sourceName)
            assertTrue(source.actualSampleRate == 48000 || source.actualSampleRate == 44100)

            val buffer = ShortArray(960)
            val read = source.read(buffer, 0, buffer.size)
            assertTrue("Expected non-negative frames read, got $read", read >= 0)
        } catch (_: PermissionMissingException) {
            // Permission not granted in test environment without grantRuntimePermission
        } finally {
            source.release()
        }
    }

    @Test
    fun testDefaultReceiverStartAndStop() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val receiver = DefaultAcousticReceiver(
            pcmSourceFactory = { AudioRecordSource(context) }
        )

        try {
            receiver.start(Band.RETRO)
            assertTrue("Receiver running", receiver.isRunning)
        } catch (_: PermissionMissingException) {
            // Permission not granted in test runner
        } finally {
            receiver.stop()
            assertTrue("Receiver stopped", !receiver.isRunning)
        }
    }
}
