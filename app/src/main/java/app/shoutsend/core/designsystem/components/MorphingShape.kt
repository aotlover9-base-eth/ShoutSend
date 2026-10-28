package app.shoutsend.core.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import app.shoutsend.core.designsystem.ShoutSendTheme

/**
 * Animated morphing shape between two [RoundedPolygon] configurations using [Morph].
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MorphingShape(
    startPolygon: RoundedPolygon,
    endPolygon: RoundedPolygon,
    modifier: Modifier = Modifier,
    targetEnd: Boolean = false,
    color: Color = MaterialTheme.colorScheme.primaryContainer,
    content: @Composable () -> Unit = {}
) {
    val morph = remember(startPolygon, endPolygon) {
        Morph(startPolygon, endPolygon)
    }
    val progress = remember { Animatable(if (targetEnd) 1f else 0f) }
    val isReducedMotion = ShoutSendTheme.isReducedMotion

    LaunchedEffect(targetEnd, isReducedMotion) {
        val target = if (targetEnd) 1f else 0f
        if (isReducedMotion) {
            progress.snapTo(target)
        } else {
            progress.animateTo(
                targetValue = target,
                animationSpec = spring(
                    dampingRatio = 0.8f,
                    stiffness = 380f
                )
            )
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = Path()
            morph.toPath(progress = progress.value, path = path)

            val matrix = Matrix()
            matrix.scale(size.width, size.height)
            path.transform(matrix)

            val bounds = path.getBounds()
            val center = Offset(size.width / 2f, size.height / 2f)
            path.translate(center - bounds.center)

            drawPath(path = path, color = color)
        }
        content()
    }
}
