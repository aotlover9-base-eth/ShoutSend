package app.shoutsend.modem.packet

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
import org.junit.Assert.fail
import org.junit.Test

class PacketProtocolTest {

    private val rng = SeededRandom(1201L)

    @Test
    fun airTimeHandCalculatedExamples() {
        // Example 1: Stealth, MFSK16, Normal (30ms), STANDARD RS(60,48) (payload = 42), repeats = 2
        // Header: 28 symbols * 30ms = 0.840s
        // 1 frame: 4 marker + 120 codeword = 124 symbols * 30ms = 3.72s
        // Repeats 2: 1 * 2 * 3.72s = 7.44s
        // Total = 0.84 + 7.44 = 8.28s
        val profile1 = ModemProfile(
            band = Band.STEALTH,
            scheme = Scheme.MFSK16,
            speed = Speed.NORMAL,
            repeats = 2,
            rsProfileId = 1 // STANDARD RS(60,48)
        )
        val est1 = AirTime.estimate(profile1, onAirBytes = 42)
        assertEquals(8.28, est1, 1e-6)

        // Example 2: Retro, DTFSK, Normal (30ms), STANDARD RS(60,48), repeats = 2
        // Header: 0.840s
        // 1 frame: 4 marker + 80 codeword = 84 symbols * 30ms = 2.52s
        // Repeats 2: 1 * 2 * 2.52s = 5.04s
        // Total = 0.84 + 5.04 = 5.88s
        val profile2 = ModemProfile(
            band = Band.RETRO,
            scheme = Scheme.DTFSK,
            speed = Speed.NORMAL,
            repeats = 2,
            rsProfileId = 1
        )
        val est2 = AirTime.estimate(profile2, onAirBytes = 42)
        assertEquals(5.88, est2, 1e-6)

        // Example 3: Stealth, DTFSK, Fast (20ms), STANDARD RS(60,48), 84 bytes (2 frames), repeats = 2
        // Header: 0.840s
        // 1 frame: 84 symbols * 20ms = 1.68s
        // 2 frames * 2 repeats * 1.68s = 6.72s
        // Total = 0.84 + 6.72 = 7.56s
        val profile3 = ModemProfile(
            band = Band.STEALTH,
            scheme = Scheme.DTFSK,
            speed = Speed.FAST,
            repeats = 2,
            rsProfileId = 1
        )
        val est3 = AirTime.estimate(profile3, onAirBytes = 84)
        assertEquals(7.56, est3, 1e-6)
    }

    @Test
    fun fragmentationAndAssemblyForCanonicalPayloadSizes() {
        val testSizes = listOf(1, 41, 42, 43, 84, 320)
        val expectedFragments = listOf(1, 1, 1, 2, 2, 8)
        val profile = RsProfile.STANDARD // payload capacity = 48 - 6 = 42 bytes

        for (i in testSizes.indices) {
            val size = testSizes[i]
            val expectedCount = expectedFragments[i]
            val originalPayload = rng.nextBytes(size)
            val messageId = 100 + i

            val frames = Fragmenter.fragment(
                payload = originalPayload,
                type = MessageType.TEXT,
                flags = MessageFlags.NONE,
                profile = profile,
                messageId = messageId
            )
            assertEquals("Fragment count mismatch for size $size", expectedCount, frames.size)

            val assembler = MessageAssembler()
            var completedMessage: ReceivedMessage? = null

            for (frame in frames) {
                // Encode frame to bytes
                val encodedBytes = FrameEncoder.encode(frame, profile)
                assertEquals(profile.k, encodedBytes.size)

                // Decode frame from bytes
                val decodeResult = FrameDecoder.decode(encodedBytes, profile)
                assertTrue("Frame decoding must succeed", decodeResult is FrameDecodeResult.Success)
                val decodedFrame = (decodeResult as FrameDecodeResult.Success).frame

                // Feed to assembler
                val feedResult = assembler.feed(decodedFrame)
                if (feedResult is AssemblerResult.Complete) {
                    completedMessage = feedResult.message
                }
            }

            assertNotNull("Assembler must complete message of size $size", completedMessage)
            assertEquals(MessageType.TEXT, completedMessage!!.type)
            assertEquals(messageId, completedMessage.messageId)
            assertArrayEquals("Payload reconstruction mismatch for size $size", originalPayload, completedMessage.payload)
        }
    }

    @Test
    fun size321AndEmptyPayloadsRejected() {
        val profile = RsProfile.STANDARD

        try {
            Fragmenter.fragment(
                payload = ByteArray(0),
                type = MessageType.TEXT,
                profile = profile,
                messageId = 1
            )
            fail("Empty payload must be rejected")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("empty", ignoreCase = true))
        }

        try {
            Fragmenter.fragment(
                payload = ByteArray(321),
                type = MessageType.TEXT,
                profile = profile,
                messageId = 1
            )
            fail("Payload of 321 bytes must exceed MAX_AIR_BYTES")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("MAX_AIR_BYTES", ignoreCase = true))
        }
    }

    @Test
    fun framesArrivingInRandomOrderReassembleCleanly() {
        val original = rng.nextBytes(320) // 8 fragments
        val profile = RsProfile.STANDARD
        val frames = Fragmenter.fragment(
            payload = original,
            type = MessageType.URL,
            flags = MessageFlags(isEncrypted = false, isCompact = true),
            profile = profile,
            messageId = 42
        )
        assertEquals(8, frames.size)

        // Shuffle frames deterministically
        val shuffledIndices = (0..7).toMutableList()
        for (i in shuffledIndices.indices.reversed()) {
            val j = rng.nextInt(0, i + 1)
            val temp = shuffledIndices[i]
            shuffledIndices[i] = shuffledIndices[j]
            shuffledIndices[j] = temp
        }

        val assembler = MessageAssembler()
        var completed: ReceivedMessage? = null

        for (idx in shuffledIndices) {
            val encoded = FrameEncoder.encode(frames[idx], profile)
            val decoded = (FrameDecoder.decode(encoded, profile) as FrameDecodeResult.Success).frame
            val result = assembler.feed(decoded)
            if (result is AssemblerResult.Complete) {
                completed = result.message
            }
        }

        assertNotNull(completed)
        assertEquals(MessageType.URL, completed!!.type)
        assertTrue(completed.flags.isCompact)
        assertFalse(completed.flags.isEncrypted)
        assertArrayEquals(original, completed.payload)
    }

    @Test
    fun duplicateFramesAreIgnoredAndEmittedExactlyOnce() {
        val original = rng.nextBytes(84) // 2 fragments
        val profile = RsProfile.STANDARD
        val frames = Fragmenter.fragment(original, MessageType.WIFI, profile = profile, messageId = 88)
        assertEquals(2, frames.size)

        val assembler = MessageAssembler()

        // Feed fragment 0
        val res1 = assembler.feed(frames[0])
        assertTrue(res1 is AssemblerResult.Incomplete)
        assertEquals(1, (res1 as AssemblerResult.Incomplete).progress.receivedFragments)

        // Feed fragment 0 duplicate
        val res2 = assembler.feed(frames[0])
        assertTrue(res2 is AssemblerResult.Duplicate)
        assertEquals(1, (res2 as AssemblerResult.Duplicate).progress.receivedFragments)

        // Feed fragment 1 -> Complete
        val res3 = assembler.feed(frames[1])
        assertTrue(res3 is AssemblerResult.Complete)
        assertArrayEquals(original, (res3 as AssemblerResult.Complete).message.payload)

        // Feed fragment 1 duplicate after complete -> Duplicate (no re-emission of Complete)
        val res4 = assembler.feed(frames[1])
        assertTrue(res4 is AssemblerResult.Duplicate)
    }

    @Test
    fun missingFragmentArrivingLaterCompletesSuccessfully() {
        val original = rng.nextBytes(120) // 3 fragments (42, 42, 36)
        val profile = RsProfile.STANDARD
        val frames = Fragmenter.fragment(original, MessageType.CONTACT, profile = profile, messageId = 99)
        assertEquals(3, frames.size)

        val assembler = MessageAssembler()

        // Feed fragment 0 and 2
        val r0 = assembler.feed(frames[0])
        assertTrue(r0 is AssemblerResult.Incomplete)
        assertEquals(1, (r0 as AssemblerResult.Incomplete).progress.receivedFragments)

        val r2 = assembler.feed(frames[2])
        assertTrue(r2 is AssemblerResult.Incomplete)
        assertEquals(2, (r2 as AssemblerResult.Incomplete).progress.receivedFragments)

        // Feed fragment 1
        val r1 = assembler.feed(frames[1])
        assertTrue(r1 is AssemblerResult.Complete)
        assertArrayEquals(original, (r1 as AssemblerResult.Complete).message.payload)
    }

    @Test
    fun corruptedCrcFrameRejectedByDecoder() {
        val original = rng.nextBytes(40)
        val profile = RsProfile.STANDARD
        val frames = Fragmenter.fragment(original, MessageType.TEXT, profile = profile, messageId = 12)
        val encoded = FrameEncoder.encode(frames[0], profile)

        // Corrupt a byte in the payload
        encoded[10] = (encoded[10].toInt() xor 0x01).toByte()

        val result = FrameDecoder.decode(encoded, profile)
        assertTrue(result is FrameDecodeResult.Failure)
        val err = (result as FrameDecodeResult.Failure).error
        assertTrue("Error must be BadCrc, was $err", err is FrameDecodeError.BadCrc)
    }

    @Test
    fun frameDecoderValidationErrors() {
        val original = rng.nextBytes(40)
        val profile = RsProfile.STANDARD
        val frame = Fragmenter.fragment(original, MessageType.TEXT, profile = profile, messageId = 15)[0]

        // 1. Version mismatch
        val enc1 = FrameEncoder.encode(frame, profile)
        enc1[0] = (enc1[0].toInt() xor 0x40).toByte() // Flip version bit
        val res1 = FrameDecoder.decode(enc1, profile)
        assertTrue((res1 as FrameDecodeResult.Failure).error is FrameDecodeError.VersionMismatch)

        // 2. Bad payload length (> k - 6 = 42)
        val enc2 = FrameEncoder.encode(frame, profile)
        enc2[3] = 45.toByte()
        val res2 = FrameDecoder.decode(enc2, profile)
        assertTrue((res2 as FrameDecodeResult.Failure).error is FrameDecodeError.BadLength)

        // 3. Bad fragment field (fragmentIndex >= fragmentCount)
        val enc3 = FrameEncoder.encode(frame, profile)
        // Set fragmentIndex = 3, fragmentCount = 2 (byte 2 = (3 shl 4) or (2 - 1) = 0x31)
        enc3[2] = 0x31.toByte()
        val res3 = FrameDecoder.decode(enc3, profile)
        // CRC will fail first if we don't recalculate CRC, but even with CRC, BadFragmentField is checked
        // Let's recalculate CRC for enc3 so it passes CRC and hits fragment check:
        val crc = app.shoutsend.modem.protocol.Crc16.calculate(enc3, 0, 4 + frame.payload.size)
        enc3[profile.k - 2] = ((crc ushr 8) and 0xFF).toByte()
        enc3[profile.k - 1] = (crc and 0xFF).toByte()
        val res3Crc = FrameDecoder.decode(enc3, profile)
        assertTrue((res3Crc as FrameDecodeResult.Failure).error is FrameDecodeError.BadFragmentField)
    }

    @Test
    fun conflictingMessageIdStreamDetectedAndReset() {
        val profile = RsProfile.STANDARD
        val assembler = MessageAssembler()

        val textFrames = Fragmenter.fragment(rng.nextBytes(100), MessageType.TEXT, profile = profile, messageId = 55)
        val urlFrames = Fragmenter.fragment(rng.nextBytes(50), MessageType.URL, profile = profile, messageId = 55)

        // Feed fragment 0 of TEXT
        val r1 = assembler.feed(textFrames[0])
        assertTrue(r1 is AssemblerResult.Incomplete)
        assertEquals(0, assembler.conflictCount)

        // Feed fragment 0 of URL (conflicting type & count for same messageId 55)
        val r2 = assembler.feed(urlFrames[0])
        assertTrue(r2 is AssemblerResult.Conflict)
        assertEquals(1, assembler.conflictCount)
        assertEquals(55, (r2 as AssemblerResult.Conflict).messageId)
        assertEquals(MessageType.URL, r2.newProgress.type)
    }

    @Test
    fun timeoutExpiryCleansPendingMessage() {
        var currentTimeMs = 10_000L
        val assembler = MessageAssembler(timeoutMs = 60_000L, clock = { currentTimeMs })
        val profile = RsProfile.STANDARD
        val frames = Fragmenter.fragment(rng.nextBytes(84), MessageType.TEXT, profile = profile, messageId = 22)
        assertEquals(2, frames.size)

        // Feed fragment 0 at t = 10,000ms
        val r1 = assembler.feed(frames[0])
        assertTrue(r1 is AssemblerResult.Incomplete)
        assertEquals(1, (r1 as AssemblerResult.Incomplete).progress.receivedFragments)

        // Advance time past 60s timeout (t = 71,000ms)
        currentTimeMs = 71_000L

        // Feed fragment 1
        // Fragment 0 timed out and was purged, so fragment 1 becomes the start of a fresh attempt
        val r2 = assembler.feed(frames[1])
        assertTrue("Should not be Complete because fragment 0 was purged by timeout", r2 is AssemblerResult.Incomplete)
        assertEquals(1, (r2 as AssemblerResult.Incomplete).progress.receivedFragments)
    }

    @Test
    fun sameMessageSentTwiceWithDifferentMessageIdsDeliveredTwice() {
        val payload = rng.nextBytes(42)
        val profile = RsProfile.STANDARD
        val assembler = MessageAssembler()

        val frames1 = Fragmenter.fragment(payload, MessageType.TEXT, profile = profile, messageId = 1)
        val frames2 = Fragmenter.fragment(payload, MessageType.TEXT, profile = profile, messageId = 2)

        val res1 = assembler.feed(frames1[0])
        assertTrue(res1 is AssemblerResult.Complete)
        assertArrayEquals(payload, (res1 as AssemblerResult.Complete).message.payload)

        val res2 = assembler.feed(frames2[0])
        assertTrue(res2 is AssemblerResult.Complete)
        assertArrayEquals(payload, (res2 as AssemblerResult.Complete).message.payload)
    }
}
