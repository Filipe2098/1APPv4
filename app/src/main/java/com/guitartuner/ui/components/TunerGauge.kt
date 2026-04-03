package com.guitartuner.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.guitartuner.model.TuningAccuracy
import com.guitartuner.ui.theme.TunerGreen
import com.guitartuner.ui.theme.TunerRed
import com.guitartuner.ui.theme.TunerYellow
import com.guitartuner.ui.theme.NeedleColor
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TunerGauge(
    cents: Double,
    accuracy: TuningAccuracy,
    modifier: Modifier = Modifier
) {
    // Animate needle position smoothly
    val animatedCents by animateFloatAsState(
        targetValue = cents.toFloat(),
        animationSpec = tween(durationMillis = 150),
        label = "needle"
    )

    val accuracyColor = when (accuracy) {
        TuningAccuracy.PERFECT -> TunerGreen
        TuningAccuracy.CLOSE -> TunerYellow
        TuningAccuracy.OFF -> TunerRed
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        val centerX = size.width / 2
        val centerY = size.height * 0.95f
        val radius = size.width * 0.42f

        // Draw arc background
        drawArc(
            color = Color.Gray.copy(alpha = 0.2f),
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(centerX - radius, centerY - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = Stroke(width = 12f, cap = StrokeCap.Round)
        )

        // Draw colored zones on the arc
        // Red zone left (-50 to -5)
        drawArc(
            color = TunerRed.copy(alpha = 0.4f),
            startAngle = 200f,
            sweepAngle = 63f,
            useCenter = false,
            topLeft = Offset(centerX - radius, centerY - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = Stroke(width = 12f, cap = StrokeCap.Round)
        )

        // Yellow zone left (-5 to -2)
        drawArc(
            color = TunerYellow.copy(alpha = 0.5f),
            startAngle = 263f,
            sweepAngle = 4.2f,
            useCenter = false,
            topLeft = Offset(centerX - radius, centerY - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = Stroke(width = 12f, cap = StrokeCap.Round)
        )

        // Green zone center (-2 to +2)
        drawArc(
            color = TunerGreen.copy(alpha = 0.6f),
            startAngle = 267.2f,
            sweepAngle = 5.6f,
            useCenter = false,
            topLeft = Offset(centerX - radius, centerY - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = Stroke(width = 14f, cap = StrokeCap.Round)
        )

        // Yellow zone right (+2 to +5)
        drawArc(
            color = TunerYellow.copy(alpha = 0.5f),
            startAngle = 272.8f,
            sweepAngle = 4.2f,
            useCenter = false,
            topLeft = Offset(centerX - radius, centerY - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = Stroke(width = 12f, cap = StrokeCap.Round)
        )

        // Red zone right (+5 to +50)
        drawArc(
            color = TunerRed.copy(alpha = 0.4f),
            startAngle = 277f,
            sweepAngle = 63f,
            useCenter = false,
            topLeft = Offset(centerX - radius, centerY - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = Stroke(width = 12f, cap = StrokeCap.Round)
        )

        // Draw tick marks
        drawTickMarks(centerX, centerY, radius)

        // Draw center marker
        val centerAngle = Math.toRadians(270.0)
        val markerInner = radius - 25f
        val markerOuter = radius + 25f
        drawLine(
            color = Color.White.copy(alpha = 0.8f),
            start = Offset(
                centerX + (markerInner * cos(centerAngle)).toFloat(),
                centerY + (markerInner * sin(centerAngle)).toFloat()
            ),
            end = Offset(
                centerX + (markerOuter * cos(centerAngle)).toFloat(),
                centerY + (markerOuter * sin(centerAngle)).toFloat()
            ),
            strokeWidth = 3f
        )

        // Draw needle
        val needleAngle = 270.0 + (animatedCents / 50.0 * 70.0)
        val needleRad = Math.toRadians(needleAngle)
        val needleLength = radius - 35f

        // Needle shadow
        drawLine(
            color = Color.Black.copy(alpha = 0.3f),
            start = Offset(centerX + 2f, centerY + 2f),
            end = Offset(
                centerX + 2f + (needleLength * cos(needleRad)).toFloat(),
                centerY + 2f + (needleLength * sin(needleRad)).toFloat()
            ),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )

        // Needle
        drawLine(
            color = NeedleColor,
            start = Offset(centerX, centerY),
            end = Offset(
                centerX + (needleLength * cos(needleRad)).toFloat(),
                centerY + (needleLength * sin(needleRad)).toFloat()
            ),
            strokeWidth = 3.5f,
            cap = StrokeCap.Round
        )

        // Needle pivot circle
        drawCircle(
            color = NeedleColor,
            radius = 8f,
            center = Offset(centerX, centerY)
        )
        drawCircle(
            color = accuracyColor,
            radius = 5f,
            center = Offset(centerX, centerY)
        )
    }
}

private fun DrawScope.drawTickMarks(centerX: Float, centerY: Float, radius: Float) {
    val tickValues = listOf(-50, -40, -30, -20, -10, 0, 10, 20, 30, 40, 50)
    for (value in tickValues) {
        val angle = Math.toRadians(270.0 + (value / 50.0 * 70.0))
        val isMainTick = value % 10 == 0
        val innerRadius = if (isMainTick) radius - 18f else radius - 10f
        val outerRadius = radius + 8f
        val tickWidth = if (isMainTick) 2f else 1f

        drawLine(
            color = Color.Gray.copy(alpha = 0.6f),
            start = Offset(
                centerX + (innerRadius * cos(angle)).toFloat(),
                centerY + (innerRadius * sin(angle)).toFloat()
            ),
            end = Offset(
                centerX + (outerRadius * cos(angle)).toFloat(),
                centerY + (outerRadius * sin(angle)).toFloat()
            ),
            strokeWidth = tickWidth
        )

        // Draw labels for main ticks
        if (isMainTick) {
            val labelRadius = radius + 28f
            val labelX = centerX + (labelRadius * cos(angle)).toFloat()
            val labelY = centerY + (labelRadius * sin(angle)).toFloat()

            drawContext.canvas.nativeCanvas.drawText(
                if (value > 0) "+$value" else "$value",
                labelX,
                labelY + 5f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 24f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
            )
        }
    }
}
