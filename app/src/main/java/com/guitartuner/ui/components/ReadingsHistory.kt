package com.guitartuner.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guitartuner.ui.theme.TunerGreen

@Composable
fun ReadingsHistory(
    readings: List<Double>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "HISTORY",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(4.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(horizontal = 16.dp)
        ) {
            if (readings.isEmpty()) return@Canvas

            val width = size.width
            val height = size.height
            val midY = height / 2

            // Draw center line (0 cents)
            drawLine(
                color = TunerGreen.copy(alpha = 0.3f),
                start = Offset(0f, midY),
                end = Offset(width, midY),
                strokeWidth = 1f
            )

            // Draw ±5 cent lines
            val fiveCentY = height * 0.4f
            drawLine(
                color = Color.Gray.copy(alpha = 0.15f),
                start = Offset(0f, midY - fiveCentY + midY * 0.1f),
                end = Offset(width, midY - fiveCentY + midY * 0.1f),
                strokeWidth = 0.5f
            )
            drawLine(
                color = Color.Gray.copy(alpha = 0.15f),
                start = Offset(0f, midY + fiveCentY - midY * 0.1f),
                end = Offset(width, midY + fiveCentY - midY * 0.1f),
                strokeWidth = 0.5f
            )

            // Draw readings path
            if (readings.size >= 2) {
                val path = Path()
                val stepX = width / (readings.size - 1).coerceAtLeast(1)

                readings.forEachIndexed { index, cents ->
                    val x = index * stepX
                    val normalizedY = (cents / 50.0).toFloat()
                    val y = midY - normalizedY * midY * 0.9f

                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                drawPath(
                    path = path,
                    color = TunerGreen,
                    style = Stroke(width = 2f)
                )
            }
        }
    }
}
