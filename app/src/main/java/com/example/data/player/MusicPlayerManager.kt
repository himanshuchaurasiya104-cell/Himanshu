package com.example.data.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.net.Uri
import android.os.Build
import com.example.data.model.EqualizerPreset
import com.example.data.model.EqualizerPresets
import com.example.data.model.PlaybackMode
import com.example.data.model.SleepTimerOption
import com.example.data.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

data class EqualizerUiState(
    val currentPreset: String = "Normal",
    val bandLevels: List<Int> = listOf(0, 0, 0, 0, 0), // in dB (-10 to +10)
    val bassBoost: Int = 10, // 0 - 100%
    val virtualizer: Int = 10, // 0 - 100%
    val isEnabled: Boolean = true
)

class MusicPlayerManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var mediaPlayer: MediaPlayer? = null

    // Audio Effects
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    // State Flows
    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _playbackMode = MutableStateFlow(PlaybackMode.NORMAL)
    val playbackMode: StateFlow<PlaybackMode> = _playbackMode.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _queueIndex = MutableStateFlow(0)
    val queueIndex: StateFlow<Int> = _queueIndex.asStateFlow()

    private val _sleepTimerOption = MutableStateFlow(SleepTimerOption.OFF)
    val sleepTimerOption: StateFlow<SleepTimerOption> = _sleepTimerOption.asStateFlow()

    private val _sleepTimerRemainingSeconds = MutableStateFlow<Int?>(null)
    val sleepTimerRemainingSeconds: StateFlow<Int?> = _sleepTimerRemainingSeconds.asStateFlow()

    private val _equalizerState = MutableStateFlow(EqualizerUiState())
    val equalizerState: StateFlow<EqualizerUiState> = _equalizerState.asStateFlow()

    // Real-time Visualizer bars (16 frequency bands normalized 0.0f..1.0f)
    private val _waveformBars = MutableStateFlow(List(16) { 0.15f })
    val waveformBars: StateFlow<List<Float>> = _waveformBars.asStateFlow()

    private var progressJob: Job? = null
    private var visualizerJob: Job? = null
    private var sleepTimerJob: Job? = null

    init {
        startProgressTracker()
        startVisualizerEngine()
    }

    fun setQueue(tracks: List<Track>, startIndex: Int = 0, autoPlay: Boolean = true) {
        if (tracks.isEmpty()) return
        _queue.value = tracks
        val safeIndex = startIndex.coerceIn(0, tracks.lastIndex)
        _queueIndex.value = safeIndex
        if (autoPlay) {
            playTrack(tracks[safeIndex])
        } else {
            _currentTrack.value = tracks[safeIndex]
        }
    }

    fun playTrack(track: Track) {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            releaseAudioFx()

            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                if (track.uri.startsWith("content://") || track.uri.startsWith("android.resource://")) {
                    setDataSource(context, Uri.parse(track.uri))
                } else {
                    setDataSource(track.uri)
                }

                setOnPreparedListener { mp ->
                    mp.start()
                    setupAudioFx(mp.audioSessionId)
                    applySpeed(mp, _playbackSpeed.value)
                    _isPlaying.value = true
                }

                setOnCompletionListener {
                    handleTrackCompletion()
                }

                setOnErrorListener { _, what, extra ->
                    _isPlaying.value = false
                    false
                }

                prepareAsync()
            }

            mediaPlayer = player
            _currentTrack.value = track
            _currentPositionMs.value = 0L

            // Sync queue index if in queue
            val idx = _queue.value.indexOfFirst { it.id == track.id }
            if (idx >= 0) {
                _queueIndex.value = idx
            }

        } catch (e: Exception) {
            e.printStackTrace()
            _isPlaying.value = false
        }
    }

    private fun setupAudioFx(audioSessionId: Int) {
        try {
            if (audioSessionId == 0) return
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = _equalizerState.value.isEnabled
            }
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = _equalizerState.value.isEnabled
                val strength = (_equalizerState.value.bassBoost * 10).toShort()
                setStrength(strength)
            }
            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = _equalizerState.value.isEnabled
                val strength = (_equalizerState.value.virtualizer * 10).toShort()
                setStrength(strength)
            }
            // Apply current band levels
            applyBandLevelsToFx(_equalizerState.value.bandLevels)
        } catch (e: Throwable) {
            // AudioFx might not be supported on all virtual emulators, handled gracefully
        }
    }

    private fun releaseAudioFx() {
        try {
            equalizer?.release()
            equalizer = null
            bassBoost?.release()
            bassBoost = null
            virtualizer?.release()
            virtualizer = null
        } catch (_: Throwable) {}
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: run {
            val track = _currentTrack.value ?: _queue.value.firstOrNull()
            if (track != null) playTrack(track)
            return
        }

        if (player.isPlaying) {
            player.pause()
            _isPlaying.value = false
        } else {
            player.start()
            _isPlaying.value = true
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
            }
        }
    }

    fun resume() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                _isPlaying.value = true
            }
        } ?: run {
            _currentTrack.value?.let { playTrack(it) }
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let {
            val safePos = positionMs.coerceIn(0, it.duration.toLong().coerceAtLeast(0L))
            it.seekTo(safePos.toInt())
            _currentPositionMs.value = safePos
        }
    }

    fun skipNext() {
        val q = _queue.value
        if (q.isEmpty()) return

        val nextIndex = when (_playbackMode.value) {
            PlaybackMode.SHUFFLE -> {
                if (q.size == 1) 0 else {
                    var r = Random.nextInt(q.size)
                    while (r == _queueIndex.value && q.size > 1) {
                        r = Random.nextInt(q.size)
                    }
                    r
                }
            }
            PlaybackMode.REPEAT_ONE -> _queueIndex.value
            PlaybackMode.REPEAT_ALL, PlaybackMode.NORMAL -> {
                if (_queueIndex.value + 1 < q.size) {
                    _queueIndex.value + 1
                } else if (_playbackMode.value == PlaybackMode.REPEAT_ALL) {
                    0
                } else {
                    -1
                }
            }
        }

        if (nextIndex in q.indices) {
            _queueIndex.value = nextIndex
            playTrack(q[nextIndex])
        }
    }

    fun skipPrevious() {
        val player = mediaPlayer
        // If current position > 3 seconds, replay current song
        if (player != null && player.currentPosition > 3000) {
            seekTo(0)
            return
        }

        val q = _queue.value
        if (q.isEmpty()) return

        val prevIndex = when (_playbackMode.value) {
            PlaybackMode.SHUFFLE -> Random.nextInt(q.size)
            PlaybackMode.REPEAT_ONE -> _queueIndex.value
            PlaybackMode.REPEAT_ALL, PlaybackMode.NORMAL -> {
                if (_queueIndex.value - 1 >= 0) {
                    _queueIndex.value - 1
                } else if (_playbackMode.value == PlaybackMode.REPEAT_ALL) {
                    q.lastIndex
                } else {
                    0
                }
            }
        }

        if (prevIndex in q.indices) {
            _queueIndex.value = prevIndex
            playTrack(q[prevIndex])
        }
    }

    fun setPlaybackMode(mode: PlaybackMode) {
        _playbackMode.value = mode
    }

    fun cyclePlaybackMode() {
        val nextMode = when (_playbackMode.value) {
            PlaybackMode.NORMAL -> PlaybackMode.REPEAT_ALL
            PlaybackMode.REPEAT_ALL -> PlaybackMode.REPEAT_ONE
            PlaybackMode.REPEAT_ONE -> PlaybackMode.SHUFFLE
            PlaybackMode.SHUFFLE -> PlaybackMode.NORMAL
        }
        _playbackMode.value = nextMode
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        mediaPlayer?.let { applySpeed(it, speed) }
    }

    private fun applySpeed(player: MediaPlayer, speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val params = player.playbackParams ?: PlaybackParams()
                params.speed = speed
                player.playbackParams = params
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setSleepTimer(option: SleepTimerOption) {
        _sleepTimerOption.value = option
        sleepTimerJob?.cancel()

        if (option == SleepTimerOption.OFF) {
            _sleepTimerRemainingSeconds.value = null
            return
        }

        if (option == SleepTimerOption.END_OF_TRACK) {
            _sleepTimerRemainingSeconds.value = -1
            return
        }

        val totalSeconds = option.minutes * 60
        _sleepTimerRemainingSeconds.value = totalSeconds

        sleepTimerJob = scope.launch {
            var rem = totalSeconds
            while (rem > 0 && isActive) {
                delay(1000)
                rem--
                _sleepTimerRemainingSeconds.value = rem
            }
            // Timer expired: stop playback smoothly
            pause()
            _sleepTimerOption.value = SleepTimerOption.OFF
            _sleepTimerRemainingSeconds.value = null
        }
    }

    private fun handleTrackCompletion() {
        if (_sleepTimerOption.value == SleepTimerOption.END_OF_TRACK) {
            pause()
            _sleepTimerOption.value = SleepTimerOption.OFF
            _sleepTimerRemainingSeconds.value = null
            return
        }

        when (_playbackMode.value) {
            PlaybackMode.REPEAT_ONE -> {
                seekTo(0)
                mediaPlayer?.start()
                _isPlaying.value = true
            }
            else -> skipNext()
        }
    }

    // Equalizer & AudioFX Controls
    fun setEqualizerPreset(preset: EqualizerPreset) {
        _equalizerState.value = _equalizerState.value.copy(
            currentPreset = preset.name,
            bandLevels = preset.levels,
            bassBoost = preset.bassBoostPercent,
            virtualizer = preset.virtualizerPercent
        )
        applyBandLevelsToFx(preset.levels)
        setBassBoost(preset.bassBoostPercent)
        setVirtualizer(preset.virtualizerPercent)
    }

    fun setBandLevel(bandIndex: Int, levelDb: Int) {
        val currentLevels = _equalizerState.value.bandLevels.toMutableList()
        if (bandIndex in currentLevels.indices) {
            currentLevels[bandIndex] = levelDb.coerceIn(-10, 10)
            _equalizerState.value = _equalizerState.value.copy(
                currentPreset = "Custom",
                bandLevels = currentLevels
            )
            applyBandLevelsToFx(currentLevels)
        }
    }

    fun setBassBoost(percent: Int) {
        val p = percent.coerceIn(0, 100)
        _equalizerState.value = _equalizerState.value.copy(bassBoost = p)
        try {
            bassBoost?.let {
                if (it.strengthSupported) {
                    it.setStrength((p * 10).toShort())
                }
            }
        } catch (_: Throwable) {}
    }

    fun setVirtualizer(percent: Int) {
        val p = percent.coerceIn(0, 100)
        _equalizerState.value = _equalizerState.value.copy(virtualizer = p)
        try {
            virtualizer?.let {
                if (it.strengthSupported) {
                    it.setStrength((p * 10).toShort())
                }
            }
        } catch (_: Throwable) {}
    }

    private fun applyBandLevelsToFx(levels: List<Int>) {
        try {
            equalizer?.let { eq ->
                val numBands = eq.numberOfBands.toInt()
                for (i in 0 until minOf(numBands, levels.size)) {
                    val millibels = (levels[i] * 100).toShort()
                    eq.setBandLevel(i.toShort(), millibels)
                }
            }
        } catch (_: Throwable) {}
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                delay(300)
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        _currentPositionMs.value = it.currentPosition.toLong()
                    }
                }
            }
        }
    }

    private fun startVisualizerEngine() {
        visualizerJob?.cancel()
        visualizerJob = scope.launch {
            var step = 0.0
            while (isActive) {
                delay(60)
                step += 0.25
                if (_isPlaying.value) {
                    val boostMult = 1.0f + (_equalizerState.value.bassBoost / 200f)
                    val bars = List(16) { i ->
                        val phase = step + i * 0.4
                        val wave = (sin(phase) * 0.5 + 0.5).toFloat()
                        val noise = Random.nextFloat() * 0.3f
                        val bandGain = (1.0f + (_equalizerState.value.bandLevels.getOrElse(i % 5) { 0 } / 15f))
                        val height = ((wave * 0.7f + noise) * bandGain * boostMult).coerceIn(0.08f, 1.0f)
                        height
                    }
                    _waveformBars.value = bars
                } else {
                    _waveformBars.value = List(16) { 0.10f }
                }
            }
        }
    }

    fun release() {
        progressJob?.cancel()
        visualizerJob?.cancel()
        sleepTimerJob?.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
        releaseAudioFx()
    }
}
