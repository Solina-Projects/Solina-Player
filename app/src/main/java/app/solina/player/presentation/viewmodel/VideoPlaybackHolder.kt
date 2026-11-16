package app.solina.player.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import app.solina.player.data.model.VideoItem
import app.solina.player.data.preferences.UserPreferencesRepository
import app.solina.player.data.repository.VideoRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * App-scoped owner of the video ExoPlayer (mini-player feedback,
 * 2026-11-16).
 *
 * The video player screen's [VideoPlayerViewModel] is navigation-scoped, so
 * it used to be cleared — and the player released — the moment the user
 * left the player. That made a "last played video" bar impossible: there
 * was no player left to show or control. The player now lives here, in a
 * @Singleton, and the ViewModel only drives it while the player screen is
 * open. Leaving the player pauses it when background playback is off (via
 * [VideoPlayerViewModel.onLeavePlayer]) but never releases it, so the
 * [app.solina.player.presentation.components.VideoMiniPlayerBar] can keep
 * showing the last video — a small live preview while it plays, the saved
 * thumbnail + position when it is paused — and play / pause / skip it.
 *
 * Limits, stated honestly: there is still no foreground service, so this
 * keeps playback alive while the user moves around *inside* the app. When
 * the app itself goes to the background it follows the background-playback
 * toggle (off = the player screen's ON_STOP observer pauses it; on = the
 * audio keeps playing), and if Android kills the process the player is
 * gone — the mini bar then falls back to the saved resume point in
 * DataStore.
 */
@OptIn(UnstableApi::class)
@Singleton
class VideoPlaybackHolder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val videoRepository: VideoRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var exoPlayer: ExoPlayer? = null

    /** The live player, or null before the first video is played. */
    val playerOrNull: ExoPlayer?
        get() = exoPlayer

    private val _player = MutableStateFlow<ExoPlayer?>(null)
    val player: StateFlow<ExoPlayer?> = _player.asStateFlow()

    private val _currentVideo = MutableStateFlow<VideoItem?>(null)
    val currentVideo: StateFlow<VideoItem?> = _currentVideo.asStateFlow()

    /** The queue the player was given (Play all), or a single-item list. */
    private val _queueUris = MutableStateFlow<List<String>>(emptyList())
    val queueUris: StateFlow<List<String>> = _queueUris.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    /** Last track set, for the subtitle/audio menus (mirrored by the VM). */
    private val _tracks = MutableStateFlow<Tracks?>(null)
    val tracks: StateFlow<Tracks?> = _tracks.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _volume = MutableStateFlow(1f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private var pollJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            _isLoading.value = playbackState == Player.STATE_BUFFERING
            _isPlaying.value = exoPlayer?.isPlaying == true
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onTracksChanged(tracks: Tracks) {
            _tracks.value = tracks
        }

        // Queue playback (Play all): keep the title in sync when the
        // player steps to the next video in the queue.
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val uri = mediaItem?.localConfiguration?.uri?.toString() ?: return
            scope.launch {
                _currentVideo.value = videoRepository.getVideoByContentUri(uri)
            }
        }
    }

    init {
        // Restore the saved speed and volume before the first play.
        scope.launch {
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

    /** Poll position / duration / playing state for the bars (250 ms). */
    private fun startPolling() {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (isActive) {
                val p = exoPlayer
                if (p != null) {
                    _positionMs.value = p.currentPosition
                    _durationMs.value = p.duration.coerceAtLeast(0L)
                    _isPlaying.value = p.isPlaying
                }
                delay(250)
            }
        }
    }

    fun play(contentUriString: String, startPositionMs: Long = 0L) {
        val current = exoPlayer
        if (_currentVideo.value?.contentUriString == contentUriString && current != null) {
            // Same video, player already alive (e.g. reopened from the mini
            // bar): don't rebuild, just resume — seeking only when the
            // requested position is actually different (a resume point).
            if (startPositionMs > 0 &&
                abs(current.currentPosition - startPositionMs) > 1_000L
            ) {
                current.seekTo(startPositionMs)
            }
            current.play()
            return
        }
        releasePlayer()
        _queueUris.value = listOf(contentUriString)
        val player = buildPlayer().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(contentUriString)))
            prepare()
            if (startPositionMs > 0) seekTo(startPositionMs)
            play()
        }
        exoPlayer = player
        _player.value = player
        _isLoading.value = true
        startPolling()
        scope.launch {
            _currentVideo.value = videoRepository.getVideoByContentUri(contentUriString)
        }
    }

    suspend fun loadVideo(contentUriString: String) {
        _currentVideo.value = videoRepository.getVideoByContentUri(contentUriString)
    }

    /**
     * Play-all: play a whole list of videos as one ExoPlayer queue,
     * starting at [startIndex]. Falls back to single-video playback when
     * the list has one item. Reopening the same queue at the same video
     * (e.g. from the mini bar) does NOT rebuild the player.
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
        val wantedIndex = startIndex.coerceIn(0, contentUriStrings.lastIndex)
        val current = exoPlayer
        if (current != null && _queueUris.value == contentUriStrings &&
            current.currentMediaItemIndex == wantedIndex
        ) {
            if (startPositionMs > 0 &&
                abs(current.currentPosition - startPositionMs) > 1_000L
            ) {
                current.seekTo(startPositionMs)
            }
            current.play()
            return
        }
        releasePlayer()
        _queueUris.value = contentUriStrings
        val player = buildPlayer().apply {
            setMediaItems(
                contentUriStrings.map { MediaItem.fromUri(Uri.parse(it)) },
                wantedIndex,
                startPositionMs.coerceAtLeast(0L)
            )
            prepare()
            play()
        }
        exoPlayer = player
        _player.value = player
        _isLoading.value = true
        startPolling()
    }

    /** Current position, for the resume point saved on leave. */
    fun currentPositionMs(): Long = exoPlayer?.currentPosition ?: 0L

    /**
     * Save the resume point (Folders screen's Resume button, and the mini
     * bar's fallback). Only mid-video positions count — the very start and
     * the very end are not worth resuming.
     */
    fun saveResumePosition() {
        val video = _currentVideo.value ?: return
        val position = currentPositionMs()
        val duration = exoPlayer?.duration ?: 0L
        scope.launch {
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
        scope.launch { userPreferencesRepository.clearVideoResume() }
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        exoPlayer?.setPlaybackSpeed(speed)
        scope.launch {
            userPreferencesRepository.setVideoPlaybackSpeed(speed)
        }
    }

    fun setVolume(volume: Float) {
        _volume.value = volume.coerceIn(0f, 1f)
        exoPlayer?.volume = _volume.value
        scope.launch { userPreferencesRepository.setVideoVolume(_volume.value) }
    }

    /**
     * Live volume change (swipe gesture): applies at once without a
     * DataStore write per move; the gesture end persists it through
     * [setVolume].
     */
    fun setVolumeLive(volume: Float) {
        _volume.value = volume.coerceIn(0f, 1f)
        exoPlayer?.volume = _volume.value
    }

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

    fun setShuffleEnabled(enabled: Boolean) {
        exoPlayer?.shuffleModeEnabled = enabled
    }

    fun setRepeatMode(@Player.RepeatMode mode: Int) {
        exoPlayer?.repeatMode = mode
    }

    private fun releasePlayer() {
        pollJob?.cancel()
        pollJob = null
        _isPlaying.value = false
        _positionMs.value = 0L
        _durationMs.value = 0L
        exoPlayer?.removeListener(playerListener)
        exoPlayer?.release()
        exoPlayer = null
        _player.value = null
        _tracks.value = null
    }
}
