package app.shoutsend.modem.payload

import app.shoutsend.modem.packet.AirTime
import app.shoutsend.modem.packet.MessageType
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.ModemProfile
import app.shoutsend.modem.profile.Scheme
import app.shoutsend.modem.profile.Speed
import app.shoutsend.modem.testutil.SeededRandom
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PayloadCompactorTest {

    private val rng = SeededRandom(1401L)

    private val profileDtfskFast = ModemProfile(
        band = Band.STEALTH,
        scheme = Scheme.DTFSK,
        speed = Speed.FAST,
        repeats = 2,
        rsProfileId = 1 // STANDARD RS(60,48)
    )

    private val profileMfskNormal = ModemProfile(
        band = Band.STEALTH,
        scheme = Scheme.MFSK16,
        speed = Speed.NORMAL,
        repeats = 2,
        rsProfileId = 1
    )

    @Test
    fun exactRoundTripAllPayloadTypes() {
        val testPayloads = listOf(
            Payload.Text("This is a compactable 7-bit ASCII text message with more than twenty characters."),
            Payload.Text("Unicode text: नमस्ते 🌍 café"),
            Payload.Url("https://github.com/aotlover9-base-eth/ShoutSend"),
            Payload.Url("https://example.com/"),
            Payload.WiFi("MyHomeNet", "secret1234", WifiSecurity.WPA2, isHidden = false),
            Payload.WiFi("PublicWifi", null, WifiSecurity.OPEN, isHidden = false),
            Payload.Contact("Ada Lovelace", listOf("1234567890", "+1 (555) 012-3456"), "ada@analytical.org")
        )

        for (payload in testPayloads) {
            val compactRes = PayloadCompactor.compact(payload)
            val canonicalBytes = PayloadCodec.encode(payload)
            assertTrue(
                "Compact form must never be larger than canonical",
                compactRes.bytes.size <= canonicalBytes.size
            )

            val expandedResult = PayloadCompactor.expand(
                bytes = compactRes.bytes,
                isCompact = compactRes.isCompact,
                type = payload.messageType
            )
            assertTrue("Expand must succeed for $payload", expandedResult is PayloadDecodeResult.Success)
            val expandedPayload = (expandedResult as PayloadDecodeResult.Success).payload
            assertEquals("Restored payload mismatch", payload, expandedPayload)
        }
    }

    @Test
    fun compactChooserNeverLargerThanCanonical() {
        // Very short string where 7-bit header overhead (2 bytes) would exceed canonical
        val shortAscii = Payload.Text("hi")
        val result = PayloadCompactor.compact(shortAscii)
        assertFalse("Chooser must reject compact when not strictly shorter", result.isCompact)
        val canonical = PayloadCodec.encode(shortAscii)
        assertArrayEquals(canonical, result.bytes)
    }

    @Test
    fun malformedCompactInputsFailCleanly() {
        val truncatedBytes = ByteArray(1) { 0xFF.toByte() }
        for (type in MessageType.entries) {
            val res = PayloadCompactor.expand(truncatedBytes, isCompact = true, type = type)
            assertTrue("Must fail on truncated input for $type", res is PayloadDecodeResult.Failure)
        }
    }

    @Test
    fun fuzzTenThousandPayloadCompactionRoundTrips() {
        val numTrials = 10_000

        for (trial in 0 until numTrials) {
            val payload: Payload = when (trial % 4) {
                0 -> {
                    // Random text
                    val len = rng.nextInt(1, 150)
                    val chars = CharArray(len) {
                        if (rng.nextBoolean()) (32 + rng.nextInt(0, 95)).toChar() else 'A'
                    }
                    Payload.Text(String(chars))
                }
                1 -> {
                    // Random URL
                    val prefixes = UrlCompactor.PREFIX_TABLE
                    val suffixes = UrlCompactor.SUFFIX_TABLE
                    val p = prefixes[rng.nextInt(0, prefixes.size)]
                    val s = suffixes[rng.nextInt(0, suffixes.size)]
                    val mid = "domain" + rng.nextInt(1, 9999) + "/test"
                    Payload.Url((p + mid + s).trim().ifEmpty { "https://example.com" })
                }
                2 -> {
                    // Random Wi-Fi
                    val sec = WifiSecurity.entries[rng.nextInt(0, WifiSecurity.entries.size)]
                    val pass = if (sec == WifiSecurity.OPEN) null else "pass" + rng.nextInt(1000, 9999)
                    Payload.WiFi("Net" + rng.nextInt(1, 999), pass, sec, isHidden = rng.nextBoolean())
                }
                else -> {
                    // Random contact
                    val name = "User" + rng.nextInt(1, 999)
                    val phone = if (rng.nextBoolean()) "555" + rng.nextInt(1000000, 9999999) else "12345"
                    Payload.Contact(name, listOf(phone), "user@test.org")
                }
            }

            if (payload.validate() !is PayloadValidationResult.Valid) continue

            val compactRes = PayloadCompactor.compact(payload)
            assertTrue(compactRes.bytes.size <= PayloadCodec.encode(payload).size)

            val expanded = PayloadCompactor.expand(compactRes.bytes, compactRes.isCompact, payload.messageType)
            assertTrue(expanded is PayloadDecodeResult.Success)
            assertEquals(payload, (expanded as PayloadDecodeResult.Success).payload)
        }
    }

    @Test
    fun benchmarkAndReportSavingsTable() {
        val english100 = "The quick brown fox jumps over the lazy dog repeatedly until the total string length hits exact 100."
        assertEquals(100, english100.length)

        val hindiSentence = "नमस्ते दोस्तों! यह एक अल्ट्रासोनिक ध्वनि संचार प्रणाली है जो बिना इंटरनेट काम करती है।"

        val testCases = listOf(
            "Short URL" to Payload.Url("https://github.com/shoutsend"),
            "Long URL with query" to Payload.Url("https://maps.google.com/?q=37.7749,-122.4194&z=15"),
            "Wi-Fi (10-char pass)" to Payload.WiFi("CoffeeShop", "Pass123456", WifiSecurity.WPA2),
            "Wi-Fi (25-char pass)" to Payload.WiFi("OfficeGuestNetwork", "SuperSecretLongPassword12", WifiSecurity.WPA2, isHidden = true),
            "100-char English text" to Payload.Text(english100),
            "Hindi sentence text" to Payload.Text(hindiSentence),
            "Contact (name, phone, email)" to Payload.Contact("Grace Hopper", listOf("1234567890"), "grace@navy.mil")
        )

        println("### Payload Compaction Savings & Air-Time Table")
        println("| Example Case | Canonical | Compact | Saved | Savings % | AirTime (DTFSK Fast) | AirTime (MFSK16 Normal) |")
        println("| :--- | :--- | :--- | :--- | :--- | :--- | :--- |")

        for ((name, payload) in testCases) {
            val canonicalBytes = PayloadCodec.encode(payload)
            val compactRes = PayloadCompactor.compact(payload)
            val compactBytes = compactRes.bytes

            val savedBytes = canonicalBytes.size - compactBytes.size
            val savingsPercent = (savedBytes.toDouble() / canonicalBytes.size) * 100.0

            val airTimeDtfskFast = AirTime.estimate(profileDtfskFast, compactBytes.size)
            val airTimeMfskNormal = AirTime.estimate(profileMfskNormal, compactBytes.size)

            println("| %-28s | %3d bytes  | %3d bytes | %2d B | %5.1f%%   | %5.2fs               | %5.2fs                 |".format(
                name,
                canonicalBytes.size,
                compactBytes.size,
                savedBytes,
                savingsPercent,
                airTimeDtfskFast,
                airTimeMfskNormal
            ))

            assertTrue("Compacted bytes must never exceed canonical", compactBytes.size <= canonicalBytes.size)
        }
    }
}
