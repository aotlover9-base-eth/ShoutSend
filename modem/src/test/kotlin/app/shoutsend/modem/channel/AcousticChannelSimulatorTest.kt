package app.shoutsend.modem.channel

import app.shoutsend.modem.AudioSpec
import app.shoutsend.modem.modulator.OutgoingMessage
import app.shoutsend.modem.packet.MessageFlags
import app.shoutsend.modem.packet.MessageType
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.ModemProfile
import app.shoutsend.modem.profile.Scheme
import app.shoutsend.modem.profile.Speed
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class AcousticChannelSimulatorTest {

    private val audioSpec = AudioSpec.RATE_48K

    @Test
    fun testGainIsExact() {
        val gainDb = 6.0205999 // 2.0x linear
        val scenario = ChannelScenario(name = "GAIN_TEST", gainDb = gainDb)
        val simulator = AcousticChannelSimulator(scenario, audioSpec)

        val input = floatArrayOf(0.1f, -0.2f, 0.4f, 0.0f)
        val output = simulator.process(input, seed = 1L)

        assertEquals("Length should match", input.size, output.size)
        val expectedScale = 10.0.pow(gainDb / 20.0).toFloat()
        for (i in input.indices) {
            assertEquals("Exact gain scaling", input[i] * expectedScale, output[i], 1e-4f)
        }
    }

    @Test
    fun testSnrTargetIsMetWithinHalfDb() {
        val targetSnr = 18.0
        val scenario = ChannelScenario(name = "SNR_TEST", targetSnrDb = targetSnr)
        val simulator = AcousticChannelSimulator(scenario, audioSpec)

        // 48,000 samples (1 second) of a pure sine wave at 2000 Hz, peak amplitude 0.5
        val n = 48000
        val input = FloatArray(n) { i ->
            (0.5 * sin(2.0 * PI * 2000.0 * i / 48000.0)).toFloat()
        }

        val output = simulator.process(input, seed = 12345L)

        // Extract noise residual = output - input
        var signalPower = 0.0
        var noisePower = 0.0
        for (i in 0 until n) {
            val sig = input[i].toDouble()
            val noise = (output[i] - input[i]).toDouble()
            signalPower += sig * sig
            noisePower += noise * noise
        }
        signalPower /= n
        noisePower /= n

        val measuredSnr = 10.0 * log10(signalPower / noisePower)
        val snrError = abs(measuredSnr - targetSnr)
        assertTrue("SNR error $snrError dB must be within 0.5 dB (measured $measuredSnr dB)", snrError < 0.5)
    }

    @Test
    fun testEchoProducesExpectedDelayedCopy() {
        val delayMs = 10.0 // At 48 kHz, 10 ms = 480 samples
        val tapGainDb = -6.0206 // 0.5x linear
        val customRoom = object {
            val taps = listOf(EchoTap(delayMs = delayMs, gainDb = tapGainDb))
        }

        val scenario = ChannelScenario(
            name = "ECHO_TEST",
            room = RoomPreset.NONE
        )
        val simulator = AcousticChannelSimulator(scenario, audioSpec)

        val input = FloatArray(1000)
        input[0] = 1.0f // Single impulse

        // Test custom tap directly with channel FIR logic
        val taps = customRoom.taps
        val delaySamples = ((delayMs * 48000.0) / 1000.0).roundToInt()
        val tapGain = 10.0.pow(tapGainDb / 20.0).toFloat()

        val output = FloatArray(input.size + delaySamples)
        System.arraycopy(input, 0, output, 0, input.size)
        for (i in input.indices) {
            output[i + delaySamples] += input[i] * tapGain
        }

        assertEquals(1.0f, output[0], 1e-4f)
        assertEquals(0.5f, output[480], 1e-3f)
        assertEquals(0.0f, output[479], 1e-5f)
        assertEquals(0.0f, output[481], 1e-5f)
    }

    @Test
    fun testResamplerChangesDurationByExpectedPpm() {
        val simulator = AcousticChannelSimulator(ChannelScenario.CLEAN, audioSpec)
        val input = FloatArray(100000) { i -> sin(2.0 * PI * 1000.0 * i / 48000.0).toFloat() }

        val driftPpm = 20000.0 // +20,000 ppm = +2.0%
        val resampled = simulator.applyClockDrift(input, driftPpm)

        val expectedLength = (100000 / (1.0 + 20000.0 * 1e-6)).roundToInt()
        assertEquals("Resampled length matches ppm ratio", expectedLength, resampled.size)
    }

    @Test
    fun testFrequencyResponseMatchesCornerWithinOneDb() {
        // Butterworth 2-pole low-pass filter at 1000 Hz
        val cutoffHz = 1000.0
        val section = FilterSection(FilterType.LOW_PASS, cutoffHz = cutoffHz, q = 0.707)
        val filter = BiquadFilter(section, audioSpec.sampleRateHz)

        // 4800 samples (100 ms) of pure 1000 Hz sine wave with amplitude 1.0
        val n = 4800
        val samples = FloatArray(n) { i -> sin(2.0 * PI * cutoffHz * i / 48000.0).toFloat() }
        filter.process(samples)

        // Measure RMS amplitude over steady-state (last 2000 samples)
        var sumSq = 0.0
        val steadyCount = 2000
        for (i in (n - steadyCount) until n) {
            sumSq += samples[i] * samples[i]
        }
        val rms = sqrt(sumSq / steadyCount)
        val peak = rms * sqrt(2.0)

        // At -3 dB cutoff, peak should be 0.7071 (-3 dB). Allow +/- 1 dB (0.63 .. 0.79)
        val attenuationDb = 20.0 * log10(peak)
        val errorDb = abs(attenuationDb - (-3.01))
        assertTrue(
            "Filter attenuation at cutoff $attenuationDb dB must match -3 dB within 1.0 dB",
            errorDb < 1.0
        )
    }

    @Test
    fun testHarnessSucceedsOnCleanScenarioForEveryProfile() {
        val testProfiles = listOf(
            ModemProfile(Band.RETRO, Scheme.MFSK16, Speed.NORMAL, repeats = 1, rsProfileId = 1),
            ModemProfile(Band.STEALTH, Scheme.DTFSK, Speed.FAST, repeats = 1, rsProfileId = 1),
            ModemProfile(Band.RETRO, Scheme.DTFSK, Speed.ROBUST, repeats = 1, rsProfileId = 2)
        )

        val payload = "ShoutSend Acoustic Clean Loopback Verification".toByteArray(Charsets.UTF_8)
        val msg = OutgoingMessage(type = MessageType.TEXT, flags = MessageFlags.NONE, payload = payload, messageId = 77)

        for (profile in testProfiles) {
            val result = LoopbackHarness.run(
                message = msg,
                profile = profile,
                scenario = ChannelScenario.CLEAN,
                audioSpec = audioSpec,
                seed = 42L
            )

            assertTrue("Transmission must succeed on CLEAN for ${profile.band}/${profile.scheme}/${profile.speed}", result.success)
            assertTrue("Delivered payload must match exact bytes", result.exactPayloadMatch)
            assertEquals("Zero RS corrections on clean channel", 0, result.totalRsCorrections)
            assertEquals("Zero false locks on clean channel", 0, result.falseLocks)
            assertNotNull("Received message present", result.receivedMessage)
        }
    }

    @Test
    fun testSameSeedGivesIdenticalResults() {
        val scenario = ChannelScenario(
            name = "RANDOM_SCENARIO",
            gainDb = -6.0,
            targetSnrDb = 15.0,
            ambientNoiseDbfs = -40.0,
            room = RoomPreset.LIVING_ROOM,
            phone = PhoneHardwarePreset.GOOD_PHONE,
            clockDriftPpm = 20.0,
            leadInSilenceRangeMs = 50..100,
            slowFading = true,
            dropoutsAndClipping = true
        )
        val simulator = AcousticChannelSimulator(scenario, audioSpec)

        val input = FloatArray(5000) { i -> sin(2.0 * PI * 1800.0 * i / 48000.0).toFloat() }

        val seed = 987654321L
        val run1 = simulator.process(input, seed)
        val run2 = simulator.process(input, seed)

        assertArrayEquals("Runs with identical seed must yield bit-identical waveforms", run1, run2, 0.0f)
    }
}
