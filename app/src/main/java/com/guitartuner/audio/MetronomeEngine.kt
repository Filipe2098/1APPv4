package com.guitartuner.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlin.math.PI
import kotlin.math.sin

/**
 * Precise-timing metronome locked to the hardware audio clock.
 *
 * Beat intervals are measured in SAMPLES, not milliseconds, so timing
 * accuracy is limited only by the DAC crystal — no coroutine/sleep drift.
 *
 * A dedicated writer thread fills the AudioTrack stream. A separate UI
 * poller fires the beat callback when the AudioTrack's playback head
 * actually reaches each beat boundary, so visual indicators are
 * synchronized with audible clicks — not with buffer-write time.
 */
class MetronomeEngine {

    private var audioTrack: AudioTrack? = null
    private var writerJob: Job? = null
    private var uiJob: Job? = null
    @Volatile private var isPlaying = false

    private val sampleRate = 44100

    private val clickHigh: ShortArray = generateClick(1000.0, 0.03)
    private val clickLow: ShortArray = generateClick(800.0, 0.02)

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
        val bufferBytes = maxOf(minBuffer, sampleRate)

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

        val samplesPerBeat = (sampleRate * 60.0 / bpm).toInt().coerceAtLeast(
            maxOf(clickHigh.size, clickLow.size) + 1
        )

        val scope = CoroutineScope(Dispatchers.Default)

        // Writer: fills the AudioTrack continuously with click + silence per beat.
        writerJob = scope.launch {
            var beat = 0
            while (isActive && isPlaying) {
                val click = if (beat == 0) clickHigh else clickLow
                val beatBuffer = ShortArray(samplesPerBeat)
                System.arraycopy(click, 0, beatBuffer, 0, click.size)

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

                beat = (beat + 1) % beatsPerMeasure
            }
        }

        // UI poller: fires onBeat when the hardware playback head reaches each beat boundary.
        uiJob = scope.launch {
            var currentBeatSample = 0L
            var currentBeatIdx = 0

            while (isActive && isPlaying) {
                val head = track.playbackHeadPosition.toLong() and 0xFFFFFFFFL
                if (head >= currentBeatSample) {
                    val idx = currentBeatIdx
                    withContext(Dispatchers.Main.immediate) { onBeat?.invoke(idx) }
                    currentBeatSample += samplesPerBeat
                    currentBeatIdx = (currentBeatIdx + 1) % beatsPerMeasure
                } else {
                    delay(2)
                }
            }
        }
    }

    fun stop() {
        isPlaying = false
        writerJob?.cancel()
        uiJob?.cancel()
        writerJob = null
        uiJob = null
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
            val envelope = (1.0 - t / durationSec).let { it * it }
            val sample = sin(2.0 * PI * frequency * t) * envelope * 0.8
            samples[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }

        return samples
    }
}
