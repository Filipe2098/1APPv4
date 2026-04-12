package com.guitartuner.data

import android.content.Context
import android.content.SharedPreferences
import com.guitartuner.model.*

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("guitar_tuner_prefs", Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() = try {
            ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.AUTO.name) ?: ThemeMode.AUTO.name)
        } catch (_: Exception) { ThemeMode.AUTO }
        set(value) = prefs.edit().putString("theme_mode", value.name).apply()

    var tunerMode: TunerMode
        get() = try {
            TunerMode.valueOf(prefs.getString("tuner_mode", TunerMode.STROBOSCOPIC.name) ?: TunerMode.STROBOSCOPIC.name)
        } catch (_: Exception) { TunerMode.STROBOSCOPIC }
        set(value) = prefs.edit().putString("tuner_mode", value.name).apply()

    var language: AppLanguage
        get() = try {
            AppLanguage.valueOf(prefs.getString("language", AppLanguage.PORTUGUESE.name) ?: AppLanguage.PORTUGUESE.name)
        } catch (_: Exception) { AppLanguage.PORTUGUESE }
        set(value) = prefs.edit().putString("language", value.name).apply()

    var a4Calibration: Double
        get() = prefs.getFloat("a4_calibration", 440f).toDouble()
        set(value) = prefs.edit().putFloat("a4_calibration", value.toFloat()).apply()

    var vibrationEnabled: Boolean
        get() = prefs.getBoolean("vibration_enabled", true)
        set(value) = prefs.edit().putBoolean("vibration_enabled", value).apply()

    var instrumentType: InstrumentType
        get() = try {
            InstrumentType.valueOf(
                prefs.getString("instrument_type", InstrumentType.GUITARRA.name)
                    ?: InstrumentType.GUITARRA.name
            )
        } catch (_: Exception) { InstrumentType.GUITARRA }
        set(value) = prefs.edit().putString("instrument_type", value.name).apply()

    var stringCount: Int
        get() = prefs.getInt("string_count", 6)
        set(value) = prefs.edit().putInt("string_count", value).apply()

    var metronomeBpm: Int
        get() = prefs.getInt("metronome_bpm", 120)
        set(value) = prefs.edit().putInt("metronome_bpm", value).apply()

    fun loadState(): TunerState {
        val instrument = instrumentType
        val storedCount = stringCount
        val count = if (storedCount in instrument.stringCountOptions) storedCount else instrument.defaultStringCount
        return TunerState(
            themeMode = themeMode,
            tunerMode = tunerMode,
            language = language,
            a4Calibration = a4Calibration,
            vibrationEnabled = vibrationEnabled,
            instrumentType = instrument,
            stringCount = count,
            metronomeBpm = metronomeBpm
        )
    }
}
