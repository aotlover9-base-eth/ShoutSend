package app.shoutsend.modem.crypto

import app.shoutsend.modem.packet.AirTime
import app.shoutsend.modem.packet.Fragmenter
import app.shoutsend.modem.packet.FrameDecoder
import app.shoutsend.modem.packet.FrameDecodeResult
import app.shoutsend.modem.packet.FrameEncoder
import app.shoutsend.modem.packet.MessageAssembler
import app.shoutsend.modem.packet.MessageFlags
import app.shoutsend.modem.packet.MessageType
import app.shoutsend.modem.payload.Payload
import app.shoutsend.modem.payload.PayloadCodec
import app.shoutsend.modem.payload.PayloadCompactor
import app.shoutsend.modem.payload.PayloadDecodeResult
import app.shoutsend.modem.payload.WifiSecurity
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.ModemProfile
import app.shoutsend.modem.profile.RsProfile
import app.shoutsend.modem.profile.Scheme
import app.shoutsend.modem.profile.Speed
import app.shoutsend.modem.testutil.SeededRandom
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class SecureBoxTest {

    private val rng = SeededRandom(1501L)
    private val javaRng = Random(1501L)
    private val testPassphrase = "CorrectHorseBatteryStaple!1".toCharArray()

    @Test
    fun roundTripAllPayloadTypes() {
        val payloads = listOf(
            Payload.Text("Secure confidential memo transmitted via sound waves."),
            Payload.Url("https://internal.vault.corp/auth?session=secret"),
            Payload.WiFi("SecureBaseNet", "P@ssw0rd1234", WifiSecurity.WPA2),
            Payload.Contact("Dr. John von Neumann", listOf("+1 555 019 9283"), "john@ias.edu")
        )

        for (payload in payloads) {
            val plainBytes = PayloadCodec.encode(payload)
            val sealed = SecureBox.seal(plainBytes, payload.messageType, testPassphrase, javaRng)
            assertEquals("Overhead must be exactly 28 bytes", plainBytes.size + 28, sealed.size)

            val openResult = SecureBox.open(sealed, payload.messageType, testPassphrase)
            assertTrue("Decryption must succeed for $payload", openResult is SecureOpenResult.Opened)
            val decryptedBytes = (openResult as SecureOpenResult.Opened).plainBytes
            assertArrayEquals("Decrypted bytes mismatch", plainBytes, decryptedBytes)

            val restored = (PayloadCodec.decode(decryptedBytes, payload.messageType) as PayloadDecodeResult.Success).payload
            assertEquals(payload, restored)
        }
    }

    @Test
    fun wrongPassphraseFailsAuthentication() {
        val plain = "Secret message".toByteArray(Charsets.UTF_8)
        val sealed = SecureBox.seal(plain, MessageType.TEXT, testPassphrase, javaRng)

        val wrongPass = "IncorrectPassphrase999!".toCharArray()
        val result = SecureBox.open(sealed, MessageType.TEXT, wrongPass)
        assertTrue("Must fail with wrong passphrase", result is SecureOpenResult.WrongPassphraseOrTampered)
    }

    @Test
    fun singleFlippedBitAnywhereFailsAuthentication() {
        val plain = rng.nextBytes(50)
        val sealed = SecureBox.seal(plain, MessageType.TEXT, testPassphrase, javaRng)

        // Test flipping 1 bit at every byte position across the entire sealed buffer (salt, nonce, ciphertext, tag)
        for (i in sealed.indices) {
            val tampered = sealed.clone()
            tampered[i] = (tampered[i].toInt() xor 0x01).toByte()

            val result = SecureBox.open(tampered, MessageType.TEXT, testPassphrase)
            assertTrue(
                "Tampered bit at offset $i must fail authentication",
                result is SecureOpenResult.WrongPassphraseOrTampered
            )
        }
    }

    @Test
    fun differentMessagesGetDistinctNoncesAndSalts() {
        val plain = "Constant message".toByteArray(Charsets.UTF_8)
        val sealed1 = SecureBox.seal(plain, MessageType.TEXT, testPassphrase, javaRng)
        val sealed2 = SecureBox.seal(plain, MessageType.TEXT, testPassphrase, javaRng)

        val salt1 = sealed1.copyOfRange(0, 4)
        val salt2 = sealed2.copyOfRange(0, 4)
        val nonce1 = sealed1.copyOfRange(4, 16)
        val nonce2 = sealed2.copyOfRange(4, 16)

        assertFalse("Salts must be distinct", salt1.contentEquals(salt2))
        assertFalse("Nonces must be distinct", nonce1.contentEquals(nonce2))
        assertFalse("Ciphertexts must be distinct", sealed1.contentEquals(sealed2))
    }

    @Test
    fun associatedDataBindsMessageType() {
        val plain = "Bound to URL type".toByteArray(Charsets.UTF_8)
        val sealed = SecureBox.seal(plain, MessageType.URL, testPassphrase, javaRng)

        // Attempting to open as TEXT must fail because AAD binds MessageType
        val result = SecureBox.open(sealed, MessageType.TEXT, testPassphrase)
        assertTrue("AAD mismatch must cause authentication failure", result is SecureOpenResult.WrongPassphraseOrTampered)

        // Opening with correct type succeeds
        val correctResult = SecureBox.open(sealed, MessageType.URL, testPassphrase)
        assertTrue(correctResult is SecureOpenResult.Opened)
    }

    @Test
    fun passphraseStrengthEvaluation() {
        val short = PassphraseEvaluator.evaluate("short".toCharArray())
        assertFalse(short.isValid)
        assertEquals(PassphraseStrengthClass.WEAK, short.strength)

        val weak = PassphraseEvaluator.evaluate("abcdefgh".toCharArray())
        assertTrue(weak.isValid)
        assertEquals(PassphraseStrengthClass.WEAK, weak.strength)

        val ok = PassphraseEvaluator.evaluate("Password123".toCharArray())
        assertTrue(ok.isValid)
        assertEquals(PassphraseStrengthClass.OK, ok.strength)

        val strong = PassphraseEvaluator.evaluate("C0rrect-H0rse-Battery!2026".toCharArray())
        assertTrue(strong.isValid)
        assertEquals(PassphraseStrengthClass.STRONG, strong.strength)
    }

    @Test
    fun kdfBenchmarkTiming() {
        val sampleSalt = ByteArray(4) { 0x42 }
        val iterations = 5

        val t0 = System.currentTimeMillis()
        for (i in 0 until iterations) {
            val sealed = SecureBox.seal(byteArrayOf(1, 2, 3), MessageType.TEXT, testPassphrase, javaRng)
            SecureBox.open(sealed, MessageType.TEXT, testPassphrase)
        }
        val elapsedMs = System.currentTimeMillis() - t0
        val avgMs = elapsedMs.toDouble() / (iterations * 2)

        println("### PBKDF2 (60,000 iterations) Single-Operation Duration: %.2f ms (Host Build Machine)".format(avgMs))
        assertTrue("KDF must execute within reasonable developer environment bounds (< 500ms)", avgMs < 500.0)
    }

    @Test
    fun fullEndToEndIntegrationWithSteps12To14() {
        val wifiPayload = Payload.WiFi(
            ssid = "CoffeeShop",
            password = "Pass12345678", // 12-char password
            security = WifiSecurity.WPA2
        )
        val profile = RsProfile.STANDARD // k = 48, payload capacity = 42 bytes

        // ==================== Plain Mode ====================
        val plainCompact = PayloadCompactor.compact(wifiPayload)
        val plainBytes = plainCompact.bytes
        val plainFrames = Fragmenter.fragment(
            payload = plainBytes,
            type = wifiPayload.messageType,
            flags = MessageFlags(isEncrypted = false, isCompact = plainCompact.isCompact),
            profile = profile,
            messageId = 10
        )

        // ==================== Secure Mode ====================
        val secureCompact = PayloadCompactor.compact(wifiPayload)
        val sealedBytes = SecureBox.seal(secureCompact.bytes, wifiPayload.messageType, testPassphrase, javaRng)
        val secureFrames = Fragmenter.fragment(
            payload = sealedBytes,
            type = wifiPayload.messageType,
            flags = MessageFlags(isEncrypted = true, isCompact = secureCompact.isCompact),
            profile = profile,
            messageId = 20
        )

        // Calculate and report physical sound air-times
        val profileDtfskFast = ModemProfile(Band.STEALTH, Scheme.DTFSK, Speed.FAST, repeats = 2, rsProfileId = 1)
        val profileMfskNormal = ModemProfile(Band.STEALTH, Scheme.MFSK16, Speed.NORMAL, repeats = 2, rsProfileId = 1)

        val plainAirTimeFast = AirTime.estimate(profileDtfskFast, plainBytes.size)
        val plainAirTimeNormal = AirTime.estimate(profileMfskNormal, plainBytes.size)

        val secureAirTimeFast = AirTime.estimate(profileDtfskFast, sealedBytes.size)
        val secureAirTimeNormal = AirTime.estimate(profileMfskNormal, sealedBytes.size)

        println("### Secure Mode vs Plain Mode Comparison (12-char Wi-Fi Password):")
        println("| Mode        | Payload Bytes | On-Air Frames | AirTime (DTFSK Fast) | AirTime (MFSK16 Normal) |")
        println("| :---        | :---          | :---          | :---                 | :---                    |")
        println("| Plain       | %3d bytes      | %2d frames     | %5.2fs                | %5.2fs                  |".format(
            plainBytes.size, plainFrames.size, plainAirTimeFast, plainAirTimeNormal
        ))
        println("| Secure (Enc)| %3d bytes      | %2d frames     | %5.2fs                | %5.2fs                  |".format(
            sealedBytes.size, secureFrames.size, secureAirTimeFast, secureAirTimeNormal
        ))

        // Verify receiver assembly, decryption, and expansion
        val assembler = MessageAssembler()
        var receivedPlainMessage: ByteArray? = null
        var receivedFlags: MessageFlags? = null

        for (frame in secureFrames) {
            val encodedBytes = FrameEncoder.encode(frame, profile)
            val decodedFrame = (FrameDecoder.decode(encodedBytes, profile) as FrameDecodeResult.Success).frame
            val feedResult = assembler.feed(decodedFrame)
            if (feedResult is app.shoutsend.modem.packet.AssemblerResult.Complete) {
                receivedPlainMessage = feedResult.message.payload
                receivedFlags = feedResult.message.flags
            }
        }

        assertNotNull("Assembler must complete secure message", receivedPlainMessage)
        assertNotNull(receivedFlags)
        assertTrue(receivedFlags!!.isEncrypted)

        // Open secure container
        val openResult = SecureBox.open(receivedPlainMessage!!, MessageType.WIFI, testPassphrase)
        assertTrue(openResult is SecureOpenResult.Opened)
        val unsealedBytes = (openResult as SecureOpenResult.Opened).plainBytes

        // Expand compacted payload
        val expandResult = PayloadCompactor.expand(unsealedBytes, receivedFlags.isCompact, MessageType.WIFI)
        assertTrue(expandResult is PayloadDecodeResult.Success)
        val restoredPayload = (expandResult as PayloadDecodeResult.Success).payload as Payload.WiFi

        assertEquals(wifiPayload, restoredPayload)
    }
}
