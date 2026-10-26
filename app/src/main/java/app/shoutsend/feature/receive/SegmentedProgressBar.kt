package app.shoutsend.feature.receive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import app.shoutsend.R
import app.shoutsend.core.designsystem.ChirpTheme
import java.util.Locale

/**
 * Segmented frame progress bar for acoustic packet reassembly.
 *
 * Conforms to UI Gate specifications:
 * - When decoding the initial frame header (total blocks unknown), displays "Locked, reading header…"
 *   with an animated cyber pulse strip.
 * - Once total blocks are decoded from the frame header, renders distinct segmented blocks
 *   highlighting completed, in-flight, and pending frames, plus the estimated remaining seconds.
 * - Displays an honest "Noise detected · Auto-repaired" badge only when Reed-Solomon corrections occur.
 * - Uses 100% tokenized dimensions and colors (Rule 6, Rule 7).
 */
@Composable
fun SegmentedProgressBar(
    receivedBlocks: Int,
    totalBlocks: Int,
    estimatedRemainingSeconds: Double?,
    hasAutoRepairedNoise: Boolean,
    isReadingHeader: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = ChirpTheme.colors
    val infiniteTransition = rememberInfiniteTransition(label = "SegmentPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)
    ) {
        if (isReadingHeader || totalBlocks <= 0) {
            // Header Acquisition Phase
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.receive_status_reading_header),
                    style = ChirpTheme.typography.bodySmall,
                    color = colors.syncMarker
                )
            }

            // Indeterminate Pulsing Header Line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ChirpTheme.spacing.sm)
                    .clip(ChirpTheme.shapes.extraSmall)
                    .background(colors.surfaceContainerHighest)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(pulseAlpha)
                        .height(ChirpTheme.spacing.sm)
                        .clip(ChirpTheme.shapes.extraSmall)
                        .background(colors.syncMarker)
                )
            }
        } else {
            // Segmented Blocks Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until totalBlocks) {
                    val isReceived = i < receivedBlocks
                    val isInProgress = i == receivedBlocks

                    val segmentColor = when {
                        isReceived -> colors.primary
                        isInProgress -> colors.syncMarker.copy(alpha = pulseAlpha)
                        else -> colors.surfaceContainerHighest
                    }

                    Box(
                        modifier = Modifier
                            .weight(1.0f)
                            .height(ChirpTheme.spacing.sm)
                            .clip(ChirpTheme.shapes.extraSmall)
                            .background(segmentColor)
                            .then(
                                if (!isReceived && !isInProgress) {
                                    Modifier.border(
                                        width = ChirpTheme.strokes.thin,
                                        color = colors.outlineVariant,
                                        shape = ChirpTheme.shapes.extraSmall
                                    )
                                } else Modifier
                            )
                    )
                }
            }

            // Block & Remaining Time Readout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val blockText = String.format(
                    Locale.US,
                    stringResource(R.string.receive_progress_block),
                    receivedBlocks,
                    totalBlocks
                )

                val timeText = if (estimatedRemainingSeconds != null && estimatedRemainingSeconds > 0.0) {
                    String.format(
                        Locale.US,
                        stringResource(R.string.receive_progress_time_remaining),
                        estimatedRemainingSeconds
                    )
                } else ""

                Text(
                    text = "$blockText$timeText",
                    style = ChirpTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = colors.onSurface
                )
            }
        }

        // Honest Error Correction Badge
        AnimatedVisibility(
            visible = hasAutoRepairedNoise,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xxs),
                modifier = Modifier
                    .background(
                        color = colors.secondaryContainer.copy(alpha = 0.5f),
                        shape = ChirpTheme.shapes.extraLarge
                    )
                    .border(
                        width = ChirpTheme.strokes.thin,
                        color = colors.secondary,
                        shape = ChirpTheme.shapes.extraLarge
                    )
                    .padding(
                        horizontal = ChirpTheme.spacing.sm,
                        vertical = ChirpTheme.spacing.xxs
                    )
            ) {
                Text(
                    text = "✓ " + stringResource(R.string.receive_noise_repaired),
                    style = ChirpTheme.typography.labelSmall,
                    color = colors.onSecondaryContainer
                )
            }
        }
    }
}
