package app.solina.player.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import app.solina.player.R
import app.solina.player.presentation.navigation.Screen
import app.solina.player.presentation.navigation.navigateSafely
import app.solina.player.presentation.viewmodel.VideoViewModel
import app.solina.player.utils.formatDuration

/**
 * Video mini-player bar (2026-11-16): in the Video section, above the
 * floating bottom bar, a bar like the music now-playing bar shows the last
 * video that was played.
 *
 * - While the film plays in the background it plays SMALL: a live preview
 *   (the shared holder's player attached to a small PlayerView) inside
 *   the bar. When it is stopped, the preview freezes on the thumbnail.
 * - Prev / next step through the queue (or the device video list when the
 *   video was played on its own); play / pause controls the holder.
 * - A small progress bar + current / total time shows how much played.
 * - Tapping the bar reopens the full player at the current position.
 * - Hidden when no video has ever been played. When the app restarted (no
 *   live player) it falls back to the saved resume point: thumbnail,
 *   title and saved position; tapping it resumes in the full player.
 * - The player lives in the app-scoped [VideoPlaybackHolder], so this bar
 *   works while moving around inside the app; when the app itself is
 *   backgrounded it follows the background-playback toggle, and if Android
 *   kills the process the bar falls back to the resume point above.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoMiniPlayerBar(
    navController: NavController,
    modifier: Modifier = Modifier,
    videoViewModel: VideoViewModel = hiltViewModel()
) {
    val holder = videoViewModel.videoPlaybackHolder
    val currentVideo by holder.currentVideo.collectAsStateWithLifecycle()
    val isPlaying by holder.isPlaying.collectAsStateWithLifecycle()
    val positionMs by holder.positionMs.collectAsStateWithLifecycle()
    val durationMs by holder.durationMs.collectAsStateWithLifecycle()
    val queueUris by holder.queueUris.collectAsStateWithLifecycle()
    val resumeUri by videoViewModel.videoResumeUri.collectAsStateWithLifecycle()
    val resumePositionMs by videoViewModel.videoResumePositionMs.collectAsStateWithLifecycle()
    val resumeTitle by videoViewModel.videoResumeTitle.collectAsStateWithLifecycle()
    val videos by videoViewModel.videos.collectAsStateWithLifecycle()

    // Live video wins; otherwise the saved resume video (looked up in the
    // device list so the thumbnail and duration are available).
    val shownVideo = currentVideo
        ?: videos.firstOrNull { it.contentUriString == resumeUri }
    val title = shownVideo?.displayTitle ?: resumeTitle
    if (shownVideo == null && title == null) return

    val uri = shownVideo?.contentUriString ?: resumeUri ?: return
    val livePosition = if (currentVideo != null) positionMs else resumePositionMs
    val liveDuration = shownVideo?.durationMs?.takeIf { it > 0 } ?: durationMs
    val progress = if (liveDuration > 0) {
        (livePosition.toFloat() / liveDuration).coerceIn(0f, 1f)
    } else {
        0f
    }

    fun openFullPlayer() {
        navController.navigateSafely(
            Screen.VideoPlayer.createRoute(
                startUri = uri,
                queueUris = queueUris,
                startPositionMs = livePosition
            )
        )
    }

    fun stepNeighbour(step: Int) {
        if (queueUris.size > 1) {
            if (step < 0) holder.seekToPrevious() else holder.seekToNext()
            return
        }
        val index = videos.indexOfFirst { it.contentUriString == uri }
        if (index < 0) return
        val neighbour = videos.getOrNull(index + step) ?: return
        holder.play(neighbour.contentUriString)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable { openFullPlayer() },
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 4.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 6.dp, end = 4.dp)
        ) {
            // Small preview: live while playing, thumbnail when stopped.
            Box(
                modifier = Modifier
                    .size(width = 96.dp, height = 54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying && holder.playerOrNull != null) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                            }
                        },
                        update = { playerView ->
                            playerView.player = holder.playerOrNull
                        },
                        onRelease = { playerView ->
                            playerView.player = null
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (shownVideo != null) {
                    VideoThumbnail(
                        video = shownVideo,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.VideoLibrary,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title ?: stringResource(R.string.video_player_title),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${formatDuration(livePosition)} / ${formatDuration(liveDuration)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            IconButton(
                onClick = { stepNeighbour(-1) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = stringResource(R.string.video_previous_video),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(
                onClick = {
                    if (holder.playerOrNull == null) {
                        holder.play(uri, resumePositionMs)
                    } else {
                        holder.togglePlayPause()
                    }
                },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) {
                        Icons.Rounded.Pause
                    } else {
                        Icons.Rounded.PlayArrow
                    },
                    contentDescription = stringResource(
                        if (isPlaying) R.string.video_pause else R.string.video_play
                    ),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(28.dp)
                )
            }
            IconButton(
                onClick = { stepNeighbour(1) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = stringResource(R.string.video_next_video),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
