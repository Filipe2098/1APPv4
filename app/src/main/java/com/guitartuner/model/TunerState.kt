package com.guitartuner.model

data class TunerState(
    val detectedFrequency: Double = 0.0,
    val detectedNote: String = "--",
    val targetFrequency: Double = 0.0,
    val cents: Double = 0.0,
    val volume: Float = 0f,
    val isListening: Boolean = false,
    val selectedString: GuitarString? = null,
    val a4Calibration: Double = 440.0,
    val isDarkMode: Boolean = true,
    val readingsHistory: List<Double> = emptyList()
) {
    val tuningAccuracy: TuningAccuracy
        get() = when {
            kotlin.math.abs(cents) <= 2.0 -> TuningAccuracy.PERFECT
            kotlin.math.abs(cents) <= 5.0 -> TuningAccuracy.CLOSE
            else -> TuningAccuracy.OFF
        }
}

enum class TuningAccuracy {
    PERFECT, CLOSE, OFF
}
