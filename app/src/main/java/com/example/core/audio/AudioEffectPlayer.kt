package com.example.core.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
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

private const val TAG = "AudioEffectPlayer"

data class PlaybackState(
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val isComparingOriginal: Boolean = false,
    val currentEffect: AudioEffectType = AudioEffectType.ORIGINAL,
    val pitch: Float = 1.0f,
    val speed: Float = 1.0f,
    val intensity: Float = 1.0f,
    val volume: Float = 1.0f,
    val isLooping: Boolean = false,
    val activeAudioFile: File? = null,
    val errorMessage: String? = null
)

class AudioEffectPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var currentFile: File? = null

    fun loadAudio(file: File) {
        if (!file.exists()) {
            _playbackState.value = _playbackState.value.copy(errorMessage = "Audio file not found")
            return
        }
        currentFile = file
        stop()

        try {
            val player = MediaPlayer().apply {
                setDataSource(context, Uri.fromFile(file))
                setOnPreparedListener { mp ->
                    _playbackState.value = _playbackState.value.copy(
                        durationMs = mp.duration,
                        currentPositionMs = 0,
                        isPlaying = false,
                        isPaused = false,
                        activeAudioFile = file,
                        errorMessage = null
                    )
                }
                setOnCompletionListener {
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = false,
                        isPaused = false,
                        currentPositionMs = 0
                    )
                    progressJob?.cancel()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = false,
                        errorMessage = "Playback error ($what)"
                    )
                    true
                }
                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load audio file", e)
            _playbackState.value = _playbackState.value.copy(errorMessage = "Could not load audio: ${e.localizedMessage}")
        }
    }

    fun play() {
        val player = mediaPlayer ?: run {
            currentFile?.let { loadAudio(it) }
            return
        }

        try {
            applyPlaybackParams(player)
            player.start()
            _playbackState.value = _playbackState.value.copy(isPlaying = true, isPaused = false)
            startProgressTracker()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start playback", e)
        }
    }

    fun pause() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                _playbackState.value = _playbackState.value.copy(isPlaying = false, isPaused = true)
                progressJob?.cancel()
            }
        }
    }

    fun togglePlayPause() {
        if (_playbackState.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun stop() {
        progressJob?.cancel()
        progressJob = null
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping player", e)
        } finally {
            mediaPlayer = null
        }
        _playbackState.value = _playbackState.value.copy(
            isPlaying = false,
            isPaused = false,
            currentPositionMs = 0
        )
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.let { player ->
            try {
                player.seekTo(positionMs)
                _playbackState.value = _playbackState.value.copy(currentPositionMs = positionMs)
            } catch (e: Exception) {
                Log.e(TAG, "Seek error", e)
            }
        }
    }

    fun setEffect(effect: AudioEffectType) {
        _playbackState.value = _playbackState.value.copy(
            currentEffect = effect,
            pitch = effect.basePitch,
            speed = effect.baseSpeed,
            intensity = effect.defaultIntensity
        )
        mediaPlayer?.let { applyPlaybackParams(it) }
    }

    fun setPitch(pitch: Float) {
        _playbackState.value = _playbackState.value.copy(pitch = pitch.coerceIn(0.5f, 2.0f))
        mediaPlayer?.let { applyPlaybackParams(it) }
    }

    fun setSpeed(speed: Float) {
        _playbackState.value = _playbackState.value.copy(speed = speed.coerceIn(0.5f, 2.0f))
        mediaPlayer?.let { applyPlaybackParams(it) }
    }

    fun setIntensity(intensity: Float) {
        _playbackState.value = _playbackState.value.copy(intensity = intensity.coerceIn(0f, 1f))
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 2.0f)
        _playbackState.value = _playbackState.value.copy(volume = clamped)
        val normalizedVol = (clamped / 2.0f).coerceIn(0f, 1f)
        mediaPlayer?.setVolume(normalizedVol, normalizedVol)
    }

    fun toggleCompareOriginal(comparing: Boolean) {
        _playbackState.value = _playbackState.value.copy(isComparingOriginal = comparing)
        mediaPlayer?.let { applyPlaybackParams(it) }
    }

    private fun applyPlaybackParams(player: MediaPlayer) {
        try {
            val state = _playbackState.value
            val targetPitch = if (state.isComparingOriginal) 1.0f else state.pitch.coerceIn(0.5f, 2.0f)
            val targetSpeed = if (state.isComparingOriginal) 1.0f else state.speed.coerceIn(0.5f, 2.0f)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val params = PlaybackParams().apply {
                    pitch = targetPitch
                    speed = targetSpeed
                }
                player.playbackParams = params
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying playback params", e)
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _playbackState.value.isPlaying) {
                val current = try {
                    mediaPlayer?.currentPosition ?: 0
                } catch (e: Exception) {
                    0
                }
                _playbackState.value = _playbackState.value.copy(currentPositionMs = current)
                delay(80)
            }
        }
    }

    fun release() {
        stop()
    }
}
