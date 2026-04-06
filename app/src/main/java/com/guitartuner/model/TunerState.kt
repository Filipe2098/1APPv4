package com.guitartuner.model

import kotlin.math.abs

enum class TunerMode {
    STROBOSCOPIC, NEEDLE
}

enum class AppLanguage(val code: String, val flag: String, val displayName: String) {
    PORTUGUESE("pt", "\uD83C\uDDF5\uD83C\uDDF9", "Português"),
    ENGLISH("en", "\uD83C\uDDEC\uD83C\uDDE7", "English"),
    POLISH("pl", "\uD83C\uDDF5\uD83C\uDDF1", "Polski"),
    SPANISH("es", "\uD83C\uDDEA\uD83C\uDDF8", "Español"),
    FRENCH("fr", "\uD83C\uDDEB\uD83C\uDDF7", "Français"),
    GERMAN("de", "\uD83C\uDDE9\uD83C\uDDEA", "Deutsch"),
    CHINESE("zh", "\uD83C\uDDE8\uD83C\uDDF3", "中文"),
    JAPANESE("ja", "\uD83C\uDDEF\uD83C\uDDF5", "日本語"),
    RUSSIAN("ru", "\uD83C\uDDF7\uD83C\uDDFA", "Русский")
}

enum class TuningAccuracy {
    PERFECT, CLOSE, OFF
}

data class TunerState(
    val detectedFrequency: Double = 0.0,
    val detectedNote: String = "--",
    val targetFrequency: Double = 0.0,
    val cents: Double = 0.0,
    val volume: Float = 0f,
    val isListening: Boolean = false,
    val a4Calibration: Double = 440.0,
    val isDarkMode: Boolean = true,
    val tunerMode: TunerMode = TunerMode.STROBOSCOPIC,
    val language: AppLanguage = AppLanguage.PORTUGUESE,
    val vibrationEnabled: Boolean = true,
    val readingsHistory: List<Double> = emptyList()
) {
    val tuningAccuracy: TuningAccuracy
        get() = when {
            detectedFrequency <= 0 -> TuningAccuracy.OFF
            abs(cents) <= 2.0 -> TuningAccuracy.PERFECT
            abs(cents) <= 5.0 -> TuningAccuracy.CLOSE
            else -> TuningAccuracy.OFF
        }
}
