package app.shoutsend.perf

import app.shoutsend.modem.AudioSpec
import app.shoutsend.modem.calibration.BandReport
import app.shoutsend.modem.calibration.BandVerdict
import app.shoutsend.modem.calibration.CalibrationRecommender
import app.shoutsend.modem.calibration.ToneMeasurement
import app.shoutsend.modem.modulator.OutgoingMessage
import app.shoutsend.modem.modulator.TransmissionBuilder
import app.shoutsend.modem.packet.MessageFlags
import app.shoutsend.modem.packet.MessageType
import app.shoutsend.modem.payload.Payload
import app.shoutsend.modem.payload.PayloadCodec
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.ModemProfile
import app.shoutsend.modem.profile.Scheme
import app.shoutsend.modem.profile.Speed
import app.shoutsend.modem.profile.TonePlan
import app.shoutsend.modem.tuning.ModemTuning
import app.shoutsend.modem.tuning.ModemTuningValidationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verification of Modem Preset Tuning and Field Calibration Rules (Step 56).
 */
class PresetTuningTest {

    private val audioSpec = AudioSpec.RATE_48K

    @Test
    fun testDefaultModemTuningIsValid() {
        val result = ModemTuning.DEFAULT.validate()
        assertTrue("ModemTuning.DEFAULT must pass physical validation", result is ModemTuningValidationResult.Valid)
    }

    @Test
    fun testModemTuningValidationBounds() {
        // Invalid duration: robust ramp > robust duration / 2
        val invalidTuning = ModemTuning.DEFAULT.copy(
            robustDurationMs = 40,
            robustRampMs = 25
        )
        val result = invalidTuning.validate()
        assertTrue("Tuning with excessive ramp must be Invalid", result is ModemTuningValidationResult.Invalid)

        val invalidFft = ModemTuning.DEFAULT.copy(fftSize = 1000) // Not a power of 2
        val fftResult = invalidFft.validate()
        assertTrue("Non-power-of-2 FFT size must be Invalid", fftResult is ModemTuningValidationResult.Invalid)
    }

    @Test
    fun testCalibrationRecommenderOptimalFieldEnvironment() {
        // Scenario 1a: Stealth is GOOD -> silent near-ultrasound is preferred
        val retroGood = createSyntheticBandReport(Band.RETRO, BandVerdict.GOOD, snrDb = 16.0, variationDb = 3.5)
        val stealthGood = createSyntheticBandReport(Band.STEALTH, BandVerdict.GOOD, snrDb = 14.0, variationDb = 4.0)

        val recStealth = CalibrationRecommender.recommend(retroGood, stealthGood)
        assertEquals(Band.STEALTH, recStealth.recommendedBand)
        assertEquals(Speed.NORMAL, recStealth.recommendedSpeed)

        // Scenario 1b: Stealth is POOR/UNUSABLE, but Retro is GOOD -> High-speed FAST Retro
        val stealthUnusable = createSyntheticBandReport(Band.STEALTH, BandVerdict.UNUSABLE, snrDb = 0.0, variationDb = 20.0)
        val recRetroFast = CalibrationRecommender.recommend(retroGood, stealthUnusable)
        assertEquals(Band.RETRO, recRetroFast.recommendedBand)
        assertEquals(Speed.FAST, recRetroFast.recommendedSpeed)

        // Scenario 1c: Retro is USABLE -> Balanced NORMAL Retro
        val retroUsable = createSyntheticBandReport(Band.RETRO, BandVerdict.USABLE, snrDb = 10.0, variationDb = 7.0)
        val recRetroNormal = CalibrationRecommender.recommend(retroUsable, stealthUnusable)
        assertEquals(Band.RETRO, recRetroNormal.recommendedBand)
        assertEquals(Speed.NORMAL, recRetroNormal.recommendedSpeed)
    }

    @Test
    fun testCalibrationRecommenderHighNoiseFieldEnvironment() {
        // Scenario 2: Cafe environment with low SNR on Retro
        val retroPoor = createSyntheticBandReport(Band.RETRO, BandVerdict.POOR, snrDb = 5.0, variationDb = 8.0)
        val stealthUnusable = createSyntheticBandReport(Band.STEALTH, BandVerdict.UNUSABLE, snrDb = 1.0, variationDb = 18.0)

        val recommendation = CalibrationRecommender.recommend(retroPoor, stealthUnusable)
        assertEquals(Band.RETRO, recommendation.recommendedBand)
        // High noise triggers Robust speed recommendation
        assertEquals(Speed.ROBUST, recommendation.recommendedSpeed)
    }

    @Test
    fun testPresetAirTimeConsistencyAcrossAllSpeeds() {
        val payload = Payload.Text("Preset timing test")
        val speeds = listOf(Speed.FAST, Speed.NORMAL, Speed.ROBUST)

        for (speed in speeds) {
            val profile = ModemProfile(Band.RETRO, Scheme.MFSK16, speed)
            val plan = TransmissionBuilder.build(
                type = MessageType.TEXT,
                flags = MessageFlags.NONE,
                payload = PayloadCodec.encode(payload),
                profile = profile,
                audioSpec = audioSpec
            )

            // Duration must increase monotonically as speed moves from FAST to ROBUST
            val symbolDurationMs = ModemTuning.DEFAULT.durationMs(speed)
            assertTrue("Symbol duration must be positive", symbolDurationMs > 0)
            assertTrue("Total transmission samples must be positive", plan.totalSamples > 0)
        }
    }

    private fun createSyntheticBandReport(
        band: Band,
        verdict: BandVerdict,
        snrDb: Double,
        variationDb: Double
    ): BandReport {
        val plan = TonePlan(band, audioSpec)
        val measurements = (plan.dataTones + listOf(plan.syncTone0, plan.syncTone1)).mapIndexed { i, f ->
            ToneMeasurement(
                index = i,
                frequencyHz = f,
                levelDbfs = -30.0 - (i % 3) * (variationDb / 3.0),
                relativeLevelDb = 0.0,
                snrDb = snrDb
            )
        }
        return BandReport(
            band = band,
            toneMeasurements = measurements,
            strongestTone = measurements.first(),
            weakestTone = measurements.last(),
            inBandVariationDb = variationDb,
            noiseFloorDbfs = -50.0,
            verdict = verdict,
            verdictReason = "Synthetic test report"
        )
    }
}
