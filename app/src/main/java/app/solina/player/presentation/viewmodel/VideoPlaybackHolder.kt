package app.solina.player.presentation.viewmodel

import android.content.Context
import android.content.Intent
import android.media.MediaCodecList
import android.media.MediaFormat
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import app.solina.player.data.model.ExternalVideoSubtitle
import app.solina.player.data.model.VideoItem
import app.solina.player.data.model.VideoProgressEntry
import app.solina.player.data.preferences.UserPreferencesRepository
import app.solina.player.data.repository.VideoRepository
import app.solina.player.data.service.VideoPlaybackService
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
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

data class VideoPlaybackError(
    val uri: String,
    val message: String,
    val canRetrySoftware: Boolean
)

/**
 * App-scoped owner of the video ExoPlayer, queue, sleep timer, A-B repeat,
 * decoder telemetry, error recovery, and background MediaSessionService lifecycle.
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
    private var playerDecoderMode: String? = null
    private var activeVideoDecoderName: String? = null

    /** The live player, or null before the first video is played. */
    val playerOrNull: ExoPlayer?
        get() = exoPlayer

    private val _player = MutableStateFlow<ExoPlayer?>(null)
    val player: StateFlow<ExoPlayer?> = _player.asStateFlow()

    private val _currentVideo = MutableStateFlow<VideoItem?>(null)
    val currentVideo: StateFlow<VideoItem?> = _currentVideo.asStateFlow()

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

    private val _tracks = MutableStateFlow<Tracks?>(null)
    val tracks: StateFlow<Tracks?> = _tracks.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _decoderMode = MutableStateFlow("auto")
    private val _resumeBehaviour = MutableStateFlow("always")
    private val _backgroundPlaybackEnabled = MutableStateFlow(false)
    val backgroundPlaybackEnabled: StateFlow<Boolean> = _backgroundPlaybackEnabled.asStateFlow()

    private val _volume = MutableStateFlow(1f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _decoderBadge = MutableStateFlow("HW")
    val decoderBadge: StateFlow<String> = _decoderBadge.asStateFlow()

    private val _playbackError = MutableStateFlow<VideoPlaybackError?>(null)
    val playbackError: StateFlow<VideoPlaybackError?> = _playbackError.asStateFlow()

    // Holder-owned playback modes (survive leaving/reopening VideoPlayerScreen via mini bar).
    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()
    private var preMuteVolume = 1f

    // A-B Repeat
    private val _aPointMs = MutableStateFlow(-1L)
    val aPointMs: StateFlow<Long> = _aPointMs.asStateFlow()
    private val _bPointMs = MutableStateFlow(-1L)
    val bPointMs: StateFlow<Long> = _bPointMs.asStateFlow()
    private var abRepeatJob: Job? = null

    // Sleep timer (lives in holder so background playback sleep timer continues ticking).
    private val _sleepTimerMinutes = MutableStateFlow(0)
    val sleepTimerMinutes: StateFlow<Int> = _sleepTimerMinutes.asStateFlow()
    private val _sleepTimerRemainingSeconds = MutableStateFlow(0)
    val sleepTimerRemainingSeconds: StateFlow<Int> = _sleepTimerRemainingSeconds.asStateFlow()
    private var sleepJob: Job? = null
    private var sleepTickJob: Job? = null

    // Resume prompt for "ask" resume behaviour.
    private val _pendingResumePromptMs = MutableStateFlow<Long?>(null)
    val pendingResumePromptMs: StateFlow<Long?> = _pendingResumePromptMs.asStateFlow()

    private val progressMap = MutableStateFlow<Map<String, VideoProgressEntry>>(emptyMap())

    private val externalSubtitles =
        MutableStateFlow<Map<String, ExternalVideoSubtitle>>(emptyMap())

    private fun isSoftwareCodecName(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        val lower = name.lowercase()
        return lower.startsWith("omx.google.") ||
            lower.startsWith("c2.android.") ||
            lower.contains("ffmpeg") ||
            lower.contains("sw")
    }

    private fun computeDecoderBadge() {
        val activeName = activeVideoDecoderName
        if (!activeName.isNullOrBlank()) {
            _decoderBadge.value = if (isSoftwareCodecName(activeName)) "SW" else "HW"
            return
        }
        if (playerDecoderMode == "software") {
            _decoderBadge.value = "SW"
            return
        }
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
                codecName == null -> "SW"
                isSoftwareCodecName(codecName) -> "SW"
                else -> "HW"
            }
        }.getOrElse { "HW" }
    }

    private var pollJob: Job? = null

    private val analyticsListener = object : AnalyticsListener {
        override fun onVideoDecoderInitialized(
            eventTime: AnalyticsListener.EventTime,
            decoderName: String,
            initializedTimestampMs: Long,
            initializationDurationMs: Long
        ) {
            activeVideoDecoderName = decoderName
            computeDecoderBadge()
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            _isLoading.value = playbackState == Player.STATE_BUFFERING
            _isPlaying.value = exoPlayer?.isPlaying == true
            if (playbackState == Player.STATE_READY) {
                _playbackError.value = null
            }
            if (playbackState == Player.STATE_ENDED) {
                _currentVideo.value?.let { finished ->
                    scope.launch {
                        userPreferencesRepository.saveVideoProgress(
                            uri = finished.contentUriString,
                            positionMs = 0L,
                            durationMs = finished.durationMs
                        )
                    }
                }
                stopBackgroundService()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (isPlaying) {
                startPolling()
                if (_backgroundPlaybackEnabled.value) {
                    startBackgroundServiceIfNeeded()
                }
            } else {
                stopPolling()
                exoPlayer?.let { p ->
                    _positionMs.value = p.currentPosition
                    _durationMs.value = p.duration.coerceAtLeast(0L)
                }
                saveResumePosition()
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            _tracks.value = tracks
            computeDecoderBadge()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val uri = mediaItem?.localConfiguration?.uri?.toString() ?: return
            activeVideoDecoderName = null
            _playbackError.value = null
            _positionMs.value = 0L
            _durationMs.value = 0L
            clearABRepeat()
            _pendingResumePromptMs.value = null
            scope.launch {
                val video = videoRepository.getVideoByContentUri(uri)
                _currentVideo.value = video
                if (video != null) {
                    maybeAutoLoadSidecarSubtitle(video)
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val uri = _currentVideo.value?.contentUriString
                ?: exoPlayer?.currentMediaItem?.localConfiguration?.uri?.toString()
                ?: ""
            _isLoading.value = false
            _isPlaying.value = false
            stopPolling()
            val message = error.cause?.localizedMessage?.takeIf { it.isNotBlank() }
                ?: error.localizedMessage
                ?: error.errorCodeName
            val canRetrySw = playerDecoderMode != "software"
            Timber.tag("VideoPlaybackHolder").e(error, "Video playback error for %s", uri)
            _playbackError.value = VideoPlaybackError(
                uri = uri,
                message = message,
                canRetrySoftware = canRetrySw
            )
        }
    }

    init {
        scope.launch {
            _playbackSpeed.value = userPreferencesRepository.videoPlaybackSpeedFlow.first()
            _volume.value = userPreferencesRepository.videoVolumeFlow.first()
        }
        scope.launch {
            userPreferencesRepository.videoDecoderModeFlow.collect { _decoderMode.value = it }
        }
        scope.launch {
            userPreferencesRepository.videoResumeBehaviourFlow.collect { _resumeBehaviour.value = it }
        }
        scope.launch {
            userPreferencesRepository.videoBackgroundPlaybackFlow.collect { enabled ->
                _backgroundPlaybackEnabled.value = enabled
                if (enabled && exoPlayer?.isPlaying == true) {
                    startBackgroundServiceIfNeeded()
                } else if (!enabled) {
                    stopBackgroundService()
                }
            }
        }
        scope.launch {
            userPreferencesRepository.videoProgressMapFlow.collect { map ->
                progressMap.value = map
            }
        }
        scope.launch {
            externalSubtitles.value = userPreferencesRepository
                .videoExternalSubtitlesFlow.first()
                .associateBy { it.videoUri }
        }
    }

    private fun buildMediaItem(contentUriString: String, titleHint: String? = null): MediaItem {
        val builder = MediaItem.Builder().setUri(Uri.parse(contentUriString))
        val title = titleHint ?: _currentVideo.value?.takeIf { it.contentUriString == contentUriString }?.displayTitle
        if (!title.isNullOrBlank()) {
            builder.setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .build()
            )
        }
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
     * Best-effort auto-discovery of a sidecar subtitle (.srt / .vtt / .ass / .ssa)
     * matching the video filename in the same local directory when no manual external
     * subtitle has been set for this video.
     */
    private suspend fun maybeAutoLoadSidecarSubtitle(video: VideoItem) {
        if (externalSubtitles.value.containsKey(video.contentUriString)) return
        val videoPath = video.path ?: return
        val sidecar = withContext(Dispatchers.IO) {
            runCatching {
                val file = File(videoPath)
                val parent = file.parentFile ?: return@runCatching null
                val stem = file.nameWithoutExtension
                if (stem.isBlank()) return@runCatching null
                val extensions = listOf(
                    "srt" to MimeTypes.APPLICATION_SUBRIP,
                    "vtt" to MimeTypes.TEXT_VTT,
                    "ass" to MimeTypes.TEXT_SSA,
                    "ssa" to MimeTypes.TEXT_SSA
                )
                for ((ext, mime) in extensions) {
                    val candidate = File(parent, "$stem.$ext")
                    if (candidate.exists() && candidate.isFile && candidate.canRead()) {
                        return@runCatching Triple(Uri.fromFile(candidate), candidate.name, mime)
                    }
                }
                null
            }.getOrNull()
        } ?: return

        val player = exoPlayer ?: return
        val currentUri = player.currentMediaItem?.localConfiguration?.uri?.toString()
        if (currentUri != video.contentUriString) return
        val (subUri, label, mime) = sidecar
        val entry = ExternalVideoSubtitle(
            videoUri = video.contentUriString,
            subUri = subUri.toString(),
            label = label,
            mimeType = mime
        )
        externalSubtitles.value = externalSubtitles.value + (video.contentUriString to entry)
        val pos = player.currentPosition
        val wasPlaying = player.isPlaying
        val idx = player.currentMediaItemIndex
        if (idx >= 0 && idx < player.mediaItemCount) {
            player.replaceMediaItem(idx, buildMediaItem(video.contentUriString, video.displayTitle))
            player.seekTo(pos)
            if (wasPlaying) player.play() else player.pause()
        }
    }

    /**
     * Attach an external subtitle file (.srt / .vtt / .ass / .ssa) to the playing video
     * while preserving its play/pause state and current position.
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
        val wasPlaying = player.isPlaying
        val index = player.currentMediaItemIndex
        if (index in 0 until player.mediaItemCount) {
            player.replaceMediaItem(index, buildMediaItem(videoUri))
            player.seekTo(position)
            if (wasPlaying) player.play() else player.pause()
        }
        return label
    }

    private fun computeStartPositionFor(
        contentUriString: String,
        explicitStartPositionMs: Long
    ): Long {
        if (_resumeBehaviour.value == "start_over") {
            _pendingResumePromptMs.value = null
            return 0L
        }
        val savedPos = if (explicitStartPositionMs > 0L) {
            explicitStartPositionMs
        } else {
            progressMap.value[contentUriString]?.positionMs ?: 0L
        }
        if (_resumeBehaviour.value == "ask" && savedPos > 10_000L) {
            _pendingResumePromptMs.value = savedPos
            return 0L
        }
        _pendingResumePromptMs.value = null
        return savedPos
    }

    fun acceptResumePrompt() {
        val target = _pendingResumePromptMs.value ?: return
        _pendingResumePromptMs.value = null
        seekTo(target)
    }

    fun dismissResumePrompt() {
        _pendingResumePromptMs.value = null
    }

    fun restartFromBeginning() {
        _pendingResumePromptMs.value = null
        seekTo(0L)
    }

    private fun buildPlayer(forceSoftware: Boolean = false): ExoPlayer {
        val effectiveMode = if (forceSoftware) "software" else _decoderMode.value
        val renderersFactory = when (effectiveMode) {
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
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
            .apply {
                addListener(playerListener)
                addAnalyticsListener(analyticsListener)
                volume = if (_isMuted.value) 0f else _volume.value
                setPlaybackSpeed(_playbackSpeed.value)
                shuffleModeEnabled = _shuffleEnabled.value
                repeatMode = _repeatMode.value
            }
    }

    private fun ensurePlayer(forceRebuildSoftware: Boolean = false): ExoPlayer {
        val current = exoPlayer
        val targetMode = if (forceRebuildSoftware) "software" else _decoderMode.value
        if (!forceRebuildSoftware && current != null && playerDecoderMode == targetMode) {
            return current
        }
        releasePlayer()
        val player = buildPlayer(forceSoftware = forceRebuildSoftware)
        exoPlayer = player
        _player.value = player
        playerDecoderMode = targetMode
        return player
    }

    fun clearError() {
        _playbackError.value = null
    }

    fun retryCurrent() {
        val uri = _currentVideo.value?.contentUriString
            ?: _playbackError.value?.uri?.takeIf { it.isNotBlank() }
            ?: return
        val pos = _positionMs.value
        _playbackError.value = null
        val p = ensurePlayer()
        p.setMediaItem(buildMediaItem(uri))
        p.prepare()
        if (pos > 0L) p.seekTo(pos)
        p.play()
        _isLoading.value = true
    }

    fun retryWithSoftwareDecoder() {
        val uri = _currentVideo.value?.contentUriString
            ?: _playbackError.value?.uri?.takeIf { it.isNotBlank() }
            ?: return
        val pos = _positionMs.value
        _playbackError.value = null
        val p = ensurePlayer(forceRebuildSoftware = true)
        p.setMediaItem(buildMediaItem(uri))
        p.prepare()
        if (pos > 0L) p.seekTo(pos)
        p.play()
        _isLoading.value = true
    }

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
        val current = exoPlayer
        _playbackError.value = null
        if (_currentVideo.value?.contentUriString == contentUriString && current != null) {
            val effectiveStart = if (startPositionMs > 0L) {
                computeStartPositionFor(contentUriString, startPositionMs)
            } else 0L
            if (effectiveStart > 0 && abs(current.currentPosition - effectiveStart) > 1_000L) {
                current.seekTo(effectiveStart)
            }
            if (current.playbackState == Player.STATE_IDLE) {
                current.prepare()
            }
            current.play()
            return
        }
        val effectiveStartPositionMs = computeStartPositionFor(contentUriString, startPositionMs)
        activeVideoDecoderName = null
        _queueUris.value = listOf(contentUriString)
        _positionMs.value = 0L
        _durationMs.value = 0L
        _tracks.value = null
        clearABRepeat()
        ensurePlayer().apply {
            setMediaItem(buildMediaItem(contentUriString))
            prepare()
            if (effectiveStartPositionMs > 0) seekTo(effectiveStartPositionMs)
            setPlaybackSpeed(_playbackSpeed.value)
            volume = if (_isMuted.value) 0f else _volume.value
            play()
        }
        _isLoading.value = true
        startPolling()
        scope.launch {
            val video = videoRepository.getVideoByContentUri(contentUriString)
            _currentVideo.value = video
            if (video != null) maybeAutoLoadSidecarSubtitle(video)
        }
    }

    suspend fun loadVideo(contentUriString: String) {
        val existing = _currentVideo.value
        if (existing?.contentUriString == contentUriString) return
        _currentVideo.value = videoRepository.getVideoByContentUri(contentUriString)
    }

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
        val wantedUri = contentUriStrings[wantedIndex]
        val current = exoPlayer
        _playbackError.value = null
        if (current != null && _queueUris.value == contentUriStrings &&
            current.currentMediaItemIndex == wantedIndex
        ) {
            val effectiveStart = if (startPositionMs > 0L) {
                computeStartPositionFor(wantedUri, startPositionMs)
            } else 0L
            if (effectiveStart > 0 && abs(current.currentPosition - effectiveStart) > 1_000L) {
                current.seekTo(effectiveStart)
            }
            if (current.playbackState == Player.STATE_IDLE) {
                current.prepare()
            }
            current.play()
            return
        }
        val effectiveStartPositionMs = computeStartPositionFor(wantedUri, startPositionMs)
        activeVideoDecoderName = null
        _queueUris.value = contentUriStrings
        _positionMs.value = 0L
        _durationMs.value = 0L
        _tracks.value = null
        clearABRepeat()
        ensurePlayer().apply {
            setMediaItems(
                contentUriStrings.map { buildMediaItem(it) },
                wantedIndex,
                effectiveStartPositionMs.coerceAtLeast(0L)
            )
            prepare()
            setPlaybackSpeed(_playbackSpeed.value)
            volume = if (_isMuted.value) 0f else _volume.value
            play()
        }
        _isLoading.value = true
        startPolling()
        scope.launch {
            val video = videoRepository.getVideoByContentUri(wantedUri)
            _currentVideo.value = video
            if (video != null) maybeAutoLoadSidecarSubtitle(video)
        }
    }

    fun removeQueueItem(index: Int) {
        val player = exoPlayer ?: return
        val currentList = _queueUris.value.toMutableList()
        if (index !in currentList.indices || currentList.size <= 1) return
        if (index == player.currentMediaItemIndex) return
        currentList.removeAt(index)
        _queueUris.value = currentList
        if (index < player.mediaItemCount) {
            player.removeMediaItem(index)
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val player = exoPlayer ?: return
        val currentList = _queueUris.value.toMutableList()
        if (fromIndex !in currentList.indices || toIndex !in currentList.indices || fromIndex == toIndex) return
        val item = currentList.removeAt(fromIndex)
        currentList.add(toIndex, item)
        _queueUris.value = currentList
        if (fromIndex < player.mediaItemCount && toIndex < player.mediaItemCount) {
            player.moveMediaItem(fromIndex, toIndex)
        }
    }

    fun currentPositionMs(): Long = exoPlayer?.currentPosition ?: 0L

    fun saveResumePosition() {
        val video = _currentVideo.value ?: return
        val position = currentPositionMs()
        val duration = video.durationMs.takeIf { it > 0L } ?: (exoPlayer?.duration ?: 0L)
        scope.launch {
            userPreferencesRepository.saveVideoProgress(
                uri = video.contentUriString,
                positionMs = position,
                durationMs = duration
            )
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
        val clamped = speed.coerceIn(0.1f, 4f)
        _playbackSpeed.value = clamped
        exoPlayer?.setPlaybackSpeed(clamped)
        scope.launch {
            userPreferencesRepository.setVideoPlaybackSpeed(clamped)
        }
    }

    fun setSpeedLive(speed: Float) {
        val clamped = speed.coerceIn(0.1f, 4f)
        _playbackSpeed.value = clamped
        exoPlayer?.setPlaybackSpeed(clamped)
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _volume.value = clamped
        if (clamped > 0f && _isMuted.value) {
            _isMuted.value = false
        }
        exoPlayer?.volume = clamped
        scope.launch { userPreferencesRepository.setVideoVolume(clamped) }
    }

    fun setVolumeLive(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _volume.value = clamped
        if (clamped > 0f && _isMuted.value) {
            _isMuted.value = false
        }
        exoPlayer?.volume = clamped
    }

    fun toggleMute() {
        if (_isMuted.value) {
            _isMuted.value = false
            setVolume(preMuteVolume.coerceAtLeast(0.1f))
        } else {
            preMuteVolume = _volume.value.coerceAtLeast(0.1f)
            _isMuted.value = true
            exoPlayer?.volume = 0f
        }
    }

    fun toggleShuffle() {
        val next = !_shuffleEnabled.value
        _shuffleEnabled.value = next
        exoPlayer?.shuffleModeEnabled = next
    }

    fun cycleRepeatMode() {
        val next = when (_repeatMode.value) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
        _repeatMode.value = next
        exoPlayer?.repeatMode = next
    }

    fun tapABRepeat() {
        val player = exoPlayer
        if (_aPointMs.value < 0) {
            _aPointMs.value = player?.currentPosition ?: 0L
            return
        }
        if (_bPointMs.value < 0) {
            val b = player?.currentPosition ?: 0L
            if (b <= _aPointMs.value) {
                _aPointMs.value = b
                return
            }
            _bPointMs.value = b
            abRepeatJob?.cancel()
            abRepeatJob = scope.launch {
                while (isActive) {
                    delay(200)
                    val p = exoPlayer ?: break
                    if (p.isPlaying && p.currentPosition >= _bPointMs.value && _bPointMs.value > 0) {
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

    fun setSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        sleepTickJob?.cancel()
        _sleepTimerMinutes.value = minutes
        if (minutes <= 0) {
            _sleepTimerRemainingSeconds.value = 0
            return
        }
        _sleepTimerRemainingSeconds.value = minutes * 60
        sleepTickJob = scope.launch {
            while (isActive && _sleepTimerRemainingSeconds.value > 0) {
                delay(1_000L)
                _sleepTimerRemainingSeconds.value =
                    (_sleepTimerRemainingSeconds.value - 1).coerceAtLeast(0)
            }
        }
        sleepJob = scope.launch {
            delay(minutes * 60_000L)
            pause()
            _sleepTimerMinutes.value = 0
            _sleepTimerRemainingSeconds.value = 0
            sleepTickJob?.cancel()
            stopBackgroundService()
        }
    }

    fun startBackgroundServiceIfNeeded() {
        if (!_backgroundPlaybackEnabled.value || exoPlayer == null) return
        runCatching {
            val intent = Intent(context, VideoPlaybackService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }.onFailure { e ->
            Timber.tag("VideoPlaybackHolder").w(e, "Unable to start VideoPlaybackService")
        }
    }

    fun stopBackgroundService() {
        runCatching {
            context.stopService(Intent(context, VideoPlaybackService::class.java))
        }
    }

    fun seekTo(positionMs: Long) {
        val player = exoPlayer ?: return
        val mediaStoreDuration = _currentVideo.value?.durationMs ?: 0L
        val duration = maxOf(player.duration.coerceAtLeast(0L), mediaStoreDuration)
        val clamped = if (duration > 0L) positionMs.coerceIn(0L, duration) else positionMs.coerceAtLeast(0L)
        player.seekTo(clamped)
        _positionMs.value = clamped
    }

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

    fun playQueueIndex(index: Int) {
        val player = exoPlayer ?: return
        if (index in 0 until player.mediaItemCount) {
            player.seekTo(index, 0L)
            player.play()
        }
    }

    suspend fun getVideoByUri(contentUriString: String): VideoItem? =
        videoRepository.getVideoByContentUri(contentUriString)

    suspend fun getVideosByUris(contentUriStrings: List<String>): List<VideoItem> =
        videoRepository.getVideosByContentUris(contentUriStrings)

    fun pause() {
        exoPlayer?.pause()
    }

    fun resume() {
        exoPlayer?.play()
    }

    fun setShuffleEnabled(enabled: Boolean) {
        _shuffleEnabled.value = enabled
        exoPlayer?.shuffleModeEnabled = enabled
    }

    fun setRepeatMode(@Player.RepeatMode mode: Int) {
        _repeatMode.value = mode
        exoPlayer?.repeatMode = mode
    }

    private fun releasePlayer() {
        pollJob?.cancel()
        pollJob = null
        clearABRepeat()
        activeVideoDecoderName = null
        _isPlaying.value = false
        _positionMs.value = 0L
        _durationMs.value = 0L
        exoPlayer?.removeListener(playerListener)
        exoPlayer?.removeAnalyticsListener(analyticsListener)
        exoPlayer?.release()
        exoPlayer = null
        _player.value = null
        playerDecoderMode = null
        _tracks.value = null
        stopBackgroundService()
    }
}
