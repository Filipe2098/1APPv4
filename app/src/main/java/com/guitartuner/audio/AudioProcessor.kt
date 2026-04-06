package com.guitartuner.audio

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

data class AudioResult(
    val frequency: Double,
    val volume: Float
)

class AudioProcessor(
    private val sampleRate: Int = 44100,
    private val bufferSize: Int = 8192
) {
    private val pitchDetector = PitchDetector(sampleRate)
    private var audioRecord: AudioRecord? = null
    private var processingJob: Job? = null
    private var isRecording = false

    private val _audioResults = MutableSharedFlow<AudioResult>(extraBufferCapacity = 1)
    val audioResults: SharedFlow<AudioResult> = _audioResults

    fun start(context: android.content.Context): Boolean {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        )

        val actualBufferSize = maxOf(bufferSize * 4, minBufferSize)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_FLOAT,
                actualBufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                return false
            }

            audioRecord?.startRecording()
            isRecording = true

            processingJob = CoroutineScope(Dispatchers.Default).launch {
                val readBuffer = FloatArray(bufferSize)
                while (isActive && isRecording) {
                    val read = audioRecord?.read(
                        readBuffer, 0, bufferSize,
                        AudioRecord.READ_BLOCKING
                    ) ?: -1

                    if (read > 0) {
                        val volume = pitchDetector.calculateRMS(readBuffer)

                        val frequency = if (volume > 0.015f) {
                            pitchDetector.detectPitch(readBuffer)
                        } else {
                            -1.0
                        }

                        _audioResults.tryEmit(AudioResult(frequency, volume))
                    }
                }
            }

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun stop() {
        isRecording = false
        processingJob?.cancel()
        processingJob = null
        try {
            audioRecord?.stop()
        } catch (_: Exception) { }
        audioRecord?.release()
        audioRecord = null
    }
}
