package app.solina.player.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.solina.player.presentation.components.scoped.miniPlayerSkipGesture
import app.solina.player.presentation.components.scoped.rememberMiniPlayerSkipGestureHandler
import app.solina.player.presentation.viewmodel.AppearanceViewModel
import app.solina.player.presentation.viewmodel.PlayerViewModel

/**
 * Fixed full-width music playback bar for Home (Home feedback,
 * 2026-11-16).
 *
 * The floating music mini player is disabled on Home now, so Home gets
 * this bar in the flow instead: it sits directly under the
 * "Recently added music" row, spans the full Home width in portrait
 * and landscape, and stays put (it is a list item, not an overlay), so
 * the user can continue music playback from Home.
 *
 * It is the same design as the floating music mini player — the same
 * [MiniPlayerContentInternal] (circular art, title/artist, live EQ
 * bars, white prev / primary play-pause / white next circles) inside
 * the same 64 dp stadium pill. The pill uses the app theme scheme; the
 * floating bar uses the same roles from its album-art scheme, which
 * falls back to this scheme whenever a song has no artwork.
 *
 * Hidden when nothing has ever been played (no current song). Tapping
 * the bar opens the full player; the transport buttons work directly.
 *
 * Swipe to skip (round 15, D1/D2): swiping the bar left / right steps
 * to the next / previous song with the floating music bar's shared-axis
 * swap animation, when the "swipe to change track" switch (Behaviour →
 * Player gestures) is on. Previous uses forcePreviousSong(), exactly
 * like the floating bar, so a right-swipe steps back instead of
 * restarting the current song.
 *
 * Bug-fix (2026-11-17, item 5): tapping the bar did NOT open the full
 * player. The bar only called [PlayerViewModel.expandPlayerSheet], but
 * the player sheet is not composed on Home (the floating mini player is
 * hidden there), so expanding did nothing visible. The caller therefore
 * passes [onOpenFullPlayer], which navigates to the Music section first
 * (where the sheet lives) and then expands it — the same full player
 * that opens from the mini bar inside the Music section.
 *
 * Appearance customisation (bug-fix round 2026-11-17, item 6): the
 * Home music bar has its own show/hide, size, corner and colour
 * settings, independent of the Video section bar and of the Home
 * video bar; when hidden it returns early so HomeScreen emits no
 * item for it at all.
 */
@Composable
fun HomeMusicPlaybackBar(
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
    onOpenFullPlayer: () -> Unit = { playerViewModel.expandPlayerSheet() },
    appearanceViewModel: AppearanceViewModel = hiltViewModel()
) {
    val stablePlayerState by playerViewModel.stablePlayerState.collectAsStateWithLifecycle()
    val currentSong = stablePlayerState.currentSong ?: return

    val appearance by appearanceViewModel.appearance.collectAsStateWithLifecycle()
    val barAppearance = appearance.homeMusicBar
    if (!barAppearance.visible) return
    val barHeight = MiniPlayerHeight * (barAppearance.scalePercent / 100f)
    val barScheme = effectivePalette(barAppearance.palette, appearance.homeSectionPalette)
        ?.let { MaterialTheme.colorScheme.withPalette(it) }
        ?: MaterialTheme.colorScheme
    // Glass & blur (blur-engine test build, 2026-11-17): when the
    // master switch and the mini-bars toggle are on, the pill goes
    // transparent and the blur behind it shows through, tinted with
    // the bar's own palette colour so the pink look survives.
    val glass = appearance.glass
    val glassOn = glass.enabled && glass.miniBars

    // Swipe to skip (round 15, D1/D2): the same shared-axis swap as the
    // floating music bar, via the same MiniPlayerSkipGestureHandler.
    // Governed by the same "swipe to change track" switch
    // (Behaviour → Player gestures, off by default).
    val playerConfig by playerViewModel.playerConfigSlice.collectAsStateWithLifecycle()
    val swipeToSkip = playerConfig.miniPlayerSwipeToSkip
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
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
        onSkipToNext = { playerViewModel.nextSong() },
        // The card has already slid away, so restarting the current track
        // instead of stepping back would read as the gesture having
        // failed — same choice as the floating bar.
        onSkipToPrevious = { playerViewModel.forcePreviousSong() }
    )
    // A preference flip mid-gesture would otherwise strand the bar
    // off-centre or faded out (same reset as the floating bar).
    LaunchedEffect(swipeToSkip) {
        if (skipOffsetX.value != 0f) skipOffsetX.snapTo(0f)
        if (skipContentAlpha.value != 1f) skipContentAlpha.snapTo(1f)
    }

    CompositionLocalProvider(LocalMaterialTheme provides barScheme) {
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
                    tintColor = barScheme.primaryContainer,
                    corners = barAppearance.effectiveCorners,
                    enabled = glassOn
                )
                .miniPlayerSkipGesture(
                    enabled = swipeToSkip,
                    skipHandler = skipGestureHandler
                )
                .clickable { onOpenFullPlayer() },
            shape = barAppearance.effectiveCorners.toShape(),
            color = if (glassOn) Color.Transparent else barScheme.primaryContainer,
            shadowElevation = 4.dp
        ) {
            MiniPlayerContentInternal(
                song = currentSong,
                isPlaying = stablePlayerState.isPlaying,
                isOutputConnecting = false,
                isPreparingPlayback = stablePlayerState.isBuffering,
                onPlayPause = { playerViewModel.playPause() },
                onPrevious = { playerViewModel.previousSong() },
                onNext = { playerViewModel.nextSong() },
                modifier = Modifier.fillMaxWidth(),
                canScroll = stablePlayerState.isPlaying,
                contentHeight = barHeight,
                glassOn = glassOn
            )
        }
    }
}
