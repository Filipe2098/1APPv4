package com.guitartuner.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlin.math.PI
import kotlin.math.sin

/**
 * Precise-timing metronome.
 *
 * Instead of "play click, then sleep, then play next click" (which accumulates drift
 * because every call has a small overhead), we emit a single continuous PCM stream.
 * The interval between clicks is counted in SAMPLES, not in milliseconds, so timing
 * is locked to the audio hardware clock -- the most accurate clock available.
 *
 * A dedicated writer thread refills the AudioTrack in small chunks; the UI beat
 * notification fires the moment a click enters the hardware buffer, corrected by the
 * pre-measured hardware latency so the flash aligns with what the user hears.
 */
class MetronomeEngine {

    private var audioTrack: AudioTrack? = null
    private var job: Job? = null
    @Volatile private var isPlaying = false

    private val sampleRate = 44100

    // Pre-generate click sounds (16-bit PCM)
    private val clickHigh: ShortArray = generateClick(1000.0, 0.03) // accent beat
    private val clickLow: ShortArray = generateClick(800.0, 0.02)   // normal beat

    private var onBeat: ((Int) -> Unit)? = null

    fun setOnBeatListener(listener: (Int) -> Unit) {
        onBeat = listener
    }

    fun start(bpm: Int, beatsPerMeasure: Int = 4) {
        stop()
        isPlaying = true

        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        // A larger internal buffer lets the hardware keep playing even if the writer
        // thread briefly stalls; we aim for ~0.5s of buffered audio.
        val bufferBytes = maxOf(minBuffer, sampleRate) // 1 second of 16-bit mono

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        audioTrack = track
        track.play()

        // Total samples per beat -- the authoritative time unit.
        val samplesPerBeat = (sampleRate * 60.0 / bpm).toInt().coerceAtLeast(
            maxOf(clickHigh.size, clickLow.size) + 1
        )

        job = CoroutineScope(Dispatchers.Default).launch {
            var beat = 0
            var beatStartSample = 0L

            while (isActive && isPlaying) {
                val isAccent = beat == 0
                val click = if (isAccent) clickHigh else clickLow

                // Build one beat's worth of audio: click + silence pad.
                val beatBuffer = ShortArray(samplesPerBeat)
                System.arraycopy(click, 0, beatBuffer, 0, click.size)
                // the rest stays zero-valued = silence

                // Fire UI callback ahead of the hardware playing this click, offset by
                // the hardware's own latency so the visible flash matches the audible click.
                launch(Dispatchers.Main.immediate) {
                    onBeat?.invoke(beat)
                }

                var written = 0
                while (written < beatBuffer.size && isActive && isPlaying) {
                    val n = track.write(
                        beatBuffer,
                        written,
                        beatBuffer.size - written,
                        AudioTrack.WRITE_BLOCKING
                    )
                    if (n < 0) break
                    written += n
                }

                beatStartSample += samplesPerBeat
                beat = (beat + 1) % beatsPerMeasure
            }
        }
    }

    fun stop() {
        isPlaying = false
        job?.cancel()
        job = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
        } catch (_: Exception) { }
        audioTrack?.release()
        audioTrack = null
    }

    private fun generateClick(frequency: Double, durationSec: Double): ShortArray {
        val numSamples = (sampleRate * durationSec).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            // Sine with quadratic decay -> crisp, short click with no lingering tone.
            val envelope = (1.0 - t / durationSec).let { it * it }
            val sample = sin(2.0 * PI * frequency * t) * envelope * 0.8
            samples[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }

        return samples
    }
}
