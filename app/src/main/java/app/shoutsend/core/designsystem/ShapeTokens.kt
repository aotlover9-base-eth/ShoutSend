package app.shoutsend.core.designsystem

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape

/**
 * Shape tokens defining corner radii and component shapes.
 */
data class ShapeTokens(
    val extraSmall: CornerBasedShape,
    val small: CornerBasedShape,
    val medium: CornerBasedShape,
    val large: CornerBasedShape,
    val extraLarge: CornerBasedShape,
    // Component shapes referencing base radii
    val button: Shape,
    val card: Shape,
    val dialog: Shape,
    val bottomSheet: Shape,
    val textField: Shape,
    val chip: Shape,
    val fab: Shape
) {
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
