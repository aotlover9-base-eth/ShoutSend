package app.shoutsend.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import app.shoutsend.core.designsystem.ChirpTheme

/**
 * Payload byte budget meter showing bytes utilized out of the 320-byte air frame limit
 * along with the calculated transmission duration in seconds.
 */
@Composable
fun ByteMeter(
    bytesUsed: Int,
    modifier: Modifier = Modifier,
    maxBytes: Int = 320,
    estimatedDurationSeconds: Double = 0.0
) {
    val fraction = (bytesUsed.toFloat() / maxBytes.toFloat()).coerceIn(0.0f, 1.0f)
    val isOverLimit = bytesUsed > maxBytes
    val isNearLimit = bytesUsed > 280

    val barColor = when {
        isOverLimit -> ChirpTheme.colors.signalError
        isNearLimit -> ChirpTheme.colors.warning
        else -> ChirpTheme.colors.primary
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$bytesUsed / $maxBytes B",
                style = ChirpTheme.typography.radioReadoutMedium,
                color = barColor
            )

            Spacer(modifier = Modifier.weight(1.0f))

            if (estimatedDurationSeconds > 0.0) {
                Text(
                    text = "~%.1f s".format(estimatedDurationSeconds),
                    style = ChirpTheme.typography.radioReadoutSmall,
                    color = ChirpTheme.colors.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(ChirpTheme.spacing.xs))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ChirpTheme.spacing.xs)
                .clip(ChirpTheme.shapes.extraSmall)
                .background(ChirpTheme.colors.surfaceContainerHighest)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(barColor)
            )
        }
    }
}
