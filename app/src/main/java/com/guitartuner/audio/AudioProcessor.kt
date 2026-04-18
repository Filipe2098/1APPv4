package com.guitartuner.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

data class AudioResult(
    val frequency: Double,
    val volume: Float
)

/**
 * Captures microphone audio and runs pitch detection.
 *
 * Designed to coexist with other audio apps:
 *  - Uses AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK so background music just ducks (does not pause).
 *  - Prefers AudioSource.UNPROCESSED (raw mic, no echo cancel / AGC / noise suppression), which
 *    captures instrument harmonics faithfully. Falls back to VOICE_RECOGNITION, then MIC.
 *  - Keeps recording while the screen is off (the foreground service in MainActivity handles that).
 */
class AudioProcessor(
    private val sampleRate: Int = 44100,
    private val bufferSize: Int = 8192
) {
    private val pitchDetector = PitchDetector(sampleRate)
    private var audioRecord: AudioRecord? = null
    private var processingJob: Job? = null
    @Volatile private var isRecording = false

    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null

    private val _audioResults = MutableSharedFlow<AudioResult>(extraBufferCapacity = 1)
    val audioResults: SharedFlow<AudioResult> = _audioResults

    fun start(context: Context): Boolean {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        requestAudioFocus(context)

        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        )

        val actualBufferSize = maxOf(bufferSize * 4, minBufferSize)

        try {
            audioRecord = buildAudioRecord(actualBufferSize)

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

    /**
     * Tries UNPROCESSED (Android 7+ raw mic), then VOICE_RECOGNITION (disables voice DSP),
     * finally falls back to MIC.
     */
    private fun buildAudioRecord(bufferBytes: Int): AudioRecord? {
        val sources = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                add(MediaRecorder.AudioSource.UNPROCESSED)
            }
            add(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            add(MediaRecorder.AudioSource.MIC)
        }

        for (source in sources) {
            val record = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    AudioRecord.Builder()
                        .setAudioSource(source)
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setSampleRate(sampleRate)
                                .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                                .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(bufferBytes)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    AudioRecord(
                        source,
                        sampleRate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_FLOAT,
                        bufferBytes
                    )
                }
            } catch (_: Exception) {
                null
            }

            if (record != null && record.state == AudioRecord.STATE_INITIALIZED) {
                return record
            }
            record?.release()
        }
        return null
    }

    /**
     * Requests transient ducking focus: if music is playing, Android lowers its volume while
     * we listen -- but never kills the other app. If focus is denied we still try to record.
     */
    private fun requestAudioFocus(context: Context) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        audioManager = am

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attrs)
                .setWillPauseWhenDucked(false)
                .setOnAudioFocusChangeListener { /* ignore transient changes */ }
                .build()
            focusRequest = req
            am.requestAudioFocus(req)
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }
    }

    private fun releaseAudioFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { am.abandonAudioFocusRequest(it) }
            focusRequest = null
        } else {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(null)
        }
        audioManager = null
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
        releaseAudioFocus()
    }
}
