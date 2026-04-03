package com.guitartuner.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.guitartuner.audio.AudioProcessor
import com.guitartuner.model.GuitarString
import com.guitartuner.model.TunerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.log2

class TunerViewModel(application: Application) : AndroidViewModel(application) {

    private val audioProcessor = AudioProcessor()
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        application.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private val _state = MutableStateFlow(TunerState())
    val state: StateFlow<TunerState> = _state.asStateFlow()

    private var lastVibrateTime = 0L
    private val historyMaxSize = 50 // ~5 seconds of readings at ~10 readings/sec

    fun startListening(): Boolean {
        val context = getApplication<Application>()
        val started = audioProcessor.start(context)
        if (started) {
            _state.update { it.copy(isListening = true) }
            viewModelScope.launch {
                audioProcessor.audioResults.collect { result ->
                    processAudioResult(result.frequency, result.volume)
                }
            }
        }
        return started
    }

    fun stopListening() {
        audioProcessor.stop()
        _state.update {
            it.copy(
                isListening = false,
                detectedFrequency = 0.0,
                detectedNote = "--",
                cents = 0.0,
                volume = 0f
            )
        }
    }

    fun selectString(guitarString: GuitarString?) {
        _state.update { it.copy(selectedString = guitarString) }
    }

    fun setCalibration(a4Freq: Double) {
        _state.update { it.copy(a4Calibration = a4Freq.coerceIn(420.0, 460.0)) }
    }

    fun toggleDarkMode() {
        _state.update { it.copy(isDarkMode = !it.isDarkMode) }
    }

    private fun processAudioResult(frequency: Double, volume: Float) {
        if (frequency <= 0) {
            _state.update { it.copy(volume = volume) }
            return
        }

        // Filter out unreasonable frequencies for guitar
        if (frequency < 60.0 || frequency > 1500.0) {
            _state.update { it.copy(volume = volume) }
            return
        }

        val currentState = _state.value
        val selectedString = currentState.selectedString

        val targetFreq: Double
        val noteName: String

        if (selectedString != null) {
            targetFreq = selectedString.frequencyWithCalibration(currentState.a4Calibration)
            noteName = selectedString.noteName
        } else {
            // Auto-detect: find closest guitar string
            val closest = GuitarString.findClosest(frequency, currentState.a4Calibration)
            targetFreq = closest.frequencyWithCalibration(currentState.a4Calibration)
            noteName = closest.noteName
        }

        val cents = 1200.0 * log2(frequency / targetFreq)

        // Clamp cents display to ±50
        val displayCents = cents.coerceIn(-50.0, 50.0)

        // Update readings history
        val newHistory = (currentState.readingsHistory + displayCents).takeLast(historyMaxSize)

        _state.update {
            it.copy(
                detectedFrequency = frequency,
                detectedNote = noteName,
                targetFrequency = targetFreq,
                cents = displayCents,
                volume = volume,
                readingsHistory = newHistory
            )
        }

        // Vibrate when in tune (±2 cents)
        if (abs(cents) <= 2.0) {
            val now = System.currentTimeMillis()
            if (now - lastVibrateTime > 500) { // Don't vibrate more than every 500ms
                lastVibrateTime = now
                try {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } catch (_: Exception) { }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioProcessor.stop()
    }
}
