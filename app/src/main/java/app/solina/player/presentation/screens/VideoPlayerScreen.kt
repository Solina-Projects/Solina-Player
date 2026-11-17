package app.solina.player.presentation.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.ContentValues
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.util.TypedValue
import android.provider.MediaStore
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.TextureView
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
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
import androidx.compose.material.icons.rounded.WbSunny
import app.solina.player.presentation.components.WavySliderExpressive
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
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import app.solina.player.R
import app.solina.player.presentation.viewmodel.VideoPlayerViewModel
import app.solina.player.utils.PhoneOrientationPolicy
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
 *   animation. The bar holds aspect, playback speed, screenshot,
 *   audio settings (volume + audio track), sleep mode, night light and
 *   background playback (audio keeps playing when the app goes to the
 *   background, like music). Rotation is NOT in this bar any more
 *   (mini-player feedback, 2026-11-16): it lives only in the bottom
 *   transport bar.
 * - Leaving the player (or every 15 s while playing) saves a resume point
 *   so the Folders screen's Resume button can continue where it left off.
 *
 * Home-entry + Player feedback (2026-11-16):
 * - Playback starts in LANDSCAPE automatically, and the rotation toggle
 *   sits in the bottom transport bar (it used to be a fixed icon on the
 *   right edge; moved in the Search + Player polish, 2026-11-16). Rotation
 *   works now: the old toggle set
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
 *
 * Search + Player polish (2026-11-16):
 * - The round-button control bar now hangs under the arrow at the top
 *   and slides in from the right edge (right to left); its buttons are
 *   smaller and more compact (46 dp circles, 22 dp icons).
 * - The fixed right-edge rotation button is gone; rotation lives in the
 *   bottom transport bar, next to fullscreen and PiP.
 * - The bottom time bar scrubs at 0.5x — the same finger travel moves
 *   the position half as far; a tap still seeks straight to the point.
 * - The fullscreen icon works now: before it only flipped a state whose
 *   orientation effect was already satisfied by the forced landscape on
 *   entry, so the tap looked dead. Entering fullscreen hides the system
 *   bars (the video grows to the whole screen) and forces landscape;
 *   exiting shows the bars and goes back to portrait, with a toast.
 * - Every menu and dialog opened during playback is transparent like
 *   the rest of the player (Black ~0.75–0.85 alpha, white text).
 *
 * Reference-player redesign (2026-11-16):
 * - Top bar like the reference: back, title (2 lines in portrait),
 *   music note (audio tracks), subtitle icon (subtitle panel), a real
 *   HW/SW decoder badge, three-dot grid menu.
 * - Quick-action row under the top bar in landscape (equalizer,
 *   screenshot, audio, aspect Fit/Fill/Zoom/Stretch, speed circle,
 *   arrow); hidden in portrait until the arrow opens it.
 * - Big centre feedback label on aspect / speed / volume / brightness
 *   changes; vertical edge sliders for brightness / volume swipes.
 * - Three-dot grid menu (Audio, Subtitle, Queue, Aspect, Display,
 *   Bookmark, Chapter, Favourite, Playlist, Info, Share, Tutorial,
 *   More) — every item real; Cut omitted (no transcoder in the app),
 *   Online subtitles omitted (no subtitle provider).
 * - Subtitle panel (side panel in landscape, bottom sheet in
 *   portrait) with embedded tracks, a subtitle-file picker and
 *   subtitle style settings.
 * - Modern layer: gradient scrims instead of flat black, spring
 *   animations, press-scale on round buttons, one blue accent.
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
    val swipeGesturesEnabled by videoPlayerViewModel.swipeGesturesEnabled.collectAsStateWithLifecycle()
    val nightLight by videoPlayerViewModel.nightLight.collectAsStateWithLifecycle()
    val brightness by videoPlayerViewModel.brightness.collectAsStateWithLifecycle()
    val volume by videoPlayerViewModel.volume.collectAsStateWithLifecycle()
    val sleepTimerMinutes by videoPlayerViewModel.sleepTimerMinutes.collectAsStateWithLifecycle()
    val sleepTimerRemainingSeconds by videoPlayerViewModel.sleepTimerRemainingSeconds.collectAsStateWithLifecycle()
    // Screenshot button state machine (d3motion deploy-button: idle ->
    // loading -> verified checkmark). 0 = idle, 1 = capturing, 2 = done.
    // The check only shows on a real successful save (see takeScreenshot);
    // a failure returns to idle and the toast explains it.
    var screenshotState by remember { mutableIntStateOf(0) }
    LaunchedEffect(screenshotState) {
        if (screenshotState == 2) {
            delay(1200)
            screenshotState = 0
        }
    }
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
    val decoderBadge by videoPlayerViewModel.decoderBadge.collectAsStateWithLifecycle()
    val isFavourite by videoPlayerViewModel.isFavourite.collectAsStateWithLifecycle()
    val bookmarks by videoPlayerViewModel.bookmarksForCurrentVideo.collectAsStateWithLifecycle()
    val queueItems by videoPlayerViewModel.queueItems.collectAsStateWithLifecycle()
    val subtitleStyle by videoPlayerViewModel.subtitleStyle.collectAsStateWithLifecycle()
    val actionMessage by videoPlayerViewModel.actionMessage.collectAsStateWithLifecycle()

    var showSpeedMenu by remember { mutableStateOf(false) }
    var showGridMenu by remember { mutableStateOf(false) }
    var showSubtitlePanel by remember { mutableStateOf(false) }
    var showAudioListDialog by remember { mutableStateOf(false) }
    var showQueueDialog by remember { mutableStateOf(false) }
    var showBookmarksDialog by remember { mutableStateOf(false) }
    var showChaptersDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showTutorialDialog by remember { mutableStateOf(false) }
    var showDisplayDialog by remember { mutableStateOf(false) }
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
    // Relative scrubbing (Search + Player polish, 2026-11-16): dragging
    // the bottom time bar moves the position at 0.5x — half the finger
    // travel — so scrubbing feels slower and finer. A tap (one big jump)
    // still seeks straight to the tapped point. The horizontal-swipe seek
    // on the video surface is 0.5x too now (mini-player feedback,
    // 2026-11-16): the same finger travel moves half as far in time.
    var scrubAnchorFraction by remember { mutableStateOf<Float?>(null) }
    var scrubAnchorMs by remember { mutableLongStateOf(0L) }
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

    // Centre feedback label (reference-player redesign): a big white
    // label in the middle of the video ("STRETCH", "1.5×", "Volume
    // 80%") that fades out after ~1 s.
    var feedbackLabel by remember { mutableStateOf<String?>(null) }
    fun showFeedback(text: String) {
        feedbackLabel = text
    }
    LaunchedEffect(feedbackLabel) {
        if (feedbackLabel != null) {
            delay(1_100)
            feedbackLabel = null
        }
    }
    fun applySpeed(speed: Float) {
        videoPlayerViewModel.setSpeed(speed)
        showFeedback(formatSpeedLabel(speed))
        poke()
    }
    fun cycleAspect() {
        val next = videoPlayerViewModel.nextAspectMode()
        videoPlayerViewModel.cycleAspectMode()
        showFeedback(next)
        poke()
    }
    fun shareVideo() {
        val uri = currentVideo?.contentUriString ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(Intent.EXTRA_STREAM, Uri.parse(uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            context.startActivity(
                Intent.createChooser(
                    intent,
                    context.getString(R.string.video_grid_menu_share)
                )
            )
        }
        poke()
    }
    // Subtitle-file picker (reference-player redesign): .srt / .vtt /
    // .ass / .ssa, loaded as an external subtitle track.
    val subtitlePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        val name = runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && index >= 0) cursor.getString(index) else null
            }
        }.getOrNull() ?: uri.lastPathSegment ?: "subtitle.srt"
        val mime = videoPlayerViewModel.subtitleMimeTypeFor(name)
        if (mime == null) {
            Toast.makeText(
                context,
                context.getString(R.string.video_subtitle_unsupported),
                Toast.LENGTH_SHORT
            ).show()
            return@rememberLauncherForActivityResult
        }
        videoPlayerViewModel.addExternalSubtitle(uri, name, mime)
        poke()
    }
    // Action messages from the ViewModel (bookmark / favourite /
    // playlist / subtitle file) as toasts.
    LaunchedEffect(actionMessage) {
        val message = actionMessage ?: return@LaunchedEffect
        val resId = when (message) {
            "bookmark_saved" -> R.string.video_bookmark_saved
            "bookmark_deleted" -> R.string.video_bookmark_deleted
            "favourite_added" -> R.string.video_favourite_added
            "favourite_removed" -> R.string.video_favourite_removed
            "added_to_playlist" -> R.string.video_added_to_playlist
            "already_in_playlist" -> R.string.video_already_in_playlist
            "subtitle_loaded" -> R.string.video_subtitle_loaded
            else -> null
        }
        if (resId != null) {
            Toast.makeText(context, context.getString(resId), Toast.LENGTH_SHORT).show()
        }
        videoPlayerViewModel.consumeActionMessage()
    }

    val brightnessLabel = stringResource(R.string.video_brightness)
    val volumeLabel = stringResource(R.string.video_volume)
    val speedOptions = videoPlayerViewModel.availableSpeeds
    // Landscape overlap (mini-player feedback, 2026-11-16): on a narrow
    // phone the bottom transport row (lock + centre group + right group)
    // is wider than the screen, so the groups overlap. In landscape — and
    // on any narrow screen — every bottom-bar icon is 20% smaller, and the
    // row is a single Row with a weighted centre group, so the groups can
    // never overlap again. Width check: lock + centre + right at 0.8x is
    // ~352 dp + 16 dp padding = ~368 dp, inside a ~393 dp portrait phone.
    val configuration = LocalConfiguration.current
    val bottomBarScale =
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE ||
            configuration.screenWidthDp < 470
        ) {
            0.8f
        } else {
            1f
        }

    // Reference-player redesign (2026-11-16): landscape gets the
    // quick-action row under the top bar; portrait letterboxes the
    // video and hides the row until the arrow opens it.
    val isLandscape =
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    // ZOOM aspect mode adds a 1.5x zoom on top of the pinch zoom.
    val aspectZoom = if (aspectMode == "ZOOM") 1.5f else 1f

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
    val anyMenuOpen = showSpeedMenu || showGridMenu || showSubtitlePanel ||
        showAudioListDialog || showQueueDialog || showBookmarksDialog ||
        showChaptersDialog || showInfoDialog || showTutorialDialog ||
        showDisplayDialog || showAudioDialog || showCustomiseDialog
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
            videoPlayerViewModel.onLeavePlayer()
            PhoneOrientationPolicy.videoPlayerActive = false
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.let { window ->
                val attributes = window.attributes
                attributes.screenBrightness = -1f
                window.attributes = attributes
                window.insetsController?.show(WindowInsets.Type.systemBars())
            }
        }
    }

    /**
     * Screenshot with a real completion callback (d3motion deploy-button
     * state machine): [onDone] receives true only when the frame was
     * actually saved, so the button's checkmark never lies.
     */
    fun takeScreenshot(onDone: (Boolean) -> Unit) {
        val playerView = playerViewRef
        if (playerView == null) {
            Toast.makeText(
                context,
                context.getString(R.string.video_screenshot_failed),
                Toast.LENGTH_SHORT
            ).show()
            onDone(false)
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
                            onDone(saveScreenshotBitmap(context, bitmap))
                        } else {
                            captureFrameWithRetriever(
                                context,
                                decodedUri,
                                videoPlayerViewModel.currentPositionMs(),
                                onDone
                            )
                        }
                    },
                    Handler(Looper.getMainLooper())
                )
            }
            is TextureView -> {
                val bitmap = surfaceView.bitmap
                if (bitmap != null) {
                    onDone(saveScreenshotBitmap(context, bitmap))
                } else {
                    captureFrameWithRetriever(
                        context,
                        decodedUri,
                        videoPlayerViewModel.currentPositionMs(),
                        onDone
                    )
                }
            }
            else -> captureFrameWithRetriever(
                context,
                decodedUri,
                videoPlayerViewModel.currentPositionMs(),
                onDone
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

    /**
     * Fullscreen that actually does something (Search + Player polish,
     * 2026-11-16): before, the bottom-bar icon only flipped
     * [VideoPlayerViewModel.isFullscreen], whose orientation effect was
     * already satisfied by the forced landscape on entry — and nothing
     * hid the system bars or changed the video size, so the tap looked
     * dead. Now entering fullscreen hides the system bars (the video
     * grows to the whole screen) and forces landscape; exiting shows the
     * bars again and goes back to portrait, with a toast either way.
     */
    fun toggleFullscreenMode() {
        val entering = !isFullscreen
        videoPlayerViewModel.toggleFullscreen()
        forcedLandscape = entering
        Toast.makeText(
            context,
            context.getString(
                if (entering) R.string.video_fullscreen else R.string.video_exit_fullscreen
            ),
            Toast.LENGTH_SHORT
        ).show()
        poke()
    }

    // Fullscreen hides the system bars; leaving fullscreen shows them.
    LaunchedEffect(isFullscreen) {
        activity?.window?.let { window ->
            val controller = window.insetsController
            if (isFullscreen) {
                controller?.hide(WindowInsets.Type.systemBars())
                controller?.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller?.show(WindowInsets.Type.systemBars())
            }
        }
    }

    // The round control buttons, in reference order. A button is shown when
    // the visible set is empty (default) or contains its id.
    val allButtons = remember { videoControlButtons }
    val shownButtons = remember(allButtons, visibleButtons) {
        if (visibleButtons.isEmpty()) allButtons
        else allButtons.filter { visibleButtons.contains(it.id) }
    }

    // The quick row's buttons (Home feedback, 2026-11-16): the bar is
    // COMPLETELY hidden while closed — only the arrow handle shows — so
    // the row only ever renders once the arrow opens it, and then it
    // shows every visible button. (It used to show five priority
    // buttons + the arrow while collapsed.)
    val quickRowButtons = shownButtons

    /** One place for every round control button's action. */
    fun onControlButtonClick(button: VideoControlButton) {
        when (button.id) {
            "night" -> videoPlayerViewModel.setNightLight(!nightLight)
            "customise" -> showCustomiseDialog = true
            "shuffle" -> videoPlayerViewModel.toggleShuffle()
            "loop" -> videoPlayerViewModel.cycleRepeatMode()
            "mute" -> {
                val muting = !isMuted
                videoPlayerViewModel.toggleMute()
                if (muting) showFeedback(context.getString(R.string.video_muted))
            }
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
            "screenshot" -> {
                if (screenshotState == 0) {
                    screenshotState = 1
                    takeScreenshot { ok ->
                        screenshotState = if (ok) 2 else 0
                    }
                }
            }
            "background" -> videoPlayerViewModel.setBackgroundPlayback(
                !backgroundPlayback
            )
            "aspect" -> cycleAspect()
            "speed" -> showSpeedMenu = true
        }
        poke()
    }

    // The arrow handle's rotation (mini-player feedback, 2026-11-16):
    // the chevron points LEFT while the bar is closed and RIGHT once it
    // is open (KeyboardArrowUp rotated -90° / +90°).
    val arrowRotation by animateFloatAsState(
        targetValue = if (controlsExpanded) 90f else -90f,
        label = "videoControlsArrow"
    )

    // Position shown on the seek bar: the drag preview while seeking.
    val displayPositionMs = if (seekPreviewMs >= 0) seekPreviewMs else positionMs
    // Centre hint: seek delta + target time only. Brightness / volume
    // use the vertical edge slider while dragging, and the centre
    // feedback label reports the final value on release.
    // (Local copy: gestureHint is a delegated property, so it cannot be
    // smart-cast after a null check.)
    val hint = gestureHint
    val indicatorText: String? = when {
        feedbackLabel != null -> null
        seekPreviewMs >= 0 && seekDeltaText != null ->
            "$seekDeltaText • ${formatVideoTime(seekPreviewMs)}"
        else -> null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
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
                // Aspect modes (reference-player redesign): FIT fits
                // inside the screen, FILL fills it cropping (aspect
                // kept), ZOOM fills + extra 1.5x (via aspectZoom
                // below), STRETCH stretches to fill, aspect ignored.
                playerView.resizeMode = when (aspectMode) {
                    "FILL" -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    "ZOOM" -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    "STRETCH" -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
                // Subtitle style (reference-player redesign): the
                // user's text size / colour / background.
                playerView.subtitleView?.let { subtitleView ->
                    subtitleView.setApplyEmbeddedStyles(false)
                    subtitleView.setFixedTextSize(
                        TypedValue.COMPLEX_UNIT_SP,
                        subtitleStyle.textSizeSp
                    )
                    subtitleView.setStyle(
                        CaptionStyleCompat(
                            subtitleStyle.textColorArgb,
                            when (subtitleStyle.backgroundMode) {
                                0 -> Color.Transparent.toArgb()
                                2 -> Color.Black.toArgb()
                                else -> Color.Black.copy(alpha = 0.5f).toArgb()
                            },
                            Color.Transparent.toArgb(),
                            CaptionStyleCompat.EDGE_TYPE_NONE,
                            Color.Black.toArgb(),
                            null
                        )
                    )
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Pinch zoom: scale the whole PlayerView (video
                    // surface) around its centre, 1.0x–3.0x — times
                    // the ZOOM aspect mode's extra 1.5x.
                    scaleX = zoomScale * aspectZoom
                    scaleY = zoomScale * aspectZoom
                }
        )

        if (controlsLocked) {
            // Controls locked (modern layer): a dim scrim with a
            // centred lock and a labelled unlock pill — nothing else
            // on screen; the PlayerView controller is off.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = stringResource(R.string.video_controls_locked_label),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        onClick = { controlsLocked = false },
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = stringResource(R.string.video_tap_to_unlock),
                            color = Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                        )
                    }
                }
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
                    .pointerInput(player, swipeGesturesEnabled) {
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
                            // A drag that moved past slop while swipe
                            // gestures are off: not a tap, so the gesture
                            // end must not toggle the bars.
                            var draggedWithGesturesOff = false

                            // Long-press is detected with
                            // withTimeoutOrNull (a member of the
                            // restricted AwaitPointerEventScope, so it
                            // is legal here): coroutineScope/launch/
                            // delay are FORBIDDEN inside
                            // awaitEachGesture — its receiver is a
                            // @RestrictsSuspension scope that may only
                            // suspend for awaitPointerEvent. Each loop
                            // iteration waits up to 400 ms for the next
                            // pointer event; a timeout with no movement
                            // means the finger is being held still, so
                            // speed mode opens. Any movement resets the
                            // 400 ms window.
                            while (true) {
                                val event = withTimeoutOrNull(400) {
                                    awaitPointerEvent()
                                }
                                if (event == null) {
                                    // Held still for 400 ms: long-press.
                                    if (mode == GestureMode.Undecided && !longPressFired) {
                                        mode = GestureMode.Speed
                                        longPressFired = true
                                        speedMode = true
                                        speedBarVisible = true
                                        barsVisible = true
                                        poke()
                                    }
                                    continue
                                }
                                    val pressed = event.changes.filter { it.pressed }
                                    if (pressed.isEmpty()) break
                                    if (pressed.size < 2 && mode == GestureMode.Pinch) {
                                        // One finger lifted mid-pinch: forget
                                        // the span so a fresh pinch re-inits.
                                        pinchLastSpan = 0f
                                    }
                                    if (pressed.size >= 2) {
                                        if (mode != GestureMode.Pinch || pinchLastSpan <= 0f) {
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
                                                // Swipe gestures off (Video
                                                // settings): drags do
                                                // nothing — no seek, no
                                                // brightness/volume. Tap,
                                                // hold-for-speed and pinch
                                                // are unaffected.
                                                if (!swipeGesturesEnabled) {
                                                    draggedWithGesturesOff = true
                                                    change.consume()
                                                    continue
                                                }
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
                                                            ((dx / size.width) * dur * 0.5f).toLong()
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
                                                    ((dx / size.width) * dur * 0.5f).toLong()
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
                                                applySpeed(speedOptions[speedIndex])
                                            }
                                        }
                                        GestureMode.Pinch -> {
                                            // Handled in the two-finger
                                            // branch above.
                                        }
                                    }
                                    change.consume()
                                }
                            // ---- Gesture end ----
                            when (mode) {
                                GestureMode.Undecided -> {
                                    if (!longPressFired && !draggedWithGesturesOff) {
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
                                    val finishedHint = gestureHint
                                    gestureHint = null
                                    if (mode == GestureMode.Volume) {
                                        // Persist the volume once, at the
                                        // end of the swipe.
                                        videoPlayerViewModel.setVolume(volume)
                                    }
                                    // Centre feedback label with the
                                    // final value (the edge slider shows
                                    // it while dragging).
                                    finishedHint?.let { hint ->
                                        showFeedback("${hint.label} ${hint.percent}%")
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
                        }
                    }
            )

            // ---- Top bar (reference-player redesign, 2026-11-16):
            // back, film title (2 lines in portrait, like the
            // reference), music note (audio tracks), subtitle icon
            // (subtitle panel), the HW/SW decoder badge — a real
            // indicator of the decoder in use, not a toggle — and the
            // three-dot menu, which opens the grid menu. All icons
            // white. The background is a soft gradient scrim (modern
            // layer), not a flat black bar. The whole top area
            // auto-hides with the other bars.
            AnimatedVisibility(
                visible = barsVisible,
                enter = fadeIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ),
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
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.65f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 16.dp)
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
                                maxLines = if (isLandscape) 1 else 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            // Audio tracks (dubbing).
                            IconButton(onClick = {
                                showAudioListDialog = true
                                poke()
                            }) {
                                Icon(
                                    imageVector = Icons.Rounded.MusicNote,
                                    contentDescription = stringResource(R.string.video_audio_settings),
                                    tint = Color.White
                                )
                            }
                            // Subtitles: opens the subtitle panel.
                            IconButton(onClick = {
                                showSubtitlePanel = true
                                poke()
                            }) {
                                Icon(
                                    imageVector = Icons.Rounded.ClosedCaption,
                                    contentDescription = stringResource(R.string.video_subtitles),
                                    tint = Color.White
                                )
                            }
                            // Decoder badge: HW or SW, the decoder
                            // actually in use (holder, real detection).
                            Text(
                                text = decoderBadge,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            // Three-dot menu: the grid menu.
                            IconButton(onClick = {
                                showGridMenu = true
                                poke()
                            }) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        }
                    }
                    // Quick-action row (reference-player redesign):
                    // in landscape it sits under the top bar, left
                    // aligned — equalizer, screenshot, audio,
                    // aspect, speed circle, then the arrow. Home
                    // feedback (2026-11-16): while closed the bar is
                    // COMPLETELY hidden — no peeking row, no partial
                    // buttons — only the arrow handle below shows, and
                    // tapping it opens the full row (it scrolls).
                    AnimatedVisibility(
                        visible = controlsExpanded,
                        enter = slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + fadeIn(),
                        exit = slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + fadeOut()
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.Black.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(
                                bottomStart = 28.dp,
                                bottomEnd = 28.dp
                            )
                        ) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(quickRowButtons, key = { it.id }) { button ->
                                    VideoRoundControlButton(
                                        button = button,
                                        isActive = when (button.id) {
                                            "night" -> nightLight
                                            "shuffle" -> shuffleEnabled
                                            "mute" -> isMuted
                                            "background" -> backgroundPlayback
                                            "loop" -> repeatMode != Player.REPEAT_MODE_OFF
                                            "sleep" -> sleepTimerMinutes > 0
                                            "ab" -> aPointMs >= 0
                                            "speed" -> playbackSpeed != 1f
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
                                            "aspect" -> stringResource(R.string.video_aspect_ratio) +
                                                ": " + when (aspectMode) {
                                                    "FILL" -> stringResource(R.string.video_aspect_fill)
                                                    "ZOOM" -> stringResource(R.string.video_aspect_zoom)
                                                    "STRETCH" -> stringResource(R.string.video_aspect_stretch)
                                                    else -> stringResource(R.string.video_aspect_fit)
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
                                        activeColor = if (button.id == "speed") {
                                            videoAccentColor
                                        } else {
                                            null
                                        },
                                        // Sleep timer ring (d3motion
                                        // countdown-timer): real remaining
                                        // fraction while the timer runs.
                                        ringProgress = if (button.id == "sleep" &&
                                            sleepTimerMinutes > 0
                                        ) {
                                            sleepTimerRemainingSeconds.toFloat() /
                                                (sleepTimerMinutes * 60f)
                                        } else {
                                            null
                                        },
                                        // Screenshot state machine
                                        // (d3motion deploy-button): spinner
                                        // while capturing, check on success.
                                        customContent = if (button.id == "screenshot" &&
                                            screenshotState != 0
                                        ) {
                                            {
                                                if (screenshotState == 1) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(20.dp),
                                                        color = Color.White,
                                                        strokeWidth = 2.dp
                                                    )
                                                } else {
                                                    ScreenshotDoneCheck()
                                                }
                                            }
                                        } else {
                                            null
                                        },
                                        onClick = { onControlButtonClick(button) }
                                    )
                                }
                                // Arrow handle, last in the row: points
                                // RIGHT while open (LEFT while closed);
                                // tapping it closes the row. In the row
                                // in both orientations now — the
                                // standalone handle outside only shows
                                // while the row is closed.
                                item(key = "arrow") {
                                    VideoRowArrowButton(
                                        expanded = controlsExpanded,
                                        arrowRotation = arrowRotation,
                                        onClick = {
                                            controlsExpanded = !controlsExpanded
                                            poke()
                                        }
                                    )
                                }
                            }
                            // Speed menu, anchored to the speed round button.
                            DropdownMenu(
                                expanded = showSpeedMenu,
                                onDismissRequest = { showSpeedMenu = false },
                                containerColor = Color.Black.copy(alpha = 0.75f)
                            ) {
                                speedOptions.forEach { speed ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = formatSpeedLabel(speed),
                                                color = Color.White
                                            )
                                        },
                                        onClick = {
                                            applySpeed(speed)
                                            showSpeedMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    // Arrow handle (Home feedback, 2026-11-16):
                    // while the bar is closed this is the ONLY thing
                    // visible of it — a single transparent circle under
                    // the three-dot menu, in both orientations. Tapping
                    // it opens the full row. (It used to be
                    // portrait-only; landscape showed a collapsed row.)
                    if (!controlsExpanded) {
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
                    }
                    // The speed bar: 8 speeds, 0.1x–4x, just below the
                    // arrow. Opens on long-press (and stays 2 s after
                    // release); the current speed is highlighted, and
                    // tapping a chip picks it directly.
                    AnimatedVisibility(
                        visible = speedBarVisible,
                        enter = fadeIn() + slideInVertically { -it / 2 },
                        exit = fadeOut() + slideOutVertically { -it / 2 },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(19.dp),
                            modifier = Modifier
                                .wrapContentWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 8.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                speedOptions.forEach { speed ->
                                    val selected = speed == playbackSpeed
                                    Surface(
                                        onClick = { applySpeed(speed) },
                                        shape = RoundedCornerShape(13.dp),
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            Color.White.copy(alpha = 0.15f)
                                        }
                                    ) {
                                        Text(
                                            text = formatSpeedLabel(speed),
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = if (selected) {
                                                FontWeight.Bold
                                            } else {
                                                FontWeight.Normal
                                            },
                                            modifier = Modifier.padding(
                                                horizontal = 10.dp,
                                                vertical = 5.dp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ---- Bottom: the custom transport bar. It auto-hides
            // with the top bar. ----
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.65f)
                                    )
                                )
                            )
                            .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // R15 F1: plain Text, not RollingTimeText.
                            // Verified: this bar IS fed by the same 250 ms
                            // poll as the mini bar (LaunchedEffect(player)
                            // above), and it has no LTR wrapper, so the
                            // rolling per-character Row could garble here
                            // too — including digit reordering in RTL.
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(
                                    text = formatVideoTime(displayPositionMs),
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                            // Bug-fix (2026-11-17, item 4): the old M3 Slider was the
                            // classic almost-blue bar with a big vertical scrubber
                            // handle. The seek bar is now the same wavy slider the
                            // music Now Playing screen uses — an animated wave while
                            // playing, with a small white dot at the playhead (a
                            // short line only while dragging). Tap jumps straight
                            // to the tapped point, drag scrubs at 0.5x — the same
                            // scrub behaviour as before, only the look changed.
                            WavySliderExpressive(
                                value = {
                                    if (durationMs > 0) {
                                        displayPositionMs.toFloat() / durationMs
                                    } else {
                                        0f
                                    }
                                },
                                onValueChange = { fraction ->
                                    scrubbing = true
                                    val duration = durationMs
                                    if (duration > 0) {
                                        val anchorFraction = scrubAnchorFraction
                                        if (anchorFraction == null) {
                                            // First move of this scrub: a
                                            // big jump is a tap — seek
                                            // straight to the tapped point.
                                            // A small move starts a 0.5x
                                            // relative drag from here.
                                            val currentFraction =
                                                positionMs.toFloat() / duration
                                            if (abs(fraction - currentFraction) > 0.25f) {
                                                scrubAnchorFraction = fraction
                                                scrubAnchorMs =
                                                    (fraction * duration).toLong()
                                                seekPreviewMs =
                                                    (fraction * duration).toLong()
                                            } else {
                                                scrubAnchorFraction = fraction
                                                scrubAnchorMs = positionMs
                                                seekPreviewMs = positionMs
                                            }
                                        } else {
                                            val delta = ((fraction - anchorFraction) *
                                                duration * 0.5f).toLong()
                                            seekPreviewMs = (scrubAnchorMs + delta)
                                                .coerceIn(0L, duration)
                                        }
                                    }
                                },
                                onValueChangeFinished = {
                                    scrubbing = false
                                    scrubAnchorFraction = null
                                    if (seekPreviewMs >= 0) {
                                        videoPlayerViewModel.seekTo(seekPreviewMs)
                                        positionMs = seekPreviewMs
                                        seekPreviewMs = -1L
                                    }
                                    poke()
                                },
                                enabled = durationMs > 0,
                                activeTrackColor = Color(0xFF8FB8FF),
                                inactiveTrackColor = Color.White.copy(alpha = 0.4f),
                                thumbColor = Color.White,
                                isPlaying = isPlaying,
                                thumbRadius = 7.dp,
                                thumbLineHeightWhenInteracting = 16.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            )
                            // R15 F1: plain Text — same reason as the
                            // current-time text above (250 ms poll, RTL).
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(
                                    text = formatVideoTime(durationMs),
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = {
                                    controlsLocked = true
                                    poke()
                                },
                                modifier = Modifier.size((48f * bottomBarScale).dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = stringResource(R.string.video_lock_controls),
                                    tint = Color.White,
                                    modifier = Modifier.size((26f * bottomBarScale).dp)
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(
                                    (4f * bottomBarScale).dp,
                                    Alignment.CenterHorizontally
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                IconButton(
                                    onClick = {
                                        videoPlayerViewModel.seekToPrevious()
                                        poke()
                                    },
                                    modifier = Modifier.size((48f * bottomBarScale).dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SkipPrevious,
                                        contentDescription = stringResource(R.string.video_previous_video),
                                        tint = Color.White,
                                        modifier = Modifier.size((30f * bottomBarScale).dp)
                                    )
                                }
                                VideoSkip15Button(
                                    label = "-15s",
                                    contentDescription = stringResource(R.string.video_seek_back_15),
                                    onClick = {
                                        videoPlayerViewModel.seekBy(-15_000L)
                                        poke()
                                    },
                                    size = (40f * bottomBarScale).dp,
                                    fontSize = (12f * bottomBarScale).sp
                                )
                                IconButton(
                                    onClick = {
                                        videoPlayerViewModel.togglePlayPause()
                                        poke()
                                    },
                                    modifier = Modifier.size((56f * bottomBarScale).dp)
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
                                        modifier = Modifier.size((36f * bottomBarScale).dp)
                                    )
                                }
                                VideoSkip15Button(
                                    label = "+15s",
                                    contentDescription = stringResource(R.string.video_seek_forward_15),
                                    onClick = {
                                        videoPlayerViewModel.seekBy(15_000L)
                                        poke()
                                    },
                                    size = (40f * bottomBarScale).dp,
                                    fontSize = (12f * bottomBarScale).sp
                                )
                                IconButton(
                                    onClick = {
                                        videoPlayerViewModel.seekToNext()
                                        poke()
                                    },
                                    modifier = Modifier.size((48f * bottomBarScale).dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SkipNext,
                                        contentDescription = stringResource(R.string.video_next_video),
                                        tint = Color.White,
                                        modifier = Modifier.size((30f * bottomBarScale).dp)
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(
                                    (4f * bottomBarScale).dp
                                )
                            ) {
                                // Screen rotation lives here now (Search +
                                // Player polish, 2026-11-16): the fixed
                                // right-edge button is gone. It was removed
                                // from the control bar in the mini-player
                                // feedback (2026-11-16), so this is the
                                // only rotation toggle left.
                                IconButton(
                                    onClick = {
                                        forcedLandscape =
                                            if (forcedLandscape == true) false else true
                                        poke()
                                    },
                                    modifier = Modifier.size((48f * bottomBarScale).dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ScreenRotation,
                                        contentDescription = stringResource(R.string.video_screen_rotation),
                                        // White like every other player icon
                                        // (mini-player feedback, 2026-11-16).
                                        tint = Color.White,
                                        modifier = Modifier.size((28f * bottomBarScale).dp)
                                    )
                                }
                                IconButton(
                                    onClick = { toggleFullscreenMode() },
                                    modifier = Modifier.size((48f * bottomBarScale).dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFullscreen) {
                                            Icons.Rounded.FullscreenExit
                                        } else {
                                            Icons.Rounded.Fullscreen
                                        },
                                        contentDescription = stringResource(R.string.video_fullscreen),
                                        tint = Color.White,
                                        modifier = Modifier.size((28f * bottomBarScale).dp)
                                    )
                                }
                                IconButton(
                                    onClick = { enterPictureInPicture() },
                                    modifier = Modifier.size((48f * bottomBarScale).dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PictureInPictureAlt,
                                        contentDescription = stringResource(R.string.video_picture_in_picture),
                                        tint = Color.White,
                                        modifier = Modifier.size((28f * bottomBarScale).dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Centre hint for gesture feedback: seek delta + target time.
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
            // Centre feedback label (reference-player redesign): a big
            // white label ("STRETCH", "1.5×", "Volume 80%") that
            // fades + scales out after ~1 s.
            AnimatedVisibility(
                visible = feedbackLabel != null,
                enter = fadeIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + scaleIn(
                    initialScale = 0.85f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                feedbackLabel?.let { text ->
                    Text(
                        text = text,
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.displaySmall.copy(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.6f),
                                blurRadius = 12f
                            )
                        )
                    )
                }
            }
            // Volume / brightness gesture: a vertical edge slider with
            // % and icon (reference portrait frame) — left edge for
            // brightness, right edge for volume.
            hint?.let { edgeHint ->
                val isBrightnessHint = edgeHint.label == brightnessLabel
                VideoEdgeSlider(
                    label = edgeHint.label,
                    percent = edgeHint.percent,
                    isBrightness = isBrightnessHint,
                    modifier = Modifier
                        .align(
                            if (isBrightnessHint) Alignment.CenterStart else Alignment.CenterEnd
                        )
                        .padding(horizontal = 16.dp)
                )
            }
            // Buffering: our own spinner in a translucent pill (the
            // built-in controller is off) — hidden while a hint or a
            // feedback label is on screen.
            if (isLoading && indicatorText == null && feedbackLabel == null) {
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = stringResource(R.string.video_loading),
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
        // Subtitle panel (landscape): a translucent side panel on the
        // right, like the reference. Portrait uses a bottom sheet
        // (below, with the other dialogs).
        if (showSubtitlePanel && isLandscape) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxWidth(0.42f)
                    .fillMaxHeight()
                    .background(Color.Black.copy(alpha = 0.82f))
                    .statusBarsPadding()
            ) {
                VideoSubtitlePanelContent(
                    subtitleTrackLabels = subtitleTrackLabels,
                    selectedSubtitleTrack = selectedSubtitleTrack,
                    onSelectTrack = {
                        videoPlayerViewModel.selectSubtitleTrack(it)
                        poke()
                    },
                    onOpenFile = { subtitlePicker.launch(arrayOf("*/*")) },
                    subtitleStyle = subtitleStyle,
                    onTextSize = { videoPlayerViewModel.setSubtitleTextSize(it) },
                    onTextColor = { videoPlayerViewModel.setSubtitleTextColor(it) },
                    onBackground = { videoPlayerViewModel.setSubtitleBackground(it) },
                    onClose = { showSubtitlePanel = false }
                )
            }
        }
    }

    // ---- Reference-player redesign: grid menu + subtitle panel
    // (portrait) + the grid's dialogs. ----
    if (showGridMenu) {
        VideoGridMenuSheet(
            isFavourite = isFavourite,
            onDismiss = { showGridMenu = false },
            onAction = { action ->
                when (action) {
                    VideoGridAction.AUDIO -> showAudioListDialog = true
                    VideoGridAction.SUBTITLE -> showSubtitlePanel = true
                    VideoGridAction.QUEUE -> showQueueDialog = true
                    VideoGridAction.ASPECT -> cycleAspect()
                    VideoGridAction.DISPLAY -> showDisplayDialog = true
                    VideoGridAction.BOOKMARK -> showBookmarksDialog = true
                    VideoGridAction.CHAPTER -> showChaptersDialog = true
                    VideoGridAction.FAVOURITE -> videoPlayerViewModel.toggleFavourite()
                    VideoGridAction.PLAYLIST -> videoPlayerViewModel.addCurrentVideoToPlaylist()
                    VideoGridAction.INFO -> showInfoDialog = true
                    VideoGridAction.SHARE -> shareVideo()
                    VideoGridAction.TUTORIAL -> showTutorialDialog = true
                    VideoGridAction.MORE -> {
                        controlsExpanded = true
                        poke()
                    }
                }
            }
        )
    }
    if (showSubtitlePanel && !isLandscape) {
        VideoSubtitlePanelSheet(
            subtitleTrackLabels = subtitleTrackLabels,
            selectedSubtitleTrack = selectedSubtitleTrack,
            onSelectTrack = {
                videoPlayerViewModel.selectSubtitleTrack(it)
                poke()
            },
            onOpenFile = { subtitlePicker.launch(arrayOf("*/*")) },
            subtitleStyle = subtitleStyle,
            onTextSize = { videoPlayerViewModel.setSubtitleTextSize(it) },
            onTextColor = { videoPlayerViewModel.setSubtitleTextColor(it) },
            onBackground = { videoPlayerViewModel.setSubtitleBackground(it) },
            onDismiss = { showSubtitlePanel = false }
        )
    }
    if (showAudioListDialog) {
        VideoAudioTrackDialog(
            audioTrackLabels = audioTrackLabels,
            selectedAudioTrack = selectedAudioTrack,
            onSelectTrack = { videoPlayerViewModel.selectAudioTrack(it) },
            onDismiss = { showAudioListDialog = false }
        )
    }
    if (showQueueDialog) {
        VideoQueueDialog(
            queueItems = queueItems,
            currentIndex = player?.currentMediaItemIndex ?: 0,
            onSelect = { videoPlayerViewModel.playQueueIndex(it) },
            onDismiss = { showQueueDialog = false }
        )
    }
    if (showBookmarksDialog) {
        VideoBookmarksDialog(
            bookmarks = bookmarks,
            onAdd = { videoPlayerViewModel.addBookmark() },
            onJump = { videoPlayerViewModel.seekTo(it) },
            onDelete = { videoPlayerViewModel.deleteBookmark(it) },
            onDismiss = { showBookmarksDialog = false }
        )
    }
    if (showChaptersDialog) {
        VideoChaptersDialog(onDismiss = { showChaptersDialog = false })
    }
    if (showInfoDialog) {
        VideoInfoDialog(
            video = currentVideo,
            decoderBadge = decoderBadge,
            onDismiss = { showInfoDialog = false }
        )
    }
    if (showTutorialDialog) {
        VideoTutorialDialog(onDismiss = { showTutorialDialog = false })
    }
    if (showDisplayDialog) {
        VideoDisplaySettingsDialog(
            nightLight = nightLight,
            brightness = brightness,
            onNightLight = { videoPlayerViewModel.setNightLight(it) },
            onBrightness = { videoPlayerViewModel.setBrightness(it) },
            onDismiss = { showDisplayDialog = false }
        )
    }

    // ---- Audio Effect dialog: volume + audio track + night light.
    // Transparent like the rest of the player (Search + Player polish,
    // 2026-11-16): no solid surface.
    if (showAudioDialog) {
        AlertDialog(
            onDismissRequest = { showAudioDialog = false },
            containerColor = Color.Black.copy(alpha = 0.85f),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text(text = stringResource(R.string.video_audio_effect)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.VolumeUp,
                            contentDescription = null,
                            tint = Color.White
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
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    if (audioTrackLabels.size <= 1) {
                        Text(
                            text = stringResource(R.string.video_audio_track_single),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.NightsStay,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.video_night_light),
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
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
            containerColor = Color.Black.copy(alpha = 0.85f),
            titleContentColor = Color.White,
            textContentColor = Color.White,
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
                                },
                                colors = androidx.compose.material3.CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = Color.White.copy(alpha = 0.6f),
                                    checkmarkColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                            Text(
                                text = stringResource(button.labelRes),
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White
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

/** One round control button: dark circle + icon, small label underneath.
 * Compact (Search + Player polish, 2026-11-16): 46 dp circles, 22 dp
 * icons, 10 sp labels, so more buttons fit on screen at once. */
@Composable
private fun VideoRoundControlButton(
    button: VideoControlButton,
    isActive: Boolean,
    labelOverride: String?,
    circleText: String? = null,
    iconOverride: ImageVector? = null,
    activeColor: Color? = null,
    // Sleep-timer radial ring (d3motion countdown-timer): remaining
    // fraction 0..1 while the timer runs, null otherwise.
    ringProgress: Float? = null,
    // Replaces the icon (e.g. the screenshot spinner / checkmark).
    customContent: (@Composable BoxScope.() -> Unit)? = null,
    onClick: () -> Unit
) {
    // Press-scale micro-feedback (modern layer): the button shrinks
    // a little while pressed, with a spring back.
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "roundButtonPress"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .width(56.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
    ) {
        // Ring + circle: the ring is drawn around the 46 dp circle.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(52.dp)
        ) {
            if (ringProgress != null) {
                Canvas(modifier = Modifier.size(52.dp)) {
                    drawArc(
                        color = videoAccentColor,
                        startAngle = -90f,
                        sweepAngle = 360f * ringProgress.coerceIn(0f, 1f),
                        useCenter = false,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
            Surface(
                onClick = onClick,
                interactionSource = interactionSource,
                shape = CircleShape,
                color = if (isActive) {
                    activeColor ?: MaterialTheme.colorScheme.primary
                } else {
                    Color.Black.copy(alpha = 0.45f)
                },
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (customContent != null) {
                        customContent()
                    } else if (circleText != null) {
                        Text(
                            text = circleText,
                            color = if (isActive && activeColor == null) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                Color.White
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    } else if (button.id == "night") {
                        // Night-mode sun/moon morph (d3motion
                        // theme-toggle): the moon fades and scales into
                        // the sun when night light turns on, and back.
                        AnimatedContent(
                            targetState = isActive,
                            transitionSpec = {
                                (fadeIn() + scaleIn(initialScale = 0.6f)) togetherWith
                                    (fadeOut() + scaleOut(targetScale = 0.6f))
                            },
                            label = "nightMorph"
                        ) { on ->
                            Icon(
                                imageVector = if (on) {
                                    Icons.Rounded.WbSunny
                                } else {
                                    Icons.Rounded.NightsStay
                                },
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = iconOverride ?: button.icon,
                            contentDescription = null,
                            tint = if (isActive && activeColor == null) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                Color.White
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
        Text(
            text = labelOverride ?: stringResource(button.labelRes),
            color = Color.White,
            fontSize = 10.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/** The screenshot "done" checkmark, popping in with the d3motion spring. */
@Composable
private fun ScreenshotDoneCheck() {
    val scale = remember { Animatable(0.5f) }
    LaunchedEffect(Unit) {
        scale.animateTo(
            1f,
            animationSpec = spring(
                dampingRatio = 0.55f,
                stiffness = 300f
            )
        )
    }
    Icon(
        imageVector = Icons.Rounded.Check,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier
            .size(22.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
    )
}

/** A small round ±15 s skip button of the bottom transport bar. */
@Composable
private fun VideoSkip15Button(
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
    size: Dp = 40.dp,
    fontSize: TextUnit = 12.sp
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.15f),
        modifier = Modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = Color.White,
                fontSize = fontSize,
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
    VideoControlButton("audiofx", Icons.Rounded.Headphones, R.string.video_audio_effect),
    VideoControlButton("equalizer", Icons.Rounded.Equalizer, R.string.video_equalizer),
    VideoControlButton("screenshot", Icons.Rounded.PhotoCamera, R.string.video_screenshot),
    VideoControlButton("aspect", Icons.Rounded.AspectRatio, R.string.video_aspect_ratio),
    VideoControlButton("background", Icons.Rounded.Headphones, R.string.video_background_play),
    VideoControlButton("speed", Icons.Rounded.Speed, R.string.video_speed)
)

/** The arrow at the end of the quick row: a round button like the others. */
@Composable
private fun VideoRowArrowButton(
    expanded: Boolean,
    arrowRotation: Float,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.width(56.dp)
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.45f),
            modifier = Modifier.size(46.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(arrowRotation)
                )
            }
        }
        Text(
            text = stringResource(
                if (expanded) {
                    R.string.video_player_hide_controls
                } else {
                    R.string.video_player_more_controls
                }
            ),
            color = Color.White,
            fontSize = 10.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

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

/** Save a captured frame under Pictures/Solina Player. Returns true on success. */
private fun saveScreenshotBitmap(context: android.content.Context, bitmap: Bitmap): Boolean {
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
    return saved
}

/**
 * Fallback frame grab for surfaces PixelCopy cannot read: pull the frame
 * at the current position straight from the file with MediaMetadataRetriever.
 */
private fun captureFrameWithRetriever(
    context: android.content.Context,
    videoUri: String,
    positionMs: Long,
    onDone: (Boolean) -> Unit
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
        onDone(saveScreenshotBitmap(context, bitmap))
    } else {
        Toast.makeText(
            context,
            context.getString(R.string.video_screenshot_failed),
            Toast.LENGTH_SHORT
        ).show()
        onDone(false)
    }
}
