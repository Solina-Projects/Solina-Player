package app.solina.player.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import app.solina.player.R
import app.solina.player.data.model.VideoItem
import app.solina.player.presentation.components.player.MorphingPlayPauseIcon
import app.solina.player.presentation.components.scoped.miniPlayerSkipGesture
import app.solina.player.presentation.components.scoped.rememberMiniPlayerSkipGestureHandler
import app.solina.player.presentation.navigation.Screen
import app.solina.player.presentation.navigation.navigateSafely
import app.solina.player.presentation.viewmodel.AppearanceViewModel
import app.solina.player.presentation.viewmodel.VideoPlaybackHolder
import app.solina.player.presentation.viewmodel.VideoViewModel
import app.solina.player.ui.theme.RoundedSans
import app.solina.player.utils.formatDuration

/**
 * Which place this video bar lives in — the Video section or the fixed
 * bar under "Recently added videos" on Home.
 */
enum class VideoMiniPlayerBarKind { SECTION, HOME }

@OptIn(UnstableApi::class)
@Composable
fun VideoMiniPlayerBar(
    navController: NavController,
    modifier: Modifier = Modifier,
    kind: VideoMiniPlayerBarKind = VideoMiniPlayerBarKind.SECTION,
    videoViewModel: VideoViewModel = hiltViewModel(),
    appearanceViewModel: AppearanceViewModel = hiltViewModel()
) {
    val holder = videoViewModel.videoPlaybackHolder
    val currentVideo by holder.currentVideo.collectAsStateWithLifecycle()
    val isPlaying by holder.isPlaying.collectAsStateWithLifecycle()
    val queueUris by holder.queueUris.collectAsStateWithLifecycle()
    val resumeUri by videoViewModel.videoResumeUri.collectAsStateWithLifecycle()
    val resumePositionMs by videoViewModel.videoResumePositionMs.collectAsStateWithLifecycle()
    val resumeTitle by videoViewModel.videoResumeTitle.collectAsStateWithLifecycle()
    val videos by videoViewModel.videos.collectAsStateWithLifecycle()

    val appearance by appearanceViewModel.appearance.collectAsStateWithLifecycle()
    val barAppearance = when (kind) {
        VideoMiniPlayerBarKind.HOME -> appearance.homeVideoBar
        VideoMiniPlayerBarKind.SECTION -> appearance.sectionVideoBar
    }
    if (!barAppearance.visible) return
    val sectionPaletteName = when (kind) {
        VideoMiniPlayerBarKind.HOME -> appearance.homeSectionPalette
        VideoMiniPlayerBarKind.SECTION -> appearance.videoSectionPalette
    }
    val barHeight = MiniPlayerHeight * (barAppearance.scalePercent / 100f)
    val fontScale = appearance.fontScalePercent / 100f

    val shownVideo = currentVideo
        ?: videos.firstOrNull { it.contentUriString == resumeUri }
    val title = shownVideo?.displayTitle ?: resumeTitle
    if (shownVideo == null && title == null) return

    val uri = shownVideo?.contentUriString ?: resumeUri ?: return

    fun openFullPlayer() {
        val startPos = if (currentVideo != null) holder.positionMs.value else resumePositionMs
        navController.navigateSafely(
            Screen.VideoPlayer.createRoute(
                startUri = uri,
                queueUris = queueUris,
                startPositionMs = startPos
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

    val colors = effectivePalette(barAppearance.palette, sectionPaletteName)
        ?.let { MaterialTheme.colorScheme.withPalette(it) }
        ?: MaterialTheme.colorScheme
    val glass = appearance.glass
    val glassOn = glass.enabled && glass.miniBars
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

    val swipeToSkip by videoViewModel.miniPlayerSwipeToSkip.collectAsStateWithLifecycle()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val scope = rememberCoroutineScope()
    val skipOffsetX = remember { Animatable(0f) }
    val skipContentAlpha = remember { Animatable(1f) }
    val screenWidthPx = remember(configuration, density) {
        with(density) { configuration.screenWidthDp.dp.toPx() }
    }
    val skipGestureHandler = rememberMiniPlayerSkipGestureHandler(
        scope = scope,
        hapticFeedback = hapticFeedback,
        translationXAnimatable = skipOffsetX,
        contentAlphaAnimatable = skipContentAlpha,
        screenWidthPx = screenWidthPx,
        onSkipToNext = { stepNeighbour(1) },
        onSkipToPrevious = { stepNeighbour(-1) }
    )
    LaunchedEffect(swipeToSkip) {
        if (skipOffsetX.value != 0f) skipOffsetX.snapTo(0f)
        if (skipContentAlpha.value != 1f) skipContentAlpha.snapTo(1f)
    }

    val thumbShape = if (kind == VideoMiniPlayerBarKind.HOME) RoundedCornerShape(10.dp) else CircleShape

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
            .graphicsLayer {
                translationX = skipOffsetX.value
                alpha = skipContentAlpha.value
            }
            .glassBar(
                glass = glass,
                tintColor = colors.primaryContainer,
                corners = barAppearance.effectiveCorners,
                enabled = glassOn
            )
            .miniPlayerSkipGesture(
                enabled = swipeToSkip,
                skipHandler = skipGestureHandler
            )
            .clickable { openFullPlayer() },
        shape = barAppearance.effectiveCorners.toShape(),
        color = if (glassOn) Color.Transparent else colors.primaryContainer,
        shadowElevation = 4.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight)
                    .padding(start = 10.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Crisp 16:9 thumbnail on Home (preserves video framing), circular in Video section.
                Box(
                    modifier = Modifier
                        .then(
                            if (kind == VideoMiniPlayerBarKind.HOME) {
                                Modifier.size(width = 56.dp, height = 38.dp)
                            } else {
                                Modifier.size(44.dp)
                            }
                        )
                        .clip(thumbShape)
                        .background(Color.Black)
                        .then(
                            if (isPlaying) {
                                Modifier.border(2.dp, colors.primary, thumbShape)
                            } else {
                                Modifier
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (shownVideo != null) {
                        VideoThumbnail(
                            video = shownVideo,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.VideoLibrary,
                            contentDescription = null,
                            tint = colors.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = title ?: stringResource(R.string.video_player_title),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 14.5.sp * fontScale,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.2).sp,
                            fontFamily = RoundedSans,
                            color = if (glassOn) Color.White else colors.onPrimaryContainer,
                            shadow = if (glassOn) Shadow(
                                color = Color.Black.copy(alpha = 0.45f),
                                offset = Offset(0f, 1f),
                                blurRadius = 4f
                            ) else null
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Isolated 250ms ticker so outer thumbnail/controls never recompose on playback tick.
                    VideoMiniBarTimeLabel(
                        holder = holder,
                        isLive = currentVideo != null,
                        shownVideo = shownVideo,
                        resumePositionMs = resumePositionMs,
                        fontScale = fontScale,
                        glassOn = glassOn,
                        colors = colors
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))

                MiniEqBars(
                    isPlaying = isPlaying,
                    color = colors.primary
                )
                Spacer(modifier = Modifier.width(10.dp))

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
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
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Box(
                            modifier = Modifier
                                .size(32.dp)
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
                                size = 20.dp
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Box(
                            modifier = Modifier
                                .size(32.dp)
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
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            VideoMiniBarProgressStrip(
                holder = holder,
                isLive = currentVideo != null,
                shownVideo = shownVideo,
                resumePositionMs = resumePositionMs,
                colors = colors,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp)
            )
        }
    }
}

@Composable
private fun VideoMiniBarTimeLabel(
    holder: VideoPlaybackHolder,
    isLive: Boolean,
    shownVideo: VideoItem?,
    resumePositionMs: Long,
    fontScale: Float,
    glassOn: Boolean,
    colors: ColorScheme
) {
    val positionMs by holder.positionMs.collectAsStateWithLifecycle()
    val durationMs by holder.durationMs.collectAsStateWithLifecycle()
    val livePosition = if (isLive) positionMs else resumePositionMs
    val liveDuration = shownVideo?.durationMs?.takeIf { it > 0 }
        ?: if (isLive) durationMs else 0L

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Text(
            text = "${formatDuration(livePosition)} / ${formatDuration(liveDuration)}",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp * fontScale,
                color = if (glassOn) Color.White.copy(alpha = 0.85f)
                else colors.onPrimaryContainer.copy(alpha = 0.72f),
                shadow = if (glassOn) Shadow(
                    color = Color.Black.copy(alpha = 0.45f),
                    offset = Offset(0f, 1f),
                    blurRadius = 4f
                ) else null
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun VideoMiniBarProgressStrip(
    holder: VideoPlaybackHolder,
    isLive: Boolean,
    shownVideo: VideoItem?,
    resumePositionMs: Long,
    colors: ColorScheme,
    modifier: Modifier = Modifier
) {
    val positionMs by holder.positionMs.collectAsStateWithLifecycle()
    val durationMs by holder.durationMs.collectAsStateWithLifecycle()
    val livePosition = if (isLive) positionMs else resumePositionMs
    val liveDuration = shownVideo?.durationMs?.takeIf { it > 0 }
        ?: if (isLive) durationMs else 0L
    val progress = if (liveDuration > 0) {
        (livePosition.toFloat() / liveDuration).coerceIn(0f, 1f)
    } else 0f

    GradientProgressBar(
        progress = progress,
        modifier = modifier,
        startColor = colors.primary,
        endColor = colors.tertiary,
        trackColor = colors.onPrimaryContainer.copy(alpha = 0.12f)
    )
}
