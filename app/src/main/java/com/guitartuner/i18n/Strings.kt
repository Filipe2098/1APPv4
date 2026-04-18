package com.guitartuner.i18n

import com.guitartuner.model.AppLanguage

object Strings {
    fun get(key: StringKey, language: AppLanguage): String {
        return ALL_TRANSLATIONS[language]?.get(key)
            ?: ALL_TRANSLATIONS[AppLanguage.ENGLISH]?.get(key)
            ?: key.name
    }
}

enum class StringKey {
    APP_TITLE,
    START_TUNING,
    STOP,
    PLAY,
    SETTINGS,
    TUNER_MODE,
    STROBOSCOPIC,
    NEEDLE,
    THEME,
    AUTO,
    DARK,
    LIGHT,
    LANGUAGE,
    CALIBRATION,
    CALIBRATION_DESC,
    CALIBRATION_RANGE,
    VIBRATION,
    VIBRATION_DESC,
    TIGHTEN,
    LOOSEN,
    IN_TUNE,
    SIGNAL,
    HISTORY,
    CENTS,
    BACK,
    MIC_PERMISSION,
    GUITAR_TYPE,
    STRINGS,
    METRONOME,
    TIME_SIGNATURE,
    INSTRUMENT,
    STRING_COUNT,
    GUITAR,
    BASS,
    VIOLIN,
    VIOLA,
    CELLO,
    DOUBLE_BASS,
    HIGH_PRECISION,
    SHOW_ALL_LANGUAGES,
    SHOW_FAVORITES,
}
