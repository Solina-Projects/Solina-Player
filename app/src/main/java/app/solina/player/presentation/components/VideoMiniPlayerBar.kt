package app.solina.player.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import app.solina.player.presentation.components.player.MorphingPlayPauseIcon
import app.solina.player.presentation.components.scoped.miniPlayerSkipGesture
import app.solina.player.presentation.components.scoped.rememberMiniPlayerSkipGestureHandler
import app.solina.player.presentation.navigation.Screen
import app.solina.player.presentation.navigation.navigateSafely
import app.solina.player.presentation.viewmodel.AppearanceViewModel
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
 * - Swiping the bar left / right steps to the next / previous video with
 *   the floating music bar's shared-axis swap animation, when the
 *   "swipe to change track" switch (Behaviour → Player gestures) is on.
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
/**
 * Which place this video bar lives in — the Video section or the fixed
 * bar under "Recently added videos" on Home. Appearance customisation
 * (bug-fix round 2026-11-17, item 6) treats the two independently.
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
    val positionMs by holder.positionMs.collectAsStateWithLifecycle()
    val durationMs by holder.durationMs.collectAsStateWithLifecycle()
    val queueUris by holder.queueUris.collectAsStateWithLifecycle()
    val resumeUri by videoViewModel.videoResumeUri.collectAsStateWithLifecycle()
    val resumePositionMs by videoViewModel.videoResumePositionMs.collectAsStateWithLifecycle()
    val resumeTitle by videoViewModel.videoResumeTitle.collectAsStateWithLifecycle()
    val videos by videoViewModel.videos.collectAsStateWithLifecycle()

    // Appearance customisation (bug-fix round 2026-11-17, item 6):
    // the Home bar and the Video section bar are tuned separately.
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

    // Round 16, item 1d: ONE snapshot drives the whole bar — title,
    // uri, position and duration all come from this one VideoItem
    // (live video, else the resume video looked up in the device
    // list), so the bar can never show one video's title with
    // another video's duration. The holder's polled duration is a
    // different state source — it belongs to whatever the player last
    // played, and the async title lookup lands at a different time —
    // so it is only a fallback while the snapshot IS the live video,
    // never in the resume branch (there it produced e.g. the saved
    // title + position of one video with the stale polled duration
    // of the video played before it). The MediaStore query only
    // returns rows with DURATION > 0, so a snapshot video always
    // carries its own duration and the fallback is near-dead code.
    // Live video wins; otherwise the saved resume video (looked up in the
    // device list so the thumbnail and duration are available).
    val shownVideo = currentVideo
        ?: videos.firstOrNull { it.contentUriString == resumeUri }
    val title = shownVideo?.displayTitle ?: resumeTitle
    if (shownVideo == null && title == null) return

    val uri = shownVideo?.contentUriString ?: resumeUri ?: return
    val livePosition = if (currentVideo != null) positionMs else resumePositionMs
    val liveDuration = shownVideo?.durationMs?.takeIf { it > 0 }
        ?: if (currentVideo != null) durationMs else 0L
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
    // to this scheme whenever a song has no artwork). Appearance
    // customisation (item 6) can recolour, resize and re-round it.
    val colors = effectivePalette(barAppearance.palette, sectionPaletteName)
        ?.let { MaterialTheme.colorScheme.withPalette(it) }
        ?: MaterialTheme.colorScheme
    // Glass & blur (blur-engine test build, 2026-11-17): when the
    // master switch and the mini-bars toggle are on, the pill goes
    // transparent and the blur behind it shows through, tinted with
    // the bar's own palette colour so the pink look survives.
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

    // Swipe to skip (round 15, D1/D2): the same shared-axis swap as the
    // floating music bar, via the same MiniPlayerSkipGestureHandler —
    // card exits 130 ms in the swipe direction while fading out, the
    // video changes while invisible, the new card springs in from the
    // opposite side, haptic at the commit threshold. Left = next video,
    // right = previous video, physical directions in LTR and RTL alike.
    // Governed by the same "swipe to change track" switch as the music
    // bars (Behaviour → Player gestures, off by default); the video
    // surface-gestures switch does NOT apply here — it governs seek /
    // brightness / volume on the full video player surface.
    // Both bar kinds (SECTION + HOME) share this composable, so wiring
    // it once here gives the swipe to both bars.
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
    // A preference flip mid-gesture would otherwise strand the bar
    // off-centre or faded out (same reset as the floating bar).
    LaunchedEffect(swipeToSkip) {
        if (skipOffsetX.value != 0f) skipOffsetX.snapTo(0f)
        if (skipContentAlpha.value != 1f) skipContentAlpha.snapTo(1f)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
            .graphicsLayer {
                // Draw-phase only: the drag / swap never recomposes the bar.
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
                // Circular thumbnail slot: uses cached thumbnail + subtle
                // animated active ring while playing instead of attaching a
                // SurfaceView-backed PlayerView inside a tiny CircleShape,
                // avoiding decoder surface races and circular clip artifacts.
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                        .then(
                            if (isPlaying) {
                                Modifier.border(2.dp, colors.primary, CircleShape)
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
                            fontSize = 15.sp * fontScale,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.2).sp,
                            fontFamily = RoundedSans,
                            // R15 F7: in glass mode the pill is transparent
                            // over the blurred list, so the dark-brown
                            // onPrimaryContainer text is weak — white text
                            // with a soft shadow instead.
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
                    // R15 F1: plain Text, not RollingTimeText. The rolling
                    // per-character AnimatedContent garbled the time
                    // ("65:00:20 / 55:91") under the holder's 250 ms
                    // position poll, and its per-character Row has no LTR
                    // pinning, so RTL could reorder the digits too.
                    // Pinned LTR so the digits keep their order in RTL.
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            text = "${formatDuration(livePosition)} / ${formatDuration(liveDuration)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 13.sp * fontScale,
                                color = if (glassOn) Color.White.copy(alpha = 0.85f)
                                else colors.onPrimaryContainer.copy(alpha = 0.7f),
                                shadow = if (glassOn) Shadow(
                                    color = Color.Black.copy(alpha = 0.45f),
                                    offset = Offset(0f, 1f),
                                    blurRadius = 4f
                                ) else null
                            ),
                            maxLines = 1
                        )
                    }
                }
                // R15 F3: 12 dp gaps (was 8 dp) so the EQ bars no longer
                // look glued to the text on long titles.
                Spacer(modifier = Modifier.width(12.dp))

                // Live EQ bars, exactly where the music bar has them.
                MiniEqBars(
                    isPlaying = isPlaying,
                    color = colors.primary
                )
                Spacer(modifier = Modifier.width(12.dp))

                // Media transport controls stay left-to-right in RTL locales.
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
