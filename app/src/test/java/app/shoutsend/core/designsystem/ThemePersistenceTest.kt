package app.shoutsend.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for ThemeController, TokenOverrides, validation, and debounce.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ThemePersistenceTest {

    private class FakeTokenOverridesRepository : TokenOverridesRepository {
        private val _overrides = MutableStateFlow(TokenOverrides())
        override val overrides: Flow<TokenOverrides> = _overrides.asStateFlow()

        override suspend fun update(transform: (TokenOverrides) -> TokenOverrides) {
            _overrides.value = transform(_overrides.value)
        }

        override suspend fun resetAll() {
            _overrides.value = TokenOverrides()
        }

        override suspend fun resetColor(role: String) {
            _overrides.value = _overrides.value.copy(colors = _overrides.value.colors - role)
        }

        override suspend fun resetRadius(name: String) {
            _overrides.value = _overrides.value.copy(radii = _overrides.value.radii - name)
        }
    }

    @Test
    fun themeControllerMergesColorOverrides() = runTest {
        val fakeRepo = FakeTokenOverridesRepository()
        val controller = ThemeController(
            initialBaseTokens = DesignTokens.defaultDark(),
            repository = fakeRepo,
            scope = backgroundScope
        )
        testScheduler.runCurrent()

        val testColor = Color(red = 0.1f, green = 0.9f, blue = 0.3f)
        controller.setColor("primary", testColor)
        testScheduler.runCurrent()

        val activeTokens = controller.tokens.value
        assertEquals(testColor, activeTokens.colors.primary)
        // Untouched colors must retain factory defaults
        assertEquals(DefaultTokens.CyberRadioDark.secondary, activeTokens.colors.secondary)
    }

    @Test
    fun themeControllerMergesRadiusOverridesAndUpdatesComponents() = runTest {
        val fakeRepo = FakeTokenOverridesRepository()
        val controller = ThemeController(
            initialBaseTokens = DesignTokens.defaultDark(),
            repository = fakeRepo,
            scope = backgroundScope
        )
        testScheduler.runCurrent()

        controller.setRadius("small", 14f)
        testScheduler.runCurrent()

        val activeTokens = controller.tokens.value
        val expectedShape = RoundedCornerShape(14.dp)
        assertEquals(expectedShape, activeTokens.shapes.small)
        assertEquals(expectedShape, activeTokens.shapes.button)
        assertEquals(expectedShape, activeTokens.shapes.textField)
        assertEquals(expectedShape, activeTokens.shapes.chip)
        // Medium and card remain untouched
        assertEquals(DefaultTokens.Shapes.medium, activeTokens.shapes.medium)
    }

    @Test
    fun resetIndividualKeysAndResetAll() = runTest {
        val fakeRepo = FakeTokenOverridesRepository()
        val controller = ThemeController(
            initialBaseTokens = DesignTokens.defaultDark(),
            repository = fakeRepo,
            scope = backgroundScope
        )
        testScheduler.runCurrent()

        controller.setColor("primary", Color(red = 0.2f, green = 0.3f, blue = 0.9f))
        controller.setRadius("medium", 20f)
        testScheduler.runCurrent()

        assertEquals(Color(red = 0.2f, green = 0.3f, blue = 0.9f), controller.tokens.value.colors.primary)
        assertEquals(RoundedCornerShape(20.dp), controller.tokens.value.shapes.medium)

        controller.resetColor("primary")
        testScheduler.runCurrent()
        assertEquals(DefaultTokens.CyberRadioDark.primary, controller.tokens.value.colors.primary)
        assertEquals(RoundedCornerShape(20.dp), controller.tokens.value.shapes.medium)

        controller.resetAll()
        testScheduler.runCurrent()
        assertEquals(DefaultTokens.Shapes.medium, controller.tokens.value.shapes.medium)
    }

    @Test
    fun validationRejectsUnknownKeysAndOutOfRangeValues() {
        val unknownColor = TokenOverrides(colors = mapOf("neonUltraviolet" to 12345L))
        assertTrue(TokenOverrides.validate(unknownColor).isFailure)

        val negativeRadius = TokenOverrides(radii = mapOf("small" to -2f))
        assertTrue(TokenOverrides.validate(negativeRadius).isFailure)

        val excessiveRadius = TokenOverrides(radii = mapOf("large" to 150f))
        assertTrue(TokenOverrides.validate(excessiveRadius).isFailure)

        val validOverrides = TokenOverrides(
            colors = mapOf("primary" to 54321L),
            radii = mapOf("extraSmall" to 6f)
        )
        assertTrue(TokenOverrides.validate(validOverrides).isSuccess)
    }

    @Test
    fun exportAndImportJsonIntegrity() = runTest {
        val fakeRepo = FakeTokenOverridesRepository()
        val controller = ThemeController(
            initialBaseTokens = DesignTokens.defaultDark(),
            repository = fakeRepo,
            scope = backgroundScope
        )
        testScheduler.runCurrent()

        controller.setColor("secondary", Color(red = 0.9f, green = 0.2f, blue = 0.2f))
        controller.setRadius("large", 24f)
        testScheduler.runCurrent()

        val exported = controller.exportToJson()
        assertTrue(exported.contains("secondary"))
        assertTrue(exported.contains("large"))

        // Reset all
        controller.resetAll()
        testScheduler.runCurrent()
        assertEquals(DefaultTokens.CyberRadioDark.secondary, controller.tokens.value.colors.secondary)

        // Import back
        val importResult = controller.importFromJson(exported)
        assertTrue(importResult.isSuccess)
        testScheduler.runCurrent()
        assertEquals(Color(red = 0.9f, green = 0.2f, blue = 0.2f), controller.tokens.value.colors.secondary)
        assertEquals(RoundedCornerShape(24.dp), controller.tokens.value.shapes.large)
    }

    @Test
    fun importRejectsCorruptedJson() = runTest {
        val fakeRepo = FakeTokenOverridesRepository()
        val controller = ThemeController(
            initialBaseTokens = DesignTokens.defaultDark(),
            repository = fakeRepo,
            scope = backgroundScope
        )
        testScheduler.runCurrent()

        val malformedJson = "{ colors: not_a_valid_json }"
        val result = controller.importFromJson(malformedJson)
        assertTrue(result.isFailure)
    }

    @OptIn(FlowPreview::class)
    @Test
    fun writeDebounceCollapsesRapidUpdates() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val pendingWrites = MutableSharedFlow<TokenOverrides>(
            extraBufferCapacity = 64,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val writtenBatches = mutableListOf<TokenOverrides>()

        testScope.launch {
            pendingWrites
                .debounce(300L)
                .collect { writtenBatches.add(it) }
        }

        // Fire 3 updates within 200ms
        pendingWrites.emit(TokenOverrides(radii = mapOf("small" to 2f)))
        testScheduler.advanceTimeBy(100L)
        pendingWrites.emit(TokenOverrides(radii = mapOf("small" to 4f)))
        testScheduler.advanceTimeBy(100L)
        pendingWrites.emit(TokenOverrides(radii = mapOf("small" to 6f)))

        // Only 200ms elapsed -> no writes yet
        testScheduler.runCurrent()
        assertEquals(0, writtenBatches.size)

        // Advance 301ms past the last emission
        testScheduler.advanceTimeBy(301L)
        testScheduler.runCurrent()

        // Exactly 1 collapsed write containing the latest state
        assertEquals(1, writtenBatches.size)
        assertEquals(6f, writtenBatches[0].radii["small"])
    }
}
