package app.shoutsend.modem.calibration

import app.shoutsend.modem.AudioSpec
import app.shoutsend.modem.detector.GoertzelBank
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.TonePlan
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

/**
 * Result of complete acoustic calibration analysis.
 */
public data class CalibrationAnalysisResult(
    public val retroReport: BandReport,
    public val stealthReport: BandReport,
    public val recommendation: RecommendationResult,
    public val ambientNoiseDbfs: Double
)

/**
 * Pure Kotlin acoustic calibration analysis engine.
 *
 * Implements:
 * 1. Ambient noise floor evaluation prior to test sequence.
 * 2. Independent marker tone detection for Retro (1000 Hz) and Stealth (16500 Hz).
 * 3. Precise Goertzel power measurement of 18 tones per band (16 data + 2 sync).
 * 4. In-band dynamic range variation and SNR qualification against empirical thresholds.
 * 5. Automatic band and speed recommendation with documented reason codes.
 */
public object CalibrationAnalyzer {

    public fun analyze(
        samples: ShortArray,
        audioSpec: AudioSpec = AudioSpec.RATE_48K
    ): CalibrationAnalysisResult {
        val floatSamples = FloatArray(samples.size) { i ->
            val s = samples[i]
            if (s >= 0) s.toFloat() / 32767.0f else s.toFloat() / 32768.0f
        }
        return analyze(floatSamples, audioSpec)
    }

    public fun analyze(
        samples: FloatArray,
        audioSpec: AudioSpec = AudioSpec.RATE_48K
    ): CalibrationAnalysisResult {
        val sampleRate = audioSpec.sampleRateHz

        // 1. Minimum duration sanity check (at least 500 ms required)
        if (samples.size < sampleRate / 2) {
            val emptyReport = createEmptyReport(Band.RETRO, "Audio recording too short (< 0.5s)")
            val emptyStealth = createEmptyReport(Band.STEALTH, "Audio recording too short (< 0.5s)")
            return CalibrationAnalysisResult(
                retroReport = emptyReport,
                stealthReport = emptyStealth,
                recommendation = CalibrationRecommender.recommend(emptyReport, emptyStealth),
                ambientNoiseDbfs = -90.0
            )
        }

        // 2. Measure ambient noise floor (initial 500 ms window or up to first quarter of samples)
        val noiseWindowSize = min((sampleRate / 2), samples.size / 4)
        var sumSquares = 0.0
        for (i in 0 until noiseWindowSize) {
            val s = samples[i].toDouble()
            sumSquares += s * s
        }
        val meanSquare = sumSquares / noiseWindowSize
        val ambientNoiseDbfs = if (meanSquare > 1e-12) {
            (10.0 * log10(meanSquare)).coerceIn(-90.0, 0.0)
        } else -90.0

        // Check for all-digital silence
        var maxAbs = 0.0f
        for (i in samples.indices) {
            val a = kotlin.math.abs(samples[i])
            if (a > maxAbs) maxAbs = a
        }
        if (maxAbs < 1e-5f) {
            val silentRetro = createEmptyReport(Band.RETRO, "Microphone silent / zero signal detected")
            val silentStealth = createEmptyReport(Band.STEALTH, "Microphone silent / zero signal detected")
            return CalibrationAnalysisResult(
                retroReport = silentRetro,
                stealthReport = silentStealth,
                recommendation = CalibrationRecommender.recommend(silentRetro, silentStealth),
                ambientNoiseDbfs = -90.0
            )
        }

        // 3. Analyze Retro and Stealth bands
        val retroReport = analyzeBand(
            band = Band.RETRO,
            markerHz = TestSequence.RETRO_MARKER_HZ,
            samples = samples,
            audioSpec = audioSpec,
            ambientNoiseDbfs = ambientNoiseDbfs,
            searchStartSample = 0
        )

        // For stealth, search starting after retro or from beginning
        val stealthSearchStart = if (retroReport.isUsable) {
            // Retro takes ~3.8 seconds after its marker
            retroReport.toneMeasurements.lastOrNull()?.let {
                // start after last measured retro tone
                (sampleRate * 3.5).toInt()
            } ?: 0
        } else 0

        val stealthReport = analyzeBand(
            band = Band.STEALTH,
            markerHz = TestSequence.STEALTH_MARKER_HZ,
            samples = samples,
            audioSpec = audioSpec,
            ambientNoiseDbfs = ambientNoiseDbfs,
            searchStartSample = stealthSearchStart
        )

        val recommendation = CalibrationRecommender.recommend(retroReport, stealthReport)

        return CalibrationAnalysisResult(
            retroReport = retroReport,
            stealthReport = stealthReport,
            recommendation = recommendation,
            ambientNoiseDbfs = ambientNoiseDbfs
        )
    }

    private fun analyzeBand(
        band: Band,
        markerHz: Double,
        samples: FloatArray,
        audioSpec: AudioSpec,
        ambientNoiseDbfs: Double,
        searchStartSample: Int
    ): BandReport {
        val sampleRate = audioSpec.sampleRateHz
        val plan = TonePlan(band, audioSpec)

        // 18 test tones: 16 data tones + sync0 + sync1
        val toneFrequencies = ArrayList<Double>(18)
        toneFrequencies.addAll(plan.dataTones)
        toneFrequencies.add(plan.syncTone0)
        toneFrequencies.add(plan.syncTone1)

        // Detect marker tone onset
        val markerOnset = findMarkerOnset(
            markerHz = markerHz,
            samples = samples,
            audioSpec = audioSpec,
            ambientNoiseDbfs = ambientNoiseDbfs,
            startSample = searchStartSample
        )

        if (markerOnset < 0) {
            return createEmptyReport(band, "Marker tone ($markerHz Hz) not detected")
        }

        // Nominal timing:
        // Marker length = 200 ms, inter-tone silence = 50 ms.
        // Tone start = markerOnset + 250 ms.
        val markerDurationSamples = (sampleRate * TestSequence.MARKER_DURATION_MS) / 1000
        val interToneSilenceSamples = (sampleRate * TestSequence.INTER_TONE_SILENCE_MS) / 1000
        val toneDurationSamples = (sampleRate * TestSequence.TONE_DURATION_MS) / 1000
        val sequenceStartSample = markerOnset + markerDurationSamples + interToneSilenceSamples

        // Measure middle 80 ms (safe steady state) of each 150 ms tone
        val measurementWindowSamples = (sampleRate * 80) / 1000
        val measurementOffsetInsideTone = (sampleRate * 35) / 1000

        val goertzelBank = GoertzelBank(
            frequencies = toneFrequencies.toDoubleArray(),
            audioSpec = audioSpec,
            blockSize = measurementWindowSamples,
            windowed = true
        )

        val energyScratch = FloatArray(18)
        val measurements = ArrayList<ToneMeasurement>(18)

        for (toneIdx in 0 until 18) {
            val freq = toneFrequencies[toneIdx]
            val toneStart = sequenceStartSample + toneIdx * (toneDurationSamples + interToneSilenceSamples)
            val windowStart = toneStart + measurementOffsetInsideTone

            if (windowStart + measurementWindowSamples > samples.size) {
                // Audio stream cut off prematurely
                measurements.add(
                    ToneMeasurement(
                        index = toneIdx,
                        frequencyHz = freq,
                        levelDbfs = -90.0,
                        relativeLevelDb = -90.0,
                        snrDb = 0.0
                    )
                )
                continue
            }

            goertzelBank.energies(samples, windowStart, energyScratch)
            val power = energyScratch[toneIdx].toDouble()
            val levelDbfs = if (power > 1e-12) {
                (10.0 * log10(power)).coerceIn(-90.0, 0.0)
            } else -90.0

            val snrDb = max(0.0, levelDbfs - ambientNoiseDbfs)

            measurements.add(
                ToneMeasurement(
                    index = toneIdx,
                    frequencyHz = freq,
                    levelDbfs = levelDbfs,
                    relativeLevelDb = 0.0, // calculated below relative to strongest
                    snrDb = snrDb
                )
            )
        }

        // Find strongest and weakest
        var maxLevel = -999.0
        var minLevel = 999.0
        var strongestIdx = 0
        var weakestIdx = 0

        for (i in measurements.indices) {
            val lvl = measurements[i].levelDbfs
            if (lvl > maxLevel) {
                maxLevel = lvl
                strongestIdx = i
            }
            if (lvl < minLevel) {
                minLevel = lvl
                weakestIdx = i
            }
        }

        // Recompute relative levels
        val finalizedMeasurements = measurements.map { m ->
            m.copy(relativeLevelDb = m.levelDbfs - maxLevel)
        }

        val strongest = finalizedMeasurements[strongestIdx]
        val weakest = finalizedMeasurements[weakestIdx]
        val inBandVariationDb = maxLevel - minLevel
        val minSnrDb = weakest.snrDb

        // Evaluate verdict
        val (verdict, reason) = when {
            maxLevel < CalibrationThresholds.MIN_ABSOLUTE_LEVEL_DBFS -> {
                BandVerdict.UNUSABLE to "Signal level too weak (${maxLevel.toInt()} dBFS < -65 dBFS)"
            }
            minSnrDb >= CalibrationThresholds.GOOD_MIN_SNR_DB && inBandVariationDb <= CalibrationThresholds.GOOD_MAX_VARIATION_DB -> {
                BandVerdict.GOOD to "Optimal SNR (${minSnrDb.toInt()} dB) and uniform response (${inBandVariationDb.toInt()} dB)"
            }
            minSnrDb >= CalibrationThresholds.USABLE_MIN_SNR_DB && inBandVariationDb <= CalibrationThresholds.USABLE_MAX_VARIATION_DB -> {
                BandVerdict.USABLE to "Adequate SNR (${minSnrDb.toInt()} dB) across all 18 tones"
            }
            minSnrDb >= CalibrationThresholds.POOR_MIN_SNR_DB && inBandVariationDb <= CalibrationThresholds.POOR_MAX_VARIATION_DB -> {
                BandVerdict.POOR to "Marginal SNR (${minSnrDb.toInt()} dB) or high in-band roll-off (${inBandVariationDb.toInt()} dB)"
            }
            else -> {
                BandVerdict.UNUSABLE to "Unacceptable in-band attenuation (${inBandVariationDb.toInt()} dB) or low SNR (${minSnrDb.toInt()} dB)"
            }
        }

        return BandReport(
            band = band,
            toneMeasurements = finalizedMeasurements,
            strongestTone = strongest,
            weakestTone = weakest,
            inBandVariationDb = inBandVariationDb,
            noiseFloorDbfs = ambientNoiseDbfs,
            verdict = verdict,
            verdictReason = reason
        )
    }

    private fun findMarkerOnset(
        markerHz: Double,
        samples: FloatArray,
        audioSpec: AudioSpec,
        ambientNoiseDbfs: Double,
        startSample: Int
    ): Int {
        val sampleRate = audioSpec.sampleRateHz
        val hopSize = (sampleRate * 10) / 1000 // 10 ms hop
        val blockSize = (sampleRate * 20) / 1000 // 20 ms window

        if (startSample + blockSize > samples.size) return -1

        val markerDetector = GoertzelBank(
            frequencies = doubleArrayOf(markerHz),
            audioSpec = audioSpec,
            blockSize = blockSize,
            windowed = true
        )

        val outScratch = FloatArray(1)
        val detectionThresholdDb = max(ambientNoiseDbfs + CalibrationThresholds.MARKER_DETECTION_MARGIN_DB, -60.0)

        var cursor = max(0, startSample)
        var consecutiveDetections = 0
        var firstDetectionSample = -1

        while (cursor + blockSize <= samples.size) {
            markerDetector.energies(samples, cursor, outScratch)
            val power = outScratch[0].toDouble()
            val levelDbfs = if (power > 1e-12) 10.0 * log10(power) else -90.0

            if (levelDbfs >= detectionThresholdDb) {
                if (consecutiveDetections == 0) {
                    firstDetectionSample = cursor
                }
                consecutiveDetections++
                // Sustained for at least 50 ms (5 hops of 10 ms)
                if (consecutiveDetections >= 5) {
                    return firstDetectionSample
                }
            } else {
                consecutiveDetections = 0
                firstDetectionSample = -1
            }

            cursor += hopSize
        }

        return -1
    }

    private fun createEmptyReport(band: Band, reason: String): BandReport {
        val plan = TonePlan(band, AudioSpec.RATE_48K)
        val dummyMeasurements = (plan.dataTones + listOf(plan.syncTone0, plan.syncTone1)).mapIndexed { i, f ->
            ToneMeasurement(
                index = i,
                frequencyHz = f,
                levelDbfs = -90.0,
                relativeLevelDb = 0.0,
                snrDb = 0.0
            )
        }
        val dummy = dummyMeasurements[0]
        return BandReport(
            band = band,
            toneMeasurements = dummyMeasurements,
            strongestTone = dummy,
            weakestTone = dummy,
            inBandVariationDb = 0.0,
            noiseFloorDbfs = -90.0,
            verdict = BandVerdict.UNUSABLE,
            verdictReason = reason
        )
    }
}
