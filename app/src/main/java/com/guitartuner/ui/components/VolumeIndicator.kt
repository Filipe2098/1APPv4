package com.guitartuner.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guitartuner.ui.theme.TunerGreen
import com.guitartuner.ui.theme.TunerRed
import com.guitartuner.ui.theme.TunerYellow

@Composable
fun VolumeIndicator(
    volume: Float,
    label: String = "SIGNAL",
    modifier: Modifier = Modifier
) {
    val animatedVolume by animateFloatAsState(
        targetValue = volume,
        animationSpec = tween(100),
        label = "volume"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Canvas(
            modifier = Modifier
                .width(120.dp)
                .height(8.dp)
        ) {
            drawRoundRect(
                color = Color.Gray.copy(alpha = 0.2f),
                cornerRadius = CornerRadius(4f, 4f),
                size = Size(size.width, size.height)
            )

            val barWidth = size.width * animatedVolume
            val barColor = when {
                animatedVolume > 0.8f -> TunerRed
                animatedVolume > 0.5f -> TunerYellow
                else -> TunerGreen
            }

            if (barWidth > 0) {
                drawRoundRect(
                    color = barColor,
                    cornerRadius = CornerRadius(4f, 4f),
                    size = Size(barWidth, size.height)
                )
            }
        }
    }
}
