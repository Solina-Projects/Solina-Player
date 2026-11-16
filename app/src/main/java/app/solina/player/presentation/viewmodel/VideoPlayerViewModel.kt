package app.solina.player.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import app.solina.player.data.model.VideoItem
import app.solina.player.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
 */
@UnstableApi
@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val videoRepository: VideoRepository
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

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            _isLoading.value = playbackState == Player.STATE_BUFFERING
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

    fun play(contentUriString: String) {
        if (_currentVideo.value?.contentUriString == contentUriString && exoPlayer != null) {
            exoPlayer?.play()
            return
        }
        releasePlayer()
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        val player = ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .build()
            .apply {
                addListener(playerListener)
                setMediaItem(MediaItem.fromUri(Uri.parse(contentUriString)))
                prepare()
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
    fun playQueue(contentUriStrings: List<String>, startIndex: Int = 0) {
        if (contentUriStrings.isEmpty()) return
        if (contentUriStrings.size == 1) {
            play(contentUriStrings.first())
            return
        }
        releasePlayer()
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        val player = ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .build()
            .apply {
                addListener(playerListener)
                setMediaItems(
                    contentUriStrings.map { MediaItem.fromUri(Uri.parse(it)) },
                    startIndex.coerceIn(0, contentUriStrings.lastIndex),
                    0L
                )
                prepare()
                play()
            }
        exoPlayer = player
        _player.value = player
        _isLoading.value = true
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        exoPlayer?.setPlaybackSpeed(speed)
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
