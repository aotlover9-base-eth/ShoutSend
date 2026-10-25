package app.shoutsend.modem.calibration

import app.shoutsend.modem.AudioSpec
import app.shoutsend.modem.channel.AcousticChannelSimulator
import app.shoutsend.modem.channel.ChannelScenario
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.Speed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CalibrationTest {

    private val audioSpec = AudioSpec.RATE_48K

    @Test
    fun testTestSequenceRendersContinuousSignalWithoutNanOrZero() {
        val sequence = TestSequence(audioSpec)
        val totalSamples = sequence.totalSamples.toInt()
        assertTrue("Sequence duration must be between 8s and 12s", sequence.durationSeconds in 8.0..12.0)

        val buffer = FloatArray(totalSamples)
        sequence.render(0L, buffer, 0, totalSamples)

        // Verify no NaNs or Infinities
        for (i in 0 until totalSamples) {
            val s = buffer[i]
            assertTrue("Sample must be finite", s.isFinite())
            assertTrue("Sample must be in [-1.0, 1.0]", s in -1.0f..1.0f)
        }

        // Verify there is initial silence and non-zero audio content
        val initialSilenceSamples = (audioSpec.sampleRateHz * (TestSequence.INITIAL_SILENCE_MS - 50)) / 1000
        var silenceEnergy = 0.0f
        for (i in 0 until initialSilenceSamples) {
            silenceEnergy += kotlin.math.abs(buffer[i])
        }
        assertEquals(0.0f, silenceEnergy, 1e-6f)

        // Non-zero content in tone regions
        var activeEnergy = 0.0f
        for (i in (audioSpec.sampleRateHz * 1.5).toInt() until (audioSpec.sampleRateHz * 3.0).toInt()) {
            activeEnergy += kotlin.math.abs(buffer[i])
        }
        assertTrue("Active region must contain energy", activeEnergy > 100.0f)
    }

    @Test
    fun testAnalyzerDetectsCleanSignalAndRecommendsStealth() {
        val sequence = TestSequence(audioSpec, amplitude = 0.8f)
        val totalSamples = sequence.totalSamples.toInt()
        val pcm = FloatArray(totalSamples)
        sequence.render(0L, pcm, 0, totalSamples)

        val result = CalibrationAnalyzer.analyze(pcm, audioSpec)

        // Retro band check
        assertEquals(BandVerdict.GOOD, result.retroReport.verdict)
        assertTrue("Retro variation must be < 3 dB in clean synthesis", result.retroReport.inBandVariationDb < 3.0)
        assertEquals(18, result.retroReport.toneMeasurements.size)

        // Stealth band check
        assertEquals(BandVerdict.GOOD, result.stealthReport.verdict)
        assertTrue("Stealth variation must be < 3 dB in clean synthesis", result.stealthReport.inBandVariationDb < 3.0)
        assertEquals(18, result.stealthReport.toneMeasurements.size)

        // Recommendation
        assertEquals(Band.STEALTH, result.recommendation.recommendedBand)
        assertEquals(Speed.NORMAL, result.recommendation.recommendedSpeed)
        assertEquals(RecommendationReason.STEALTH_GOOD, result.recommendation.reason)
    }

    @Test
    fun testAnalyzerWithSimulatedWeakUltrasoundPhoneFromStep22() {
        val sequence = TestSequence(audioSpec, amplitude = 0.8f)
        val totalSamples = sequence.totalSamples.toInt()
        val pcm = FloatArray(totalSamples)
        sequence.render(0L, pcm, 0, totalSamples)

        // Impair using WEAK_ULTRASOUND_PHONE scenario (biquad roll-off cutting >16 kHz)
        val simulator = AcousticChannelSimulator(
            scenario = ChannelScenario.WEAK_ULTRASOUND_PHONE,
            audioSpec = audioSpec
        )
        val impairedPcm = simulator.process(pcm, seed = 42L)

        val result = CalibrationAnalyzer.analyze(impairedPcm, audioSpec)

        // Retro should remain GOOD or USABLE
        assertTrue("Retro must remain usable", result.retroReport.isUsable)

        // Stealth must suffer significant roll-off (> 18 dB attenuation)
        assertTrue("Stealth in-band variation must exceed 12 dB due to roll-off", result.stealthReport.inBandVariationDb > 12.0)
        assertTrue(
            "Stealth must not be GOOD on weak ultrasound hardware",
            result.stealthReport.verdict == BandVerdict.POOR || result.stealthReport.verdict == BandVerdict.UNUSABLE
        )

        // Recommends RETRO
        assertEquals(Band.RETRO, result.recommendation.recommendedBand)
    }

    @Test
    fun testMarkerDetectionWithRandomStartDelays() {
        val sequence = TestSequence(audioSpec, amplitude = 0.8f)
        val seqSamples = sequence.totalSamples.toInt()
        val pcm = FloatArray(seqSamples)
        sequence.render(0L, pcm, 0, seqSamples)

        val randomDelaysMs = intArrayOf(200, 650, 1200)
        for (delayMs in randomDelaysMs) {
            val delaySamples = (audioSpec.sampleRateHz * delayMs) / 1000
            val delayedAudio = FloatArray(delaySamples + seqSamples)
            System.arraycopy(pcm, 0, delayedAudio, delaySamples, seqSamples)

            val result = CalibrationAnalyzer.analyze(delayedAudio, audioSpec)
            assertEquals("Retro must be detected despite $delayMs ms delay", BandVerdict.GOOD, result.retroReport.verdict)
            assertEquals("Stealth must be detected despite $delayMs ms delay", BandVerdict.GOOD, result.stealthReport.verdict)
        }
    }

    @Test
    fun testAllSilentRecordingGivesUnusableWithClearReason() {
        val silentAudio = FloatArray(audioSpec.sampleRateHz * 3) { 0.0f }
        val result = CalibrationAnalyzer.analyze(silentAudio, audioSpec)

        assertEquals(BandVerdict.UNUSABLE, result.retroReport.verdict)
        assertEquals(BandVerdict.UNUSABLE, result.stealthReport.verdict)
        assertTrue("Reason indicates silence", result.retroReport.verdictReason.contains("silent", ignoreCase = true))
        assertEquals(Band.RETRO, result.recommendation.recommendedBand)
        assertEquals(Speed.ROBUST, result.recommendation.recommendedSpeed)
    }

    @Test
    fun testRecommendationTruthTable() {
        fun makeReport(band: Band, verdict: BandVerdict, noiseDbfs: Double = -60.0): BandReport {
            val tone = ToneMeasurement(0, 1000.0, -20.0, 0.0, 30.0)
            return BandReport(
                band = band,
                toneMeasurements = listOf(tone),
                strongestTone = tone,
                weakestTone = tone,
                inBandVariationDb = 5.0,
                noiseFloorDbfs = noiseDbfs,
                verdict = verdict,
                verdictReason = "Test"
            )
        }

        // 1. Stealth GOOD -> Stealth Normal
        val r1 = CalibrationRecommender.recommend(makeReport(Band.RETRO, BandVerdict.GOOD), makeReport(Band.STEALTH, BandVerdict.GOOD))
        assertEquals(Band.STEALTH, r1.recommendedBand)
        assertEquals(Speed.NORMAL, r1.recommendedSpeed)
        assertEquals(RecommendationReason.STEALTH_GOOD, r1.reason)

        // 2. Stealth USABLE, Retro GOOD -> Retro Fast
        val r2 = CalibrationRecommender.recommend(makeReport(Band.RETRO, BandVerdict.GOOD), makeReport(Band.STEALTH, BandVerdict.USABLE))
        assertEquals(Band.RETRO, r2.recommendedBand)
        assertEquals(Speed.FAST, r2.recommendedSpeed)
        assertEquals(RecommendationReason.RETRO_GOOD_FAST, r2.reason)

        // 3. Stealth POOR, Retro USABLE -> Retro Normal
        val r3 = CalibrationRecommender.recommend(makeReport(Band.RETRO, BandVerdict.USABLE), makeReport(Band.STEALTH, BandVerdict.POOR))
        assertEquals(Band.RETRO, r3.recommendedBand)
        assertEquals(Speed.NORMAL, r3.recommendedSpeed)
        assertEquals(RecommendationReason.RETRO_USABLE_NORMAL, r3.reason)

        // 4. Stealth UNUSABLE, Retro POOR -> Retro Robust
        val r4 = CalibrationRecommender.recommend(makeReport(Band.RETRO, BandVerdict.POOR), makeReport(Band.STEALTH, BandVerdict.UNUSABLE))
        assertEquals(Band.RETRO, r4.recommendedBand)
        assertEquals(Speed.ROBUST, r4.recommendedSpeed)
        assertEquals(RecommendationReason.RETRO_POOR_ROBUST, r4.reason)

        // 5. Both UNUSABLE with high ambient noise
        val r5 = CalibrationRecommender.recommend(makeReport(Band.RETRO, BandVerdict.UNUSABLE, noiseDbfs = -25.0), makeReport(Band.STEALTH, BandVerdict.UNUSABLE))
        assertEquals(RecommendationReason.ALL_UNUSABLE_HIGH_NOISE, r5.reason)
    }

    @Test
    fun testLinkQualityScore() {
        // Flawless reception
        val q1 = LinkTest.calculateQuality(snrDb = 25.0, correctedBytes = 0, totalBytes = 100, lostFrames = 0, totalFrames = 2)
        assertTrue("Flawless score must be >= 90", q1.score >= 90)
        assertEquals(LinkRating.EXCELLENT, q1.rating)

        // Moderate noise with few RS corrections
        val q2 = LinkTest.calculateQuality(snrDb = 16.0, correctedBytes = 4, totalBytes = 100, lostFrames = 0, totalFrames = 2)
        assertTrue("Good score must be between 60 and 85", q2.score in 60..85)
        assertEquals(LinkRating.GOOD, q2.rating)

        // Marginal reception
        val q3 = LinkTest.calculateQuality(snrDb = 9.0, correctedBytes = 15, totalBytes = 100, lostFrames = 0, totalFrames = 2)
        assertTrue("Fair score must be between 30 and 60", q3.score in 30..60)

        // 100% loss
        val q4 = LinkTest.calculateQuality(snrDb = 0.0, correctedBytes = 0, totalBytes = 100, lostFrames = 2, totalFrames = 2)
        assertEquals(0, q4.score)
        assertEquals(LinkRating.POOR, q4.rating)
    }
}
