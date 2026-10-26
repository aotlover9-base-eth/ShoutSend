package app.shoutsend.core.designsystem.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.shoutsend.core.designsystem.ChirpTheme

enum class AcousticStatus {
    IDLE,
    LISTENING,
    LOCKED,
    RECEIVING,
    SUCCESS,
    ERROR
}

/**
 * Capsule status pill displaying acoustic state with tokenized colors and an animated pulse indicator.
 */
@Composable
fun StatusPill(
    status: AcousticStatus,
    modifier: Modifier = Modifier,
    customLabel: String? = null
) {
    val (statusColor, defaultLabel) = when (status) {
        AcousticStatus.IDLE -> ChirpTheme.colors.signalIdle to "IDLE"
        AcousticStatus.LISTENING -> ChirpTheme.colors.signalActive to "LISTENING"
        AcousticStatus.LOCKED -> ChirpTheme.colors.syncMarker to "LOCKED"
        AcousticStatus.RECEIVING -> ChirpTheme.colors.primary to "RECEIVING"
        AcousticStatus.SUCCESS -> ChirpTheme.colors.success to "COMPLETE"
        AcousticStatus.ERROR -> ChirpTheme.colors.signalError to "ERROR"
    }

    val isReducedMotion = ChirpTheme.isReducedMotion
    val dotAlpha = if (isReducedMotion || status == AcousticStatus.IDLE) {
        1.0f
    } else {
        val transition = rememberInfiniteTransition(label = "pulseTransition")
        val alpha by transition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(ChirpTheme.motion.durationExtraLong1),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
        alpha
    }

    val labelText = customLabel ?: defaultLabel

    Row(
        modifier = modifier
            .semantics {
                contentDescription = "Status: $labelText"
            }
            .clip(ChirpTheme.shapes.extraLarge)
            .background(ChirpTheme.colors.surfaceContainerHigh)
            .border(
                width = ChirpTheme.strokes.thin,
                color = statusColor.copy(alpha = ChirpTheme.opacity.glow),
                shape = ChirpTheme.shapes.extraLarge
            )
            .padding(
                horizontal = ChirpTheme.spacing.sm,
                vertical = ChirpTheme.spacing.xs
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(ChirpTheme.spacing.sm)
                .alpha(dotAlpha)
                .clip(CircleShape)
                .background(statusColor)
        )

        Spacer(modifier = Modifier.width(ChirpTheme.spacing.xs))

        Text(
            text = labelText,
            style = ChirpTheme.typography.radioReadoutSmall,
            color = statusColor
        )
    }
}
