package app.shoutsend.modem.payload

import app.shoutsend.modem.packet.MessageType
import app.shoutsend.modem.testutil.SeededRandom
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PayloadCodecTest {

    private val rng = SeededRandom(1301L)

    @Test
    fun unicodeTextRoundTrip() {
        val samples = listOf(
            "Hello, world! 🌍",
            "नमस्ते भारत! ध्वनि तरंगों से डेटा ट्रांसफर 🚀",
            "Français: café, naïve, Noël, déjà-vu, façade",
            "CJK: 東京, 宇宙, 音波通信, ⚡️",
            "Arabic: مرحبا بالعالم",
            "Mixed: Hello 🚀 नमस्ते café 123"
        )

        for (sample in samples) {
            val payload = Payload.Text(sample)
            assertTrue("Payload must be valid: $sample", payload.validate() is PayloadValidationResult.Valid)

            val encoded = PayloadCodec.encode(payload)
            val result = PayloadCodec.decode(encoded, MessageType.TEXT)
            assertTrue("Decoding must succeed", result is PayloadDecodeResult.Success)
            val decoded = (result as PayloadDecodeResult.Success).payload as Payload.Text
            assertEquals(sample, decoded.value)
        }
    }

    @Test
    fun textLengthBoundaryValidation() {
        // 1 byte -> valid
        assertTrue(Payload.Text("a").validate() is PayloadValidationResult.Valid)

        // 280 bytes -> valid
        val exact280 = "a".repeat(280)
        assertTrue(Payload.Text(exact280).validate() is PayloadValidationResult.Valid)

        // 281 bytes -> invalid
        val over280 = "a".repeat(281)
        val res = Payload.Text(over280).validate()
        assertTrue(res is PayloadValidationResult.Invalid)
        assertEquals("text", (res as PayloadValidationResult.Invalid).error.field)

        // Empty -> invalid
        assertTrue(Payload.Text("").validate() is PayloadValidationResult.Invalid)
    }

    @Test
    fun wifiEscapingAllSpecialCharacters() {
        // Test all required special chars: \ ; , : "
        val specialSsid = """Home\Net;5G,Fast:Main"Guest"""
        val specialPass = """P@ss\;word,with:every"special\char"""

        val wifi = Payload.WiFi(
            ssid = specialSsid,
            password = specialPass,
            security = WifiSecurity.WPA2,
            isHidden = true
        )
        assertTrue(wifi.validate() is PayloadValidationResult.Valid)

        val encodedBytes = PayloadCodec.encode(wifi)
        val result = PayloadCodec.decode(encodedBytes, MessageType.WIFI)
        assertTrue("Wi-Fi decode must succeed", result is PayloadDecodeResult.Success)

        val decoded = (result as PayloadDecodeResult.Success).payload as Payload.WiFi
        assertEquals(specialSsid, decoded.ssid)
        assertEquals(specialPass, decoded.password)
        assertEquals(WifiSecurity.WPA2, decoded.security)
        assertTrue(decoded.isHidden)
    }

    @Test
    fun wifiSecurityAndLengthValidation() {
        // Open Wi-Fi with null password -> Valid
        val openWifi = Payload.WiFi(ssid = "CoffeeShop", password = null, security = WifiSecurity.OPEN)
        assertTrue(openWifi.validate() is PayloadValidationResult.Valid)

        // Open Wi-Fi with non-empty password -> Invalid
        val openWithPass = Payload.WiFi(ssid = "CoffeeShop", password = "secretpassword", security = WifiSecurity.OPEN)
        assertTrue(openWithPass.validate() is PayloadValidationResult.Invalid)

        // WPA2 with 8 char pass -> Valid
        val minPass = Payload.WiFi(ssid = "MyWifi", password = "12345678", security = WifiSecurity.WPA2)
        assertTrue(minPass.validate() is PayloadValidationResult.Valid)

        // WPA2 with 63 char pass -> Valid
        val maxPass = Payload.WiFi(ssid = "MyWifi", password = "a".repeat(63), security = WifiSecurity.WPA2)
        assertTrue(maxPass.validate() is PayloadValidationResult.Valid)

        // WPA2 with 7 char pass -> Invalid
        val shortPass = Payload.WiFi(ssid = "MyWifi", password = "1234567", security = WifiSecurity.WPA2)
        assertTrue(shortPass.validate() is PayloadValidationResult.Invalid)

        // WPA2 with 64 hex char pass -> Valid
        val hex64Pass = "0123456789abcdef".repeat(4)
        assertEquals(64, hex64Pass.length)
        val hexWifi = Payload.WiFi(ssid = "MyWifi", password = hex64Pass, security = WifiSecurity.WPA2)
        assertTrue(hexWifi.validate() is PayloadValidationResult.Valid)

        // WPA2 with 64 non-hex char pass -> Invalid
        val nonHex64 = "g".repeat(64)
        val nonHexWifi = Payload.WiFi(ssid = "MyWifi", password = nonHex64, security = WifiSecurity.WPA2)
        assertTrue(nonHexWifi.validate() is PayloadValidationResult.Invalid)

        // SSID bounds: 1 to 32 bytes
        assertTrue(Payload.WiFi(ssid = "a", password = "password123").validate() is PayloadValidationResult.Valid)
        assertTrue(Payload.WiFi(ssid = "a".repeat(32), password = "password123").validate() is PayloadValidationResult.Valid)
        assertTrue(Payload.WiFi(ssid = "a".repeat(33), password = "password123").validate() is PayloadValidationResult.Invalid)
        assertTrue(Payload.WiFi(ssid = "", password = "password123").validate() is PayloadValidationResult.Invalid)
    }

    @Test
    fun malformedWifiStringsRejectedCleanly() {
        val badInputs = listOf(
            "NOT_WIFI_STRING",
            "WIFI:S:MyNet;;", // Missing T
            "WIFI:T:INVALID;S:MyNet;;", // Unknown T
            "WIFI:T:WPA;P:pass;;", // Missing S
            "WIFI:T:WPA;S:MyNet;P:short;;", // Password too short
            "WIFI:T:WPA;S:MyNet;P:password;" // Missing final semicolon (only one ';')
        )

        for (bad in badInputs) {
            val decoded = WifiQrCodec.decode(bad)
            assertNull("Malformed Wi-Fi string must return null: $bad", decoded)
        }
    }

    @Test
    fun contactTlvRoundTripAndValidation() {
        val contact = Payload.Contact(
            displayName = "Ada Lovelace",
            phones = listOf("+1 (555) 019-2834", "+44 20 7946 0912"),
            email = "ada@analytical.engine"
        )
        assertTrue(contact.validate() is PayloadValidationResult.Valid)

        val encoded = PayloadCodec.encode(contact)
        val result = PayloadCodec.decode(encoded, MessageType.CONTACT)
        assertTrue("Contact decode must succeed", result is PayloadDecodeResult.Success)

        val decoded = (result as PayloadDecodeResult.Success).payload as Payload.Contact
        assertEquals(contact.displayName, decoded.displayName)
        assertEquals(contact.phones, decoded.phones)
        assertEquals(contact.email, decoded.email)

        // Contact without email
        val noEmail = Payload.Contact(displayName = "Alan Turing", phones = listOf("1234567890"))
        val encNoEmail = PayloadCodec.encode(noEmail)
        val decNoEmail = (PayloadCodec.decode(encNoEmail, MessageType.CONTACT) as PayloadDecodeResult.Success).payload as Payload.Contact
        assertEquals("Alan Turing", decNoEmail.displayName)
        assertEquals(listOf("1234567890"), decNoEmail.phones)
        assertNull(decNoEmail.email)

        // Validation bounds
        // Name 1..60 bytes
        assertTrue(Payload.Contact(displayName = "a".repeat(60)).validate() is PayloadValidationResult.Valid)
        assertTrue(Payload.Contact(displayName = "a".repeat(61)).validate() is PayloadValidationResult.Invalid)
        assertTrue(Payload.Contact(displayName = "").validate() is PayloadValidationResult.Invalid)

        // Phones max 2
        assertTrue(Payload.Contact(displayName = "Bob", phones = listOf("1", "2")).validate() is PayloadValidationResult.Valid)
        assertTrue(Payload.Contact(displayName = "Bob", phones = listOf("1", "2", "3")).validate() is PayloadValidationResult.Invalid)

        // Phone invalid characters
        assertTrue(Payload.Contact(displayName = "Bob", phones = listOf("123-abc")).validate() is PayloadValidationResult.Invalid)

        // Email format
        assertTrue(Payload.Contact(displayName = "Bob", email = "bob@example.com").validate() is PayloadValidationResult.Valid)
        assertTrue(Payload.Contact(displayName = "Bob", email = "bobexample.com").validate() is PayloadValidationResult.Invalid)
    }

    @Test
    fun urlValidationAndSafetyAnalysisTrickyExamples() {
        // Valid URLs
        val validUrl = Payload.Url("https://example.com/path?query=1")
        assertTrue(validUrl.validate() is PayloadValidationResult.Valid)

        // Disallowed schemes
        val badSchemes = listOf(
            "javascript:alert(1)",
            "intent://scan/#Intent;scheme=zxing;package=com.google.zxing.client.android;end",
            "file:///etc/passwd",
            "content://contacts/people/1",
            "data:text/plain;base64,SGVsbG8="
        )
        for (bad in badSchemes) {
            val url = Payload.Url(bad)
            assertTrue("Scheme must be rejected: $bad", url.validate() is PayloadValidationResult.Invalid)
        }

        // Tricky Example 1: Deceptive user@host trick
        // https://google.com@evil.example/
        val trick1 = UrlSafety.analyze("https://google.com@evil.example/")
        assertEquals("evil.example", trick1.displayHost)
        assertTrue("hasUserInfo must be true", trick1.hasUserInfo)
        assertTrue(trick1.isSuspicious)

        // Tricky Example 2: Punycode / Internationalized Domain Name
        // https://xn--80ak6aa92e.com
        val trick2 = UrlSafety.analyze("https://xn--80ak6aa92e.com")
        assertEquals("xn--80ak6aa92e.com", trick2.displayHost)
        assertTrue("hostHasNonAscii must be true for punycode", trick2.hostHasNonAscii)
        assertTrue(trick2.isSuspicious)

        // Tricky Example 3: IPv6 Literal
        // https://[::1]/
        val trick3 = UrlSafety.analyze("https://[::1]/")
        assertTrue("hostIsIpLiteral must be true for IPv6", trick3.hostIsIpLiteral)
        assertTrue(trick3.isSuspicious)

        // Tricky Example 4: Non-standard TCP port
        // https://example.com:8443/
        val trick4 = UrlSafety.analyze("https://example.com:8443/")
        assertEquals("example.com", trick4.displayHost)
        assertTrue("nonStandardPort must be true for 8443", trick4.nonStandardPort)
        assertTrue(trick4.isSuspicious)

        // Normal clean URL
        val clean = UrlSafety.analyze("https://google.com/search?q=sound")
        assertEquals("google.com", clean.displayHost)
        assertFalse(clean.hasUserInfo)
        assertFalse(clean.hostIsIpLiteral)
        assertFalse(clean.hostHasNonAscii)
        assertFalse(clean.nonStandardPort)
        assertFalse(clean.veryLong)
        assertFalse(clean.isSuspicious)
    }

    @Test
    fun fuzzTenThousandRandomByteArraysNeverThrow() {
        val numTrials = 10_000

        for (trial in 0 until numTrials) {
            val length = rng.nextInt(0, 300)
            val randomBytes = rng.nextBytes(length)
            val type = MessageType.entries[rng.nextInt(0, MessageType.entries.size)]

            try {
                val result = PayloadCodec.decode(randomBytes, type)
                assertNotNull("Decode result must never be null", result)
            } catch (t: Throwable) {
                fail("Decoder threw unexpected exception on trial $trial with type $type: ${t.message}")
            }
        }
    }
}
