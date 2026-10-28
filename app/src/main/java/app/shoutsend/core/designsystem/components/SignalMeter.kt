package app.shoutsend.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.shoutsend.core.designsystem.ShoutSendTheme

/**
 * Segmented SNR meter displaying signal strength relative to ambient room noise.
 * Built on Material 3 tonal surfaces with zero neon glow.
 */
@Composable
fun SignalMeter(
    snrDb: Double,
    modifier: Modifier = Modifier,
    maxSnrDb: Double = 24.0,
    showReadout: Boolean = true
) {
    val totalSegments = ShoutSendTheme.acousticVisuals.meterSegmentCount
    val normalized = (snrDb / maxSnrDb).coerceIn(0.0, 1.0)
    val activeSegments = (normalized * totalSegments).toInt()

    Column(
        modifier = modifier.semantics {
            contentDescription = "Signal to noise ratio: ${snrDb.toInt()} decibels"
        },
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(ShoutSendTheme.spacing.xxs),
            verticalAlignment = Alignment.Bottom
        ) {
            for (i in 0 until totalSegments) {
                val isActive = i < activeSegments
                val segmentColor = when {
                    !isActive -> MaterialTheme.colorScheme.surfaceVariant
                    i > totalSegments * 3 / 4 -> MaterialTheme.colorScheme.secondary
                    i > totalSegments / 2 -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.primaryContainer
                }

                val segmentHeight = ShoutSendTheme.spacing.sm + (ShoutSendTheme.spacing.md * i / totalSegments)

                Box(
                    modifier = Modifier
                        .width(ShoutSendTheme.spacing.xs)
                        .height(segmentHeight)
                        .clip(ShoutSendTheme.shapes.extraSmall)
                        .background(segmentColor)
                )
            }
        }

        if (showReadout) {
            Spacer(modifier = Modifier.height(ShoutSendTheme.spacing.xxs))
            Text(
                text = "${snrDb.toInt()} dB SNR",
                style = MaterialTheme.typography.labelSmall,
                color = if (snrDb >= 6.0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
