package com.guitartuner.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guitartuner.model.GuitarString
import com.guitartuner.ui.theme.AccentBlue

@Composable
fun StringSelector(
    selectedString: GuitarString?,
    onStringSelected: (GuitarString?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "STRING",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            // Auto button
            StringButton(
                label = "AUTO",
                isSelected = selectedString == null,
                onClick = { onStringSelected(null) }
            )
            Spacer(modifier = Modifier.width(4.dp))

            // Individual string buttons
            GuitarString.entries.reversed().forEach { string ->
                StringButton(
                    label = string.noteName,
                    isSelected = selectedString == string,
                    onClick = { onStringSelected(string) }
                )
                if (string != GuitarString.entries.first()) {
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }
    }
}

@Composable
private fun StringButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .height(40.dp)
            .widthIn(min = 42.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) AccentBlue else Color.Transparent,
            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) AccentBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
