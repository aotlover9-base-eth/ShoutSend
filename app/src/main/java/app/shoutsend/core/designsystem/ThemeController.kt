package app.shoutsend.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Manages runtime theme state, merging base design tokens with sparse developer overrides.
 */
class ThemeController(
    initialBaseTokens: DesignTokens = DesignTokens.defaultDark(),
    private val repository: TokenOverridesRepository,
    private val scope: CoroutineScope
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    private val _baseTokens = MutableStateFlow(initialBaseTokens)
    private var currentOverrides = TokenOverrides()

    val tokens: StateFlow<DesignTokens> = combine(_baseTokens, repository.overrides) { base, overrides ->
        currentOverrides = overrides
        applyOverrides(base, overrides)
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = applyOverrides(initialBaseTokens, TokenOverrides())
    )

    fun updateThemeMode(mode: ThemeMode) {
        val basePalette = when (mode) {
            ThemeMode.DARK -> DefaultTokens.CyberRadioDark
            ThemeMode.LIGHT -> DefaultTokens.CyberRadioLight
            ThemeMode.SYSTEM -> _baseTokens.value.colors
        }
        _baseTokens.value = _baseTokens.value.copy(
            themeMode = mode,
            colors = basePalette
        )
    }

    fun updateDynamicColor(enabled: Boolean) {
        _baseTokens.value = _baseTokens.value.copy(dynamicColor = enabled)
    }

    suspend fun setColor(role: String, color: Color) {
        require(role in TokenOverrides.ALLOWED_COLOR_ROLES) { "Invalid color role: $role" }
        repository.update { current ->
            current.copy(colors = current.colors + (role to color.value.toLong()))
        }
    }

    suspend fun setRadius(name: String, radiusDp: Float) {
        require(name in TokenOverrides.ALLOWED_RADIUS_NAMES) { "Invalid radius name: $name" }
        require(radiusDp in TokenOverrides.MIN_RADIUS_DP..TokenOverrides.MAX_RADIUS_DP) {
            "Radius $radiusDp out of range [${TokenOverrides.MIN_RADIUS_DP}, ${TokenOverrides.MAX_RADIUS_DP}]"
        }
        repository.update { current ->
            current.copy(radii = current.radii + (name to radiusDp))
        }
    }

    suspend fun resetColor(role: String) {
        repository.resetColor(role)
    }

    suspend fun resetRadius(name: String) {
        repository.resetRadius(name)
    }

    suspend fun resetAll() {
        repository.resetAll()
    }

    fun exportToJson(): String {
        return json.encodeToString(currentOverrides)
    }

    suspend fun importFromJson(jsonString: String): Result<Unit> {
        return runCatching {
            val parsed = json.decodeFromString<TokenOverrides>(jsonString)
            TokenOverrides.validate(parsed).getOrThrow()
            repository.update { parsed }
        }
    }

    private fun applyOverrides(base: DesignTokens, overrides: TokenOverrides): DesignTokens {
        if (overrides.isEmpty()) return base

        var colors = base.colors
        for ((role, value) in overrides.colors) {
            val c = Color(value.toULong())
            colors = when (role) {
                "primary" -> colors.copy(primary = c)
                "onPrimary" -> colors.copy(onPrimary = c)
                "primaryContainer" -> colors.copy(primaryContainer = c)
                "onPrimaryContainer" -> colors.copy(onPrimaryContainer = c)
                "secondary" -> colors.copy(secondary = c)
                "onSecondary" -> colors.copy(onSecondary = c)
                "secondaryContainer" -> colors.copy(secondaryContainer = c)
                "onSecondaryContainer" -> colors.copy(onSecondaryContainer = c)
                "tertiary" -> colors.copy(tertiary = c)
                "onTertiary" -> colors.copy(onTertiary = c)
                "tertiaryContainer" -> colors.copy(tertiaryContainer = c)
                "onTertiaryContainer" -> colors.copy(onTertiaryContainer = c)
                "error" -> colors.copy(error = c)
                "onError" -> colors.copy(onError = c)
                "errorContainer" -> colors.copy(errorContainer = c)
                "onErrorContainer" -> colors.copy(onErrorContainer = c)
                "background" -> colors.copy(background = c)
                "onBackground" -> colors.copy(onBackground = c)
                "surface" -> colors.copy(surface = c)
                "onSurface" -> colors.copy(onSurface = c)
                "surfaceVariant" -> colors.copy(surfaceVariant = c)
                "onSurfaceVariant" -> colors.copy(onSurfaceVariant = c)
                "surfaceContainerLowest" -> colors.copy(surfaceContainerLowest = c)
                "surfaceContainerLow" -> colors.copy(surfaceContainerLow = c)
                "surfaceContainer" -> colors.copy(surfaceContainer = c)
                "surfaceContainerHigh" -> colors.copy(surfaceContainerHigh = c)
                "surfaceContainerHighest" -> colors.copy(surfaceContainerHighest = c)
                "outline" -> colors.copy(outline = c)
                "outlineVariant" -> colors.copy(outlineVariant = c)
                "scrim" -> colors.copy(scrim = c)
                "inverseSurface" -> colors.copy(inverseSurface = c)
                "inverseOnSurface" -> colors.copy(inverseOnSurface = c)
                "inversePrimary" -> colors.copy(inversePrimary = c)
                "surfaceTint" -> colors.copy(surfaceTint = c)
                "spectrumGlow" -> colors.copy(spectrumGlow = c)
                "signalActive" -> colors.copy(signalActive = c)
                "signalIdle" -> colors.copy(signalIdle = c)
                "signalError" -> colors.copy(signalError = c)
                "signalLow" -> colors.copy(signalLow = c)
                "signalMid" -> colors.copy(signalMid = c)
                "signalHigh" -> colors.copy(signalHigh = c)
                "signalPeak" -> colors.copy(signalPeak = c)
                "noiseFloorLine" -> colors.copy(noiseFloorLine = c)
                "syncMarker" -> colors.copy(syncMarker = c)
                "toneGuide" -> colors.copy(toneGuide = c)
                "glowPrimary" -> colors.copy(glowPrimary = c)
                "glowSecondary" -> colors.copy(glowSecondary = c)
                "success" -> colors.copy(success = c)
                "onSuccess" -> colors.copy(onSuccess = c)
                "warning" -> colors.copy(warning = c)
                "onWarning" -> colors.copy(onWarning = c)
                "scanline" -> colors.copy(scanline = c)
                else -> colors
            }
        }

        var shapes = base.shapes
        for ((name, radiusDp) in overrides.radii) {
            val shape = RoundedCornerShape(radiusDp.dp)
            shapes = when (name) {
                "extraSmall" -> shapes.copy(extraSmall = shape)
                "small" -> shapes.copy(
                    small = shape,
                    button = shape,
                    textField = shape,
                    chip = shape
                )
                "medium" -> shapes.copy(
                    medium = shape,
                    card = shape
                )
                "large" -> shapes.copy(
                    large = shape,
                    fab = shape
                )
                "extraLarge" -> shapes.copy(
                    extraLarge = shape,
                    dialog = shape
                )
                else -> shapes
            }
        }

        return base.copy(colors = colors, shapes = shapes)
    }
}
