package app.shoutsend.core.devtools

import app.shoutsend.modem.profile.ModemProfile
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * Acoustic environment descriptor for field test logging.
 */
enum class FieldEnvironment(val label: String) {
    QUIET_ROOM("Quiet Room (< 35 dBA)"),
    LIVING_ROOM("Living Room / Office (45-55 dBA)"),
    CAFE("Busy Cafe (65-75 dBA)"),
    OUTDOOR("Outdoor Transit (70-80 dBA)")
}

/**
 * Handset relative orientation during field acoustic test.
 */
enum class FieldOrientation(val label: String) {
    DESK_FLAT("Desk Flat"),
    LINE_OF_SIGHT("Direct Line of Sight"),
    ANGLED_45("Angled (45 deg)"),
    OBSTRUCTED_HAND("Obstructed / Hand")
}

/**
 * An individual acoustic transmission/reception field test trial.
 * Conforms to Rule 25: Stores purely acoustic link telemetry and metadata;
 * payload contents, SSIDs, and passwords are never recorded.
 */
data class FieldTestRecord(
    val runId: Int,
    val timestampEpochMs: Long,
    val profileName: String,
    val environment: FieldEnvironment,
    val distanceMeters: Float,
    val orientation: FieldOrientation,
    val snrDb: Double,
    val ambientDbfs: Double,
    val isSuccess: Boolean,
    val errorReason: String? = null,
    val correctedBytes: Int = 0,
    val airTimeMs: Long = 0L
)

/**
 * Aggregator statistics for a set of field test records.
 */
data class FieldTestStats(
    val totalRuns: Int,
    val successCount: Int,
    val successRate: Float,
    val averageSnrDb: Double,
    val maxSnrDb: Double,
    val minSnrDb: Double,
    val totalErrorsRepaired: Int
)

/**
 * Thread-safe dev-only telemetry logger for physical device field trials (Step 55).
 * Enables one-click JSON/CSV clipboard export for analysis without leaking credentials.
 */
object FieldTestLogger {

    private val runCounter = AtomicInteger(1)
    private val records = CopyOnWriteArrayList<FieldTestRecord>()

    /**
     * Records an acoustic field test result.
     */
    fun record(
        profile: ModemProfile,
        environment: FieldEnvironment,
        distanceMeters: Float,
        orientation: FieldOrientation,
        snrDb: Double,
        ambientDbfs: Double,
        isSuccess: Boolean,
        errorReason: String? = null,
        correctedBytes: Int = 0,
        airTimeMs: Long = 0L,
        timestampEpochMs: Long = System.currentTimeMillis()
    ): FieldTestRecord {
        val entry = FieldTestRecord(
            runId = runCounter.getAndIncrement(),
            timestampEpochMs = timestampEpochMs,
            profileName = "${profile.band.name}_${profile.scheme.name}_${profile.speed.name}",
            environment = environment,
            distanceMeters = distanceMeters,
            orientation = orientation,
            snrDb = snrDb,
            ambientDbfs = ambientDbfs,
            isSuccess = isSuccess,
            errorReason = errorReason,
            correctedBytes = correctedBytes,
            airTimeMs = airTimeMs
        )
        records.add(entry)
        return entry
    }

    /**
     * Clears all recorded field test trials.
     */
    fun clear() {
        records.clear()
        runCounter.set(1)
    }

    /**
     * Returns a snapshot of all recorded trials.
     */
    fun getRecords(): List<FieldTestRecord> = records.toList()

    /**
     * Computes aggregate statistical metrics over the collected runs.
     */
    fun computeStats(): FieldTestStats {
        val list = records.toList()
        if (list.isEmpty()) {
            return FieldTestStats(
                totalRuns = 0,
                successCount = 0,
                successRate = 0.0f,
                averageSnrDb = 0.0,
                maxSnrDb = 0.0,
                minSnrDb = 0.0,
                totalErrorsRepaired = 0
            )
        }

        val total = list.size
        val successes = list.count { it.isSuccess }
        val snrs = list.map { it.snrDb }
        val totalErrors = list.sumOf { it.correctedBytes }

        return FieldTestStats(
            totalRuns = total,
            successCount = successes,
            successRate = successes.toFloat() / total.toFloat(),
            averageSnrDb = snrs.average(),
            maxSnrDb = snrs.maxOrNull() ?: 0.0,
            minSnrDb = snrs.minOrNull() ?: 0.0,
            totalErrorsRepaired = totalErrors
        )
    }

    /**
     * Exports recorded field test runs as formatted JSON.
     */
    fun exportJson(): String {
        val list = records.toList()
        val stats = computeStats()

        val recordsJson = list.joinToString(",\n    ") { r ->
            """{"runId": ${r.runId}, "profile": "${r.profileName}", "env": "${r.environment.name}", "distanceM": ${r.distanceMeters}, "orientation": "${r.orientation.name}", "snrDb": ${r.snrDb}, "ambientDbfs": ${r.ambientDbfs}, "success": ${r.isSuccess}, "correctedBytes": ${r.correctedBytes}, "airTimeMs": ${r.airTimeMs}}"""
        }

        return """
{
  "summary": {
    "totalRuns": ${stats.totalRuns},
    "successCount": ${stats.successCount},
    "successRate": ${stats.successRate},
    "averageSnrDb": ${stats.averageSnrDb},
    "totalErrorsRepaired": ${stats.totalErrorsRepaired}
  },
  "runs": [
    $recordsJson
  ]
}
""".trimIndent()
    }

    /**
     * Exports recorded field test runs as CSV.
     */
    fun exportCsv(): String {
        val header = "runId,timestamp,profile,environment,distanceM,orientation,snrDb,ambientDbfs,success,errorReason,correctedBytes,airTimeMs"
        val rows = records.joinToString("\n") { r ->
            "${r.runId},${r.timestampEpochMs},${r.profileName},${r.environment.name},${r.distanceMeters},${r.orientation.name},${r.snrDb},${r.ambientDbfs},${r.isSuccess},${r.errorReason ?: ""},${r.correctedBytes},${r.airTimeMs}"
        }
        return "$header\n$rows"
    }
}
