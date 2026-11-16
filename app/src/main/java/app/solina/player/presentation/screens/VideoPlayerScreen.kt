package app.solina.player.presentation.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.ContentValues
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.TextureView
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.awaitEachGesture
import androidx.compose.ui.input.pointer.awaitFirstDown
import androidx.compose.ui.input.pointer.awaitPointerEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import app.solina.player.R
import app.solina.player.presentation.viewmodel.VideoPlayerViewModel
import app.solina.player.utils.PhoneOrientationPolicy
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Video player: a dedicated ExoPlayer with video + subtitle tracks, in the
 * same Expressive visual language as the music player. Separate from the
 * music player — no music queue, no crossfade, no audio-only service.
 * Subtitles: PlayerView's built-in CC button appears when the video carries
 * a subtitle track.
 *
 * Folders + Player feedback (2026-11-16):
 * - The floating bottom bar is HIDDEN while this screen is open (see
 *   MainActivity's hidden routes), so it never covers the player.
 * - A hidden extra control bar sits at the bottom, with an animated arrow
 *   handle in front of it: tapping the arrow slides the bar in/out with
 *   animation. The bar holds aspect / rotation, playback speed, screenshot,
 *   audio settings (volume + audio track), sleep mode, night light and
 *   background playback (audio keeps playing when the app goes to the
 *   background, like music).
 * - Leaving the player (or every 15 s while playing) saves a resume point
 *   so the Folders screen's Resume button can continue where it left off.
 *
 * Home-entry + Player feedback (2026-11-16):
 * - Playback starts in LANDSCAPE automatically, and a FIXED rotation icon
 *   is always visible on the right edge of the player (not only inside the
 *   hidden bar). Rotation works now: the old toggle set
 *   requestedOrientation, but [PhoneOrientationPolicy] forced the phone
 *   back to portrait from MainActivity.onConfigurationChanged, so the
 *   request was reverted the moment the orientation changed. The player
 *   now marks itself active in the policy while open, so the portrait
 *   lock is suspended for it and restored on leave.
 * - The top bar sits below the status bar and holds only back, the film
 *   title, subtitles, audio (dubbing) and the three-dot menu.
 * - The extra bar is a horizontal scrollable row of ROUND icon buttons
 *   with a label under each, like the reference player: Night Mode,
 *   Customise Items, Shuffle, Loop, Mute, Sleep Timer, A-B Repeat, Audio
 *   Effect, Equalizer, Screenshot, Background Play, Screen Rotation,
 *   Speed. The PlayerView's own controller still gives the seek bar and
 *   prev/pause/next below it.
 *
 * Gestures + bars feedback (2026-11-16):
 * - The arrow handle is now transparent (same dark translucent colour as
 *   its bar) and sits at the top, right under the three-dot menu. The top
 *   bar's background is the same translucent dark, not a solid surface.
 * - ALL bars auto-hide ~3 s after the last interaction while playing, and
 *   a tap anywhere on the video shows them again. The built-in PlayerView
 *   controller is OFF — the bottom transport bar below is custom, exactly
 *   like the reference: seek bar with times, then lock | prev -15s
 *   play/pause +15s next | fullscreen PiP.
 * - One gesture layer over the video surface, with clear thresholds, so
 *   tap / long-press / swipe-seek / brightness / volume / pinch do not
 *   fight each other: short tap toggles the bars; holding ~400 ms opens
 *   a speed bar just below the arrow (8 speeds, 0.1x–4x, slide while
 *   holding to pick, last speed remembered); horizontal drag seeks (the
 *   delta shows on screen, applied on release); vertical drag on the left
 *   half sets brightness, on the right half volume; two-finger pinch
 *   zooms the video (graphicsLayer scale on the PlayerView, 1.0x–3.0x,
 *   double-tap resets to 1.0x).
 */
@OptIn(ExperimentalMaterial3Api::class, UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    navController: NavController,
    videoUri: String,
    videoQueue: List<String> = emptyList(),
    startPositionMs: Long = 0L,
    videoPlayerViewModel: VideoPlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val player by videoPlayerViewModel.player.collectAsStateWithLifecycle()
    val currentVideo by videoPlayerViewModel.currentVideo.collectAsStateWithLifecycle()
    val playbackSpeed by videoPlayerViewModel.playbackSpeed.collectAsStateWithLifecycle()
    val isFullscreen by videoPlayerViewModel.isFullscreen.collectAsStateWithLifecycle()
    val isLoading by videoPlayerViewModel.isLoading.collectAsStateWithLifecycle()
    val aspectMode by videoPlayerViewModel.aspectMode.collectAsStateWithLifecycle()
    val backgroundPlayback by videoPlayerViewModel.backgroundPlayback.collectAsStateWithLifecycle()
    val nightLight by videoPlayerViewModel.nightLight.collectAsStateWithLifecycle()
    val brightness by videoPlayerViewModel.brightness.collectAsStateWithLifecycle()
    val volume by videoPlayerViewModel.volume.collectAsStateWithLifecycle()
    val sleepTimerMinutes by videoPlayerViewModel.sleepTimerMinutes.collectAsStateWithLifecycle()
    val audioTrackLabels by videoPlayerViewModel.audioTrackLabels.collectAsStateWithLifecycle()
    val selectedAudioTrack by videoPlayerViewModel.selectedAudioTrack.collectAsStateWithLifecycle()
    val subtitleTrackLabels by videoPlayerViewModel.subtitleTrackLabels.collectAsStateWithLifecycle()
    val selectedSubtitleTrack by videoPlayerViewModel.selectedSubtitleTrack.collectAsStateWithLifecycle()
    val shuffleEnabled by videoPlayerViewModel.shuffleEnabled.collectAsStateWithLifecycle()
    val repeatMode by videoPlayerViewModel.repeatMode.collectAsStateWithLifecycle()
    val isMuted by videoPlayerViewModel.isMuted.collectAsStateWithLifecycle()
    val aPointMs by videoPlayerViewModel.aPointMs.collectAsStateWithLifecycle()
    val bPointMs by videoPlayerViewModel.bPointMs.collectAsStateWithLifecycle()
    val visibleButtons by videoPlayerViewModel.visibleControlButtons.collectAsStateWithLifecycle()

    var showSpeedMenu by remember { mutableStateOf(false) }
    var showSubtitleMenu by remember { mutableStateOf(false) }
    var showAudioMenu by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showCustomiseDialog by remember { mutableStateOf(false) }
    var controlsExpanded by rememberSaveable { mutableStateOf(false) }
    var controlsLocked by rememberSaveable { mutableStateOf(false) }
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }
    // Rotation: null = follow the system, true = landscape, false = portrait.
    // Starts at true: selecting a film plays it horizontal automatically.
    var forcedLandscape by rememberSaveable { mutableStateOf<Boolean?>(true) }

    // ---- Gestures + bars state (2026-11-16) ----
    // All bars auto-hide ~3 s after the last interaction while playing;
    // a tap on the video shows them again. interactionTick restarts the
    // timer; poke() is called on every user interaction with the player.
    var barsVisible by remember { mutableStateOf(true) }
    var interactionTick by remember { mutableIntStateOf(0) }
    fun poke() {
        interactionTick++
    }
    // Pinch zoom: graphicsLayer scale on the PlayerView, 1.0x–3.0x.
    var zoomScale by remember { mutableStateOf(1f) }
    // Long-press speed mode: the speed bar just below the arrow.
    var speedMode by remember { mutableStateOf(false) }
    var speedBarVisible by remember { mutableStateOf(false) }
    // Seek preview while dragging (horizontal swipe or the seek slider).
    var seekPreviewMs by remember { mutableLongStateOf(-1L) }
    var seekDeltaText by remember { mutableStateOf<String?>(null) }
    var scrubbing by remember { mutableStateOf(false) }
    // Centre hint while a vertical swipe adjusts brightness / volume.
    var gestureHint by remember { mutableStateOf<GestureHint?>(null) }
    // Brightness set by a left-half swipe (session only); null means the
    // night-light value (or the system brightness) applies instead.
    var gestureBrightness by rememberSaveable { mutableStateOf<Float?>(null) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var lastTapTimeMs by remember { mutableLongStateOf(0L) }

    val brightnessLabel = stringResource(R.string.video_brightness)
    val volumeLabel = stringResource(R.string.video_volume)
    val speedOptions = videoPlayerViewModel.availableSpeeds
    // Pixels per speed step while sliding in long-press speed mode.
    // (Computed here with LocalDensity: inside pointerInput, `density`
    // resolves to Density.density, a Float, so toPx() would not resolve.)
    val speedStepPx = with(LocalDensity.current) { 60.dp.toPx() }

    // Position / duration / playing state, polled for the custom bottom
    // bar (the built-in PlayerView controller is off now).
    LaunchedEffect(player) {
        while (currentCoroutineContext().isActive) {
            val p = player
            if (p != null) {
                if (!scrubbing) positionMs = p.currentPosition
                durationMs = p.duration.coerceAtLeast(0L)
                isPlaying = p.isPlaying
            }
            delay(250)
        }
    }

    // Auto-hide: hide all bars 3 s after the last interaction, only while
    // playing, and never while a menu/dialog or the speed bar is open.
    val anyMenuOpen = showSpeedMenu || showSubtitleMenu || showAudioMenu ||
        showOverflowMenu || showAudioDialog || showCustomiseDialog
    LaunchedEffect(barsVisible, interactionTick, isPlaying, anyMenuOpen, speedMode, speedBarVisible) {
        if (barsVisible && isPlaying && !anyMenuOpen && !speedMode && !speedBarVisible) {
            delay(3_000)
            barsVisible = false
        }
    }

    // Speed bar: stays up while the finger is held down choosing a speed,
    // and for 2 s after release so a speed can still be tapped.
    LaunchedEffect(speedBarVisible, speedMode) {
        if (speedBarVisible && !speedMode) {
            delay(2_000)
            speedBarVisible = false
        }
    }

    val decodedUri = remember(videoUri) {
        runCatching { URLDecoder.decode(videoUri, "UTF-8") }.getOrElse { videoUri }
    }

    LaunchedEffect(decodedUri, videoQueue) {
        videoPlayerViewModel.loadVideo(decodedUri)
        if (videoQueue.size > 1) {
            videoPlayerViewModel.playQueue(
                contentUriStrings = videoQueue,
                startIndex = videoQueue.indexOf(decodedUri).coerceAtLeast(0),
                startPositionMs = startPositionMs
            )
        } else {
            videoPlayerViewModel.play(decodedUri, startPositionMs)
        }
    }

    // Save the resume point every 15 s while this screen is open.
    LaunchedEffect(Unit) {
        while (currentCoroutineContext().isActive) {
            delay(15_000L)
            videoPlayerViewModel.saveResumePosition()
        }
    }

    // Background playback (like music): when OFF, pause the video as soon
    // as the app goes to the background; when ON, the audio keeps playing.
    DisposableEffect(lifecycleOwner, backgroundPlayback) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && !backgroundPlayback) {
                videoPlayerViewModel.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Brightness: the left-half swipe wins when it has set a value,
    // otherwise the night-light brightness applies, otherwise the system.
    LaunchedEffect(nightLight, brightness, gestureBrightness) {
        activity?.window?.let { window ->
            val attributes = window.attributes
            attributes.screenBrightness = when {
                gestureBrightness != null -> gestureBrightness!!.coerceIn(0.05f, 1f)
                nightLight -> brightness.coerceIn(10, 100) / 100f
                else -> -1f
            }
            window.attributes = attributes
        }
    }

    // Rotation: the player owns the orientation while it is open. The
    // policy's portrait lock is suspended, the requested orientation is
    // applied (landscape on entry), and everything is restored on leave.
    // This is the fix for the rotation button doing nothing: before, the
    // policy re-applied portrait in onConfigurationChanged and reverted
    // every landscape request immediately.
    LaunchedEffect(Unit) {
        PhoneOrientationPolicy.videoPlayerActive = true
    }
    LaunchedEffect(isFullscreen, forcedLandscape) {
        activity?.requestedOrientation = when {
            isFullscreen || forcedLandscape == true ->
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            forcedLandscape == false -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            videoPlayerViewModel.saveResumePosition()
            videoPlayerViewModel.pause()
            videoPlayerViewModel.setSleepTimer(0)
            PhoneOrientationPolicy.videoPlayerActive = false
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.let { window ->
                val attributes = window.attributes
                attributes.screenBrightness = -1f
                window.attributes = attributes
            }
        }
    }

    fun takeScreenshot() {
        val playerView = playerViewRef
        if (playerView == null) {
            Toast.makeText(
                context,
                context.getString(R.string.video_screenshot_failed),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val surfaceView = playerView.videoSurfaceView
        when (surfaceView) {
            is SurfaceView -> {
                val width = surfaceView.width.takeIf { it > 0 } ?: 1280
                val height = surfaceView.height.takeIf { it > 0 } ?: 720
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                PixelCopy.request(
                    surfaceView,
                    bitmap,
                    { result ->
                        if (result == PixelCopy.SUCCESS) {
                            saveScreenshotBitmap(context, bitmap)
                        } else {
                            captureFrameWithRetriever(
                                context,
                                decodedUri,
                                videoPlayerViewModel.currentPositionMs()
                            )
                        }
                    },
                    Handler(Looper.getMainLooper())
                )
            }
            is TextureView -> {
                val bitmap = surfaceView.bitmap
                if (bitmap != null) {
                    saveScreenshotBitmap(context, bitmap)
                } else {
                    captureFrameWithRetriever(
                        context,
                        decodedUri,
                        videoPlayerViewModel.currentPositionMs()
                    )
                }
            }
            else -> captureFrameWithRetriever(
                context,
                decodedUri,
                videoPlayerViewModel.currentPositionMs()
            )
        }
    }

    /** Open the system equalizer for this player's audio session. */
    fun openSystemEqualizer() {
        val sessionId = player?.audioSessionId ?: 0
        val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MOVIE)
            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
        }
        runCatching { context.startActivity(intent) }.onFailure {
            Toast.makeText(
                context,
                context.getString(R.string.video_equalizer_unavailable),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun enterPictureInPicture() {
        val act = activity ?: return
        runCatching {
            act.enterPictureInPictureMode(
                PictureInPictureParams.Builder().build()
            )
        }.onFailure {
            Toast.makeText(
                context,
                context.getString(R.string.video_pip_unavailable),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // The round control buttons, in reference order. A button is shown when
    // the visible set is empty (default) or contains its id.
    val allButtons = remember { videoControlButtons }
    val shownButtons = remember(allButtons, visibleButtons) {
        if (visibleButtons.isEmpty()) allButtons
        else allButtons.filter { visibleButtons.contains(it.id) }
    }

    // The arrow handle's rotation (0° closed, 180° open).
    val arrowRotation by animateFloatAsState(
        targetValue = if (controlsExpanded) 180f else 0f,
        label = "videoControlsArrow"
    )

    // Position shown on the seek bar: the drag preview while seeking.
    val displayPositionMs = if (seekPreviewMs >= 0) seekPreviewMs else positionMs
    // Centre hint: seek delta + target time, or brightness / volume %.
    val indicatorText: String? = when {
        seekPreviewMs >= 0 && seekDeltaText != null ->
            "$seekDeltaText • ${formatVideoTime(seekPreviewMs)}"
        gestureHint != null -> "${gestureHint.label} ${gestureHint.percent}%"
        else -> null
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
                    // The built-in controller is OFF: the bottom transport
                    // bar below is custom, and all bars share one
                    // auto-hide state, so the built-in controller must not
                    // pop up on its own while our bars are hidden.
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    playerViewRef = this
                }
            },
            update = { playerView ->
                playerView.player = player
                playerView.resizeMode = when (aspectMode) {
                    "FILL" -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    "ZOOM" -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Pinch zoom: scale the whole PlayerView (video
                    // surface) around its centre, 1.0x–3.0x.
                    scaleX = zoomScale
                    scaleY = zoomScale
                }
        )

        if (controlsLocked) {
            // Controls locked (reference bottom-left lock): only the unlock
            // button stays on screen; the PlayerView controller is off.
            IconButton(
                onClick = { controlsLocked = false },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 32.dp)
                    .size(56.dp)
                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = stringResource(R.string.video_unlock_controls),
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        } else {
            // ---- One gesture layer over the video surface. It sits above
            // the PlayerView and below all the bars, so taps on the bars
            // never reach it. A single custom detector (not stacked
            // detectTap/detectDrag/detectTransform modifiers) decides the
            // gesture by thresholds, so tap / long-press / seek / brightness
            // / volume / pinch cannot fight each other:
            //  * short tap (no move, < 400 ms) → toggle all bars;
            //    two quick taps → reset the pinch zoom to 1.0x.
            //  * hold > ~400 ms without moving → speed mode: the speed bar
            //    opens just below the arrow; sliding the finger
            //    forward/back while holding steps through the 8 speeds.
            //  * horizontal drag → seek (delta shown, applied on release).
            //  * vertical drag, left half → brightness; right half → volume.
            //  * second finger down → pinch zoom (span ratio).
            // Every pointer change is consumed, so the PlayerView never
            // sees these touches.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(player) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            down.consume()
                            var mode = GestureMode.Undecided
                            val slopPx = viewConfiguration.touchSlop
                            var seekStartMs = player?.currentPosition ?: 0L
                            var seekAccumMs = 0L
                            var brightStart = gestureBrightness
                                ?: activity?.window?.attributes?.screenBrightness
                                    ?.takeIf { it >= 0f }
                                ?: if (nightLight) brightness / 100f else 0.5f
                            var volStart = volume
                            var speedAnchorX = down.position.x
                            var speedIndex = speedOptions
                                .indexOfFirst { it == playbackSpeed }
                                .coerceAtLeast(0)
                            var pinchLastSpan = 0f
                            var longPressFired = false

                            // The long-press timer runs as a child of this
                            // gesture (coroutineScope): awaitEachGesture's
                            // receiver is not a CoroutineScope itself.
                            coroutineScope {
                            val longPressJob = launch {
                                delay(400)
                                if (mode == GestureMode.Undecided) {
                                    mode = GestureMode.Speed
                                    longPressFired = true
                                    speedMode = true
                                    speedBarVisible = true
                                    barsVisible = true
                                    poke()
                                }
                            }
                            try {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val pressed = event.changes.filter { it.pressed }
                                    if (pressed.isEmpty()) break
                                    if (pressed.size < 2 && mode == GestureMode.Pinch) {
                                        // One finger lifted mid-pinch: forget
                                        // the span so a fresh pinch re-inits.
                                        pinchLastSpan = 0f
                                    }
                                    if (pressed.size >= 2) {
                                        if (mode != GestureMode.Pinch || pinchLastSpan <= 0f) {
                                            longPressJob.cancel()
                                            if (mode == GestureMode.Speed) {
                                                speedMode = false
                                            }
                                            // A drag that turns into a pinch
                                            // is abandoned: clear its preview
                                            // so no stale hint stays on screen.
                                            if (mode == GestureMode.Seek) {
                                                seekPreviewMs = -1L
                                                seekDeltaText = null
                                                scrubbing = false
                                            }
                                            if (mode == GestureMode.Brightness ||
                                                mode == GestureMode.Volume
                                            ) {
                                                gestureHint = null
                                            }
                                            mode = GestureMode.Pinch
                                            val p0 = pressed[0].position
                                            val p1 = pressed[1].position
                                            pinchLastSpan = (p0 - p1).getDistance()
                                        } else {
                                            val p0 = pressed[0].position
                                            val p1 = pressed[1].position
                                            val span = (p0 - p1).getDistance()
                                            if (pinchLastSpan > 0f && span > 0f) {
                                                zoomScale = (zoomScale * span / pinchLastSpan)
                                                    .coerceIn(1f, 3f)
                                            }
                                            pinchLastSpan = span
                                        }
                                        pressed.forEach { it.consume() }
                                        continue
                                    }
                                    val change = pressed.firstOrNull { it.id == down.id }
                                        ?: pressed.first()
                                    val pos = change.position
                                    when (mode) {
                                        GestureMode.Undecided -> {
                                            val dx = pos.x - down.position.x
                                            val dy = pos.y - down.position.y
                                            if (abs(dx) > slopPx || abs(dy) > slopPx) {
                                                longPressJob.cancel()
                                                mode = if (abs(dx) > abs(dy)) {
                                                    GestureMode.Seek
                                                } else if (down.position.x < size.width / 2f) {
                                                    GestureMode.Brightness
                                                } else {
                                                    GestureMode.Volume
                                                }
                                                barsVisible = true
                                                poke()
                                                if (mode == GestureMode.Seek) {
                                                    val dur = player?.duration ?: 0L
                                                    if (dur > 0) {
                                                        scrubbing = true
                                                        seekAccumMs =
                                                            ((dx / size.width) * dur).toLong()
                                                        val target = (seekStartMs + seekAccumMs)
                                                            .coerceIn(0L, dur)
                                                        seekPreviewMs = target
                                                        seekDeltaText =
                                                            formatSeekDelta(seekAccumMs)
                                                    }
                                                }
                                            }
                                        }
                                        GestureMode.Seek -> {
                                            val dur = player?.duration ?: 0L
                                            if (dur > 0) {
                                                val dx = pos.x - down.position.x
                                                seekAccumMs =
                                                    ((dx / size.width) * dur).toLong()
                                                val target = (seekStartMs + seekAccumMs)
                                                    .coerceIn(0L, dur)
                                                seekPreviewMs = target
                                                seekDeltaText = formatSeekDelta(seekAccumMs)
                                            }
                                        }
                                        GestureMode.Brightness -> {
                                            val dy = pos.y - down.position.y
                                            val frac = (-dy / size.height).coerceIn(-1f, 1f)
                                            val newVal =
                                                (brightStart + frac).coerceIn(0.05f, 1f)
                                            gestureBrightness = newVal
                                            gestureHint = GestureHint(
                                                brightnessLabel,
                                                (newVal * 100).roundToInt()
                                            )
                                        }
                                        GestureMode.Volume -> {
                                            val dy = pos.y - down.position.y
                                            val frac = (-dy / size.height).coerceIn(-1f, 1f)
                                            val newVal =
                                                (volStart + frac).coerceIn(0f, 1f)
                                            // Live only: no DataStore write
                                            // per move; persisted on release.
                                            videoPlayerViewModel.setVolumeLive(newVal)
                                            gestureHint = GestureHint(
                                                volumeLabel,
                                                (newVal * 100).roundToInt()
                                            )
                                        }
                                        GestureMode.Speed -> {
                                            val stepPx = speedStepPx
                                            val dx = pos.x - speedAnchorX
                                            if (abs(dx) >= stepPx) {
                                                val steps = (dx / stepPx).toInt()
                                                speedIndex = (speedIndex + steps)
                                                    .coerceIn(0, speedOptions.lastIndex)
                                                speedAnchorX = pos.x
                                                videoPlayerViewModel.setSpeed(
                                                    speedOptions[speedIndex]
                                                )
                                                poke()
                                            }
                                        }
                                        GestureMode.Pinch -> {
                                            // Handled in the two-finger
                                            // branch above.
                                        }
                                    }
                                    change.consume()
                                }
                            } finally {
                                longPressJob.cancel()
                            }
                            // ---- Gesture end ----
                            when (mode) {
                                GestureMode.Undecided -> {
                                    if (!longPressFired) {
                                        // Tap: two quick taps reset the
                                        // pinch zoom, otherwise toggle bars.
                                        val now = System.currentTimeMillis()
                                        if (now - lastTapTimeMs < 350) {
                                            zoomScale = 1f
                                            lastTapTimeMs = 0L
                                        } else {
                                            lastTapTimeMs = now
                                            barsVisible = !barsVisible
                                            poke()
                                        }
                                    }
                                }
                                GestureMode.Seek -> {
                                    if (seekPreviewMs >= 0) {
                                        videoPlayerViewModel.seekTo(seekPreviewMs)
                                        positionMs = seekPreviewMs
                                    }
                                    seekPreviewMs = -1L
                                    seekDeltaText = null
                                    scrubbing = false
                                }
                                GestureMode.Brightness, GestureMode.Volume -> {
                                    gestureHint = null
                                    if (mode == GestureMode.Volume) {
                                        // Persist the volume once, at the
                                        // end of the swipe.
                                        videoPlayerViewModel.setVolume(volume)
                                    }
                                }
                                GestureMode.Speed -> {
                                    // The chosen speed stays (persisted by
                                    // setSpeed); the bar hides by itself
                                    // 2 s after release.
                                    speedMode = false
                                }
                                GestureMode.Pinch -> {
                                    // Nothing to commit.
                                }
                            }
                            poke()
                            } // coroutineScope: the long-press timer child
                        }
                    }
            )

            // ---- Top bar: back, film title, subtitles, audio (dubbing),
            // three-dot menu — and nothing else. Its background is the
            // same translucent dark as the arrow bar (no solid surface),
            // and it sits below the status bar (statusBarsPadding + extra
            // margin) so the title never clashes with it, in portrait and
            // in fullscreen-landscape. The animated arrow handle sits
            // right under the three-dot menu, and the long-press speed bar
            // opens just below the arrow. The whole top area auto-hides
            // with the other bars.
            AnimatedVisibility(
                visible = barsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                            Text(
                                text = currentVideo?.displayTitle
                                    ?: stringResource(R.string.video_player_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            // Subtitles: the file's subtitle tracks, or "off".
                            Box {
                                IconButton(onClick = { showSubtitleMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Rounded.ClosedCaption,
                                        contentDescription = stringResource(R.string.video_subtitles),
                                        tint = if (selectedSubtitleTrack >= 0) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            Color.White
                                        }
                                    )
                                }
                                DropdownMenu(
                                    expanded = showSubtitleMenu,
                                    onDismissRequest = { showSubtitleMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(text = stringResource(R.string.video_subtitle_off))
                                        },
                                        onClick = {
                                            videoPlayerViewModel.selectSubtitleTrack(-1)
                                            showSubtitleMenu = false
                                        }
                                    )
                                    if (subtitleTrackLabels.isEmpty()) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = stringResource(R.string.video_no_subtitles),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            },
                                            onClick = { showSubtitleMenu = false },
                                            enabled = false
                                        )
                                    } else {
                                        subtitleTrackLabels.forEachIndexed { index, label ->
                                            DropdownMenuItem(
                                                text = { Text(text = label) },
                                                onClick = {
                                                    videoPlayerViewModel.selectSubtitleTrack(index)
                                                    showSubtitleMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            // Audio: the file's audio tracks (dubbing).
                            Box {
                                IconButton(onClick = { showAudioMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Audiotrack,
                                        contentDescription = stringResource(R.string.video_audio_settings),
                                        tint = if (audioTrackLabels.size > 1) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            Color.White
                                        }
                                    )
                                }
                                DropdownMenu(
                                    expanded = showAudioMenu,
                                    onDismissRequest = { showAudioMenu = false }
                                ) {
                                    if (audioTrackLabels.isEmpty()) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = stringResource(R.string.video_no_audio_tracks),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            },
                                            onClick = { showAudioMenu = false },
                                            enabled = false
                                        )
                                    } else {
                                        audioTrackLabels.forEachIndexed { index, label ->
                                            DropdownMenuItem(
                                                text = { Text(text = label) },
                                                onClick = {
                                                    videoPlayerViewModel.selectAudioTrack(index)
                                                    showAudioMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            // Three-dot menu: the remaining actions.
                            Box {
                                IconButton(onClick = { showOverflowMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Rounded.MoreVert,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                                DropdownMenu(
                                    expanded = showOverflowMenu,
                                    onDismissRequest = { showOverflowMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = stringResource(R.string.video_aspect_ratio) +
                                                    ": " + when (aspectMode) {
                                                        "FILL" -> stringResource(R.string.video_aspect_fill)
                                                        "ZOOM" -> stringResource(R.string.video_aspect_zoom)
                                                        else -> stringResource(R.string.video_aspect_fit)
                                                    }
                                            )
                                        },
                                        onClick = {
                                            videoPlayerViewModel.cycleAspectMode()
                                            showOverflowMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(text = stringResource(R.string.video_screenshot))
                                        },
                                        onClick = {
                                            takeScreenshot()
                                            showOverflowMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = stringResource(R.string.video_background_playback) +
                                                    if (backgroundPlayback) " ✓" else ""
                                            )
                                        },
                                        onClick = {
                                            videoPlayerViewModel.setBackgroundPlayback(!backgroundPlayback)
                                            showOverflowMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(text = stringResource(R.string.video_fullscreen))
                                        },
                                        onClick = {
                                            videoPlayerViewModel.toggleFullscreen()
                                            showOverflowMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(text = stringResource(R.string.video_picture_in_picture))
                                        },
                                        onClick = {
                                            enterPictureInPicture()
                                            showOverflowMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(text = stringResource(R.string.video_lock_controls))
                                        },
                                        onClick = {
                                            controlsLocked = true
                                            showOverflowMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    // The animated arrow handle, transparent like its bar,
                    // right under the three-dot menu. Tapping it slides
                    // the round-button control bar in/out; the arrow
                    // itself rotates 180°.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 16.dp, top = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Surface(
                            onClick = {
                                controlsExpanded = !controlsExpanded
                                poke()
                            },
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.45f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowUp,
                                    contentDescription = stringResource(
                                        if (controlsExpanded) {
                                            R.string.video_player_hide_controls
                                        } else {
                                            R.string.video_player_more_controls
                                        }
                                    ),
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .rotate(arrowRotation)
                                )
                            }
                        }
                    }
                    // The speed bar: 8 speeds, 0.1x–4x, just below the
                    // arrow. Opens on long-press (and stays 2 s after
                    // release); the current speed is highlighted, and
                    // tapping a chip picks it directly.
                    AnimatedVisibility(
                        visible = speedBarVisible,
                        enter = fadeIn() + slideInVertically { -it / 2 },
                        exit = fadeOut() + slideOutVertically { -it / 2 }
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 8.dp)
                        ) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(speedOptions) { speed ->
                                    val selected = speed == playbackSpeed
                                    Surface(
                                        onClick = {
                                            videoPlayerViewModel.setSpeed(speed)
                                            poke()
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            Color.White.copy(alpha = 0.15f)
                                        }
                                    ) {
                                        Text(
                                            text = formatSpeedLabel(speed),
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = if (selected) {
                                                FontWeight.Bold
                                            } else {
                                                FontWeight.Normal
                                            },
                                            modifier = Modifier.padding(
                                                horizontal = 12.dp,
                                                vertical = 6.dp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ---- Fixed rotation icon: always one tap away on the right
            // edge, outside the collapsible bar. It hides with the other
            // bars and comes back with a tap on the video. ----
            AnimatedVisibility(
                visible = barsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Surface(
                    onClick = {
                        forcedLandscape = if (forcedLandscape == true) false else true
                        poke()
                    },
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.55f),
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.ScreenRotation,
                            contentDescription = stringResource(R.string.video_screen_rotation),
                            tint = if (forcedLandscape == true) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.White
                            },
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // ---- Bottom: the round-button control bar, then the custom
            // transport bar. Both auto-hide with the top bar. ----
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Extra control bar (round buttons, like the reference):
                // hidden unless the arrow opened it.
                AnimatedVisibility(
                    visible = barsVisible && controlsExpanded,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(
                            topStart = 28.dp,
                            topEnd = 28.dp
                        )
                    ) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(shownButtons, key = { it.id }) { button ->
                                VideoRoundControlButton(
                                    button = button,
                                    isActive = when (button.id) {
                                        "night" -> nightLight
                                        "shuffle" -> shuffleEnabled
                                        "mute" -> isMuted
                                        "background" -> backgroundPlayback
                                        "rotation" -> forcedLandscape == true
                                        "loop" -> repeatMode != Player.REPEAT_MODE_OFF
                                        "sleep" -> sleepTimerMinutes > 0
                                        "ab" -> aPointMs >= 0
                                        else -> false
                                    },
                                    labelOverride = when (button.id) {
                                        "loop" -> when (repeatMode) {
                                            Player.REPEAT_MODE_ONE ->
                                                stringResource(R.string.video_loop_one)
                                            Player.REPEAT_MODE_ALL ->
                                                stringResource(R.string.video_loop_all)
                                            else -> stringResource(R.string.video_loop)
                                        }
                                        "sleep" -> if (sleepTimerMinutes > 0) {
                                            stringResource(
                                                R.string.video_sleep_timer_active,
                                                sleepTimerMinutes
                                            )
                                        } else {
                                            stringResource(R.string.video_sleep_timer)
                                        }
                                        "ab" -> when {
                                            bPointMs >= 0 ->
                                                stringResource(R.string.video_ab_repeat_active)
                                            aPointMs >= 0 ->
                                                stringResource(R.string.video_ab_repeat_set_a)
                                            else -> stringResource(R.string.video_ab_repeat)
                                        }
                                        else -> null
                                    },
                                    iconOverride = if (button.id == "loop" &&
                                        repeatMode == Player.REPEAT_MODE_ONE
                                    ) {
                                        Icons.Rounded.RepeatOne
                                    } else {
                                        null
                                    },
                                    circleText = if (button.id == "speed") {
                                        formatSpeedLabel(playbackSpeed)
                                    } else {
                                        null
                                    },
                                    onClick = {
                                        when (button.id) {
                                            "night" -> videoPlayerViewModel.setNightLight(!nightLight)
                                            "customise" -> showCustomiseDialog = true
                                            "shuffle" -> videoPlayerViewModel.toggleShuffle()
                                            "loop" -> videoPlayerViewModel.cycleRepeatMode()
                                            "mute" -> videoPlayerViewModel.toggleMute()
                                            "sleep" -> videoPlayerViewModel.setSleepTimer(
                                                when (sleepTimerMinutes) {
                                                    0 -> 15
                                                    15 -> 30
                                                    30 -> 60
                                                    else -> 0
                                                }
                                            )
                                            "ab" -> videoPlayerViewModel.tapABRepeat()
                                            "audiofx" -> showAudioDialog = true
                                            "equalizer" -> openSystemEqualizer()
                                            "screenshot" -> takeScreenshot()
                                            "background" -> videoPlayerViewModel.setBackgroundPlayback(
                                                !backgroundPlayback
                                            )
                                            "rotation" -> {
                                                forcedLandscape =
                                                    if (forcedLandscape == true) false else true
                                            }
                                            "speed" -> showSpeedMenu = true
                                        }
                                        poke()
                                    },
                                )
                            }
                        }
                        // Speed menu, anchored to the speed round button.
                        DropdownMenu(
                            expanded = showSpeedMenu,
                            onDismissRequest = { showSpeedMenu = false }
                        ) {
                            speedOptions.forEach { speed ->
                                DropdownMenuItem(
                                    text = {
                                        Text(text = formatSpeedLabel(speed))
                                    },
                                    onClick = {
                                        videoPlayerViewModel.setSpeed(speed)
                                        showSpeedMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
                // Bottom transport bar, exactly like the reference: a seek
                // bar with the current time on the left and the total time
                // on the right, and below it lock on the far left, prev
                // -15s play/pause +15s next in the centre, fullscreen +
                // picture-in-picture on the far right.
                AnimatedVisibility(
                    visible = barsVisible,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = formatVideoTime(displayPositionMs),
                                color = Color.White,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                            Slider(
                                value = if (durationMs > 0) {
                                    displayPositionMs.toFloat() / durationMs
                                } else {
                                    0f
                                },
                                onValueChange = { fraction ->
                                    scrubbing = true
                                    seekPreviewMs = (fraction * durationMs).toLong()
                                },
                                onValueChangeFinished = {
                                    scrubbing = false
                                    if (seekPreviewMs >= 0) {
                                        videoPlayerViewModel.seekTo(seekPreviewMs)
                                        positionMs = seekPreviewMs
                                        seekPreviewMs = -1L
                                    }
                                    poke()
                                },
                                enabled = durationMs > 0,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF8FB8FF),
                                    activeTrackColor = Color(0xFF8FB8FF),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            )
                            Text(
                                text = formatVideoTime(durationMs),
                                color = Color.White,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = {
                                    controlsLocked = true
                                    poke()
                                },
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = stringResource(R.string.video_lock_controls),
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.align(Alignment.Center)
                            ) {
                                IconButton(
                                    onClick = {
                                        videoPlayerViewModel.seekToPrevious()
                                        poke()
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SkipPrevious,
                                        contentDescription = stringResource(R.string.video_previous_video),
                                        tint = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                                VideoSkip15Button(
                                    label = "-15s",
                                    contentDescription = stringResource(R.string.video_seek_back_15),
                                    onClick = {
                                        videoPlayerViewModel.seekBy(-15_000L)
                                        poke()
                                    }
                                )
                                IconButton(
                                    onClick = {
                                        videoPlayerViewModel.togglePlayPause()
                                        poke()
                                    },
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) {
                                            Icons.Rounded.Pause
                                        } else {
                                            Icons.Rounded.PlayArrow
                                        },
                                        contentDescription = stringResource(
                                            if (isPlaying) {
                                                R.string.video_pause
                                            } else {
                                                R.string.video_play
                                            }
                                        ),
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                VideoSkip15Button(
                                    label = "+15s",
                                    contentDescription = stringResource(R.string.video_seek_forward_15),
                                    onClick = {
                                        videoPlayerViewModel.seekBy(15_000L)
                                        poke()
                                    }
                                )
                                IconButton(
                                    onClick = {
                                        videoPlayerViewModel.seekToNext()
                                        poke()
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SkipNext,
                                        contentDescription = stringResource(R.string.video_next_video),
                                        tint = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.align(Alignment.CenterEnd)
                            ) {
                                IconButton(
                                    onClick = {
                                        videoPlayerViewModel.toggleFullscreen()
                                        poke()
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFullscreen) {
                                            Icons.Rounded.FullscreenExit
                                        } else {
                                            Icons.Rounded.Fullscreen
                                        },
                                        contentDescription = stringResource(R.string.video_fullscreen),
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { enterPictureInPicture() },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PictureInPictureAlt,
                                        contentDescription = stringResource(R.string.video_picture_in_picture),
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Centre hint for gesture feedback: seek delta + target time,
            // or brightness / volume while swiping.
            indicatorText?.let { text ->
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text(
                        text = text,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
            }
            // Buffering spinner (the built-in controller is off, so show
            // our own) — hidden while a gesture hint is on screen.
            if (isLoading && indicatorText == null) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                )
            }
        }
    }

    // ---- Audio Effect dialog: volume + audio track + night light. ----
    if (showAudioDialog) {
        AlertDialog(
            onDismissRequest = { showAudioDialog = false },
            title = { Text(text = stringResource(R.string.video_audio_effect)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Slider(
                            value = volume,
                            onValueChange = { videoPlayerViewModel.setVolume(it) },
                            valueRange = 0f..1f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    audioTrackLabels.forEachIndexed { index, label ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    videoPlayerViewModel.selectAudioTrack(index)
                                }
                                .padding(vertical = 6.dp)
                        ) {
                            Text(
                                text = if (index == selectedAudioTrack) "●  $label" else "○  $label",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    if (audioTrackLabels.size <= 1) {
                        Text(
                            text = stringResource(R.string.video_audio_track_single),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.NightsStay,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.video_night_light),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )
                        Slider(
                            value = brightness.toFloat(),
                            onValueChange = { videoPlayerViewModel.setBrightness(it.toInt()) },
                            valueRange = 10f..100f,
                            enabled = nightLight,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = nightLight,
                            onCheckedChange = { videoPlayerViewModel.setNightLight(it) }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAudioDialog = false }) {
                    Text(text = stringResource(R.string.video_player_done))
                }
            }
        )
    }

    // ---- Customise Items dialog: show/hide the round buttons. ----
    if (showCustomiseDialog) {
        val selectedIds = remember(visibleButtons) {
            mutableStateOf(
                if (visibleButtons.isEmpty()) allButtons.map { it.id }.toSet()
                else visibleButtons
            )
        }
        AlertDialog(
            onDismissRequest = { showCustomiseDialog = false },
            title = { Text(text = stringResource(R.string.video_customise_items)) },
            text = {
                Column {
                    allButtons.forEach { button ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val current = selectedIds.value
                                    selectedIds.value =
                                        if (current.contains(button.id)) current - button.id
                                        else current + button.id
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = selectedIds.value.contains(button.id),
                                onCheckedChange = { checked ->
                                    val current = selectedIds.value
                                    selectedIds.value =
                                        if (checked) current + button.id
                                        else current - button.id
                                }
                            )
                            Text(
                                text = stringResource(button.labelRes),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    // Saving the full set = "show all" (empty stored set).
                    val ids = selectedIds.value
                    videoPlayerViewModel.setVisibleControlButtons(
                        if (ids.size == allButtons.size) emptySet() else ids
                    )
                    showCustomiseDialog = false
                }) {
                    Text(text = stringResource(R.string.video_player_done))
                }
            }
        )
    }
}

/** One round control button: dark circle + icon, small label underneath. */
@Composable
private fun VideoRoundControlButton(
    button: VideoControlButton,
    isActive: Boolean,
    labelOverride: String?,
    circleText: String? = null,
    iconOverride: ImageVector? = null,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.width(64.dp)
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = if (isActive) {
                MaterialTheme.colorScheme.primary
            } else {
                Color.Black.copy(alpha = 0.45f)
            },
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (circleText != null) {
                    Text(
                        text = circleText,
                        color = if (isActive) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            Color.White
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Icon(
                        imageVector = iconOverride ?: button.icon,
                        contentDescription = null,
                        tint = if (isActive) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            Color.White
                        },
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
        Text(
            text = labelOverride ?: stringResource(button.labelRes),
            color = Color.White,
            fontSize = 11.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/** A small round ±15 s skip button of the bottom transport bar. */
@Composable
private fun VideoSkip15Button(
    label: String,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.15f),
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** A round button of the player's extra control bar. */
private data class VideoControlButton(
    val id: String,
    val icon: ImageVector,
    val labelRes: Int
)

private val videoControlButtons = listOf(
    VideoControlButton("night", Icons.Rounded.NightsStay, R.string.video_night_mode),
    VideoControlButton("customise", Icons.Rounded.Tune, R.string.video_customise_items),
    VideoControlButton("shuffle", Icons.Rounded.Shuffle, R.string.video_shuffle),
    VideoControlButton("loop", Icons.Rounded.Repeat, R.string.video_loop),
    VideoControlButton("mute", Icons.Rounded.VolumeOff, R.string.video_mute),
    VideoControlButton("sleep", Icons.Rounded.Bedtime, R.string.video_sleep_timer),
    VideoControlButton("ab", Icons.Rounded.RepeatOne, R.string.video_ab_repeat),
    VideoControlButton("audiofx", Icons.Rounded.Audiotrack, R.string.video_audio_effect),
    VideoControlButton("equalizer", Icons.Rounded.Equalizer, R.string.video_equalizer),
    VideoControlButton("screenshot", Icons.Rounded.PhotoCamera, R.string.video_screenshot),
    VideoControlButton("background", Icons.Rounded.Headphones, R.string.video_background_play),
    VideoControlButton("rotation", Icons.Rounded.ScreenRotation, R.string.video_screen_rotation),
    VideoControlButton("speed", Icons.Rounded.Speed, R.string.video_speed)
)

/** Which gesture the video-surface detector is currently tracking. */
private enum class GestureMode {
    Undecided, Seek, Brightness, Volume, Speed, Pinch
}

/** Centre hint while a vertical swipe adjusts brightness / volume. */
private data class GestureHint(val label: String, val percent: Int)

/** "mm:ss" (or "h:mm:ss" for long videos), for the bottom seek bar. */
private fun formatVideoTime(positionMs: Long): String {
    val totalSeconds = (positionMs / 1000).coerceAtLeast(0L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

/** "+15s" / "-10s": the seek delta shown while swiping horizontally. */
private fun formatSeekDelta(deltaMs: Long): String {
    val seconds = abs(deltaMs) / 1000
    return if (deltaMs >= 0) "+${seconds}s" else "-${seconds}s"
}

/** "1×" / "0.25×" / "4×": playback speed without trailing ".0". */
private fun formatSpeedLabel(speed: Float): String {
    val text = if (speed == speed.toInt().toFloat()) {
        speed.toInt().toString()
    } else {
        speed.toString()
    }
    return "${text}×"
}

/** Save a captured frame under Pictures/Solina Player. */
private fun saveScreenshotBitmap(context: android.content.Context, bitmap: Bitmap) {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "SolinaPlayer_$timestamp.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(
            MediaStore.Images.Media.RELATIVE_PATH,
            Environment.DIRECTORY_PICTURES + "/Solina Player"
        )
    }
    val uri = context.contentResolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        values
    )
    var saved = false
    if (uri != null) {
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                saved = bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            }
        }
        if (!saved) {
            runCatching { context.contentResolver.delete(uri, null, null) }
        }
    }
    Toast.makeText(
        context,
        if (saved) {
            context.getString(R.string.video_screenshot_saved)
        } else {
            context.getString(R.string.video_screenshot_failed)
        },
        Toast.LENGTH_SHORT
    ).show()
}

/**
 * Fallback frame grab for surfaces PixelCopy cannot read: pull the frame
 * at the current position straight from the file with MediaMetadataRetriever.
 */
private fun captureFrameWithRetriever(
    context: android.content.Context,
    videoUri: String,
    positionMs: Long
) {
    val retriever = MediaMetadataRetriever()
    val bitmap = runCatching {
        retriever.setDataSource(context, Uri.parse(videoUri))
        retriever.getFrameAtTime(
            positionMs * 1000,
            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
        )
    }.getOrNull()
    runCatching { retriever.release() }
    if (bitmap != null) {
        saveScreenshotBitmap(context, bitmap)
    } else {
        Toast.makeText(
            context,
            context.getString(R.string.video_screenshot_failed),
            Toast.LENGTH_SHORT
        ).show()
    }
}
