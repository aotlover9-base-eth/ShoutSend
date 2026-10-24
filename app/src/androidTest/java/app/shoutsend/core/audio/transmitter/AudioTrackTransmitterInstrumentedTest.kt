package app.shoutsend.core.audio.transmitter

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.shoutsend.modem.AudioSpec
import app.shoutsend.modem.modulator.Modulator
import app.shoutsend.modem.modulator.TransmissionBuilder
import app.shoutsend.modem.packet.MessageFlags
import app.shoutsend.modem.packet.MessageType
import app.shoutsend.modem.profile.ModemProfile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test playing a short Retro transmission through Android [AudioTrack].
 * Requires physical device or emulator with audio hardware.
 */
@RunWith(AndroidJUnit4::class)
class AudioTrackTransmitterInstrumentedTest {

    @Test
    fun testShortRetroTransmissionRunsWithoutException() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val routeInfo = getOutputRouteInfo(appContext)

        val plan = TransmissionBuilder.build(
            type = MessageType.TEXT,
            flags = MessageFlags.NONE,
            payload = "OK".toByteArray(Charsets.UTF_8),
            profile = ModemProfile.DEFAULT_RETRO,
            audioSpec = AudioSpec(routeInfo.nativeSampleRateHz),
            leadInSilenceMs = 50,
            tailSilenceMs = 50
        )
        val modulator = Modulator(plan)
        val transmitter = DefaultAcousticTransmitter()

        val result = runBlocking {
            transmitter.transmit(modulator)
        }

        assertTrue("Expected Success, got $result", result is TransmitResult.Success)
    }
}
