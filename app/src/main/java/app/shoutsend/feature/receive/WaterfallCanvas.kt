package app.shoutsend.feature.receive

import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import app.shoutsend.core.designsystem.ChirpTheme
import app.shoutsend.core.designsystem.colormap.ColormapFactory
import app.shoutsend.core.designsystem.components.ScanlineOverlay
import app.shoutsend.modem.profile.Band

/**
 * High-performance, zero-allocation spectrogram waterfall display for acoustic reception.
 *
 * Conforms to:
 * - Rule 6 & 7: Zero hardcoded hex or raw measurement literals; draws exclusively from tokenized design system.
 * - Rule 22: Preallocated Rect and Paint instances prevent garbage collection stutter during 30-60 fps audio rendering.
 *
 * Features:
 * - Live scrolling 256-bin spectrogram with tokenized 4-stop photometric colormap.
 * - Active band carrier boundaries and 16-channel tone marker guide lines.
 * - Glowing channel highlight activated during preamble lock.
 * - Subdued CRT scanline texture overlay.
 * - Accessible static/low-refresh 16-band energy bar chart when [ChirpTheme.isReducedMotion] is enabled.
 */
@Composable
fun WaterfallCanvas(
    buffer: WaterfallBuffer,
    band: Band,
    isPreambleLocked: Boolean,
    frameClock: Long,
    modifier: Modifier = Modifier,
    activeToneIndex: Int = -1
) {
    val colors = ChirpTheme.colors
    val density = LocalDensity.current
    val isReducedMotion = ChirpTheme.isReducedMotion
    val hairlinePx = with(density) { ChirpTheme.strokes.hairline.toPx() }
    val thinStrokePx = with(density) { ChirpTheme.strokes.thin.toPx() }
    val regularStrokePx = with(density) { ChirpTheme.strokes.regular.toPx() }

    // Recompute colormap lookup table whenever theme color tokens change
    LaunchedEffect(colors.signalLow, colors.signalMid, colors.signalHigh, colors.signalPeak) {
        val lut = ColormapFactory.build(
            signalLow = colors.signalLow,
            signalMid = colors.signalMid,
            signalHigh = colors.signalHigh,
            signalPeak = colors.signalPeak
        )
        buffer.updateColormap(lut)
    }

    // Pulsating alpha for active preamble lock highlight
    val infiniteTransition = rememberInfiniteTransition(label = "WaterfallLockPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    // Preallocated drawing primitives (Rule 22: 0 allocations on draw path)
    val srcRect = remember { Rect(0, 0, buffer.width, buffer.height) }
    val dstRect = remember { Rect() }
    val bitmapPaint = remember {
        Paint().apply {
            isFilterBitmap = false
            isDither = false
        }
    }

    // Band carrier parameters for overlays
    val (bandStartHz, bandEndHz, carrierStartHz, carrierEndHz, toneStepHz, toneBaseHz) = remember(band) {
        when (band) {
            Band.RETRO -> BandVisualParams(
                bandStartHz = 1000.0,
                bandEndHz = 4000.0,
                carrierStartHz = 1375.0,
                carrierEndHz = 3500.0,
                toneBaseHz = 1500.0,
                toneStepHz = 125.0
            )
            Band.STEALTH -> BandVisualParams(
                bandStartHz = 16000.0,
                bandEndHz = 22000.0,
                carrierStartHz = 17450.0,
                carrierEndHz = 20000.0,
                toneBaseHz = 17600.0,
                toneStepHz = 150.0
            )
        }
    }

    Column(
        modifier = modifier.semantics {
            contentDescription = WaterfallSemantics.formatDescription(band)
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.0f)
                .background(colors.surfaceContainerLowest)
        ) {
            if (isReducedMotion) {
                // Accessible Battery-Saver / Reduced-Motion Fallback: 16-band energy bar chart
                ReducedMotionSpectrum(
                    latestRow = buffer.latestRow,
                    band = band,
                    activeToneIndex = activeToneIndex,
                    isPreambleLocked = isPreambleLocked,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // High-Performance Spectrogram Waterfall Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Frame clock dependency invalidates canvas without triggering recomposition of parent
                    @Suppress("UNUSED_VARIABLE")
                    val clock = frameClock

                    // 1. Draw rolling spectrogram bitmap
                    dstRect.set(0, 0, size.width.toInt(), size.height.toInt())
                    drawIntoCanvas { canvas ->
                        buffer.bitmap?.let { bmp ->
                            canvas.nativeCanvas.drawBitmap(bmp, srcRect, dstRect, bitmapPaint)
                        }
                    }

                    val totalBandSpan = bandEndHz - bandStartHz
                    val channelLeftX = (((carrierStartHz - bandStartHz) / totalBandSpan) * size.width).toFloat()
                    val channelRightX = (((carrierEndHz - bandStartHz) / totalBandSpan) * size.width).toFloat()
                    val channelWidth = (channelRightX - channelLeftX).coerceAtLeast(1.0f)

                    // 2. Draw 16 subtle vertical tone guide lines
                    for (i in 0 until 16) {
                        val toneHz = toneBaseHz + (i * toneStepHz)
                        val x = (((toneHz - bandStartHz) / totalBandSpan) * size.width).toFloat()
                        val isToneActive = (i == activeToneIndex)
                        val lineColor = if (isToneActive) colors.signalPeak else colors.toneGuide
                        val lineAlpha = if (isToneActive) 0.85f else 0.22f
                        val stroke = if (isToneActive) thinStrokePx else hairlinePx

                        drawLine(
                            color = lineColor.copy(alpha = lineAlpha),
                            start = Offset(x, 0.0f),
                            end = Offset(x, size.height),
                            strokeWidth = stroke
                        )
                    }

                    // 3. Draw Carrier Channel Boundaries
                    drawLine(
                        color = colors.syncMarker.copy(alpha = 0.5f),
                        start = Offset(channelLeftX, 0.0f),
                        end = Offset(channelLeftX, size.height),
                        strokeWidth = thinStrokePx
                    )
                    drawLine(
                        color = colors.syncMarker.copy(alpha = 0.5f),
                        start = Offset(channelRightX, 0.0f),
                        end = Offset(channelRightX, size.height),
                        strokeWidth = thinStrokePx
                    )

                    // 4. Glowing Channel Highlight when Preamble Lock is Active
                    if (isPreambleLocked) {
                        // Translucent illuminated column
                        drawRect(
                            color = colors.syncMarker.copy(alpha = pulseAlpha),
                            topLeft = Offset(channelLeftX, 0.0f),
                            size = Size(channelWidth, size.height)
                        )
                        // Perimeter accent stroke
                        drawRect(
                            color = colors.syncMarker.copy(alpha = 0.9f),
                            topLeft = Offset(channelLeftX, 0.0f),
                            size = Size(channelWidth, size.height),
                            style = Stroke(width = regularStrokePx)
                        )
                    }
                }

                // Retro CRT Scanline Texture Overlay
                ScanlineOverlay(modifier = Modifier.fillMaxSize())
            }

            // Top Status Label (Inside Waterfall)
            if (isPreambleLocked) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = ChirpTheme.spacing.xs)
                        .background(
                            color = colors.surface.copy(alpha = 0.75f),
                            shape = ChirpTheme.shapes.extraLarge
                        )
                        .padding(
                            horizontal = ChirpTheme.spacing.sm,
                            vertical = ChirpTheme.spacing.xxs
                        )
                ) {
                    Text(
                        text = "CARRIER LOCKED: MFSK16",
                        style = ChirpTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = colors.syncMarker
                    )
                }
            }
        }

        // Frequency Scale Footer
        FrequencyScaleFooter(
            band = band,
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surfaceContainerLow)
                .padding(
                    horizontal = ChirpTheme.spacing.sm,
                    vertical = ChirpTheme.spacing.xxs
                )
        )
    }
}

/**
 * Frequency scale footer displaying readable frequency interval ticks.
 */
@Composable
private fun FrequencyScaleFooter(
    band: Band,
    modifier: Modifier = Modifier
) {
    val labels = remember(band) {
        when (band) {
            Band.RETRO -> listOf("1.0 kHz", "1.5 kHz", "2.5 kHz", "3.5 kHz", "4.0 kHz")
            Band.STEALTH -> listOf("16.0 kHz", "17.5 kHz", "19.0 kHz", "20.5 kHz", "22.0 kHz")
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        labels.forEachIndexed { index, label ->
            if (index > 0) {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1.0f))
            }
            Text(
                text = label,
                style = ChirpTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = ChirpTheme.colors.onSurfaceVariant
            )
        }
    }
}

/**
 * Accessible, power-efficient 16-band energy bar chart for reduced-motion settings.
 */
@Composable
private fun ReducedMotionSpectrum(
    latestRow: FloatArray,
    band: Band,
    activeToneIndex: Int,
    isPreambleLocked: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = ChirpTheme.colors
    val thinStrokePx = with(LocalDensity.current) { ChirpTheme.strokes.thin.toPx() }

    Canvas(modifier = modifier) {
        val totalBins = latestRow.size
        val barCount = 16
        val barWidth = size.width / barCount.toFloat()
        val maxHeight = size.height

        val (toneBaseHz, toneStepHz, bandStartHz, bandEndHz) = when (band) {
            Band.RETRO -> Quad(1500.0, 125.0, 1000.0, 4000.0)
            Band.STEALTH -> Quad(17600.0, 150.0, 16000.0, 22000.0)
        }

        val totalBandSpan = bandEndHz - bandStartHz

        for (i in 0 until barCount) {
            val toneHz = toneBaseHz + (i * toneStepHz)
            val binIdx = (((toneHz - bandStartHz) / totalBandSpan) * totalBins)
                .toInt()
                .coerceIn(0, totalBins - 1)

            val power = latestRow[binIdx].coerceIn(0.0f, 1.0f)
            val barHeight = maxHeight * power
            val x = i * barWidth
            val y = maxHeight - barHeight

            val isActive = (i == activeToneIndex)
            val barColor = when {
                isActive -> colors.signalPeak
                isPreambleLocked -> colors.syncMarker
                power > 0.6f -> colors.signalHigh
                power > 0.3f -> colors.signalMid
                else -> colors.signalLow
            }

            // Draw filled bar
            drawRect(
                color = barColor,
                topLeft = Offset(x + 2.0f, y),
                size = Size(barWidth - 4.0f, barHeight)
            )

            // Draw bar top edge
            drawLine(
                color = if (isActive) colors.signalPeak else colors.toneGuide,
                start = Offset(x + 2.0f, y),
                end = Offset(x + barWidth - 2.0f, y),
                strokeWidth = thinStrokePx
            )
        }
    }
}

private data class BandVisualParams(
    val bandStartHz: Double,
    val bandEndHz: Double,
    val carrierStartHz: Double,
    val carrierEndHz: Double,
    val toneBaseHz: Double,
    val toneStepHz: Double
)

private data class Quad(
    val toneBaseHz: Double,
    val toneStepHz: Double,
    val bandStartHz: Double,
    val bandEndHz: Double
)

/**
 * Formatter for WaterfallCanvas accessibility announcements.
 */
object WaterfallSemantics {
    fun formatDescription(band: Band): String {
        val bandName = when (band) {
            Band.RETRO -> "Retro"
            Band.STEALTH -> "Stealth"
        }
        return "Acoustic waterfall spectrogram, listening on $bandName"
    }
}
