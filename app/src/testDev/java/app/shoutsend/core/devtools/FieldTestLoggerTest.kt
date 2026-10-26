package app.shoutsend.core.devtools

import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.ModemProfile
import app.shoutsend.modem.profile.Scheme
import app.shoutsend.modem.profile.Speed
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FieldTestLoggerTest {

    @Before
    @After
    fun resetLogger() {
        FieldTestLogger.clear()
    }

    @Test
    fun testEmptyStats() {
        val stats = FieldTestLogger.computeStats()
        assertEquals(0, stats.totalRuns)
        assertEquals(0, stats.successCount)
        assertEquals(0.0f, stats.successRate, 0.001f)
        assertEquals(0.0, stats.averageSnrDb, 0.001)
    }

    @Test
    fun testRecordAndComputeStats() {
        val retroNormal = ModemProfile(Band.RETRO, Scheme.MFSK16, Speed.NORMAL)
        val stealthRobust = ModemProfile(Band.STEALTH, Scheme.MFSK16, Speed.ROBUST)

        FieldTestLogger.record(
            profile = retroNormal,
            environment = FieldEnvironment.QUIET_ROOM,
            distanceMeters = 0.5f,
            orientation = FieldOrientation.LINE_OF_SIGHT,
            snrDb = 14.5,
            ambientDbfs = -52.0,
            isSuccess = true,
            correctedBytes = 2,
            airTimeMs = 3200L
        )

        FieldTestLogger.record(
            profile = retroNormal,
            environment = FieldEnvironment.CAFE,
            distanceMeters = 1.0f,
            orientation = FieldOrientation.DESK_FLAT,
            snrDb = 8.2,
            ambientDbfs = -38.0,
            isSuccess = true,
            correctedBytes = 5,
            airTimeMs = 3200L
        )

        FieldTestLogger.record(
            profile = stealthRobust,
            environment = FieldEnvironment.OUTDOOR,
            distanceMeters = 2.5f,
            orientation = FieldOrientation.OBSTRUCTED_HAND,
            snrDb = 3.1,
            ambientDbfs = -32.0,
            isSuccess = false,
            errorReason = "Sync lost under burst noise",
            correctedBytes = 0,
            airTimeMs = 4500L
        )

        val stats = FieldTestLogger.computeStats()
        assertEquals(3, stats.totalRuns)
        assertEquals(2, stats.successCount)
        assertEquals(2.0f / 3.0f, stats.successRate, 0.001f)
        assertEquals((14.5 + 8.2 + 3.1) / 3.0, stats.averageSnrDb, 0.001)
        assertEquals(14.5, stats.maxSnrDb, 0.001)
        assertEquals(3.1, stats.minSnrDb, 0.001)
        assertEquals(7, stats.totalErrorsRepaired)
    }

    @Test
    fun testExportJsonFormatAndPrivacy() {
        val profile = ModemProfile(Band.RETRO, Scheme.MFSK16, Speed.NORMAL)
        FieldTestLogger.record(
            profile = profile,
            environment = FieldEnvironment.LIVING_ROOM,
            distanceMeters = 1.0f,
            orientation = FieldOrientation.ANGLED_45,
            snrDb = 11.0,
            ambientDbfs = -45.0,
            isSuccess = true,
            correctedBytes = 0,
            airTimeMs = 3000L
        )

        val json = FieldTestLogger.exportJson()
        assertTrue("JSON should contain totalRuns", json.contains("\"totalRuns\": 1"))
        assertTrue("JSON should contain profile", json.contains("RETRO_MFSK16_NORMAL"))
        assertTrue("JSON should contain distance", json.contains("\"distanceM\": 1.0"))
        // Strict privacy: no password, secret, or payload keys
        assertFalse("JSON must never contain password field", json.contains("password"))
        assertFalse("JSON must never contain payload field", json.contains("payload"))
    }

    @Test
    fun testExportCsvFormat() {
        val profile = ModemProfile(Band.RETRO, Scheme.MFSK16, Speed.NORMAL)
        FieldTestLogger.record(
            profile = profile,
            environment = FieldEnvironment.QUIET_ROOM,
            distanceMeters = 0.1f,
            orientation = FieldOrientation.DESK_FLAT,
            snrDb = 18.0,
            ambientDbfs = -60.0,
            isSuccess = true,
            correctedBytes = 0,
            airTimeMs = 2800L
        )

        val csv = FieldTestLogger.exportCsv()
        assertTrue("CSV must have standard header", csv.startsWith("runId,timestamp,profile,environment,distanceM"))
        assertTrue("CSV must contain recorded data row", csv.contains("RETRO_MFSK16_NORMAL,QUIET_ROOM,0.1,DESK_FLAT,18.0,-60.0,true"))
    }
}
