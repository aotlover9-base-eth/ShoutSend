package app.shoutsend.core.devtools

import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.Speed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying DSP Lab tone synthesis, Goertzel filter bank inspection,
 * band switching, and telemetry calculation (Step 50).
 */
class DspLabViewModelTest {

    @Test
    fun initialStateHas16TonesForRetro() {
        val vm = DspLabViewModel()
        val state = vm.uiState.value

        assertEquals(Band.RETRO, state.band)
        assertEquals(Speed.NORMAL, state.speed)
        assertEquals(16, state.toneFrequencies.size)
        assertEquals(1500.0, state.toneFrequencies[0], 0.1)
        assertEquals(3375.0, state.toneFrequencies[15], 0.1)
        assertEquals(-1, state.activeToneIndex)
        assertEquals(-1, state.detectedToneIndex)
    }

    @Test
    fun selectBandSwitchesToStealth() {
        val vm = DspLabViewModel()
        vm.selectBand(Band.STEALTH)

        val state = vm.uiState.value
        assertEquals(Band.STEALTH, state.band)
        assertEquals(16, state.toneFrequencies.size)
        assertEquals(17600.0, state.toneFrequencies[0], 0.1)
        assertEquals(19850.0, state.toneFrequencies[15], 0.1)
    }

    @Test
    fun selectSpeedUpdatesSpeed() {
        val vm = DspLabViewModel()
        vm.selectSpeed(Speed.FAST)
        assertEquals(Speed.FAST, vm.uiState.value.speed)

        vm.selectSpeed(Speed.ROBUST)
        assertEquals(Speed.ROBUST, vm.uiState.value.speed)
    }

    @Test
    fun injectToneCalculatesAccurateGoertzelPower() {
        val vm = DspLabViewModel()

        // Test tone 0
        vm.injectTone(0)
        var state = vm.uiState.value
        assertEquals(0, state.activeToneIndex)
        assertEquals(0, state.detectedToneIndex)
        assertTrue("Detected power must be positive", state.binPowers[0] > 0.1f)
        assertTrue("SNR must exceed 10 dB", state.snrDb > 10.0)

        // Test tone 7
        vm.injectTone(7)
        state = vm.uiState.value
        assertEquals(7, state.activeToneIndex)
        assertEquals(7, state.detectedToneIndex)
        assertTrue(state.binPowers[7] > 0.1f)
        assertTrue(state.snrDb > 10.0)

        // Test tone 15
        vm.injectTone(15)
        state = vm.uiState.value
        assertEquals(15, state.activeToneIndex)
        assertEquals(15, state.detectedToneIndex)
        assertTrue(state.binPowers[15] > 0.1f)
        assertTrue(state.snrDb > 10.0)
    }

    @Test
    fun toggleNoiseInjectionUpdatesState() {
        val vm = DspLabViewModel()
        assertFalse(vm.uiState.value.isInjectingNoise)

        vm.toggleNoiseInjection()
        assertTrue(vm.uiState.value.isInjectingNoise)

        vm.toggleNoiseInjection()
        assertFalse(vm.uiState.value.isInjectingNoise)
    }

    @Test
    fun clearResetsMeters() {
        val vm = DspLabViewModel()
        vm.injectTone(5)
        assertTrue(vm.uiState.value.activeToneIndex == 5)

        vm.clear()
        val state = vm.uiState.value
        assertEquals(-1, state.activeToneIndex)
        assertEquals(-1, state.detectedToneIndex)
        assertEquals(0.0, state.snrDb, 0.001)
    }
}
