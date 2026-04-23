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
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

data class AudioResult(
    val frequency: Double,
    val volume: Float
)

class AudioProcessor(
    private val preferredSampleRate: Int = 44100,
    private val bufferSize: Int = 8192
) {
    private var pitchDetector = PitchDetector(preferredSampleRate)
    private var audioRecord: AudioRecord? = null
    private var processingJob: Job? = null
    @Volatile private var isRecording = false

    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private var usesFloatEncoding = true
    private var actualSampleRate = preferredSampleRate

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

        try {
            val result = buildAudioRecord()
            if (result == null) {
                Log.e(TAG, "Failed to create AudioRecord with any source/encoding/rate combination")
                return false
            }

            audioRecord = result.record
            usesFloatEncoding = result.isFloat
            actualSampleRate = result.sampleRate

            if (actualSampleRate != preferredSampleRate) {
                pitchDetector = PitchDetector(actualSampleRate)
            }

            Log.i(TAG, "AudioRecord created: source=${result.sourceName}, " +
                    "encoding=${if (result.isFloat) "FLOAT" else "16BIT"}, " +
                    "rate=${result.sampleRate}")

            result.record.startRecording()

            if (result.record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                Log.e(TAG, "AudioRecord failed to enter RECORDING state")
                result.record.release()
                audioRecord = null
                return false
            }

            isRecording = true

            processingJob = CoroutineScope(Dispatchers.Default).launch {
                if (usesFloatEncoding) {
                    readFloat(result.record)
                } else {
                    readShort(result.record)
                }
            }

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio capture", e)
            return false
        }
    }

    private suspend fun readFloat(record: AudioRecord) = coroutineScope {
        val readBuffer = FloatArray(bufferSize)
        while (isActive && isRecording) {
            val read = record.read(
                readBuffer, 0, bufferSize, AudioRecord.READ_BLOCKING
            )
            if (read > 0) {
                processBuffer(readBuffer)
            } else if (read < 0) {
                Log.w(TAG, "AudioRecord.read(float) returned $read")
                delay(10)
            }
        }
    }

    private suspend fun readShort(record: AudioRecord) = coroutineScope {
        val shortBuffer = ShortArray(bufferSize)
        val floatBuffer = FloatArray(bufferSize)
        while (isActive && isRecording) {
            val read = record.read(
                shortBuffer, 0, bufferSize, AudioRecord.READ_BLOCKING
            )
            if (read > 0) {
                for (i in 0 until read) {
                    floatBuffer[i] = shortBuffer[i] / 32768f
                }
                processBuffer(floatBuffer)
            } else if (read < 0) {
                Log.w(TAG, "AudioRecord.read(short) returned $read")
                delay(10)
            }
        }
    }

    private fun processBuffer(buffer: FloatArray) {
        val volume = pitchDetector.calculateRMS(buffer)
        val frequency = if (volume > 0.002f) {
            pitchDetector.detectPitch(buffer)
        } else {
            -1.0
        }
        _audioResults.tryEmit(AudioResult(frequency, volume))
    }

    private data class RecordConfig(
        val record: AudioRecord,
        val isFloat: Boolean,
        val sampleRate: Int,
        val sourceName: String
    )

    private fun buildAudioRecord(): RecordConfig? {
        val sources = buildList {
            add(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                add(MediaRecorder.AudioSource.UNPROCESSED)
            }
            add(MediaRecorder.AudioSource.MIC)
            add(MediaRecorder.AudioSource.DEFAULT)
        }

        val sampleRates = intArrayOf(preferredSampleRate, 48000, 44100, 22050, 16000)
            .distinct().toIntArray()

        // Phase 1: try PCM_FLOAT (best precision) at each source + rate
        for (source in sources) {
            for (rate in sampleRates) {
                val record = tryCreateRecord(source, AudioFormat.ENCODING_PCM_FLOAT, rate)
                if (record != null) {
                    return RecordConfig(record, true, rate, sourceLabel(source))
                }
            }
        }

        // Phase 2: fall back to PCM_16BIT (universally supported)
        for (source in sources) {
            for (rate in sampleRates) {
                val record = tryCreateRecord(source, AudioFormat.ENCODING_PCM_16BIT, rate)
                if (record != null) {
                    return RecordConfig(record, false, rate, sourceLabel(source))
                }
            }
        }

        return null
    }

    private fun tryCreateRecord(source: Int, encoding: Int, sampleRate: Int): AudioRecord? {
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, encoding
        )
        if (minBuf <= 0) return null

        val bytesPerSample = if (encoding == AudioFormat.ENCODING_PCM_FLOAT) 4 else 2
        val bufBytes = maxOf(bufferSize * bytesPerSample, minBuf)

        val record = try {
            AudioRecord.Builder()
                .setAudioSource(source)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(encoding)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufBytes)
                .build()
        } catch (e: Exception) {
            Log.d(TAG, "AudioRecord.Builder failed: source=${sourceLabel(source)}, " +
                    "enc=${if (encoding == AudioFormat.ENCODING_PCM_FLOAT) "FLOAT" else "16BIT"}, " +
                    "rate=$sampleRate — ${e.message}")
            null
        }

        if (record != null && record.state == AudioRecord.STATE_INITIALIZED) {
            return record
        }
        record?.release()
        return null
    }

    private fun sourceLabel(source: Int): String = when (source) {
        MediaRecorder.AudioSource.UNPROCESSED -> "UNPROCESSED"
        MediaRecorder.AudioSource.VOICE_RECOGNITION -> "VOICE_RECOGNITION"
        MediaRecorder.AudioSource.MIC -> "MIC"
        MediaRecorder.AudioSource.DEFAULT -> "DEFAULT"
        else -> "SOURCE_$source"
    }

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
                .setOnAudioFocusChangeListener { }
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

    companion object {
        private const val TAG = "AudioProcessor"
    }
}
