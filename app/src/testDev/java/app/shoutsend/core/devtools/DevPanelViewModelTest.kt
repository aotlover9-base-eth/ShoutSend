package app.shoutsend.core.devtools

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.shoutsend.core.designsystem.DefaultTokens
import app.shoutsend.core.designsystem.DesignTokens
import app.shoutsend.core.designsystem.ThemeController
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
            _overrides.value = _overrides.value.copy(colors = _overrides.value.colors - role)
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
    fun viewModelUpdatesColorAndMarksModified() = runTest {
        val fakeRepo = FakeTokenOverridesRepository()
        val controller = ThemeController(
            initialBaseTokens = DesignTokens.defaultDark(),
            repository = fakeRepo,
            scope = backgroundScope
        )
        val viewModel = DevPanelViewModel(controller, backgroundScope)
        testScheduler.runCurrent()

        assertFalse(viewModel.uiState.value.hasOverrides)

        viewModel.onColorChanged("primary", Color(red = 0.9f, green = 0.9f, blue = 0.1f))
        testScheduler.runCurrent()

        assertTrue(viewModel.uiState.value.hasOverrides)
        val primaryItem = viewModel.uiState.value.colorGroups["Primary"]?.find { it.role == "primary" }
        assertNotNull(primaryItem)
        assertTrue(primaryItem!!.isModified)
        assertEquals(Color(red = 0.9f, green = 0.9f, blue = 0.1f), primaryItem.currentColor)
        assertEquals(Color(red = 0.9f, green = 0.9f, blue = 0.1f), controller.tokens.value.colors.primary)
    }

    @Test
    fun viewModelResetsColor() = runTest {
        val fakeRepo = FakeTokenOverridesRepository()
        val controller = ThemeController(
            initialBaseTokens = DesignTokens.defaultDark(),
            repository = fakeRepo,
            scope = backgroundScope
        )
        val viewModel = DevPanelViewModel(controller, backgroundScope)
        testScheduler.runCurrent()

        viewModel.onColorChanged("secondary", Color(red = 0.9f, green = 0.1f, blue = 0.9f))
        testScheduler.runCurrent()
        assertTrue(viewModel.uiState.value.hasOverrides)

        viewModel.resetColor("secondary")
        testScheduler.runCurrent()

        val secondaryItem = viewModel.uiState.value.colorGroups["Secondary"]?.find { it.role == "secondary" }
        assertNotNull(secondaryItem)
        assertFalse(secondaryItem!!.isModified)
        assertEquals(DefaultTokens.CyberRadioDark.secondary, controller.tokens.value.colors.secondary)
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

        viewModel.onRadiusChanged("small", 18f)
        testScheduler.runCurrent()

        assertTrue(controller.tokens.value.shapes.small.toString().contains("18"))
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

        viewModel.onColorChanged("tertiary", Color(red = 0.1f, green = 0.9f, blue = 0.9f))
        viewModel.onRadiusChanged("large", 22f)
        testScheduler.runCurrent()

        val exported = viewModel.exportJson()
        assertTrue(exported.contains("tertiary"))
        assertTrue(exported.contains("large"))

        viewModel.resetAll()
        testScheduler.runCurrent()
        assertEquals(DefaultTokens.CyberRadioDark.tertiary, controller.tokens.value.colors.tertiary)

        var importSuccess = false
        viewModel.importJson(exported) { result ->
            importSuccess = result.isSuccess
        }
        testScheduler.runCurrent()

        assertTrue(importSuccess)
        assertEquals(Color(red = 0.1f, green = 0.9f, blue = 0.9f), controller.tokens.value.colors.tertiary)
        assertTrue(controller.tokens.value.shapes.large.toString().contains("22"))
    }
}
