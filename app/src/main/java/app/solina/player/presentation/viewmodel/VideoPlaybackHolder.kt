package app.solina.player.presentation.viewmodel

import android.content.Context
import android.media.MediaCodecList
import android.media.MediaFormat
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import app.solina.player.data.model.ExternalVideoSubtitle
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

    /**
     * Decoder mode the live player was built with (round 16, item 1c).
     * The renderers factory depends on the decoder preference, so the
     * player is rebuilt only when this differs from [_decoderMode] —
     * never on a plain video switch. Rebuilding on every switch tore
     * the PlayerView surface down and re-attached a new player, which
     * left the previous video's frame frozen on screen.
     */
    private var playerDecoderMode: String? = null

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

    /**
     * Decoder choice (round 15, wave 1, B2) and resume behaviour (B5),
     * from the Video settings category. Cached here so [buildPlayer] and
     * [play]/[playQueue] can read them synchronously; defaults match
     * today's behaviour ("auto" / "always") until the DataStore value
     * loads.
     */
    private val _decoderMode = MutableStateFlow("auto")
    private val _resumeBehaviour = MutableStateFlow("always")

    private val _volume = MutableStateFlow(1f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    /**
     * Reference-player redesign (2026-11-16): the "HW" / "SW" decoder
     * badge. This is a REAL indicator, not a toggle: when the video
     * tracks change, the platform [MediaCodecList] is asked which decoder
     * would decode this video format. A Google software decoder
     * (c2.android.* / OMX.google.*) — or no platform decoder at all, in
     * which case the FFmpeg extension decodes in software — means "SW";
     * any vendor/hardware decoder means "HW".
     */
    private val _decoderBadge = MutableStateFlow("HW")
    val decoderBadge: StateFlow<String> = _decoderBadge.asStateFlow()

    /**
     * External subtitle files picked per video (persisted, so they reload
     * with the video). Keyed by video content URI.
     */
    private val externalSubtitles =
        MutableStateFlow<Map<String, ExternalVideoSubtitle>>(emptyMap())

    private fun computeDecoderBadge() {
        val format = exoPlayer?.videoFormat
        if (format == null) {
            _decoderBadge.value = "HW"
            return
        }
        _decoderBadge.value = runCatching {
            val mime = format.sampleMimeType ?: return@runCatching "HW"
            val mediaFormat = MediaFormat.createVideoFormat(
                mime,
                format.width.takeIf { it > 0 } ?: 1280,
                format.height.takeIf { it > 0 } ?: 720
            )
            val codecName = MediaCodecList(MediaCodecList.REGULAR_CODECS)
                .findDecoderForFormat(mediaFormat)
            when {
                codecName == null -> "SW" // no platform decoder: FFmpeg (software)
                codecName.startsWith("OMX.google.") ||
                    codecName.startsWith("c2.android.") -> "SW"
                else -> "HW"
            }
        }.getOrElse { "HW" }
    }

    private var pollJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            _isLoading.value = playbackState == Player.STATE_BUFFERING
            _isPlaying.value = exoPlayer?.isPlaying == true
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (isPlaying) {
                startPolling()
            } else {
                stopPolling()
                exoPlayer?.let { p ->
                    _positionMs.value = p.currentPosition
                    _durationMs.value = p.duration.coerceAtLeast(0L)
                }
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            _tracks.value = tracks
            computeDecoderBadge()
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
        // Decoder choice and resume behaviour (Video settings, round 15):
        // collected continuously, not just once — the holder outlives the
        // settings screen, and the choice must apply to the next video
        // played after the user changes it.
        scope.launch {
            userPreferencesRepository.videoDecoderModeFlow.collect { _decoderMode.value = it }
        }
        scope.launch {
            userPreferencesRepository.videoResumeBehaviourFlow.collect { _resumeBehaviour.value = it }
        }
        // External subtitle files picked earlier, keyed by video URI.
        scope.launch {
            externalSubtitles.value = userPreferencesRepository
                .videoExternalSubtitlesFlow.first()
                .associateBy { it.videoUri }
        }
    }

    /**
     * Build a MediaItem for a video URI, attaching a previously picked
     * external subtitle file for this video (if any) as a
     * [MediaItem.SubtitleConfiguration].
     */
    private fun buildMediaItem(contentUriString: String): MediaItem {
        val builder = MediaItem.Builder().setUri(Uri.parse(contentUriString))
        externalSubtitles.value[contentUriString]?.let { external ->
            builder.setSubtitleConfigurations(
                listOf(
                    MediaItem.SubtitleConfiguration.Builder(
                        Uri.parse(external.subUri)
                    )
                        .setMimeType(external.mimeType)
                        .setLanguage(C.LANGUAGE_UNDETERMINED)
                        .setLabel(external.label)
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build()
                )
            )
        }
        return builder.build()
    }

    /**
     * Attach an external subtitle file (.srt / .vtt / .ass / .ssa) to the
     * video that is playing now: the current media item is rebuilt with
     * the subtitle configuration and swapped in place (queue untouched),
     * the position is kept, and the file is remembered so it reloads
     * next time this video plays. Returns the label to auto-select.
     */
    fun attachExternalSubtitle(subUri: Uri, label: String, mimeType: String): String {
        val player = exoPlayer ?: return label
        val videoUri = _currentVideo.value?.contentUriString ?: return label
        val entry = ExternalVideoSubtitle(
            videoUri = videoUri,
            subUri = subUri.toString(),
            label = label,
            mimeType = mimeType
        )
        externalSubtitles.value = externalSubtitles.value + (videoUri to entry)
        scope.launch { userPreferencesRepository.setExternalVideoSubtitle(entry) }
        val position = player.currentPosition
        val index = player.currentMediaItemIndex
        player.replaceMediaItem(index, buildMediaItem(videoUri))
        player.seekTo(position)
        player.play()
        return label
    }

    /**
     * Resume entry points pass a saved position here. "start over" zeroes
     * it; "ask" has no resume prompt anywhere in the app yet, so it falls
     * back to today's always-resume behaviour (stated in the setting's
     * description rather than faking a dialog).
     */
    private fun effectiveStartPositionMs(requestedStartPositionMs: Long): Long =
        if (_resumeBehaviour.value == "start_over") 0L else requestedStartPositionMs

    private fun buildPlayer(): ExoPlayer {
        // Decoder choice (round 15, wave 1, B2) — standard Media3:
        // "auto" is today's setup (FFmpeg extension renderers on, default
        // platform selector); "hardware" turns the FFmpeg software
        // renderers off so only platform (hardware) decoders are used;
        // "software" prefers software platform decoders and keeps the
        // FFmpeg software renderers on.
        //
        // The renderers factory is fixed at build time, so a decoder
        // change takes effect on the next video — but ONLY a decoder
        // change rebuilds the player (see [ensurePlayer]). A plain video
        // switch reuses the live player and swaps the media item, so the
        // PlayerView surface stays attached (round 16, item 1c: rebuilding
        // per switch left the previous video's frame frozen on screen).
        val renderersFactory = when (_decoderMode.value) {
            "hardware" -> DefaultRenderersFactory(context)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
            "software" -> DefaultRenderersFactory(context)
                .setMediaCodecSelector(MediaCodecSelector.PREFER_SOFTWARE)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            else -> DefaultRenderersFactory(context)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        }
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
        return ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                addListener(playerListener)
                volume = _volume.value
                setPlaybackSpeed(_playbackSpeed.value)
            }
    }

    /**
     * One player for the lifetime of the holder (round 16, item 1c).
     * Returns the live player when it was built with the current decoder
     * mode; rebuilds it only when there is no player yet or the decoder
     * preference changed since it was built. Reusing the instance keeps
     * [_player] emitting the same object, so the PlayerView's
     * `playerView.player = player` assignment is a no-op and the video
     * surface is never torn down and re-attached on a video switch —
     * the re-attach is what left the previous video's frame frozen on
     * screen. Never rebuilds mid-playback: callers only reach here
     * when starting a (different) video.
     */
    private fun ensurePlayer(): ExoPlayer {
        val current = exoPlayer
        if (current != null && playerDecoderMode == _decoderMode.value) return current
        releasePlayer()
        val player = buildPlayer()
        exoPlayer = player
        _player.value = player
        playerDecoderMode = _decoderMode.value
        return player
    }

    /** Poll position / duration / playing state while playback is active (250 ms). */
    private fun startPolling() {
        if (pollJob?.isActive == true) return
        pollJob = scope.launch {
            while (isActive) {
                val p = exoPlayer
                if (p == null || !p.isPlaying) {
                    break
                }
                _positionMs.value = p.currentPosition
                _durationMs.value = p.duration.coerceAtLeast(0L)
                _isPlaying.value = true
                delay(250)
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    fun play(contentUriString: String, startPositionMs: Long = 0L) {
        val effectiveStartPositionMs = effectiveStartPositionMs(startPositionMs)
        val current = exoPlayer
        if (_currentVideo.value?.contentUriString == contentUriString && current != null) {
            // Same video, player already alive (e.g. reopened from the mini
            // bar): don't rebuild, just resume — seeking only when the
            // requested position is actually different (a resume point).
            if (effectiveStartPositionMs > 0 &&
                abs(current.currentPosition - effectiveStartPositionMs) > 1_000L
            ) {
                current.seekTo(effectiveStartPositionMs)
            }
            current.play()
            return
        }
        // Different video: reuse the live player (round 16, item 1c) —
        // swap the media item in place instead of releasing + rebuilding
        // the player, so the PlayerView surface stays attached and the
        // new video's frames actually render. Only a decoder-preference
        // change rebuilds the player (inside ensurePlayer). The stale
        // position / duration / tracks of the previous video are cleared
        // here, exactly as releasePlayer() used to do on a switch.
        _queueUris.value = listOf(contentUriString)
        _positionMs.value = 0L
        _durationMs.value = 0L
        _tracks.value = null
        ensurePlayer().apply {
            setMediaItem(buildMediaItem(contentUriString))
            prepare()
            if (effectiveStartPositionMs > 0) seekTo(effectiveStartPositionMs)
            setPlaybackSpeed(_playbackSpeed.value)
            volume = _volume.value
            play()
        }
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
        val effectiveStartPositionMs = effectiveStartPositionMs(startPositionMs)
        val current = exoPlayer
        if (current != null && _queueUris.value == contentUriStrings &&
            current.currentMediaItemIndex == wantedIndex
        ) {
            if (effectiveStartPositionMs > 0 &&
                abs(current.currentPosition - effectiveStartPositionMs) > 1_000L
            ) {
                current.seekTo(effectiveStartPositionMs)
            }
            current.play()
            return
        }
        // Different queue / video: reuse the live player (round 16,
        // item 1c) — swap the media items in place instead of releasing
        // + rebuilding, so the PlayerView surface stays attached.
        // Only a decoder-preference change rebuilds (inside
        // ensurePlayer). Stale position / duration / tracks are cleared,
        // exactly as releasePlayer() used to do on a switch.
        _queueUris.value = contentUriStrings
        _positionMs.value = 0L
        _durationMs.value = 0L
        _tracks.value = null
        ensurePlayer().apply {
            setMediaItems(
                contentUriStrings.map { buildMediaItem(it) },
                wantedIndex,
                effectiveStartPositionMs.coerceAtLeast(0L)
            )
            prepare()
            setPlaybackSpeed(_playbackSpeed.value)
            volume = _volume.value
            play()
        }
        _isLoading.value = true
        startPolling()
        scope.launch {
            _currentVideo.value = videoRepository.getVideoByContentUri(contentUriStrings[wantedIndex])
        }
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

    /**
     * Live speed change (two-finger speed gesture, 2026-11-17): applies
     * at once without a DataStore write per move; the gesture end
     * persists it through [setSpeed].
     */
    fun setSpeedLive(speed: Float) {
        _playbackSpeed.value = speed
        exoPlayer?.setPlaybackSpeed(speed)
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
        val clamped = positionMs.coerceIn(0L, duration)
        player.seekTo(clamped)
        _positionMs.value = clamped
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

    /** Playing Queue (reference-player redesign): jump to a queue item. */
    fun playQueueIndex(index: Int) {
        val player = exoPlayer ?: return
        if (index in 0 until player.mediaItemCount) {
            player.seekTo(index, 0L)
            player.play()
        }
    }

    /** Resolve a queue URI to its [VideoItem] (for the Playing Queue list). */
    suspend fun getVideoByUri(contentUriString: String): VideoItem? =
        videoRepository.getVideoByContentUri(contentUriString)

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
        playerDecoderMode = null
        _tracks.value = null
    }
}
