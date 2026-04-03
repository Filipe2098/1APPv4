package com.guitartuner.audio

import kotlin.math.abs
import kotlin.math.pow

/**
 * YIN pitch detection algorithm implementation.
 * Detects fundamental frequency with precision suitable for musical tuning.
 */
class PitchDetector(
    private val sampleRate: Int = 44100,
    private val threshold: Double = 0.15
) {
    /**
     * Detect pitch using the YIN algorithm.
     * @param buffer Audio samples as FloatArray
     * @return Detected frequency in Hz, or -1.0 if no pitch detected
     */
    fun detectPitch(buffer: FloatArray): Double {
        val bufferSize = buffer.size
        val halfBuffer = bufferSize / 2

        // Step 1: Difference function
        val difference = FloatArray(halfBuffer)
        for (tau in 0 until halfBuffer) {
            var sum = 0f
            for (i in 0 until halfBuffer) {
                val delta = buffer[i] - buffer[i + tau]
                sum += delta * delta
            }
            difference[tau] = sum
        }

        // Step 2: Cumulative mean normalized difference function
        val cmndf = FloatArray(halfBuffer)
        cmndf[0] = 1f
        var runningSum = 0f
        for (tau in 1 until halfBuffer) {
            runningSum += difference[tau]
            cmndf[tau] = if (runningSum != 0f) {
                difference[tau] * tau / runningSum
            } else {
                1f
            }
        }

        // Step 3: Absolute threshold
        // Find the first tau where cmndf dips below threshold, then find the minimum
        var tauEstimate = -1
        val minTau = (sampleRate / 1200.0).toInt().coerceAtLeast(2) // Max ~1200 Hz
        val maxTau = (sampleRate / 50.0).toInt().coerceAtMost(halfBuffer - 1) // Min ~50 Hz

        for (tau in minTau..maxTau) {
            if (cmndf[tau] < threshold) {
                // Find the local minimum after this point
                var localMin = tau
                while (localMin + 1 <= maxTau && cmndf[localMin + 1] < cmndf[localMin]) {
                    localMin++
                }
                tauEstimate = localMin
                break
            }
        }

        if (tauEstimate == -1) {
            // Fallback: find absolute minimum in range
            var minVal = Float.MAX_VALUE
            for (tau in minTau..maxTau) {
                if (cmndf[tau] < minVal) {
                    minVal = cmndf[tau]
                    tauEstimate = tau
                }
            }
            // Only use fallback if confidence is reasonable
            if (minVal > 0.5) return -1.0
        }

        // Step 4: Parabolic interpolation for sub-sample accuracy
        val betterTau = parabolicInterpolation(cmndf, tauEstimate, halfBuffer)

        return if (betterTau > 0) sampleRate / betterTau else -1.0
    }

    /**
     * Parabolic interpolation around the estimated tau for better precision.
     */
    private fun parabolicInterpolation(array: FloatArray, tau: Int, size: Int): Double {
        if (tau < 1 || tau >= size - 1) return tau.toDouble()

        val s0 = array[tau - 1].toDouble()
        val s1 = array[tau].toDouble()
        val s2 = array[tau + 1].toDouble()

        val adjustment = (s2 - s0) / (2.0 * (2.0 * s1 - s2 - s0))

        return if (abs(adjustment) < 1.0) tau + adjustment else tau.toDouble()
    }

    /**
     * Calculate RMS volume of the buffer.
     * @return Volume level from 0.0 to 1.0
     */
    fun calculateRMS(buffer: FloatArray): Float {
        var sum = 0.0
        for (sample in buffer) {
            sum += (sample * sample).toDouble()
        }
        val rms = kotlin.math.sqrt(sum / buffer.size)
        // Normalize to 0-1 range (typical microphone RMS is 0 to ~0.3)
        return (rms * 3.0).toFloat().coerceIn(0f, 1f)
    }
}
