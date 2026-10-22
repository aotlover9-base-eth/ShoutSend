package app.shoutsend.core.audio

import app.shoutsend.core.audio.permissions.InMemoryMicPermissionRequestedProvider
import app.shoutsend.core.audio.permissions.MicPermissionState
import app.shoutsend.core.audio.permissions.resolve
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MicPermissionStateTest {

    @Test
    fun exhaustiveTruthTableResolution() {
        val testCases = listOf(
            // (isGranted, shouldShowRationale, hasRequestedBefore, expectedState)
            TestCase(isGranted = true, shouldShowRationale = false, hasRequestedBefore = false, expected = MicPermissionState.Granted),
            TestCase(isGranted = true, shouldShowRationale = false, hasRequestedBefore = true, expected = MicPermissionState.Granted),
            TestCase(isGranted = true, shouldShowRationale = true, hasRequestedBefore = false, expected = MicPermissionState.Granted),
            TestCase(isGranted = true, shouldShowRationale = true, hasRequestedBefore = true, expected = MicPermissionState.Granted),
            TestCase(isGranted = false, shouldShowRationale = true, hasRequestedBefore = false, expected = MicPermissionState.ShouldExplain),
            TestCase(isGranted = false, shouldShowRationale = true, hasRequestedBefore = true, expected = MicPermissionState.ShouldExplain),
            TestCase(isGranted = false, shouldShowRationale = false, hasRequestedBefore = true, expected = MicPermissionState.PermanentlyDenied),
            TestCase(isGranted = false, shouldShowRationale = false, hasRequestedBefore = false, expected = MicPermissionState.CanAsk),
        )

        assertEquals("Must cover exactly 8 truth table combinations", 8, testCases.size)

        for (case in testCases) {
            val companionResult = MicPermissionState.resolve(
                isGranted = case.isGranted,
                shouldShowRationale = case.shouldShowRationale,
                hasRequestedBefore = case.hasRequestedBefore
            )
            assertEquals(
                "Failed for companion resolve(isGranted=${case.isGranted}, shouldShowRationale=${case.shouldShowRationale}, hasRequestedBefore=${case.hasRequestedBefore})",
                case.expected,
                companionResult
            )

            val topLevelResult = resolve(
                isGranted = case.isGranted,
                shouldShowRationale = case.shouldShowRationale,
                hasRequestedBefore = case.hasRequestedBefore
            )
            assertEquals(
                "Failed for top-level resolve(isGranted=${case.isGranted}, shouldShowRationale=${case.shouldShowRationale}, hasRequestedBefore=${case.hasRequestedBefore})",
                case.expected,
                topLevelResult
            )
        }
    }

    @Test
    fun inMemoryMicPermissionRequestedProviderDefaultFalse() = runTest {
        val provider = InMemoryMicPermissionRequestedProvider()
        assertFalse(provider.hasRequestedBefore())

        provider.setRequestedBefore(true)
        assertTrue(provider.hasRequestedBefore())

        provider.setRequestedBefore(false)
        assertFalse(provider.hasRequestedBefore())
    }

    @Test
    fun inMemoryMicPermissionRequestedProviderInitialTrue() = runTest {
        val provider = InMemoryMicPermissionRequestedProvider(initialValue = true)
        assertTrue(provider.hasRequestedBefore())

        provider.setRequestedBefore(false)
        assertFalse(provider.hasRequestedBefore())
    }

    private data class TestCase(
        val isGranted: Boolean,
        val shouldShowRationale: Boolean,
        val hasRequestedBefore: Boolean,
        val expected: MicPermissionState
    )
}
