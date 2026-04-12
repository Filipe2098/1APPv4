package com.guitartuner.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.guitartuner.model.TuningAccuracy
import com.guitartuner.ui.theme.TunerGreen
import com.guitartuner.ui.theme.TunerRed
import com.guitartuner.ui.theme.TunerYellow
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * V4 Stroboscopic tuner: rotating radial stripes inside a circular dial.
 *
 * - Stripes rotate counter-clockwise when flat (cents < 0) and clockwise when sharp.
 * - Rotation speed is proportional to |cents|; stripes come to a standstill when in tune.
 * - Stripe color reflects tuning accuracy:
 *     |cents| <= 5  -> green
 *     |cents| <= 10 -> yellow
 *     otherwise     -> red
 */
@Composable
fun StroboscopicTuner(
    cents: Double,
    accuracy: TuningAccuracy,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "strobe")

    val speed = if (isActive) cents.toFloat() else 0f
    // Duration of one full rotation. Faster when further from pitch; effectively static when in tune.
    val duration = if (abs(speed) < 0.5f) 20000
        else (4000.0 / abs(speed).coerceAtLeast(1f)).toInt().coerceIn(120, 6000)

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (speed >= 0f) 360f else -360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = duration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val stripeColor: Color = when {
        !isActive -> Color.Gray.copy(alpha = 0.3f)
        abs(cents) <= 5.0 -> TunerGreen
        abs(cents) <= 10.0 -> TunerYellow
        else -> TunerRed
    }

    val bgColor = MaterialTheme.colorScheme.surface
    val ringColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(180.dp)
                .clip(CircleShape)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cx = w / 2f
                val cy = h / 2f
                val radius = min(w, h) / 2f

                // Background disk
                drawCircle(color = bgColor, radius = radius, center = Offset(cx, cy))

                // Radial stripes - rotated as a group
                rotate(rotation, pivot = Offset(cx, cy)) {
                    val stripeCount = 12
                    val halfAngle = (Math.PI / stripeCount).toFloat() * 0.45f // thinner stripes w/ gaps
                    for (i in 0 until stripeCount) {
                        val angle = (i * 2.0 * Math.PI / stripeCount).toFloat()
                        val a1 = angle - halfAngle
                        val a2 = angle + halfAngle
                        val path = Path().apply {
                            moveTo(cx, cy)
                            lineTo(
                                cx + radius * cos(a1),
                                cy + radius * sin(a1)
                            )
                            lineTo(
                                cx + radius * cos(a2),
                                cy + radius * sin(a2)
                            )
                            close()
                        }
                        drawPath(path, color = stripeColor.copy(alpha = 0.85f))
                    }
                }

                // Outer ring
                drawCircle(
                    color = ringColor,
                    radius = radius - 1f,
                    center = Offset(cx, cy),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                )

                // Inner hub
                drawCircle(
                    color = bgColor,
                    radius = radius * 0.18f,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = ringColor,
                    radius = radius * 0.18f,
                    center = Offset(cx, cy),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                )
            }
        }
    }
}
