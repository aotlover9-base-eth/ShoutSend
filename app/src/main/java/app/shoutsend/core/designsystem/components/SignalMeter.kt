package app.shoutsend.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.shoutsend.core.designsystem.ChirpTheme

/**
 * Segmented SNR meter displaying signal strength relative to ambient room noise.
 *
 * Employs [ChirpTheme.radioVisuals.meterSegmentCount] vertical bars filling progressively
 * based on normalized SNR (0 to 24 dB).
 */
@Composable
fun SignalMeter(
    snrDb: Double,
    modifier: Modifier = Modifier,
    maxSnrDb: Double = 24.0,
    showReadout: Boolean = true
) {
    val totalSegments = ChirpTheme.radioVisuals.meterSegmentCount
    val normalized = (snrDb / maxSnrDb).coerceIn(0.0, 1.0)
    val activeSegments = (normalized * totalSegments).toInt()

    Column(
        modifier = modifier.semantics {
            contentDescription = "Signal to noise ratio: ${snrDb.toInt()} decibels"
        },
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xxs),
            verticalAlignment = Alignment.Bottom
        ) {
            for (i in 0 until totalSegments) {
                val isActive = i < activeSegments
                val segmentColor = when {
                    !isActive -> ChirpTheme.colors.signalIdle.copy(alpha = ChirpTheme.opacity.subtle)
                    i > totalSegments * 3 / 4 -> ChirpTheme.colors.signalPeak
                    i > totalSegments / 2 -> ChirpTheme.colors.signalHigh
                    else -> ChirpTheme.colors.signalActive
                }

                // Progressive height increase across meter segments
                val segmentHeight = ChirpTheme.spacing.sm + (ChirpTheme.spacing.md * i / totalSegments)

                Box(
                    modifier = Modifier
                        .width(ChirpTheme.spacing.xs)
                        .height(segmentHeight)
                        .clip(ChirpTheme.shapes.extraSmall)
                        .background(segmentColor)
                )
            }
        }

        if (showReadout) {
            Spacer(modifier = Modifier.height(ChirpTheme.spacing.xxs))
            Text(
                text = "${snrDb.toInt()} dB SNR",
                style = ChirpTheme.typography.radioReadoutSmall,
                color = if (snrDb >= 6.0) ChirpTheme.colors.signalActive else ChirpTheme.colors.signalIdle
            )
        }
    }
}
