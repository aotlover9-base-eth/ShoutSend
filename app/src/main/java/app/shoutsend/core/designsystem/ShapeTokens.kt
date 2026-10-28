package app.shoutsend.core.designsystem

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Shapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import androidx.graphics.shapes.RoundedPolygon

/**
 * Shape tokens adhering to the Material 3 Expressive scale.
 */
data class ShapeTokens(
    val extraSmall: CornerBasedShape,
    val small: CornerBasedShape,
    val medium: CornerBasedShape,
    val large: CornerBasedShape,
    val largeIncreased: CornerBasedShape,
    val extraLarge: CornerBasedShape,
    val extraLargeIncreased: CornerBasedShape,
    val full: Shape,
    // Component shape bindings
    val button: Shape,
    val card: Shape,
    val dialog: Shape,
    val bottomSheet: Shape,
    val textField: Shape,
    val chip: Shape,
    val fab: Shape
) {
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    fun toMaterialShapes(): Shapes {
        return Shapes(
            extraSmall = extraSmall,
            small = small,
            medium = medium,
            large = large,
            extraLarge = extraLarge
        )
    }
}

/**
 * Registry of expressive hero shapes exposed via [MaterialShapes].
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object ShoutShapes {
    val Cookie9Sided: RoundedPolygon get() = MaterialShapes.Cookie9Sided
    val Sunny: RoundedPolygon get() = MaterialShapes.Sunny
    val Clover4Leaf: RoundedPolygon get() = MaterialShapes.Clover4Leaf
    val Pill: RoundedPolygon get() = MaterialShapes.Pill
    val SoftBurst: RoundedPolygon get() = MaterialShapes.SoftBurst
    val Heart: RoundedPolygon get() = MaterialShapes.Heart
    val Flower: RoundedPolygon get() = MaterialShapes.Flower

    val AVAILABLE_HERO_SHAPES: Map<String, RoundedPolygon> = mapOf(
        "Cookie9Sided" to Cookie9Sided,
        "Sunny" to Sunny,
        "Clover4Leaf" to Clover4Leaf,
        "Pill" to Pill,
        "SoftBurst" to SoftBurst,
        "Heart" to Heart,
        "Flower" to Flower
    )

    fun getHeroPolygon(name: String): RoundedPolygon {
        return AVAILABLE_HERO_SHAPES[name] ?: Cookie9Sided
    }

    @Composable
    fun getHeroShape(name: String): Shape {
        return getHeroPolygon(name).toShape()
    }
}
