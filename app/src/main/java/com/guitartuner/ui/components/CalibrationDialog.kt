package com.guitartuner.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CalibrationDialog(
    currentCalibration: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var textValue by remember { mutableStateOf(currentCalibration.toInt().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("A4 Calibration")
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Set reference frequency for A4",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    FilledIconButton(
                        onClick = {
                            val current = textValue.toDoubleOrNull() ?: 440.0
                            textValue = (current - 1).coerceIn(420.0, 460.0).toInt().toString()
                        }
                    ) {
                        Text("-", fontSize = 20.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.matches(Regex("^\\d{0,3}$"))) {
                                textValue = newValue
                            }
                        },
                        modifier = Modifier.width(100.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        suffix = { Text("Hz") }
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    FilledIconButton(
                        onClick = {
                            val current = textValue.toDoubleOrNull() ?: 440.0
                            textValue = (current + 1).coerceIn(420.0, 460.0).toInt().toString()
                        }
                    ) {
                        Text("+", fontSize = 20.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Standard: 440 Hz (Range: 420-460 Hz)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val freq = textValue.toDoubleOrNull() ?: 440.0
                    onConfirm(freq.coerceIn(420.0, 460.0))
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
