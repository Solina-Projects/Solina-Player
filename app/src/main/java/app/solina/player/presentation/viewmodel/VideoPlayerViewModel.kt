package app.solina.player.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import app.solina.player.data.model.VideoItem
import app.solina.player.data.preferences.UserPreferencesRepository
import app.solina.player.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A dedicated video player, separate from the music [PlayerViewModel] /
 * MusicService (whose renderers factory deliberately strips video).
 *
 * One ExoPlayer per video screen, built with the default renderers factory
 * so video and text (subtitle) tracks render, with the FFmpeg extension
 * enabled so containers/codecs the platform cannot decode get a second
 * chance through FFmpeg. Audio-only handling, crossfade and the music queue
 * are untouched.
 *
 * Folders + Player feedback (2026-11-16): the extra player controls
 * (aspect, speed, volume, audio track, sleep timer, night light,
 * background playback) and the resume point are persisted in the shared
 * settings DataStore through [UserPreferencesRepository].
 */
@UnstableApi
@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val videoRepository: VideoRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private var exoPlayer: ExoPlayer? = null

    private val _player = MutableStateFlow<ExoPlayer?>(null)
    val player: StateFlow<ExoPlayer?> = _player.asStateFlow()

    private val _currentVideo = MutableStateFlow<VideoItem?>(null)
    val currentVideo: StateFlow<VideoItem?> = _currentVideo.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** Long-press speed bar (2026-11-16): 8 options, 0.1x–4x. */
    val availableSpeeds: List<Float> = listOf(0.1f, 0.25f, 0.5f, 0.75f, 1f, 1.5f, 2f, 4f)

    // ---- Persisted player settings (Folders + Player, 2026-11-16) ----

    /** "FIT" / "FILL" / "ZOOM" — applied to the PlayerView resize mode. */
    val aspectMode: StateFlow<String> =
        userPreferencesRepository.videoAspectModeFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "FIT")

    fun cycleAspectMode() {
        val next = when (aspectMode.value) {
            "FIT" -> "FILL"
            "FILL" -> "ZOOM"
            else -> "FIT"
        }
        viewModelScope.launch { userPreferencesRepository.setVideoAspectMode(next) }
    }

    /** Audio keeps playing when the app goes to the background (like music). */
    val backgroundPlayback: StateFlow<Boolean> =
        userPreferencesRepository.videoBackgroundPlaybackFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setBackgroundPlayback(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setVideoBackgroundPlayback(enabled)
        }
    }

    /** Night light: dims the screen while the video plays. */
    val nightLight: StateFlow<Boolean> =
        userPreferencesRepository.videoNightLightFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setNightLight(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setVideoNightLight(enabled) }
    }

    /** Screen brightness percent (10–100) while night light is on. */
    val brightness: StateFlow<Int> =
        userPreferencesRepository.videoBrightnessFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 40)

    fun setBrightness(percent: Int) {
        viewModelScope.launch { userPreferencesRepository.setVideoBrightness(percent) }
    }

    private val _volume = MutableStateFlow(1f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    fun setVolume(volume: Float) {
        _volume.value = volume.coerceIn(0f, 1f)
        exoPlayer?.volume = _volume.value
        viewModelScope.launch { userPreferencesRepository.setVideoVolume(_volume.value) }
    }

    /**
     * Live volume change (swipe gesture, 2026-11-16): applies at once
     * without a DataStore write per move; the gesture end persists it
     * through [setVolume].
     */
    fun setVolumeLive(volume: Float) {
        _volume.value = volume.coerceIn(0f, 1f)
        exoPlayer?.volume = _volume.value
    }

    // ---- Customise Items: which round control buttons are shown. ----

    /** Empty set = show every button. */
    val visibleControlButtons: StateFlow<Set<String>> =
        userPreferencesRepository.videoPlayerVisibleButtonsFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun setVisibleControlButtons(buttonIds: Set<String>) {
        viewModelScope.launch {
            userPreferencesRepository.setVideoPlayerVisibleButtons(buttonIds)
        }
    }

    // ---- Sleep timer: pauses playback after N minutes. ----

    private var sleepJob: Job? = null
    private val _sleepTimerMinutes = MutableStateFlow(0)
    val sleepTimerMinutes: StateFlow<Int> = _sleepTimerMinutes.asStateFlow()

    fun setSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        sleepJob = null
        _sleepTimerMinutes.value = minutes
        if (minutes > 0) {
            sleepJob = viewModelScope.launch {
                delay(minutes * 60_000L)
                exoPlayer?.pause()
                _sleepTimerMinutes.value = 0
            }
        }
    }

    // ---- Audio tracks: one entry per audio track group in the file. ----

    private val _audioTrackLabels = MutableStateFlow<List<String>>(emptyList())
    val audioTrackLabels: StateFlow<List<String>> = _audioTrackLabels.asStateFlow()

    private val _selectedAudioTrack = MutableStateFlow(0)
    val selectedAudioTrack: StateFlow<Int> = _selectedAudioTrack.asStateFlow()

    private fun refreshAudioTracks() {
        val player = exoPlayer ?: return
        val audioGroups = player.currentTracks.groups
            .filter { it.type == C.TRACK_TYPE_AUDIO }
        _audioTrackLabels.value = audioGroups.mapIndexed { index, group ->
            val format = group.mediaTrackGroup.getFormat(0)
            format.label?.takeIf { it.isNotBlank() }
                ?: format.language?.takeIf { it.isNotBlank() }
                ?: "Track ${index + 1}"
        }
        _selectedAudioTrack.value = audioGroups.indexOfFirst { group ->
            group.isSelected
        }.coerceAtLeast(0)
    }

    fun selectAudioTrack(index: Int) {
        val player = exoPlayer ?: return
        val audioGroups = player.currentTracks.groups
            .filter { it.type == C.TRACK_TYPE_AUDIO }
        val group = audioGroups.getOrNull(index) ?: return
        val override = TrackSelectionOverride(group.mediaTrackGroup, 0)
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setOverrideForType(override)
            .build()
        _selectedAudioTrack.value = index
    }

    // ---- Subtitle tracks (text tracks in the file). -1 = off. ----

    private val _subtitleTrackLabels = MutableStateFlow<List<String>>(emptyList())
    val subtitleTrackLabels: StateFlow<List<String>> = _subtitleTrackLabels.asStateFlow()

    private val _selectedSubtitleTrack = MutableStateFlow(-1)
    val selectedSubtitleTrack: StateFlow<Int> = _selectedSubtitleTrack.asStateFlow()

    private fun refreshSubtitleTracks() {
        val player = exoPlayer ?: return
        val textGroups = player.currentTracks.groups
            .filter { it.type == C.TRACK_TYPE_TEXT }
        _subtitleTrackLabels.value = textGroups.mapIndexed { index, group ->
            val format = group.mediaTrackGroup.getFormat(0)
            format.label?.takeIf { it.isNotBlank() }
                ?: format.language?.takeIf { it.isNotBlank() }
                ?: "Subtitle ${index + 1}"
        }
        _selectedSubtitleTrack.value = textGroups.indexOfFirst { group ->
            group.isSelected
        }.let { if (it < 0) -1 else it }
    }

    /** index -1 turns subtitles off; otherwise selects that text track. */
    fun selectSubtitleTrack(index: Int) {
        val player = exoPlayer ?: return
        if (index < 0) {
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
            _selectedSubtitleTrack.value = -1
            return
        }
        val textGroups = player.currentTracks.groups
            .filter { it.type == C.TRACK_TYPE_TEXT }
        val group = textGroups.getOrNull(index) ?: return
        val override = TrackSelectionOverride(group.mediaTrackGroup, 0)
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .setOverrideForType(override)
            .build()
        _selectedSubtitleTrack.value = index
    }

    // ---- Shuffle / loop (Home-entry + Player, 2026-11-16). ----

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    fun toggleShuffle() {
        _shuffleEnabled.value = !_shuffleEnabled.value
        exoPlayer?.shuffleModeEnabled = _shuffleEnabled.value
    }

    /** Player.REPEAT_MODE_OFF / ONE / ALL, cycled by the Loop button. */
    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    fun cycleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
        exoPlayer?.repeatMode = _repeatMode.value
    }

    // ---- Mute: remembers the volume it replaced. ----

    private var preMuteVolume = 1f
    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    fun toggleMute() {
        if (_isMuted.value) {
            _isMuted.value = false
            setVolume(preMuteVolume)
        } else {
            preMuteVolume = _volume.value.coerceAtLeast(0.05f)
            _isMuted.value = true
            setVolume(0f)
        }
    }

    // ---- A-B repeat: loops the segment between A and B. ----

    /** -1 = point not set. */
    private val _aPointMs = MutableStateFlow(-1L)
    val aPointMs: StateFlow<Long> = _aPointMs.asStateFlow()
    private val _bPointMs = MutableStateFlow(-1L)
    val bPointMs: StateFlow<Long> = _bPointMs.asStateFlow()

    private var abRepeatJob: Job? = null

    /**
     * First tap sets A (current position), second tap sets B and starts
     * looping A→B, third tap clears both points.
     */
    fun tapABRepeat() {
        val player = exoPlayer
        if (_aPointMs.value < 0) {
            _aPointMs.value = player?.currentPosition ?: 0L
            return
        }
        if (_bPointMs.value < 0) {
            val b = player?.currentPosition ?: 0L
            if (b <= _aPointMs.value) {
                // B must be after A — restart from a fresh A point.
                _aPointMs.value = b
                return
            }
            _bPointMs.value = b
            abRepeatJob?.cancel()
            abRepeatJob = viewModelScope.launch {
                while (isActive) {
                    delay(200)
                    val p = exoPlayer ?: break
                    if (p.currentPosition >= _bPointMs.value && _bPointMs.value > 0) {
                        p.seekTo(_aPointMs.value)
                    }
                }
            }
            return
        }
        clearABRepeat()
    }

    fun clearABRepeat() {
        abRepeatJob?.cancel()
        abRepeatJob = null
        _aPointMs.value = -1L
        _bPointMs.value = -1L
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            _isLoading.value = playbackState == Player.STATE_BUFFERING
        }

        override fun onTracksChanged(tracks: Tracks) {
            refreshAudioTracks()
            refreshSubtitleTracks()
        }

        // Queue playback (Play all): keep the top-bar title in sync when the
        // player steps to the next video in the queue.
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val uri = mediaItem?.localConfiguration?.uri?.toString() ?: return
            viewModelScope.launch {
                _currentVideo.value = videoRepository.getVideoByContentUri(uri)
            }
        }
    }

    init {
        // Restore the saved speed and volume before the first play.
        viewModelScope.launch {
            _playbackSpeed.value = userPreferencesRepository.videoPlaybackSpeedFlow.first()
            _volume.value = userPreferencesRepository.videoVolumeFlow.first()
        }
    }

    private fun buildPlayer(): ExoPlayer {
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        return ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .build()
            .apply {
                addListener(playerListener)
                volume = _volume.value
                setPlaybackSpeed(_playbackSpeed.value)
                shuffleModeEnabled = _shuffleEnabled.value
                repeatMode = _repeatMode.value
            }
    }

    fun play(contentUriString: String, startPositionMs: Long = 0L) {
        if (_currentVideo.value?.contentUriString == contentUriString && exoPlayer != null) {
            exoPlayer?.play()
            return
        }
        releasePlayer()
        val player = buildPlayer().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(contentUriString)))
            prepare()
            if (startPositionMs > 0) seekTo(startPositionMs)
            play()
        }
        exoPlayer = player
        _player.value = player
        _isLoading.value = true
    }

    suspend fun loadVideo(contentUriString: String) {
        _currentVideo.value = videoRepository.getVideoByContentUri(contentUriString)
    }

    /**
     * Play-all (bottom-bar feedback, 2026-11-16): play a whole list of
     * videos as one ExoPlayer queue, starting at [startIndex]. The
     * PlayerView next/previous controls step through the queue; the title
     * follows via [Player.Listener.onMediaItemTransition]. Falls back to
     * single-video playback when the list has one item.
     */
    fun playQueue(
        contentUriStrings: List<String>,
        startIndex: Int = 0,
        startPositionMs: Long = 0L
    ) {
        if (contentUriStrings.isEmpty()) return
        if (contentUriStrings.size == 1) {
            play(contentUriStrings.first(), startPositionMs)
            return
        }
        releasePlayer()
        val player = buildPlayer().apply {
            setMediaItems(
                contentUriStrings.map { MediaItem.fromUri(Uri.parse(it)) },
                startIndex.coerceIn(0, contentUriStrings.lastIndex),
                startPositionMs.coerceAtLeast(0L)
            )
            prepare()
            play()
        }
        exoPlayer = player
        _player.value = player
        _isLoading.value = true
    }

    /** Current position, for the resume point saved on leave. */
    fun currentPositionMs(): Long = exoPlayer?.currentPosition ?: 0L

    /**
     * Save the resume point (Folders screen's Resume button). Only
     * mid-video positions count — the very start and the very end are not
     * worth resuming.
     */
    fun saveResumePosition() {
        val video = _currentVideo.value ?: return
        val position = currentPositionMs()
        val duration = exoPlayer?.duration ?: 0L
        viewModelScope.launch {
            if (position > 10_000L && (duration <= 0L || position < duration - 5_000L)) {
                userPreferencesRepository.saveVideoResume(
                    uri = video.contentUriString,
                    positionMs = position,
                    title = video.displayTitle
                )
            }
        }
    }

    fun clearResumePosition() {
        viewModelScope.launch { userPreferencesRepository.clearVideoResume() }
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        exoPlayer?.setPlaybackSpeed(speed)
        viewModelScope.launch {
            userPreferencesRepository.setVideoPlaybackSpeed(speed)
        }
    }

    fun toggleFullscreen() {
        _isFullscreen.value = !_isFullscreen.value
    }

    // ---- Transport (custom bottom bar, gestures, 2026-11-16). ----

    /** Seek to an absolute position, clamped to the video's duration. */
    fun seekTo(positionMs: Long) {
        val player = exoPlayer ?: return
        val duration = player.duration.coerceAtLeast(0L)
        player.seekTo(positionMs.coerceIn(0L, duration))
    }

    /** Relative seek (the ±15 s buttons, swipe seek). */
    fun seekBy(deltaMs: Long) {
        val player = exoPlayer ?: return
        seekTo(player.currentPosition + deltaMs)
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekToNext() {
        exoPlayer?.seekToNext()
    }

    fun seekToPrevious() {
        exoPlayer?.seekToPrevious()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun resume() {
        exoPlayer?.play()
    }

    private fun releasePlayer() {
        sleepJob?.cancel()
        sleepJob = null
        _sleepTimerMinutes.value = 0
        clearABRepeat()
        exoPlayer?.removeListener(playerListener)
        exoPlayer?.release()
        exoPlayer = null
        _player.value = null
    }

    override fun onCleared() {
        releasePlayer()
        super.onCleared()
    }
}
