package com.guitartuner.audio

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.PI

/**
 * Hybrid pitch detector: YIN + Harmonic Product Spectrum.
 *
 * YIN alone has excellent sub-cent time-domain resolution but is prone to octave errors
 * (it sometimes locks to a sub-harmonic or a strong upper harmonic, especially on the
 * low strings of basses, cellos and 7/8-string guitars). We run HPS on the same buffer
 * and use it as an independent oracle: if HPS says the fundamental is roughly half, double
 * or triple what YIN reported, we snap YIN to the correct octave. YIN keeps the final
 * say on cents-level precision through parabolic interpolation.
 */
class PitchDetector(
    private val sampleRate: Int = 44100,
    private val threshold: Double = 0.20
) {
    fun detectPitch(buffer: FloatArray): Double {
        val yinFreq = detectPitchYin(buffer)
        if (yinFreq <= 0.0) return -1.0

        val hpsFreq = detectPitchHps(buffer)
        return reconcileOctave(yinFreq, hpsFreq)
    }

    /**
     * If HPS has a confident estimate and YIN is off by a musical octave (×2, ÷2, ÷3),
     * trust HPS's octave but keep YIN's fine precision by scaling.
     */
    private fun reconcileOctave(yinFreq: Double, hpsFreq: Double): Double {
        if (hpsFreq <= 0.0) return yinFreq

        val ratio = yinFreq / hpsFreq
        // Tolerance ~6% (≈ one semitone) -- enough to catch octave errors without
        // overriding legitimate detection on the correct octave.
        val tol = 0.06

        return when {
            abs(ratio - 1.0) < tol -> yinFreq                                // same octave
            abs(ratio - 2.0) < 2 * tol -> yinFreq / 2.0                      // YIN picked harmonic
            abs(ratio - 0.5) < tol -> yinFreq * 2.0                          // YIN picked sub-harmonic
            abs(ratio - 3.0) < 3 * tol -> yinFreq / 3.0                      // YIN picked 3rd harmonic
            else -> yinFreq // Disagreement too large; trust YIN rather than chase noise.
        }
    }

    private fun detectPitchYin(buffer: FloatArray): Double {
        val bufferSize = buffer.size
        val halfBuffer = bufferSize / 2

        val difference = FloatArray(halfBuffer)
        for (tau in 1 until halfBuffer) {
            var sum = 0f
            for (i in 0 until halfBuffer) {
                val delta = buffer[i] - buffer[i + tau]
                sum += delta * delta
            }
            difference[tau] = sum
        }

        val cmndf = FloatArray(halfBuffer)
        cmndf[0] = 1f
        var runningSum = 0f
        for (tau in 1 until halfBuffer) {
            runningSum += difference[tau]
            cmndf[tau] = if (runningSum > 0f) {
                difference[tau] * tau / runningSum
            } else {
                1f
            }
        }

        // Full instrument range: B0 (30.87 Hz, double bass) to E5 (659 Hz, violin) plus harmonics.
        val minTau = (sampleRate / 900.0).toInt().coerceAtLeast(2)
        val maxTau = (sampleRate / 28.0).toInt().coerceAtMost(halfBuffer - 2)

        var tauEstimate = -1

        var tau = minTau
        while (tau <= maxTau) {
            if (cmndf[tau] < threshold) {
                while (tau + 1 <= maxTau && cmndf[tau + 1] < cmndf[tau]) {
                    tau++
                }
                tauEstimate = tau
                break
            }
            tau++
        }

        if (tauEstimate == -1) {
            var minVal = Float.MAX_VALUE
            for (t in minTau..maxTau) {
                if (cmndf[t] < minVal) {
                    minVal = cmndf[t]
                    tauEstimate = t
                }
            }
            if (minVal > 0.45) return -1.0
        }

        val betterTau = parabolicInterpolation(cmndf, tauEstimate, halfBuffer)
        val frequency = if (betterTau > 0) sampleRate.toDouble() / betterTau else -1.0

        if (frequency < 28.0 || frequency > 900.0) return -1.0
        return frequency
    }

    /**
     * Harmonic Product Spectrum.
     *
     * HPS exploits that a periodic signal has energy at f, 2f, 3f, ...  We compute the
     * magnitude spectrum, downsample it by 2x, 3x, 4x, and multiply the four together.
     * The fundamental is the only bin that survives (all harmonics line up on it), while
     * noise and stray peaks get suppressed. Works especially well on bowed and plucked
     * strings, which have rich harmonic content.
     */
    private fun detectPitchHps(buffer: FloatArray): Double {
        // Power-of-two sub-window for FFT (use largest pow2 ≤ buffer length).
        val fftSize = largestPowerOfTwo(buffer.size).coerceAtMost(8192)
        if (fftSize < 1024) return -1.0

        // Hann window reduces spectral leakage so harmonic peaks are sharp.
        val re = DoubleArray(fftSize)
        val im = DoubleArray(fftSize)
        for (i in 0 until fftSize) {
            val w = 0.5 * (1.0 - cos(2.0 * PI * i / (fftSize - 1)))
            re[i] = buffer[i] * w
        }

        fft(re, im)

        val bins = fftSize / 2
        val mag = DoubleArray(bins)
        for (k in 0 until bins) {
            mag[k] = sqrt(re[k] * re[k] + im[k] * im[k])
        }

        val downsamples = 4 // multiply magnitude × mag@2k × mag@3k × mag@4k
        val hpsLen = bins / downsamples
        val hps = DoubleArray(hpsLen)
        for (k in 0 until hpsLen) {
            var p = mag[k]
            for (d in 2..downsamples) {
                p *= mag[k * d]
            }
            hps[k] = p
        }

        // Search inside the same frequency range as YIN.
        val binWidth = sampleRate.toDouble() / fftSize
        val kMin = (28.0 / binWidth).toInt().coerceAtLeast(1)
        val kMax = (900.0 / binWidth).toInt().coerceAtMost(hpsLen - 2)
        if (kMax <= kMin) return -1.0

        var peakK = kMin
        var peakVal = hps[kMin]
        for (k in kMin..kMax) {
            if (hps[k] > peakVal) {
                peakVal = hps[k]
                peakK = k
            }
        }
        if (peakVal <= 0.0) return -1.0

        // Parabolic interpolation in log-magnitude for ~1/10-bin accuracy.
        val kInterp = if (peakK in 1 until hpsLen - 1) {
            val l = ln(hps[peakK - 1].coerceAtLeast(1e-20))
            val c = ln(hps[peakK].coerceAtLeast(1e-20))
            val r = ln(hps[peakK + 1].coerceAtLeast(1e-20))
            val denom = l - 2.0 * c + r
            if (abs(denom) < 1e-12) peakK.toDouble()
            else peakK + 0.5 * (l - r) / denom
        } else peakK.toDouble()

        return kInterp * binWidth
    }

    private fun largestPowerOfTwo(n: Int): Int {
        var p = 1
        while (p * 2 <= n) p *= 2
        return p
    }

    /**
     * Iterative Cooley-Tukey radix-2 FFT, in place on re/im.
     * Size must be a power of two. O(N log N).
     */
    private fun fft(re: DoubleArray, im: DoubleArray) {
        val n = re.size
        // Bit-reversal permutation.
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                var t = re[i]; re[i] = re[j]; re[j] = t
                t = im[i]; im[i] = im[j]; im[j] = t
            }
        }
        // Butterflies.
        var len = 2
        while (len <= n) {
            val ang = -2.0 * PI / len
            val wlenRe = cos(ang)
            val wlenIm = sin(ang)
            var i = 0
            while (i < n) {
                var wRe = 1.0
                var wIm = 0.0
                val half = len / 2
                for (k in 0 until half) {
                    val uRe = re[i + k]
                    val uIm = im[i + k]
                    val vRe = re[i + k + half] * wRe - im[i + k + half] * wIm
                    val vIm = re[i + k + half] * wIm + im[i + k + half] * wRe
                    re[i + k] = uRe + vRe
                    im[i + k] = uIm + vIm
                    re[i + k + half] = uRe - vRe
                    im[i + k + half] = uIm - vIm
                    val nwRe = wRe * wlenRe - wIm * wlenIm
                    wIm = wRe * wlenIm + wIm * wlenRe
                    wRe = nwRe
                }
                i += len
            }
            len = len shl 1
        }
    }

    private fun parabolicInterpolation(array: FloatArray, tau: Int, size: Int): Double {
        if (tau < 1 || tau >= size - 1) return tau.toDouble()

        val s0 = array[tau - 1].toDouble()
        val s1 = array[tau].toDouble()
        val s2 = array[tau + 1].toDouble()

        val denominator = 2.0 * s1 - s2 - s0
        if (abs(denominator) < 1e-12) return tau.toDouble()

        val adjustment = (s2 - s0) / (2.0 * denominator)

        return if (abs(adjustment) < 1.0) tau + adjustment else tau.toDouble()
    }

    fun calculateRMS(buffer: FloatArray): Float {
        var sum = 0.0
        for (sample in buffer) {
            sum += (sample * sample).toDouble()
        }
        val rms = sqrt(sum / buffer.size)
        return (rms * 3.0).toFloat().coerceIn(0f, 1f)
    }
}
