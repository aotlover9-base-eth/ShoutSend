package app.shoutsend.feature.receive

import app.shoutsend.modem.payload.Payload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM tests for payload parsing, URL security validation,
 * phone number sanitization, and contact formatting in [PayloadActionHelper].
 */
class PayloadActionHelperTest {

    @Test
    fun testParseHttpsUrlExtractsHostAndSetsHttpsTrue() {
        val parsed = PayloadActionHelper.parseUrl("https://shoutsend.app/download?ref=acoustic")
        assertTrue(parsed.isHttps)
        assertEquals("https", parsed.scheme)
        assertEquals("shoutsend.app", parsed.host)
        assertEquals("/download?ref=acoustic", parsed.pathAndQuery)
    }

    @Test
    fun testParseHttpUrlMarksHttpsFalse() {
        val parsed = PayloadActionHelper.parseUrl("http://unencrypted-site.org/plain")
        assertFalse(parsed.isHttps)
        assertEquals("http", parsed.scheme)
        assertEquals("unencrypted-site.org", parsed.host)
        assertEquals("/plain", parsed.pathAndQuery)
    }

    @Test
    fun testParseUrlWithPortAndQueryParameters() {
        val parsed = PayloadActionHelper.parseUrl("https://cyber.radio:8443/stream?codec=mfsk16#live")
        assertTrue(parsed.isHttps)
        assertEquals("https", parsed.scheme)
        assertEquals("cyber.radio", parsed.host)
        assertEquals(":8443/stream?codec=mfsk16#live", parsed.pathAndQuery)
    }

    @Test
    fun testParseMalformedUrlFallback() {
        val parsed = PayloadActionHelper.parseUrl("not a valid url at all")
        assertFalse(parsed.isHttps)
        assertEquals("http", parsed.scheme)
        assertTrue(parsed.host.contains("not a valid url"))
    }

    @Test
    fun testSanitizePhoneNumberStripsFormatting() {
        val sanitized = PayloadActionHelper.sanitizePhoneNumber("+1 (555) 019-2834")
        assertEquals("+15550192834", sanitized)
    }

    @Test
    fun testSanitizePhoneNumberPreservesSpecialChars() {
        val sanitized = PayloadActionHelper.sanitizePhoneNumber("*#06#")
        assertEquals("*#06#", sanitized)
    }

    @Test
    fun testSanitizePhoneNumberWithDashesAndSpaces() {
        val sanitized = PayloadActionHelper.sanitizePhoneNumber("  080-1234-5678  ")
        assertEquals("08012345678", sanitized)
    }

    @Test
    fun testFormatContactSummaryWithAllFields() {
        val contact = Payload.Contact(
            displayName = "Alex Rivera",
            phones = listOf("+15550192834", "+15550199999"),
            email = "alex@shoutsend.app"
        )
        val summary = PayloadActionHelper.formatContactSummary(contact)
        val expected = "Alex Rivera\n+15550192834\n+15550199999\nalex@shoutsend.app"
        assertEquals(expected, summary)
    }

    @Test
    fun testFormatContactSummaryWithoutEmail() {
        val contact = Payload.Contact(
            displayName = "Taylor Swift",
            phones = listOf("+15550001111"),
            email = null
        )
        val summary = PayloadActionHelper.formatContactSummary(contact)
        val expected = "Taylor Swift\n+15550001111"
        assertEquals(expected, summary)
    }

    @Test
    fun testFormatContactSummarySinglePhone() {
        val contact = Payload.Contact(
            displayName = "Emergency Contact",
            phones = listOf("911"),
            email = ""
        )
        val summary = PayloadActionHelper.formatContactSummary(contact)
        val expected = "Emergency Contact\n911"
        assertEquals(expected, summary)
    }
}
