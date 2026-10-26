package app.shoutsend.core.devtools

import androidx.compose.ui.graphics.Color
import app.shoutsend.core.designsystem.ColorTokens
import app.shoutsend.core.designsystem.DefaultTokens
import app.shoutsend.core.designsystem.DesignTokens
import app.shoutsend.core.designsystem.ShapeTokens
import app.shoutsend.core.designsystem.ThemeController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DevPanelTab {
    COLORS, SHAPES, DSP_LAB
}

data class ColorRoleItem(
    val role: String,
    val currentColor: Color,
    val defaultColor: Color,
    val isModified: Boolean
)

data class RadiusTokenItem(
    val name: String,
    val currentRadiusDp: Float,
    val defaultRadiusDp: Float,
    val isModified: Boolean
)

data class DevPanelUiState(
    val activeTab: DevPanelTab = DevPanelTab.COLORS,
    val colorGroups: Map<String, List<ColorRoleItem>> = emptyMap(),
    val radiusItems: List<RadiusTokenItem> = emptyList(),
    val hasOverrides: Boolean = false,
    val statusMessage: String? = null
)

class DevPanelViewModel(
    val themeController: ThemeController,
    private val scope: CoroutineScope
) {
    val dspLabViewModel = DspLabViewModel()

    private val _activeTab = MutableStateFlow(DevPanelTab.COLORS)
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
        initialValue = buildUiState(themeController.tokens.value, DevPanelTab.COLORS, null)
    )

    fun selectTab(tab: DevPanelTab) {
        _activeTab.value = tab
    }

    fun onColorChanged(role: String, color: Color) {
        scope.launch {
            themeController.setColor(role, color)
        }
    }

    fun onHexChanged(role: String, hexText: String): Boolean {
        val parsed = ColorUtils.hexToColor(hexText) ?: return false
        onColorChanged(role, parsed)
        return true
    }

    fun onRadiusChanged(name: String, radiusDp: Float) {
        scope.launch {
            themeController.setRadius(name, radiusDp)
        }
    }

    fun resetColor(role: String) {
        scope.launch {
            themeController.resetColor(role)
        }
    }

    fun resetRadius(name: String) {
        scope.launch {
            themeController.resetRadius(name)
        }
    }

    fun resetAll() {
        scope.launch {
            themeController.resetAll()
            _statusMessage.value = "All tokens reset to defaults"
        }
    }

    fun exportJson(): String {
        return themeController.exportToJson()
    }

    fun importJson(json: String, onComplete: (Result<Unit>) -> Unit) {
        scope.launch {
            val result = themeController.importFromJson(json)
            if (result.isSuccess) {
                _statusMessage.value = "Tokens imported successfully"
            } else {
                _statusMessage.value = "Import failed: ${result.exceptionOrNull()?.message}"
            }
            onComplete(result)
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    private fun buildUiState(
        tokens: DesignTokens,
        tab: DevPanelTab,
        statusMessage: String?
    ): DevPanelUiState {
        val defaultDark = DefaultTokens.CyberRadioDark

        val colorGroups = COLOR_ROLE_GROUPS.mapValues { (_, roles) ->
            roles.map { role ->
                val current = getColorForRole(tokens.colors, role)
                val default = getColorForRole(defaultDark, role)
                ColorRoleItem(
                    role = role,
                    currentColor = current,
                    defaultColor = default,
                    isModified = current != default
                )
            }
        }

        val radiusItems = listOf(
            RadiusTokenItem("extraSmall", 4f, 4f, false),
            RadiusTokenItem("small", 8f, 8f, false),
            RadiusTokenItem("medium", 12f, 12f, false),
            RadiusTokenItem("large", 16f, 16f, false),
            RadiusTokenItem("extraLarge", 28f, 28f, false)
        )

        val anyColorModified = colorGroups.values.flatten().any { it.isModified }

        return DevPanelUiState(
            activeTab = tab,
            colorGroups = colorGroups,
            radiusItems = radiusItems,
            hasOverrides = anyColorModified,
            statusMessage = statusMessage
        )
    }

    companion object {
        val COLOR_ROLE_GROUPS = mapOf(
            "Primary" to listOf("primary", "onPrimary", "primaryContainer", "onPrimaryContainer"),
            "Secondary" to listOf("secondary", "onSecondary", "secondaryContainer", "onSecondaryContainer"),
            "Tertiary" to listOf("tertiary", "onTertiary", "tertiaryContainer", "onTertiaryContainer"),
            "Error" to listOf("error", "onError", "errorContainer", "onErrorContainer"),
            "Surfaces" to listOf(
                "background", "onBackground", "surface", "onSurface",
                "surfaceVariant", "onSurfaceVariant",
                "surfaceContainerLowest", "surfaceContainerLow",
                "surfaceContainer", "surfaceContainerHigh", "surfaceContainerHighest"
            ),
            "Outlines" to listOf("outline", "outlineVariant", "scrim"),
            "Extended Signal" to listOf("spectrumGlow", "signalActive", "signalIdle", "signalError"),
            "Acoustic & Waterfall" to listOf(
                "signalLow", "signalMid", "signalHigh", "signalPeak",
                "noiseFloorLine", "syncMarker", "toneGuide"
            ),
            "Glow & Status" to listOf(
                "glowPrimary", "glowSecondary", "success", "onSuccess",
                "warning", "onWarning", "scanline"
            )
        )

        fun getColorForRole(colors: ColorTokens, role: String): Color {
            return when (role) {
                "primary" -> colors.primary
                "onPrimary" -> colors.onPrimary
                "primaryContainer" -> colors.primaryContainer
                "onPrimaryContainer" -> colors.onPrimaryContainer
                "secondary" -> colors.secondary
                "onSecondary" -> colors.onSecondary
                "secondaryContainer" -> colors.secondaryContainer
                "onSecondaryContainer" -> colors.onSecondaryContainer
                "tertiary" -> colors.tertiary
                "onTertiary" -> colors.onTertiary
                "tertiaryContainer" -> colors.tertiaryContainer
                "onTertiaryContainer" -> colors.onTertiaryContainer
                "error" -> colors.error
                "onError" -> colors.onError
                "errorContainer" -> colors.errorContainer
                "onErrorContainer" -> colors.onErrorContainer
                "background" -> colors.background
                "onBackground" -> colors.onBackground
                "surface" -> colors.surface
                "onSurface" -> colors.onSurface
                "surfaceVariant" -> colors.surfaceVariant
                "onSurfaceVariant" -> colors.onSurfaceVariant
                "surfaceContainerLowest" -> colors.surfaceContainerLowest
                "surfaceContainerLow" -> colors.surfaceContainerLow
                "surfaceContainer" -> colors.surfaceContainer
                "surfaceContainerHigh" -> colors.surfaceContainerHigh
                "surfaceContainerHighest" -> colors.surfaceContainerHighest
                "outline" -> colors.outline
                "outlineVariant" -> colors.outlineVariant
                "scrim" -> colors.scrim
                "inverseSurface" -> colors.inverseSurface
                "inverseOnSurface" -> colors.inverseOnSurface
                "inversePrimary" -> colors.inversePrimary
                "surfaceTint" -> colors.surfaceTint
                "spectrumGlow" -> colors.spectrumGlow
                "signalActive" -> colors.signalActive
                "signalIdle" -> colors.signalIdle
                "signalError" -> colors.signalError
                "signalLow" -> colors.signalLow
                "signalMid" -> colors.signalMid
                "signalHigh" -> colors.signalHigh
                "signalPeak" -> colors.signalPeak
                "noiseFloorLine" -> colors.noiseFloorLine
                "syncMarker" -> colors.syncMarker
                "toneGuide" -> colors.toneGuide
                "glowPrimary" -> colors.glowPrimary
                "glowSecondary" -> colors.glowSecondary
                "success" -> colors.success
                "onSuccess" -> colors.onSuccess
                "warning" -> colors.warning
                "onWarning" -> colors.onWarning
                "scanline" -> colors.scanline
                else -> colors.primary
            }
        }
    }
}
