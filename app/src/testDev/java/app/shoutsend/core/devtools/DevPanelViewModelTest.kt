package app.shoutsend.core.devtools

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.shoutsend.core.designsystem.DefaultTokens
import app.shoutsend.core.designsystem.DesignTokens
import app.shoutsend.core.designsystem.ThemeController
import app.shoutsend.core.designsystem.ThemeMode
import app.shoutsend.core.designsystem.TokenOverrides
import app.shoutsend.core.designsystem.TokenOverridesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

@OptIn(ExperimentalCoroutinesApi::class)
class DevPanelViewModelTest {

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
    fun colorUtilsHsvRoundtrip() {
        val testColor = Color(red = 0.2f, green = 0.8f, blue = 0.4f, alpha = 1f)
        val hsv = ColorUtils.rgbToHsv(testColor)
        val restored = ColorUtils.hsvToColor(hsv[0], hsv[1], hsv[2])

        assertTrue(abs(testColor.red - restored.red) < 0.02f)
        assertTrue(abs(testColor.green - restored.green) < 0.02f)
        assertTrue(abs(testColor.blue - restored.blue) < 0.02f)
    }

    @Test
    fun colorUtilsHexParsing() {
        val parsed = ColorUtils.hexToColor("00FF00")
        assertNotNull(parsed)
        assertEquals(0f, parsed!!.red, 0.01f)
        assertEquals(1f, parsed.green, 0.01f)
        assertEquals(0f, parsed.blue, 0.01f)

        assertNull(ColorUtils.hexToColor("not_hex"))
        assertNull(ColorUtils.hexToColor("#123"))
    }

    @Test
    fun viewModelUpdatesSeedColorAndMarksModified() = runTest {
        val fakeRepo = FakeTokenOverridesRepository()
        val controller = ThemeController(
            initialBaseTokens = DesignTokens.defaultDark(),
            repository = fakeRepo,
            scope = backgroundScope
        )
        val viewModel = DevPanelViewModel(controller, backgroundScope)
        testScheduler.runCurrent()

        viewModel.onDynamicColorChanged(false)
        testScheduler.runCurrent()
        assertTrue(viewModel.uiState.value.hasOverrides)

        val newSeed = 0xFFBA1A1AL // Coral
        viewModel.onSeedColorChanged(newSeed)
        testScheduler.runCurrent()

        assertEquals(newSeed, viewModel.uiState.value.seedColor)
        assertEquals(newSeed, controller.tokens.value.seedColor)
    }

    @Test
    fun viewModelUpdatesHeroShape() = runTest {
        val fakeRepo = FakeTokenOverridesRepository()
        val controller = ThemeController(
            initialBaseTokens = DesignTokens.defaultDark(),
            repository = fakeRepo,
            scope = backgroundScope
        )
        val viewModel = DevPanelViewModel(controller, backgroundScope)
        testScheduler.runCurrent()

        viewModel.onHeroShapeChanged("Sunny")
        testScheduler.runCurrent()

        assertEquals("Sunny", viewModel.uiState.value.heroShape)
        assertEquals("Sunny", controller.tokens.value.heroShape)
    }

    @Test
    fun viewModelUpdatesRadius() = runTest {
        val fakeRepo = FakeTokenOverridesRepository()
        val controller = ThemeController(
            initialBaseTokens = DesignTokens.defaultDark(),
            repository = fakeRepo,
            scope = backgroundScope
        )
        val viewModel = DevPanelViewModel(controller, backgroundScope)
        testScheduler.runCurrent()

        viewModel.onRadiusChanged("extraSmall", 10f)
        testScheduler.runCurrent()

        val shape = controller.tokens.value.shapes.extraSmall as androidx.compose.foundation.shape.CornerBasedShape
        assertEquals(10f, shape.topStart.toPx(androidx.compose.ui.geometry.Size(100f, 100f), androidx.compose.ui.unit.Density(1f)), 0.01f)
    }

    @Test
    fun viewModelExportAndImport() = runTest {
        val fakeRepo = FakeTokenOverridesRepository()
        val controller = ThemeController(
            initialBaseTokens = DesignTokens.defaultDark(),
            repository = fakeRepo,
            scope = backgroundScope
        )
        val viewModel = DevPanelViewModel(controller, backgroundScope)
        testScheduler.runCurrent()

        viewModel.onHeroShapeChanged("Heart")
        viewModel.onRadiusChanged("large", 28f)
        testScheduler.runCurrent()

        val exported = viewModel.exportJson()
        assertTrue(exported.contains("Heart"))

        viewModel.resetAll()
        testScheduler.runCurrent()
        assertEquals("Cookie9Sided", controller.tokens.value.heroShape)

        val importResult = viewModel.importJson(exported)
        testScheduler.runCurrent()

        assertTrue(importResult.isSuccess)
        assertEquals("Heart", controller.tokens.value.heroShape)
    }
}
