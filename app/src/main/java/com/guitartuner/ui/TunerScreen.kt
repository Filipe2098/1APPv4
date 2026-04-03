package com.guitartuner.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guitartuner.model.TunerState
import com.guitartuner.model.TuningAccuracy
import com.guitartuner.model.GuitarString
import com.guitartuner.ui.components.*
import com.guitartuner.ui.theme.TunerGreen
import com.guitartuner.ui.theme.TunerRed
import com.guitartuner.ui.theme.TunerYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TunerScreen(
    state: TunerState,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onStringSelected: (GuitarString?) -> Unit,
    onCalibrationChanged: (Double) -> Unit,
    onToggleDarkMode: () -> Unit
) {
    var showCalibrationDialog by remember { mutableStateOf(false) }

    val accuracyColor by animateColorAsState(
        targetValue = when (state.tuningAccuracy) {
            TuningAccuracy.PERFECT -> TunerGreen
            TuningAccuracy.CLOSE -> TunerYellow
            TuningAccuracy.OFF -> TunerRed
        },
        animationSpec = tween(300),
        label = "accuracyColor"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Guitar Tuner",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Row {
                IconButton(onClick = { showCalibrationDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Calibration",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
                IconButton(onClick = onToggleDarkMode) {
                    Icon(
                        imageVector = if (state.isDarkMode) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                        contentDescription = "Toggle theme",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calibration info
        Text(
            text = "A4 = ${state.a4Calibration.toInt()} Hz",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Note display
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 32.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = state.detectedNote,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (state.detectedFrequency > 0) accuracyColor
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    textAlign = TextAlign.Center
                )

                if (state.detectedFrequency > 0) {
                    Text(
                        text = String.format("%.1f Hz", state.detectedFrequency),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tuner gauge
        TunerGauge(
            cents = if (state.detectedFrequency > 0) state.cents else 0.0,
            accuracy = state.tuningAccuracy
        )

        // Cents display
        if (state.detectedFrequency > 0) {
            Text(
                text = String.format("%+.1f cents", state.cents),
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = accuracyColor
            )
        } else {
            Text(
                text = "-- cents",
                fontSize = 22.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Volume indicator
        VolumeIndicator(volume = state.volume)

        Spacer(modifier = Modifier.height(12.dp))

        // Readings history
        if (state.readingsHistory.isNotEmpty()) {
            ReadingsHistory(
                readings = state.readingsHistory,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // String selector
        StringSelector(
            selectedString = state.selectedString,
            onStringSelected = onStringSelected
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Start/Stop button
        Button(
            onClick = {
                if (state.isListening) onStopListening() else onStartListening()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.isListening)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = if (state.isListening) "STOP" else "START TUNING",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    // Calibration dialog
    if (showCalibrationDialog) {
        CalibrationDialog(
            currentCalibration = state.a4Calibration,
            onDismiss = { showCalibrationDialog = false },
            onConfirm = { freq ->
                onCalibrationChanged(freq)
                showCalibrationDialog = false
            }
        )
    }
}
