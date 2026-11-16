package app.solina.player.presentation.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import app.solina.player.R
import app.solina.player.presentation.components.player.MorphingPlayPauseIcon
import app.solina.player.presentation.navigation.Screen
import app.solina.player.presentation.navigation.navigateSafely
import app.solina.player.presentation.viewmodel.VideoViewModel
import app.solina.player.ui.theme.RoundedSans
import app.solina.player.utils.formatDuration

/**
 * Video mini-player bar (2026-11-16): in the Video section, above the
 * floating bottom bar, a bar like the music now-playing bar shows the last
 * video that was played.
 *
 * Home feedback (2026-11-16): this bar must look EXACTLY like the music
 * mini-player bar — the same pink stadium pill (64 dp, 32 dp corners,
 * primaryContainer), the same 44 dp circular slot on the left, the same
 * title/artist typography, the same live EQ bars, and the same three
 * transport circles (white prev / primary play-pause / white next).
 * The only difference is content: the video thumbnail (a live preview
 * while playing, the frame thumbnail while paused) sits in the
 * note-icon slot, and the video title + played time sit in the text
 * slot. The progress indication stays as a thin gradient line along
 * the pill's bottom edge.
 *
 * - While the film plays in the background it plays SMALL: a live
 *   preview (the shared holder's player attached to a small PlayerView)
 *   inside the circular slot. When it is stopped, the preview freezes
 *   on the thumbnail.
 * - Prev / next step through the queue (or the device video list when the
 *   video was played on its own); play / pause controls the holder.
 * - Tapping the bar reopens the full player at the current position.
 * - Hidden when no video has ever been played. When the app restarted (no
 *   live player) it falls back to the saved resume point: thumbnail,
 *   title and saved position; tapping it resumes in the full player.
 * - The player lives in the app-scoped [VideoPlaybackHolder], so this bar
 *   works while moving around inside the app; when the app itself is
 *   backgrounded it follows the background-playback toggle, and if Android
 *   kills the process the bar falls back to the resume point above.
 *
 * The bar is also used as the fixed full-width playback bar under the
 * "Recently added videos" row on Home (Home feedback, 2026-11-16) — the
 * caller only changes the outer [modifier]; the pill itself is identical.
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

    // Same pill as the music mini player (UnifiedPlayerSheetShared):
    // 64 dp high, 32 dp stadium corners, primaryContainer background,
    // onPrimaryContainer text, primary play circle, white prev/next
    // circles. Colours come from the app theme scheme (the music bar
    // uses the same roles from its album-art scheme, which falls back
    // to this scheme whenever a song has no artwork).
    val colors = MaterialTheme.colorScheme
    val hapticFeedback = LocalHapticFeedback.current
    val previousInteraction = remember { MutableInteractionSource() }
    val playPauseInteraction = remember { MutableInteractionSource() }
    val nextInteraction = remember { MutableInteractionSource() }
    val miniPlayerIndication = remember { ripple(bounded = false) }
    val playPauseCorner by animateDpAsState(
        targetValue = if (isPlaying) 10.dp else 18.dp,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "VideoMiniPlayerPlayPauseCorner"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(MiniPlayerHeight)
            .clickable { openFullPlayer() },
        shape = RoundedCornerShape(32.dp),
        color = colors.primaryContainer,
        shadowElevation = 4.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MiniPlayerHeight)
                    .padding(start = 10.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular slot: live preview while playing, frame
                // thumbnail while paused — the video's answer to the
                // music bar's circular album art.
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
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
                            tint = colors.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                ) {
                    Text(
                        text = title ?: stringResource(R.string.video_player_title),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.2).sp,
                            fontFamily = RoundedSans,
                            color = colors.onPrimaryContainer
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    RollingTimeText(
                        text = "${formatDuration(livePosition)} / ${formatDuration(liveDuration)}",
                        color = colors.onPrimaryContainer.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))

                // Live EQ bars, exactly where the music bar has them.
                MiniEqBars(
                    isPlaying = isPlaying,
                    color = colors.primary
                )
                Spacer(modifier = Modifier.width(8.dp))

                // Media transport controls stay left-to-right in RTL locales.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(colors.onPrimary)
                                .clickable(
                                    interactionSource = previousInteraction,
                                    indication = miniPlayerIndication
                                ) {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    stepNeighbour(-1)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipPrevious,
                                contentDescription = stringResource(R.string.video_previous_video),
                                tint = colors.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(playPauseCorner))
                                .background(colors.primary)
                                .clickable(
                                    interactionSource = playPauseInteraction,
                                    indication = miniPlayerIndication
                                ) {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (holder.playerOrNull == null) {
                                        holder.play(uri, resumePositionMs)
                                    } else {
                                        holder.togglePlayPause()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            MorphingPlayPauseIcon(
                                isPlaying = isPlaying,
                                tint = colors.onPrimary,
                                size = 22.dp
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(colors.onPrimary)
                                .clickable(
                                    interactionSource = nextInteraction,
                                    indication = miniPlayerIndication
                                ) {
                                    stepNeighbour(1)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = stringResource(R.string.video_next_video),
                                tint = colors.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
            // Thin progress line along the pill's bottom edge (the Surface
            // clips it to the stadium shape).
            GradientProgressBar(
                progress = progress,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp),
                startColor = colors.primary,
                endColor = Color(0xFF8FB8FF),
                trackColor = colors.onPrimaryContainer.copy(alpha = 0.12f)
            )
        }
    }
}
