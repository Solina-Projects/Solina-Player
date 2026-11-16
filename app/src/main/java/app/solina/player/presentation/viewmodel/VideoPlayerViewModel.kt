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

    val availableSpeeds: List<Float> = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

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

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            _isLoading.value = playbackState == Player.STATE_BUFFERING
        }

        override fun onTracksChanged(tracks: Tracks) {
            refreshAudioTracks()
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
