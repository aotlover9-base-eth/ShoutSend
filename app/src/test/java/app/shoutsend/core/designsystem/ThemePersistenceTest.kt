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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for ThemeController, TokenOverrides, validation, and debounce in Material 3 Expressive.
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
            _overrides.value = _overrides.value.copy(seedColor = DefaultTokens.SEED_COLOR_LONG)
        }

        override suspend fun resetRadius(name: String) {
            _overrides.value = _overrides.value.copy(radii = _overrides.value.radii - name)
        }
    }

    @Test
    fun themeControllerUpdatesSeedColorAndThemeMode() = runTest {
        val fakeRepo = FakeTokenOverridesRepository()
        val controller = ThemeController(
            initialBaseTokens = DesignTokens.defaultDark(),
            repository = fakeRepo,
            scope = backgroundScope
        )
        testScheduler.runCurrent()

        val testSeed = 0xFFBA1A1AL // Coral
        controller.setSeedColor(testSeed)
        controller.updateThemeMode(ThemeMode.LIGHT)
        testScheduler.runCurrent()

        val activeTokens = controller.tokens.value
        assertEquals(testSeed, activeTokens.seedColor)
        assertEquals(ThemeMode.LIGHT, activeTokens.themeMode)
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

        controller.setRadius("extraSmall", 10f)
        testScheduler.runCurrent()

        val activeTokens = controller.tokens.value
        val expectedShape = RoundedCornerShape(10.dp)
        assertEquals(expectedShape, activeTokens.shapes.extraSmall)
        // Medium and card remain untouched
        assertEquals(DefaultTokens.Shapes.medium, activeTokens.shapes.medium)
    }

    @Test
    fun themeControllerUpdatesHeroShape() = runTest {
        val fakeRepo = FakeTokenOverridesRepository()
        val controller = ThemeController(
            initialBaseTokens = DesignTokens.defaultDark(),
            repository = fakeRepo,
            scope = backgroundScope
        )
        testScheduler.runCurrent()

        controller.setHeroShape("Sunny")
        testScheduler.runCurrent()

        assertEquals("Sunny", controller.tokens.value.heroShape)
    }

    @Test
    fun resetAllRestoresFactoryDefaults() = runTest {
        val fakeRepo = FakeTokenOverridesRepository()
        val controller = ThemeController(
            initialBaseTokens = DesignTokens.defaultDark(),
            repository = fakeRepo,
            scope = backgroundScope
        )
        testScheduler.runCurrent()

        val oceanSeed = DefaultTokens.SeedPresets[3].second
        controller.setSeedColor(oceanSeed)
        controller.setRadius("medium", 20f)
        controller.setHeroShape("Flower")
        testScheduler.runCurrent()

        assertEquals(oceanSeed, controller.tokens.value.seedColor)
        assertEquals("Flower", controller.tokens.value.heroShape)

        controller.resetAll()
        testScheduler.runCurrent()

        assertEquals(DefaultTokens.SEED_COLOR_LONG, controller.tokens.value.seedColor)
        assertEquals("Cookie9Sided", controller.tokens.value.heroShape)
        assertEquals(DefaultTokens.Shapes.medium, controller.tokens.value.shapes.medium)
    }

    @Test
    fun validationRejectsOutOfRangeContrastAndRadii() {
        val invalidContrast = TokenOverrides(contrast = 2.5f)
        assertTrue(TokenOverrides.validate(invalidContrast).isFailure)

        val negativeRadius = TokenOverrides(radii = mapOf("small" to -2f))
        assertTrue(TokenOverrides.validate(negativeRadius).isFailure)

        val excessiveRadius = TokenOverrides(radii = mapOf("large" to 150f))
        assertTrue(TokenOverrides.validate(excessiveRadius).isFailure)

        val unknownHero = TokenOverrides(heroShape = "NonExistentShape")
        assertTrue(TokenOverrides.validate(unknownHero).isFailure)

        val validOverrides = TokenOverrides(
            dynamicColor = false,
            themeMode = "DARK",
            seedColor = DefaultTokens.SEED_COLOR_LONG,
            contrast = 0.5f,
            heroShape = "Sunny",
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

        val forestSeed = DefaultTokens.SeedPresets[2].second
        controller.setSeedColor(forestSeed)
        controller.setHeroShape("Heart")
        testScheduler.runCurrent()

        val exported = controller.exportToJson()
        assertTrue(exported.contains("Heart"))

        // Reset all
        controller.resetAll()
        testScheduler.runCurrent()
        assertEquals("Cookie9Sided", controller.tokens.value.heroShape)

        // Import back
        val importResult = controller.importFromJson(exported)
        assertTrue(importResult.isSuccess)
        testScheduler.runCurrent()
        assertEquals("Heart", controller.tokens.value.heroShape)
        assertEquals(forestSeed, controller.tokens.value.seedColor)
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

        val malformedJson = "{ heroShape: not_a_valid_json }"
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
