package app.solina.player.presentation.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import app.solina.player.R
import app.solina.player.presentation.viewmodel.VideoPlayerViewModel
import java.net.URLDecoder

/**
 * Video player: a dedicated ExoPlayer with video + subtitle tracks, in the
 * same Expressive visual language as the music player. Separate from the
 * music player — no music queue, no crossfade, no audio-only service.
 * Subtitles: PlayerView's built-in CC button appears when the video carries
 * a subtitle track.
 */
@OptIn(ExperimentalMaterial3Api::class, UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    navController: NavController,
    videoUri: String,
    videoPlayerViewModel: VideoPlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val player by videoPlayerViewModel.player.collectAsStateWithLifecycle()
    val currentVideo by videoPlayerViewModel.currentVideo.collectAsStateWithLifecycle()
    val playbackSpeed by videoPlayerViewModel.playbackSpeed.collectAsStateWithLifecycle()
    val isFullscreen by videoPlayerViewModel.isFullscreen.collectAsStateWithLifecycle()
    var showSpeedMenu by remember { mutableStateOf(false) }

    val decodedUri = remember(videoUri) {
        runCatching { URLDecoder.decode(videoUri, "UTF-8") }.getOrElse { videoUri }
    }

    LaunchedEffect(decodedUri) {
        videoPlayerViewModel.loadVideo(decodedUri)
        videoPlayerViewModel.play(decodedUri)
    }

    DisposableEffect(Unit) {
        onDispose {
            videoPlayerViewModel.pause()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Fullscreen = landscape + hide the top bar; the PlayerView fills the screen.
    LaunchedEffect(isFullscreen) {
        activity?.requestedOrientation = if (isFullscreen) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            update = { playerView ->
                playerView.player = player
            },
            modifier = Modifier.fillMaxSize()
        )

        if (!isFullscreen) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null
                        )
                    }
                    Text(
                        text = currentVideo?.displayTitle
                            ?: stringResource(R.string.video_player_title),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Box {
                        IconButton(onClick = { showSpeedMenu = true }) {
                            Icon(
                                imageVector = Icons.Rounded.Speed,
                                contentDescription = stringResource(R.string.video_playback_speed)
                            )
                        }
                        DropdownMenu(
                            expanded = showSpeedMenu,
                            onDismissRequest = { showSpeedMenu = false }
                        ) {
                            videoPlayerViewModel.availableSpeeds.forEach { speed ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (speed == 1f) {
                                                "1×"
                                            } else {
                                                "${speed}×"
                                            }
                                        )
                                    },
                                    onClick = {
                                        videoPlayerViewModel.setSpeed(speed)
                                        showSpeedMenu = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { videoPlayerViewModel.toggleFullscreen() }) {
                        Icon(
                            imageVector = Icons.Rounded.Fullscreen,
                            contentDescription = stringResource(R.string.video_fullscreen)
                        )
                    }
                }
            }
        } else {
            IconButton(
                onClick = { videoPlayerViewModel.toggleFullscreen() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.FullscreenExit,
                    contentDescription = stringResource(R.string.video_exit_fullscreen),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
