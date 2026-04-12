package com.guitartuner.model

import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.round

/**
 * Note table covering the full range required by all supported instruments,
 * from Double Bass B0 (30.87 Hz) up to Violin E5 (659.26 Hz).
 */
object GuitarString {

    private val ALL_NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    /**
     * Base frequencies (A4 = 440 Hz reference) for every note used by any instrument tuning.
     */
    val NOTE_FREQUENCIES: Map<String, Double> = mapOf(
        "B0" to 30.87,
        "E1" to 41.20,
        "F#1" to 46.25,
        "A1" to 55.00,
        "B1" to 61.74,
        "C2" to 65.41,
        "D2" to 73.42,
        "E2" to 82.41,
        "F2" to 87.31,
        "G2" to 98.00,
        "A2" to 110.00,
        "B2" to 123.47,
        "C3" to 130.81,
        "D3" to 146.83,
        "E3" to 164.81,
        "F3" to 174.61,
        "G3" to 196.00,
        "A3" to 220.00,
        "B3" to 246.94,
        "C4" to 261.63,
        "D4" to 293.66,
        "E4" to 329.63,
        "G4" to 392.00,
        "A4" to 440.00,
        "C5" to 523.25,
        "D5" to 587.33,
        "E5" to 659.26
    )

    /**
     * Returns the frequency of a named note scaled to the given A4 calibration.
     * Falls back to 440-based value if no custom calibration is needed.
     */
    fun frequencyOf(noteName: String, a4Frequency: Double = 440.0): Double {
        val base = NOTE_FREQUENCIES[noteName] ?: return 0.0
        val ratio = a4Frequency / 440.0
        return base * ratio
    }

    /**
     * Find the closest target string from a specific instrument tuning.
     */
    fun findClosestString(
        frequency: Double,
        tuning: List<String>,
        a4Frequency: Double = 440.0
    ): String {
        if (frequency <= 0 || tuning.isEmpty()) return tuning.firstOrNull() ?: "--"
        return tuning.minBy { note ->
            val calibrated = frequencyOf(note, a4Frequency)
            if (calibrated <= 0) Double.MAX_VALUE
            else abs(1200.0 * log2(frequency / calibrated))
        }
    }

    /**
     * Chromatic detection: find the nearest named note (e.g. "A4") for an arbitrary frequency.
     */
    fun findClosestNote(frequency: Double, a4Frequency: Double = 440.0): Pair<String, Double> {
        if (frequency <= 0) return "--" to 0.0

        val semitonesFromA4 = 12.0 * log2(frequency / a4Frequency)
        val roundedSemitones = round(semitonesFromA4).toInt()

        val noteIndex = Math.floorMod(roundedSemitones + 9, 12)
        val octave = 4 + Math.floorDiv(roundedSemitones + 9, 12)

        val noteName = ALL_NOTE_NAMES[noteIndex]
        val exactFrequency = a4Frequency * 2.0.pow(roundedSemitones / 12.0)

        return "$noteName$octave" to exactFrequency
    }

    fun chromaticNoteIndex(noteName: String): Int {
        val name = noteName.replace(Regex("\\d"), "")
        return ALL_NOTE_NAMES.indexOf(name)
    }

    val ALL_NOTES: Array<String> get() = ALL_NOTE_NAMES
}
