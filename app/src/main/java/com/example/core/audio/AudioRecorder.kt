package com.example.core.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

private const val TAG = "AudioRecorder"

enum class RecordState {
    IDLE,
    RECORDING,
    PAUSED,
    FINISHED
}

data class RecordingInfo(
    val state: RecordState = RecordState.IDLE,
    val durationMs: Long = 0L,
    val currentAmplitude: Float = 0f,
    val amplitudeHistory: List<Float> = emptyList(),
    val outputFile: File? = null,
    val errorMessage: String? = null
)

class AudioRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _recordingInfo = MutableStateFlow(RecordingInfo())
    val recordingInfo: StateFlow<RecordingInfo> = _recordingInfo.asStateFlow()

    private var startTimeMs: Long = 0L
    private var accumulatedDurationMs: Long = 0L
    private val maxWaveformSamples = 40

    fun startRecording(): Boolean {
        try {
            stopRecording() // Clean up any previous session

            val file = File(context.cacheDir, "rec_${System.currentTimeMillis()}.m4a")
            currentOutputFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            startTimeMs = System.currentTimeMillis()
            accumulatedDurationMs = 0L

            _recordingInfo.value = RecordingInfo(
                state = RecordState.RECORDING,
                outputFile = file,
                amplitudeHistory = List(maxWaveformSamples) { 0.05f }
            )

            startPollingAmplitude()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio recording", e)
            _recordingInfo.value = _recordingInfo.value.copy(
                state = RecordState.IDLE,
                errorMessage = "Microphone error: ${e.localizedMessage ?: "Unable to access mic"}"
            )
            releaseRecorder()
            return false
        }
    }

    fun pauseRecording() {
        if (_recordingInfo.value.state == RecordState.RECORDING && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder?.pause()
                accumulatedDurationMs += System.currentTimeMillis() - startTimeMs
                _recordingInfo.value = _recordingInfo.value.copy(state = RecordState.PAUSED)
            } catch (e: Exception) {
                Log.e(TAG, "Error pausing recording", e)
            }
        }
    }

    fun resumeRecording() {
        if (_recordingInfo.value.state == RecordState.PAUSED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder?.resume()
                startTimeMs = System.currentTimeMillis()
                _recordingInfo.value = _recordingInfo.value.copy(state = RecordState.RECORDING)
            } catch (e: Exception) {
                Log.e(TAG, "Error resuming recording", e)
            }
        }
    }

    fun stopRecording(): File? {
        recordingJob?.cancel()
        recordingJob = null

        val file = currentOutputFile
        try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recorder", e)
        } finally {
            mediaRecorder = null
        }

        val totalDuration = if (_recordingInfo.value.state == RecordState.RECORDING) {
            accumulatedDurationMs + (System.currentTimeMillis() - startTimeMs)
        } else {
            accumulatedDurationMs
        }

        _recordingInfo.value = _recordingInfo.value.copy(
            state = if (file != null && file.exists() && file.length() > 0) RecordState.FINISHED else RecordState.IDLE,
            durationMs = totalDuration,
            currentAmplitude = 0f,
            outputFile = file
        )

        return file
    }

    fun reset() {
        stopRecording()
        _recordingInfo.value = RecordingInfo()
    }

    private fun startPollingAmplitude() {
        recordingJob?.cancel()
        recordingJob = scope.launch {
            val history = _recordingInfo.value.amplitudeHistory.toMutableList()
            while (isActive && _recordingInfo.value.state == RecordState.RECORDING) {
                val rawAmp = try {
                    mediaRecorder?.maxAmplitude ?: 0
                } catch (e: Exception) {
                    0
                }
                // Normalize 0..32767 to 0..1 with logarithmic scaling for pleasing visual dynamics
                val normalizedAmp = (rawAmp.toFloat() / 32767f).coerceIn(0f, 1f)
                val visualAmp = (normalizedAmp * 2.2f).coerceIn(0.04f, 1.0f)

                if (history.size >= maxWaveformSamples) {
                    history.removeAt(0)
                }
                history.add(visualAmp)

                val elapsed = accumulatedDurationMs + (System.currentTimeMillis() - startTimeMs)
                _recordingInfo.value = _recordingInfo.value.copy(
                    durationMs = elapsed,
                    currentAmplitude = visualAmp,
                    amplitudeHistory = history.toList()
                )

                delay(60)
            }
        }
    }

    private fun releaseRecorder() {
        try {
            mediaRecorder?.reset()
            mediaRecorder?.release()
        } catch (ignored: Exception) {
        } finally {
            mediaRecorder = null
        }
    }
}
