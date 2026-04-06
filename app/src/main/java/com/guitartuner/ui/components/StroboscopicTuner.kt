package com.guitartuner.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.guitartuner.model.TuningAccuracy
import com.guitartuner.ui.theme.TunerGreen
import com.guitartuner.ui.theme.TunerRed
import com.guitartuner.ui.theme.TunerYellow
import kotlin.math.abs

/**
 * Stroboscopic tuner display.
 * Bars scroll left when flat, right when sharp, and stop when in tune.
 * Speed of scrolling indicates how far off the tuning is.
 */
@Composable
fun StroboscopicTuner(
    cents: Double,
    accuracy: TuningAccuracy,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "strobe")

    // Speed proportional to cents deviation: 0 cents = stopped, 50 cents = very fast
    // Direction: negative cents (flat) = scroll left, positive (sharp) = scroll right
    val speed = if (isActive) cents.toFloat() else 0f

    // Animate a phase offset that drives the stripe positions
    // Duration inversely proportional to |cents| — faster when more off-tune
    val duration = if (abs(speed) < 0.5f) 10000 else (2000.0 / abs(speed).coerceAtLeast(1f)).toInt().coerceIn(80, 3000)

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (speed >= 0) 1f else -1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = duration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val barColor = when (accuracy) {
        TuningAccuracy.PERFECT -> TunerGreen
        TuningAccuracy.CLOSE -> TunerYellow
        TuningAccuracy.OFF -> TunerRed
    }

    val bgColor = MaterialTheme.colorScheme.surface

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        val width = size.width
        val height = size.height
        val stripeWidth = 28f
        val gapWidth = 28f
        val period = stripeWidth + gapWidth

        // Background
        drawRect(color = bgColor)

        if (!isActive) {
            // Draw static center bars when inactive
            val numBars = (width / period).toInt() + 2
            val startX = (width - numBars * period) / 2
            for (i in 0 until numBars) {
                val x = startX + i * period
                drawRect(
                    color = Color.Gray.copy(alpha = 0.15f),
                    topLeft = Offset(x, 0f),
                    size = Size(stripeWidth, height)
                )
            }
            return@Canvas
        }

        // Calculate offset from phase
        val offset = phase * period

        // Draw scrolling bars
        val numBars = (width / period).toInt() + 3
        val alpha = if (accuracy == TuningAccuracy.PERFECT) 0.85f else 0.7f

        for (i in -1 until numBars) {
            val x = i * period + offset
            if (x + stripeWidth < 0 || x > width) continue

            drawRect(
                color = barColor.copy(alpha = alpha),
                topLeft = Offset(x, 0f),
                size = Size(stripeWidth, height)
            )
        }

        // Draw center marker line
        drawLine(
            color = Color.White.copy(alpha = 0.6f),
            start = Offset(width / 2, 0f),
            end = Offset(width / 2, height),
            strokeWidth = 2f
        )

        // Draw edge fade gradients
        val fadeWidth = 40f
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(bgColor, bgColor.copy(alpha = 0f)),
                startX = 0f,
                endX = fadeWidth
            ),
            topLeft = Offset.Zero,
            size = Size(fadeWidth, height)
        )
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(bgColor.copy(alpha = 0f), bgColor),
                startX = width - fadeWidth,
                endX = width
            ),
            topLeft = Offset(width - fadeWidth, 0f),
            size = Size(fadeWidth, height)
        )
    }
}
