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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.shoutsend.core.designsystem.ShoutSendTheme

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
        AcousticStatus.IDLE -> MaterialTheme.colorScheme.outline to "Idle"
        AcousticStatus.LISTENING -> MaterialTheme.colorScheme.primary to "Listening"
        AcousticStatus.LOCKED -> MaterialTheme.colorScheme.tertiary to "Locked"
        AcousticStatus.RECEIVING -> MaterialTheme.colorScheme.primary to "Receiving"
        AcousticStatus.SUCCESS -> ShoutSendTheme.colors.success to "Complete"
        AcousticStatus.ERROR -> MaterialTheme.colorScheme.error to "Error"
    }

    val isReducedMotion = ShoutSendTheme.isReducedMotion
    val dotAlpha = if (isReducedMotion || status == AcousticStatus.IDLE) {
        1.0f
    } else {
        val transition = rememberInfiniteTransition(label = "pulseTransition")
        val alpha by transition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(ShoutSendTheme.motion.durationExtraLong1),
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
            .clip(ShoutSendTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(
                width = ShoutSendTheme.strokes.thin,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = ShoutSendTheme.shapes.extraLarge
            )
            .padding(
                horizontal = ShoutSendTheme.spacing.sm,
                vertical = ShoutSendTheme.spacing.xs
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(ShoutSendTheme.spacing.sm)
                .alpha(dotAlpha)
                .clip(CircleShape)
                .background(statusColor)
        )

        Spacer(modifier = Modifier.width(ShoutSendTheme.spacing.xs))

        Text(
            text = labelText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
