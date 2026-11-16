package app.solina.player.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import app.solina.player.data.model.VideoBookmark
import app.solina.player.data.model.VideoItem
import app.solina.player.data.model.VideoPlaylistEntry
import app.solina.player.data.model.VideoSubtitleStyle
import app.solina.player.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A dedicated video player, separate from the music [PlayerViewModel] /
 * MusicService (whose renderers factory deliberately strips video).
 *
 * The ExoPlayer itself lives in the app-scoped [VideoPlaybackHolder]
 * (mini player, 2026-11-16), built with the default renderers factory so
 * video and text (subtitle) tracks render, with the FFmpeg extension
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
    private val userPreferencesRepository: UserPreferencesRepository,
    private val videoPlaybackHolder: VideoPlaybackHolder
) : ViewModel() {

    /**
     * The ExoPlayer lives in the app-scoped [VideoPlaybackHolder] (mini
     * player, 2026-11-16), so it survives leaving this screen; this
     * ViewModel only drives it while the player screen is open. It is
     * deliberately never released here.
     */
    private val exoPlayer: ExoPlayer?
        get() = videoPlaybackHolder.playerOrNull

    val player: StateFlow<ExoPlayer?> = videoPlaybackHolder.player

    val currentVideo: StateFlow<VideoItem?> = videoPlaybackHolder.currentVideo

    val playbackSpeed: StateFlow<Float> = videoPlaybackHolder.playbackSpeed

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    val isLoading: StateFlow<Boolean> = videoPlaybackHolder.isLoading

    /** Long-press speed bar (2026-11-16): 8 options, 0.1x–4x. */
    val availableSpeeds: List<Float> = listOf(0.1f, 0.25f, 0.5f, 0.75f, 1f, 1.5f, 2f, 4f)

    // ---- Persisted player settings (Folders + Player, 2026-11-16) ----

    /** "FIT" / "FILL" / "ZOOM" / "STRETCH" — applied to the PlayerView resize mode. */
    val aspectMode: StateFlow<String> =
        userPreferencesRepository.videoAspectModeFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "FIT")

    /**
     * Reference-player redesign (2026-11-16): four aspect modes, in the
     * reference's order. FIT = fit inside the screen; FILL = fill the
     * screen, cropping (aspect kept); ZOOM = fill + extra 1.5x zoom;
     * STRETCH = stretch to fill, aspect ignored.
     */
    fun cycleAspectMode() {
        val next = when (aspectMode.value) {
            "FIT" -> "FILL"
            "FILL" -> "ZOOM"
            "ZOOM" -> "STRETCH"
            else -> "FIT"
        }
        viewModelScope.launch { userPreferencesRepository.setVideoAspectMode(next) }
    }

    /** Next aspect mode, for the centre feedback label. */
    fun nextAspectMode(): String = when (aspectMode.value) {
        "FIT" -> "FILL"
        "FILL" -> "ZOOM"
        "ZOOM" -> "STRETCH"
        else -> "FIT"
    }

    /** "HW" / "SW": the decoder actually in use (holder, real detection). */
    val decoderBadge: StateFlow<String> = videoPlaybackHolder.decoderBadge

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

    val volume: StateFlow<Float> = videoPlaybackHolder.volume

    fun setVolume(volume: Float) = videoPlaybackHolder.setVolume(volume)

    /**
     * Live volume change (swipe gesture, 2026-11-16): applies at once
     * without a DataStore write per move; the gesture end persists it
     * through [setVolume].
     */
    fun setVolumeLive(volume: Float) = videoPlaybackHolder.setVolumeLive(volume)

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
                videoPlaybackHolder.pause()
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
        // A subtitle file just picked by the user: select it as soon as
        // it appears in the track list (the SELECTION_FLAG_DEFAULT on
        // the configuration usually does this already; this is the
        // fallback so it never stays unselected).
        val pending = pendingExternalSubtitleLabel
        if (pending != null) {
            val index = _subtitleTrackLabels.value.indexOfFirst { it == pending }
            if (index >= 0) {
                pendingExternalSubtitleLabel = null
                selectSubtitleTrack(index)
            }
        }
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

    // ---- Reference-player redesign (2026-11-16): action feedback,
    // bookmarks, favourites, video playlist, queue, subtitle style,
    // external subtitle files. ----

    /** One-shot message for the screen to show as a toast; null = none. */
    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    fun postActionMessage(message: String) {
        _actionMessage.value = message
    }

    fun consumeActionMessage() {
        _actionMessage.value = null
    }

    /** Bookmarks of the video that is playing now, newest first. */
    val bookmarksForCurrentVideo: StateFlow<List<VideoBookmark>> =
        combine(
            userPreferencesRepository.videoBookmarksFlow,
            currentVideo
        ) { bookmarks, video ->
            if (video == null) emptyList()
            else bookmarks.filter { it.uri == video.contentUriString }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Save a bookmark at the current position. */
    fun addBookmark() {
        val video = currentVideo.value ?: return
        val position = currentPositionMs()
        viewModelScope.launch {
            userPreferencesRepository.addVideoBookmark(
                VideoBookmark(
                    uri = video.contentUriString,
                    positionMs = position,
                    title = video.displayTitle
                )
            )
            postActionMessage("bookmark_saved")
        }
    }

    fun deleteBookmark(positionMs: Long) {
        val video = currentVideo.value ?: return
        viewModelScope.launch {
            userPreferencesRepository.deleteVideoBookmark(
                uri = video.contentUriString,
                positionMs = positionMs
            )
            postActionMessage("bookmark_deleted")
        }
    }

    /** True when the video playing now is marked as a favourite. */
    val isFavourite: StateFlow<Boolean> =
        combine(
            userPreferencesRepository.videoFavouriteUrisFlow,
            currentVideo
        ) { favourites, video ->
            video != null && favourites.contains(video.contentUriString)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun toggleFavourite() {
        val video = currentVideo.value ?: return
        val favourite = !isFavourite.value
        viewModelScope.launch {
            userPreferencesRepository.setVideoFavourite(
                uri = video.contentUriString,
                favourite = favourite
            )
            postActionMessage(if (favourite) "favourite_added" else "favourite_removed")
        }
    }

    /** Add the video playing now to the simple "Videos" playlist. */
    fun addCurrentVideoToPlaylist() {
        val video = currentVideo.value ?: return
        viewModelScope.launch {
            val added = userPreferencesRepository.addToVideoPlaylist(
                VideoPlaylistEntry(
                    uri = video.contentUriString,
                    title = video.displayTitle
                )
            )
            postActionMessage(
                if (added) "added_to_playlist" else "already_in_playlist"
            )
        }
    }

    /** The playing queue as video items (for the Playing Queue list). */
    val queueItems: StateFlow<List<VideoItem>> =
        videoPlaybackHolder.queueUris.map { uris ->
            uris.mapNotNull { uri ->
                runCatching { videoPlaybackHolder.getVideoByUri(uri) }.getOrNull()
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Playing Queue: jump to a queue item. */
    fun playQueueIndex(index: Int) = videoPlaybackHolder.playQueueIndex(index)

    /** Subtitle style (text size / colour / background), persisted. */
    val subtitleStyle: StateFlow<VideoSubtitleStyle> =
        combine(
            userPreferencesRepository.videoSubtitleTextSizeFlow,
            userPreferencesRepository.videoSubtitleTextColorFlow,
            userPreferencesRepository.videoSubtitleBackgroundFlow
        ) { sizeSp, colorArgb, backgroundMode ->
            VideoSubtitleStyle(
                textSizeSp = sizeSp,
                textColorArgb = colorArgb,
                backgroundMode = backgroundMode
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            VideoSubtitleStyle()
        )

    fun setSubtitleTextSize(sizeSp: Float) {
        viewModelScope.launch {
            userPreferencesRepository.setVideoSubtitleTextSize(sizeSp)
        }
    }

    fun setSubtitleTextColor(argb: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setVideoSubtitleTextColor(argb)
        }
    }

    fun setSubtitleBackground(mode: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setVideoSubtitleBackground(mode)
        }
    }

    /**
     * External subtitle file picked by the user (.srt / .vtt / .ass /
     * .ssa): attached to the video playing now and remembered so it
     * reloads next time. The new track is auto-selected once the tracks
     * refresh (see [pendingExternalSubtitleLabel]).
     */
    private var pendingExternalSubtitleLabel: String? = null

    fun addExternalSubtitle(subUri: Uri, label: String, mimeType: String) {
        val selectedLabel = videoPlaybackHolder.attachExternalSubtitle(
            subUri = subUri,
            label = label,
            mimeType = mimeType
        )
        pendingExternalSubtitleLabel = selectedLabel
        postActionMessage("subtitle_loaded")
    }

    /** Mime type for a subtitle file, by extension. Null = unsupported. */
    fun subtitleMimeTypeFor(fileName: String): String? = when (
        fileName.substringAfterLast('.', "").lowercase()
    ) {
        "srt" -> MimeTypes.APPLICATION_SUBRIP
        "vtt" -> MimeTypes.TEXT_VTT
        "ass", "ssa" -> MimeTypes.TEXT_SSA
        else -> null
    }

    // ---- Shuffle / loop (Home-entry + Player, 2026-11-16). ----

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    fun toggleShuffle() {
        _shuffleEnabled.value = !_shuffleEnabled.value
        videoPlaybackHolder.setShuffleEnabled(_shuffleEnabled.value)
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
        videoPlaybackHolder.setRepeatMode(_repeatMode.value)
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
            preMuteVolume = volume.value.coerceAtLeast(0.05f)
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

    init {
        // The holder owns the player listener; this ViewModel only mirrors
        // the track lists for the subtitle/audio menus.
        viewModelScope.launch {
            videoPlaybackHolder.tracks.collect {
                refreshAudioTracks()
                refreshSubtitleTracks()
            }
        }
    }

    fun play(contentUriString: String, startPositionMs: Long = 0L) =
        videoPlaybackHolder.play(contentUriString, startPositionMs)

    suspend fun loadVideo(contentUriString: String) {
        videoPlaybackHolder.loadVideo(contentUriString)
    }

    /**
     * Play-all (bottom-bar feedback, 2026-11-16): play a whole list of
     * videos as one ExoPlayer queue, starting at [startIndex]. The
     * PlayerView next/previous controls step through the queue; the title
     * follows via the holder's media-item listener. Falls back to
     * single-video playback when the list has one item.
     */
    fun playQueue(
        contentUriStrings: List<String>,
        startIndex: Int = 0,
        startPositionMs: Long = 0L
    ) = videoPlaybackHolder.playQueue(contentUriStrings, startIndex, startPositionMs)

    /** Current position, for the resume point saved on leave. */
    fun currentPositionMs(): Long = videoPlaybackHolder.currentPositionMs()

    /**
     * Save the resume point (Folders screen's Resume button, and the mini
     * bar's fallback). Only mid-video positions count.
     */
    fun saveResumePosition() = videoPlaybackHolder.saveResumePosition()

    fun clearResumePosition() = videoPlaybackHolder.clearResumePosition()

    fun setSpeed(speed: Float) = videoPlaybackHolder.setSpeed(speed)

    /**
     * Called when the player screen is left (mini player, 2026-11-16):
     * save the resume point, clear the sleep timer, and pause — unless
     * background playback is on, in which case the holder keeps playing
     * for the video mini bar. The player itself is never released here.
     */
    fun onLeavePlayer() {
        saveResumePosition()
        setSleepTimer(0)
        if (!backgroundPlayback.value) videoPlaybackHolder.pause()
    }

    fun toggleFullscreen() {
        _isFullscreen.value = !_isFullscreen.value
    }

    // ---- Transport (custom bottom bar, gestures, 2026-11-16). ----

    /** Seek to an absolute position, clamped to the video's duration. */
    fun seekTo(positionMs: Long) = videoPlaybackHolder.seekTo(positionMs)

    /** Relative seek (the ±15 s buttons, swipe seek). */
    fun seekBy(deltaMs: Long) = videoPlaybackHolder.seekBy(deltaMs)

    fun togglePlayPause() = videoPlaybackHolder.togglePlayPause()

    fun seekToNext() = videoPlaybackHolder.seekToNext()

    fun seekToPrevious() = videoPlaybackHolder.seekToPrevious()

    fun pause() = videoPlaybackHolder.pause()

    fun resume() = videoPlaybackHolder.resume()

    override fun onCleared() {
        // The player is NOT released here: it lives in the app-scoped
        // holder so the video mini bar can keep showing it after leaving
        // the player screen. The screen's onDispose pauses it when
        // background playback is off.
        sleepJob?.cancel()
        sleepJob = null
        clearABRepeat()
        super.onCleared()
    }
}
