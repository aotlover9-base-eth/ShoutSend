package app.shoutsend.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Manages runtime theme state, merging base design tokens with sparse developer overrides.
 */
class ThemeController(
    initialBaseTokens: DesignTokens = DesignTokens.default(),
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
        _baseTokens.value = _baseTokens.value.copy(themeMode = mode)
        scope.launch {
            repository.update { it.copy(themeMode = mode.name) }
        }
    }

    fun updateDynamicColor(enabled: Boolean) {
        _baseTokens.value = _baseTokens.value.copy(dynamicColor = enabled)
        scope.launch {
            repository.update { it.copy(dynamicColor = enabled) }
        }
    }

    suspend fun setSeedColor(color: Long) {
        repository.update { it.copy(seedColor = color) }
    }

    suspend fun setContrast(contrast: Float) {
        val clamped = contrast.coerceIn(-1f, 1f)
        repository.update { it.copy(contrast = clamped) }
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

    suspend fun setHeroShape(shapeName: String) {
        require(shapeName in TokenOverrides.ALLOWED_HERO_SHAPES) { "Invalid hero shape: $shapeName" }
        repository.update { it.copy(heroShape = shapeName) }
    }

    suspend fun resetRadius(name: String) {
        repository.resetRadius(name)
    }

    // Compatibility method for existing tests/callers
    suspend fun setColor(role: String, color: Color) {
        if (role == "primary" || role == "seed") {
            setSeedColor(color.value.toLong())
        }
    }

    suspend fun resetColor(role: String) {
        repository.resetColor(role)
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
        val mode = when (overrides.themeMode.uppercase()) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }

        val dynamicColor = overrides.dynamicColor
        val seedColor = overrides.seedColor
        val contrast = overrides.contrast
        val heroShape = overrides.heroShape

        var shapes = DefaultTokens.Shapes
        if (overrides.radii.isNotEmpty()) {
            val xs = overrides.radii["extraSmall"]?.let { RoundedCornerShape(it.dp) } ?: shapes.extraSmall
            val sm = overrides.radii["small"]?.let { RoundedCornerShape(it.dp) } ?: shapes.small
            val md = overrides.radii["medium"]?.let { RoundedCornerShape(it.dp) } ?: shapes.medium
            val lg = overrides.radii["large"]?.let { RoundedCornerShape(it.dp) } ?: shapes.large
            val lgInc = overrides.radii["largeIncreased"]?.let { RoundedCornerShape(it.dp) } ?: shapes.largeIncreased
            val xl = overrides.radii["extraLarge"]?.let { RoundedCornerShape(it.dp) } ?: shapes.extraLarge
            val xlInc = overrides.radii["extraLargeIncreased"]?.let { RoundedCornerShape(it.dp) } ?: shapes.extraLargeIncreased

            shapes = shapes.copy(
                extraSmall = xs,
                small = sm,
                medium = md,
                large = lg,
                largeIncreased = lgInc,
                extraLarge = xl,
                extraLargeIncreased = xlInc,
                card = lg,
                dialog = xl,
                bottomSheet = RoundedCornerShape(
                    topStart = overrides.radii["extraLarge"]?.let { it.dp } ?: 32.dp,
                    topEnd = overrides.radii["extraLarge"]?.let { it.dp } ?: 32.dp
                ),
                textField = lg,
                chip = md,
                fab = lg
            )
        }

        val isDark = mode == ThemeMode.DARK
        val scheme = dynamicColorScheme(
            seedColor = Color(seedColor),
            isDark = isDark,
            isAmoled = false,
            style = PaletteStyle.TonalSpot,
            contrastLevel = contrast.toDouble()
        )
        val ext = ExtendedColors.build(scheme, isDark)

        return base.copy(
            themeMode = mode,
            dynamicColor = dynamicColor,
            seedColor = seedColor,
            contrast = contrast,
            heroShape = heroShape,
            shapes = shapes,
            colors = ColorTokens(scheme, ext),
            extendedColors = ext
        )
    }
}
