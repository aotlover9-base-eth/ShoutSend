package app.shoutsend.feature.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.shoutsend.core.designsystem.ChirpTheme

/**
 * Page 1 Canvas Illustration: Two device silhouettes transmitting concentric acoustic wave ripples.
 */
@Composable
fun TalkInSoundIllustration(
    modifier: Modifier = Modifier
) {
    val isReducedMotion = ChirpTheme.isReducedMotion
    val primaryColor = ChirpTheme.colors.primary
    val outlineColor = ChirpTheme.colors.outline
    val strokeThin = ChirpTheme.strokes.thin
    val strokeRegular = ChirpTheme.strokes.regular
    val glowOpacity = ChirpTheme.opacity.glow

    val phase = if (isReducedMotion) {
        0.5f
    } else {
        val transition = rememberInfiniteTransition(label = "soundWaveTransition")
        val progress by transition.animateFloat(
            initialValue = 0.0f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = ChirpTheme.motion.durationExtraLong2,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "soundWavePhase"
        )
        progress
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .semantics {
                contentDescription = "Two smartphones communicating via sound waves."
            }
    ) {
        val w = size.width
        val h = size.height
        val phoneWidth = w * 0.22f
        val phoneHeight = h * 0.65f
        val phoneCorner = phoneWidth * 0.2f
        val thinPx = strokeThin.toPx()
        val regPx = strokeRegular.toPx()

        // Left Phone (Transmitter)
        val leftPhoneRect = RoundRect(
            rect = Rect(
                offset = Offset(w * 0.10f, h * 0.18f),
                size = Size(phoneWidth, phoneHeight)
            ),
            cornerRadius = CornerRadius(phoneCorner, phoneCorner)
        )
        val leftPath = Path().apply { addRoundRect(leftPhoneRect) }
        drawPath(leftPath, outlineColor, style = Stroke(width = thinPx))

        // Right Phone (Receiver)
        val rightPhoneRect = RoundRect(
            rect = Rect(
                offset = Offset(w * 0.68f, h * 0.18f),
                size = Size(phoneWidth, phoneHeight)
            ),
            cornerRadius = CornerRadius(phoneCorner, phoneCorner)
        )
        val rightPath = Path().apply { addRoundRect(rightPhoneRect) }
        drawPath(rightPath, outlineColor, style = Stroke(width = thinPx))

        // Acoustic Wave Ripples between phones
        val waveCenter = Offset(w * 0.25f, h * 0.50f)
        val maxWaveRadius = w * 0.45f
        val waveCount = 4

        for (i in 0 until waveCount) {
            val ringFrac = if (isReducedMotion) {
                (i + 1).toFloat() / waveCount.toFloat()
            } else {
                ((phase + i.toFloat() / waveCount.toFloat()) % 1.0f)
            }
            val radius = maxWaveRadius * ringFrac
            val alpha = (1.0f - ringFrac).coerceIn(0.0f, 1.0f) * glowOpacity

            drawArc(
                color = primaryColor.copy(alpha = alpha),
                startAngle = -45.0f,
                sweepAngle = 90.0f,
                useCenter = false,
                topLeft = Offset(waveCenter.x - radius, waveCenter.y - radius),
                size = Size(radius * 2.0f, radius * 2.0f),
                style = Stroke(width = regPx)
            )
        }
    }
}

/**
 * Page 2 Canvas Illustration: 3-step pipeline icons (Emit, Listen, Verify).
 */
@Composable
fun HowItWorksIllustration(
    modifier: Modifier = Modifier
) {
    val primaryColor = ChirpTheme.colors.primary
    val secondaryColor = ChirpTheme.colors.secondary
    val outlineVariant = ChirpTheme.colors.outlineVariant
    val strokeThin = ChirpTheme.strokes.thin
    val strokeRegular = ChirpTheme.strokes.regular

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .semantics {
                contentDescription = "Three step workflow: emit acoustic soundwave, listen with receiver, restore verified data."
            }
    ) {
        val w = size.width
        val h = size.height
        val centerY = h * 0.50f
        val regPx = strokeRegular.toPx()
        val thinPx = strokeThin.toPx()
        val radius = w * 0.10f

        val step1Center = Offset(w * 0.20f, centerY)
        val step2Center = Offset(w * 0.50f, centerY)
        val step3Center = Offset(w * 0.80f, centerY)

        // Step 1: Emitter circle
        drawCircle(color = primaryColor, radius = radius, center = step1Center, style = Stroke(regPx))
        drawCircle(color = primaryColor, radius = radius * 0.35f, center = step1Center)

        // Connecting arrow 1 -> 2
        drawLine(
            color = outlineVariant,
            start = Offset(step1Center.x + radius + w * 0.02f, centerY),
            end = Offset(step2Center.x - radius - w * 0.02f, centerY),
            strokeWidth = thinPx
        )

        // Step 2: Listener concentric rings
        drawCircle(color = secondaryColor, radius = radius, center = step2Center, style = Stroke(regPx))
        drawCircle(color = secondaryColor, radius = radius * 0.6f, center = step2Center, style = Stroke(thinPx))

        // Connecting arrow 2 -> 3
        drawLine(
            color = outlineVariant,
            start = Offset(step2Center.x + radius + w * 0.02f, centerY),
            end = Offset(step3Center.x - radius - w * 0.02f, centerY),
            strokeWidth = thinPx
        )

        // Step 3: Verified Checkmark Circle
        drawCircle(color = primaryColor, radius = radius, center = step3Center, style = Stroke(regPx))
        val checkPath = Path().apply {
            moveTo(step3Center.x - radius * 0.4f, step3Center.y)
            lineTo(step3Center.x - radius * 0.1f, step3Center.y + radius * 0.35f)
            lineTo(step3Center.x + radius * 0.45f, step3Center.y - radius * 0.35f)
        }
        drawPath(checkPath, color = primaryColor, style = Stroke(width = regPx))
    }
}

/**
 * Page 3 Canvas Illustration: Speaker cone with acoustic wavefronts and safety shield.
 */
@Composable
fun GoodToKnowIllustration(
    modifier: Modifier = Modifier
) {
    val primaryColor = ChirpTheme.colors.primary
    val warningColor = ChirpTheme.colors.warning
    val strokeThin = ChirpTheme.strokes.thin
    val strokeRegular = ChirpTheme.strokes.regular

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .semantics {
                contentDescription = "Speaker with sound waves and privacy safety shield."
            }
    ) {
        val w = size.width
        val h = size.height
        val center = Offset(w * 0.45f, h * 0.50f)
        val regPx = strokeRegular.toPx()
        val thinPx = strokeThin.toPx()

        // Speaker cone
        val speakerPath = Path().apply {
            moveTo(center.x - w * 0.16f, center.y - h * 0.12f)
            lineTo(center.x - w * 0.06f, center.y - h * 0.12f)
            lineTo(center.x + w * 0.08f, center.y - h * 0.25f)
            lineTo(center.x + w * 0.08f, center.y + h * 0.25f)
            lineTo(center.x - w * 0.06f, center.y + h * 0.12f)
            lineTo(center.x - w * 0.16f, center.y + h * 0.12f)
            close()
        }
        drawPath(speakerPath, primaryColor, style = Stroke(width = regPx))

        // Sound waves radiating outward
        for (i in 1..3) {
            val waveDist = w * 0.08f * i
            drawArc(
                color = warningColor,
                startAngle = -35.0f,
                sweepAngle = 70.0f,
                useCenter = false,
                topLeft = Offset(center.x + w * 0.08f + waveDist * 0.5f - waveDist, center.y - waveDist),
                size = Size(waveDist * 2.0f, waveDist * 2.0f),
                style = Stroke(width = thinPx)
            )
        }
    }
}

/**
 * Page 4 Canvas Illustration: Microphone silhouette with pulsing green privacy indicator dot.
 */
@Composable
fun AllowMicrophoneIllustration(
    modifier: Modifier = Modifier
) {
    val isReducedMotion = ChirpTheme.isReducedMotion
    val primaryColor = ChirpTheme.colors.primary
    val signalActive = ChirpTheme.colors.signalActive
    val strokeThin = ChirpTheme.strokes.thin
    val strokeRegular = ChirpTheme.strokes.regular

    val pulseAlpha = if (isReducedMotion) {
        1.0f
    } else {
        val transition = rememberInfiniteTransition(label = "micPulseTransition")
        val alpha by transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = ChirpTheme.motion.durationMedium2,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "micPulseAlpha"
        )
        alpha
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .semantics {
                contentDescription = "Microphone silhouette with active privacy indicator."
            }
    ) {
        val w = size.width
        val h = size.height
        val center = Offset(w * 0.50f, h * 0.45f)
        val micWidth = w * 0.18f
        val micHeight = h * 0.35f
        val regPx = strokeRegular.toPx()
        val thinPx = strokeThin.toPx()

        // Microphone capsule body
        val capsuleRect = RoundRect(
            rect = Rect(
                offset = Offset(center.x - micWidth / 2.0f, center.y - micHeight / 2.0f),
                size = Size(micWidth, micHeight)
            ),
            cornerRadius = CornerRadius(micWidth / 2.0f, micWidth / 2.0f)
        )
        drawPath(Path().apply { addRoundRect(capsuleRect) }, primaryColor, style = Stroke(regPx))

        // Cradle arc under capsule
        drawArc(
            color = primaryColor,
            startAngle = 0.0f,
            sweepAngle = 180.0f,
            useCenter = false,
            topLeft = Offset(center.x - micWidth * 0.85f, center.y - micHeight * 0.2f),
            size = Size(micWidth * 1.7f, micHeight * 0.9f),
            style = Stroke(width = regPx)
        )

        // Stand stem
        drawLine(
            color = primaryColor,
            start = Offset(center.x, center.y + micHeight * 0.7f),
            end = Offset(center.x, center.y + micHeight * 1.1f),
            strokeWidth = regPx
        )

        // Stand base
        drawLine(
            color = primaryColor,
            start = Offset(center.x - micWidth * 0.6f, center.y + micHeight * 1.1f),
            end = Offset(center.x + micWidth * 0.6f, center.y + micHeight * 1.1f),
            strokeWidth = regPx
        )

        // Active green privacy dot (top right)
        drawCircle(
            color = signalActive.copy(alpha = pulseAlpha),
            radius = w * 0.035f,
            center = Offset(center.x + micWidth * 0.8f, center.y - micHeight * 0.4f)
        )
    }
}
