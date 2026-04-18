package com.guitartuner.model

import kotlin.math.abs

enum class TunerMode {
    STROBOSCOPIC, NEEDLE
}

enum class ThemeMode {
    AUTO, DARK, LIGHT
}

enum class AppLanguage(val code: String, val flag: String, val displayName: String) {
    PORTUGUESE("pt", "\uD83C\uDDF5\uD83C\uDDF9", "Português"),
    ENGLISH("en", "\uD83C\uDDEC\uD83C\uDDE7", "English"),
    POLISH("pl", "\uD83C\uDDF5\uD83C\uDDF1", "Polski"),
    SPANISH("es", "\uD83C\uDDEA\uD83C\uDDF8", "Español"),
    FRENCH("fr", "\uD83C\uDDEB\uD83C\uDDF7", "Français"),
    GERMAN("de", "\uD83C\uDDE9\uD83C\uDDEA", "Deutsch"),
    CHINESE_SIMPLIFIED("zh-CN", "\uD83C\uDDE8\uD83C\uDDF3", "简体中文"),
    CHINESE_TRADITIONAL("zh-TW", "\uD83C\uDDF9\uD83C\uDDFC", "繁體中文"),
    JAPANESE("ja", "\uD83C\uDDEF\uD83C\uDDF5", "日本語"),
    RUSSIAN("ru", "\uD83C\uDDF7\uD83C\uDDFA", "Русский"),
    SWEDISH("sv", "\uD83C\uDDF8\uD83C\uDDEA", "Svenska"),
    NORWEGIAN("no", "\uD83C\uDDF3\uD83C\uDDF4", "Norsk"),
    DANISH("da", "\uD83C\uDDE9\uD83C\uDDF0", "Dansk"),
    DUTCH("nl", "\uD83C\uDDF3\uD83C\uDDF1", "Nederlands"),
    ITALIAN("it", "\uD83C\uDDEE\uD83C\uDDF9", "Italiano"),
    TURKISH("tr", "\uD83C\uDDF9\uD83C\uDDF7", "Türkçe"),
    ARABIC("ar", "\uD83C\uDDF8\uD83C\uDDE6", "العربية"),
    VIETNAMESE("vi", "\uD83C\uDDFB\uD83C\uDDF3", "Tiếng Việt"),
    THAI("th", "\uD83C\uDDF9\uD83C\uDDED", "ไทย"),
    INDONESIAN("id", "\uD83C\uDDEE\uD83C\uDDE9", "Indonesia"),
    HINDI("hi", "\uD83C\uDDEE\uD83C\uDDF3", "हिन्दी"),
    KOREAN("ko", "\uD83C\uDDF0\uD83C\uDDF7", "한국어"),
    ROMANIAN("ro", "\uD83C\uDDF7\uD83C\uDDF4", "Română"),
    UKRAINIAN("uk", "\uD83C\uDDFA\uD83C\uDDE6", "Українська"),
    FINNISH("fi", "\uD83C\uDDEB\uD83C\uDDEE", "Suomi"),
    GREEK("el", "\uD83C\uDDEC\uD83C\uDDF7", "Ελληνικά"),
    HUNGARIAN("hu", "\uD83C\uDDED\uD83C\uDDFA", "Magyar"),
    BENGALI("bn", "\uD83C\uDDE7\uD83C\uDDE9", "বাংলা")
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
    val themeMode: ThemeMode = ThemeMode.AUTO,
    val tunerMode: TunerMode = TunerMode.STROBOSCOPIC,
    val language: AppLanguage = AppLanguage.PORTUGUESE,
    val vibrationEnabled: Boolean = true,
    val instrumentType: InstrumentType = InstrumentType.GUITARRA,
    val stringCount: Int = 6,
    val readingsHistory: List<Double> = emptyList(),
    // Metronome
    val metronomeBpm: Int = 120,
    val metronomeIsPlaying: Boolean = false,
    val metronomeBeat: Int = 0,
    val metronomeBeatsPerMeasure: Int = 4,
    // Language filter
    val showAllLanguages: Boolean = false,
    val favoriteLanguages: Set<String> = setOf(
        AppLanguage.PORTUGUESE.name,
        AppLanguage.ENGLISH.name,
        AppLanguage.SPANISH.name,
        AppLanguage.FRENCH.name,
        AppLanguage.GERMAN.name,
    )
) {
    val isHighPrecision: Boolean
        get() = instrumentType.highPrecision

    val centsRange: Double
        get() = if (isHighPrecision) 20.0 else 50.0

    val tuningAccuracy: TuningAccuracy
        get() = when {
            detectedFrequency <= 0 -> TuningAccuracy.OFF
            abs(cents) <= 2.0 -> TuningAccuracy.PERFECT
            abs(cents) <= 5.0 -> TuningAccuracy.CLOSE
            else -> TuningAccuracy.OFF
        }

    val isDarkMode: Boolean
        get() = themeMode == ThemeMode.DARK

    val isRtl: Boolean
        get() = language == AppLanguage.ARABIC

    val visibleLanguages: List<AppLanguage>
        get() = if (showAllLanguages) {
            AppLanguage.entries.toList()
        } else {
            AppLanguage.entries.filter { it.name in favoriteLanguages || it == language }
        }
}
