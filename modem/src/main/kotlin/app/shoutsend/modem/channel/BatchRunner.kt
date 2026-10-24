package app.shoutsend.modem.channel

import app.shoutsend.modem.AudioSpec
import app.shoutsend.modem.modulator.OutgoingMessage
import app.shoutsend.modem.packet.MessageFlags
import app.shoutsend.modem.packet.MessageType
import app.shoutsend.modem.payload.Payload
import app.shoutsend.modem.payload.PayloadCodec
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.ModemProfile
import app.shoutsend.modem.profile.Scheme
import app.shoutsend.modem.profile.Speed
import java.io.File
import kotlin.math.roundToInt

/**
 * Aggregated trial metrics for a specific (Profile, Scenario) evaluation cell.
 */
public data class CellBenchmarkResult(
    public val profile: ModemProfile,
    public val scenario: ChannelScenario,
    public val trials: Int,
    public val successes: Int,
    public val successRatePercent: Double,
    public val meanDurationSeconds: Double,
    public val p95DurationSeconds: Double,
    public val meanThroughputBytesPerSec: Double,
    public val meanRsCorrections: Double
)

/**
 * Automated batch runner executing multi-scenario channel simulation benchmarks.
 */
public object BatchRunner {

    /**
     * Executes [trialCount] trials for each (profile, scenario) pair across consecutive seeds.
     */
    public fun runBenchmark(
        profiles: List<ModemProfile>,
        scenarios: List<ChannelScenario>,
        samplePayload: ByteArray,
        trialsPerCell: Int = 20,
        baseSeed: Long = 1000L
    ): List<CellBenchmarkResult> {
        val results = ArrayList<CellBenchmarkResult>()

        for (profile in profiles) {
            for (scenario in scenarios) {
                var successes = 0
                val durations = ArrayList<Double>(trialsPerCell)
                var totalThroughput = 0.0
                var totalCorrections = 0

                for (t in 0 until trialsPerCell) {
                    val seed = baseSeed + t
                    val msg = OutgoingMessage(
                        type = MessageType.TEXT,
                        flags = MessageFlags.NONE,
                        payload = samplePayload,
                        messageId = (t % 250) + 1
                    )
                    val result = LoopbackHarness.run(
                        message = msg,
                        profile = profile,
                        scenario = scenario,
                        audioSpec = AudioSpec.RATE_48K,
                        seed = seed
                    )

                    if (result.success && result.exactPayloadMatch) {
                        successes++
                    }
                    durations.add(result.durationSeconds)
                    val throughput = if (result.success && result.durationSeconds > 0) {
                        samplePayload.size / result.durationSeconds
                    } else 0.0
                    totalThroughput += throughput
                    totalCorrections += result.totalRsCorrections
                }

                durations.sort()
                val meanDuration = durations.average()
                val p95Idx = ((trialsPerCell - 1) * 0.95).roundToInt()
                val p95Duration = durations[p95Idx]
                val successRate = (successes.toDouble() / trialsPerCell) * 100.0
                val meanThroughput = totalThroughput / trialsPerCell
                val meanCorrections = totalCorrections.toDouble() / trialsPerCell

                results.add(
                    CellBenchmarkResult(
                        profile = profile,
                        scenario = scenario,
                        trials = trialsPerCell,
                        successes = successes,
                        successRatePercent = successRate,
                        meanDurationSeconds = meanDuration,
                        p95DurationSeconds = p95Duration,
                        meanThroughputBytesPerSec = meanThroughput,
                        meanRsCorrections = meanCorrections
                    )
                )
            }
        }

        return results
    }

    /**
     * Formats [results] into a structured GitHub Flavored Markdown table.
     */
    public fun formatMarkdown(results: List<CellBenchmarkResult>): String {
        val sb = StringBuilder()
        sb.append("# ShoutSend DSP Channel Benchmark Report\n\n")
        sb.append("> Note: These results are deterministic channel simulations modeling acoustic room responses, noise, and hardware frequency shaping.\n\n")
        sb.append("| Profile (Band / Scheme / Speed / RS) | Scenario | Trials | Success Rate | Mean Duration | P95 Duration | Net Throughput | Mean RS Corr |\n")
        sb.append("|---|---|---|---|---|---|---|---|\n")

        for (r in results) {
            val prof = "${r.profile.band.name} / ${r.profile.scheme.name} / ${r.profile.speed.name} / RS${r.profile.rsProfileId}"
            val rate = "%.1f%%".format(r.successRatePercent)
            val meanDur = "%.2fs".format(r.meanDurationSeconds)
            val p95Dur = "%.2fs".format(r.p95DurationSeconds)
            val throughput = "%.1f B/s".format(r.meanThroughputBytesPerSec)
            val corr = "%.1f".format(r.meanRsCorrections)
            sb.append("| $prof | ${r.scenario.name} | ${r.trials} | $rate | $meanDur | $p95Dur | $throughput | $corr |\n")
        }

        return sb.toString()
    }

    /**
     * Entrypoint for Gradle `:modem:modemBenchmark` task.
     */
    @JvmStatic
    public fun main(args: Array<String>) {
        println("Starting ShoutSend Modem Simulation Benchmark...")
        val profiles = listOf(
            ModemProfile(Band.RETRO, Scheme.MFSK16, Speed.NORMAL, repeats = 1, rsProfileId = 1),
            ModemProfile(Band.STEALTH, Scheme.DTFSK, Speed.FAST, repeats = 1, rsProfileId = 1),
            ModemProfile(Band.RETRO, Scheme.DTFSK, Speed.ROBUST, repeats = 2, rsProfileId = 2)
        )
        val scenarios = listOf(
            ChannelScenario.CLEAN,
            ChannelScenario.QUIET_ROOM,
            ChannelScenario.LIVING_ROOM,
            ChannelScenario.CLOSE_RANGE,
            ChannelScenario.CAFE
        )

        val payload = PayloadCodec.encode(Payload.Text("ShoutSend Acoustic Channel Benchmark Payload"))
        val results = runBenchmark(profiles, scenarios, payload, trialsPerCell = 10, baseSeed = 4200L)
        val markdown = formatMarkdown(results)

        println("\nBenchmark Completed:\n")
        println(markdown)

        val outFile = File("docs/DSP_BENCHMARK.md")
        outFile.parentFile?.mkdirs()
        outFile.writeText(markdown)
        println("Report successfully written to ${outFile.absolutePath}")
    }
}
