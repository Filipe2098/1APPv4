package com.guitartuner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val TunerGreen = Color(0xFF4CAF50)
val TunerYellow = Color(0xFFFFC107)
val TunerRed = Color(0xFFF44336)

val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkCard = Color(0xFF2C2C2C)
val LightBackground = Color(0xFFF5F5F5)
val LightSurface = Color(0xFFFFFFFF)
val LightCard = Color(0xFFE8E8E8)

val NeedleColor = Color(0xFFFF5722)
val AccentBlue = Color(0xFF2196F3)

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    secondary = TunerGreen,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = AccentBlue,
    secondary = TunerGreen,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black
)

@Composable
fun GuitarTunerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
