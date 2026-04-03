package com.guitartuner.model

enum class GuitarString(
    val noteName: String,
    val frequency: Double,
    val stringNumber: Int
) {
    E2("E2", 82.41, 6),
    A2("A2", 110.00, 5),
    D3("D3", 146.83, 4),
    G3("G3", 196.00, 3),
    B3("B3", 246.94, 2),
    E4("e4", 329.63, 1);

    fun frequencyWithCalibration(a4Frequency: Double): Double {
        val standardA4 = 440.0
        val ratio = a4Frequency / standardA4
        return frequency * ratio
    }

    companion object {
        fun findClosest(frequency: Double, a4Frequency: Double): GuitarString {
            return entries.minBy { entry ->
                val calibrated = entry.frequencyWithCalibration(a4Frequency)
                kotlin.math.abs(1200.0 * kotlin.math.log2(frequency / calibrated))
            }
        }

        fun findClosestNote(frequency: Double): Pair<String, Double> {
            val noteNames = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
            val a4 = 440.0
            val semitonesFromA4 = 12.0 * kotlin.math.log2(frequency / a4)
            val roundedSemitones = kotlin.math.round(semitonesFromA4).toInt()
            val noteIndex = ((roundedSemitones % 12) + 12 + 9) % 12 // A is index 9 from C
            val octave = 4 + (roundedSemitones + 9) / 12
            val noteName = noteNames[noteIndex]
            val exactFrequency = a4 * kotlin.math.pow(2.0, roundedSemitones / 12.0)
            return "$noteName$octave" to exactFrequency
        }
    }
}
