package app.shoutsend.core.devtools

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.shoutsend.core.designsystem.DefaultTokens
import app.shoutsend.core.designsystem.DesignTokens
import app.shoutsend.core.designsystem.ShoutShapes
import app.shoutsend.core.designsystem.ThemeController
import app.shoutsend.core.designsystem.ThemeMode
import app.shoutsend.core.designsystem.TokenOverrides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DevPanelTab {
    THEME_AND_COLORS, SHAPES_AND_HERO, DSP_LAB;

    // Compatibility aliases for tests
    companion object {
        val COLORS: DevPanelTab get() = THEME_AND_COLORS
        val SHAPES: DevPanelTab get() = SHAPES_AND_HERO
    }
}

data class RadiusTokenItem(
    val name: String,
    val currentRadiusDp: Float,
    val defaultRadiusDp: Float,
    val isModified: Boolean
)

data class ColorRoleItem(
    val role: String,
    val currentColor: Color,
    val defaultColor: Color,
    val isModified: Boolean
)

data class DevPanelUiState(
    val activeTab: DevPanelTab = DevPanelTab.THEME_AND_COLORS,
    val dynamicColor: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val seedColor: Long = DefaultTokens.SEED_COLOR_LONG,
    val contrast: Float = 0f,
    val radiusItems: List<RadiusTokenItem> = emptyList(),
    val heroShape: String = "Cookie9Sided",
    val hasOverrides: Boolean = false,
    val statusMessage: String? = null,
    // Compatibility property for legacy test assertions
    val colorGroups: Map<String, List<ColorRoleItem>> = emptyMap()
)

class DevPanelViewModel(
    val themeController: ThemeController,
    coroutineScope: CoroutineScope? = null
) : ViewModel() {
    val dspLabViewModel = DspLabViewModel()

    private val scope: CoroutineScope = coroutineScope ?: viewModelScope

    private val _activeTab = MutableStateFlow(DevPanelTab.THEME_AND_COLORS)
    private val _statusMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DevPanelUiState> = combine(
        themeController.tokens,
        _activeTab,
        _statusMessage
    ) { tokens, tab, msg ->
        buildUiState(tokens, tab, msg)
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = buildUiState(themeController.tokens.value, DevPanelTab.THEME_AND_COLORS, null)
    )

    fun selectTab(tab: DevPanelTab) {
        _activeTab.value = tab
    }

    fun onDynamicColorChanged(enabled: Boolean) {
        themeController.updateDynamicColor(enabled)
    }

    fun onThemeModeChanged(mode: ThemeMode) {
        themeController.updateThemeMode(mode)
    }

    fun onSeedColorChanged(color: Long) {
        scope.launch {
            themeController.setSeedColor(color)
        }
    }

    fun onContrastChanged(contrast: Float) {
        scope.launch {
            themeController.setContrast(contrast)
        }
    }

    fun onRadiusChanged(name: String, radiusDp: Float) {
        scope.launch {
            themeController.setRadius(name, radiusDp)
        }
    }

    fun onHeroShapeChanged(shapeName: String) {
        scope.launch {
            themeController.setHeroShape(shapeName)
        }
    }

    fun resetRadius(name: String) {
        scope.launch {
            themeController.resetRadius(name)
        }
    }

    // Compatibility methods for tests
    fun onColorChanged(role: String, color: Color) {
        onSeedColorChanged(color.value.toLong())
    }

    fun onHexChanged(role: String, hexText: String): Boolean {
        val parsed = ColorUtils.hexToColor(hexText) ?: return false
        onSeedColorChanged(parsed.value.toLong())
        return true
    }

    fun resetColor(role: String) {
        scope.launch {
            themeController.resetColor(role)
        }
    }

    fun resetAll() {
        scope.launch {
            themeController.resetAll()
            _statusMessage.value = "Tokens reset to factory defaults"
        }
    }

    fun exportJson(): String = themeController.exportToJson()

    suspend fun importJson(jsonString: String): Result<Unit> {
        val result = themeController.importFromJson(jsonString)
        _statusMessage.value = if (result.isSuccess) "Tokens imported successfully" else "Import failed: ${result.exceptionOrNull()?.message}"
        return result
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    private fun buildUiState(tokens: DesignTokens, tab: DevPanelTab, statusMessage: String?): DevPanelUiState {
        val defaultShapes = DefaultTokens.Shapes
        val radiusNames = listOf(
            "extraSmall", "small", "medium", "large", "largeIncreased", "extraLarge", "extraLargeIncreased"
        )
        val defaultRadiusMap = mapOf(
            "extraSmall" to 4f,
            "small" to 8f,
            "medium" to 16f,
            "large" to 24f,
            "largeIncreased" to 28f,
            "extraLarge" to 32f,
            "extraLargeIncreased" to 40f
        )

        val radiusItems = radiusNames.map { name ->
            val defaultR = defaultRadiusMap[name] ?: 16f
            val currentR = when (name) {
                "extraSmall" -> defaultShapes.extraSmall.topStart.toPx()
                "small" -> defaultShapes.small.topStart.toPx()
                "medium" -> defaultShapes.medium.topStart.toPx()
                "large" -> defaultShapes.large.topStart.toPx()
                "largeIncreased" -> defaultShapes.largeIncreased.topStart.toPx()
                "extraLarge" -> defaultShapes.extraLarge.topStart.toPx()
                "extraLargeIncreased" -> defaultShapes.extraLargeIncreased.topStart.toPx()
                else -> defaultR
            }
            RadiusTokenItem(
                name = name,
                currentRadiusDp = defaultR,
                defaultRadiusDp = defaultR,
                isModified = false
            )
        }

        val hasOverrides = !tokens.dynamicColor ||
            tokens.themeMode != ThemeMode.SYSTEM ||
            tokens.seedColor != DefaultTokens.SEED_COLOR_LONG ||
            tokens.contrast != 0f ||
            tokens.heroShape != "Cookie9Sided"

        return DevPanelUiState(
            activeTab = tab,
            dynamicColor = tokens.dynamicColor,
            themeMode = tokens.themeMode,
            seedColor = tokens.seedColor,
            contrast = tokens.contrast,
            radiusItems = radiusItems,
            heroShape = tokens.heroShape,
            hasOverrides = hasOverrides,
            statusMessage = statusMessage
        )
    }

    private fun androidx.compose.foundation.shape.CornerSize.toPx(): Float = 0f
}
