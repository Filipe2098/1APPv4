package com.guitartuner.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.guitartuner.model.TuningAccuracy
import com.guitartuner.ui.theme.TunerGreen
import com.guitartuner.ui.theme.TunerRed
import com.guitartuner.ui.theme.TunerYellow
import kotlin.math.abs

/**
 * Professional stroboscopic tuner with lateral-moving vertical bands.
 *
 * - Bands scroll LEFT when the note is flat (cents < 0).
 * - Bands scroll RIGHT when the note is sharp (cents > 0).
 * - Bands freeze when the note is in tune (|cents| < 0.5).
 * - Scroll speed is proportional to |cents|.
 * - Color: green when tuned (≤5), yellow when close (≤10), red when off (>10).
 */
@Composable
fun StroboscopicTuner(
    cents: Double,
    accuracy: TuningAccuracy,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val stripeColor: Color = when {
        !isActive -> Color.Gray.copy(alpha = 0.25f)
        abs(cents) <= 5.0 -> TunerGreen
        abs(cents) <= 10.0 -> TunerYellow
        else -> TunerRed
    }

    val bgColor = MaterialTheme.colorScheme.surface
    val borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

    // Speed: degrees per second of "virtual rotation" (mapped to lateral pixel offset).
    // Near zero cents → nearly frozen; far from zero → fast scrolling.
    val speed = if (isActive && abs(cents) > 0.3) cents.toFloat() else 0f

    val duration = if (abs(speed) < 0.5f) 30000
    else (5000.0 / abs(speed).coerceAtLeast(1f)).toInt().coerceIn(80, 8000)

    val infiniteTransition = rememberInfiniteTransition(label = "strobe")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (speed >= 0f) 1f else -1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = duration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Background
                drawRect(color = bgColor, size = size)

                val bandCount = 16
                val bandWidth = w / bandCount
                val gapRatio = 0.45f
                val barWidth = bandWidth * gapRatio

                // phase goes 0→1 (or 0→-1); map to one full band-width of lateral offset
                val offset = phase * bandWidth

                // Draw bands from beyond left edge to beyond right edge so scrolling wraps
                val startIdx = -2
                val endIdx = bandCount + 2
                for (i in startIdx until endIdx) {
                    val x = i * bandWidth + offset
                    if (x + barWidth < 0f || x > w) continue
                    drawRect(
                        color = stripeColor,
                        topLeft = Offset(x, 0f),
                        size = Size(barWidth, h)
                    )
                }

                // Center reference line (thin marker showing "in tune" position)
                drawRect(
                    color = if (isActive && abs(cents) <= 2.0)
                        TunerGreen.copy(alpha = 0.9f)
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                    topLeft = Offset(w / 2f - 1.5f, 0f),
                    size = Size(3f, h)
                )

                // Border
                drawRect(
                    color = borderColor,
                    size = size,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                )
            }
        }
    }
}
