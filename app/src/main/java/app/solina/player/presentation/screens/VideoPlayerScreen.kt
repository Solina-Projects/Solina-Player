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
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
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
import androidx.compose.material.icons.rounded.StayCurrentLandscape
import androidx.compose.material.icons.rounded.StayCurrentPortrait
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
import androidx.compose.ui.platform.LocalView
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
import app.solina.player.data.preferences.SeekBarStyleNames
import app.solina.player.presentation.utils.ScreenAwakeCoordinator
import app.solina.player.presentation.viewmodel.AppearanceViewModel
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
import kotlin.math.pow
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
 *   like the reference: seek bar with times, then prev -15s
 *   play/pause +15s next in the centre, aspect / rotation / PiP on the
 *   right (landscape: lock far left, rotation 2nd from the right;
 *   portrait: aspect + PiP only — fullscreen is gone, round 18).
 * - One gesture layer over the video surface, with clear thresholds, so
 *   tap / long-press / swipe-seek / brightness / volume / pinch do not
 *   fight each other: short tap toggles the bars (deferred ~350 ms so a
 *   double-tap can seek instead); double-tap left / right seeks ∓10 s,
 *   centre resets the pinch zoom to 1.0x; holding ~400 ms opens a speed
 *   bar just below the arrow (8 speeds, 0.1x–4x, slide while holding to
 *   pick — momentary, the pre-hold speed returns on release, unless a
 *   chip was tapped); horizontal drag seeks at 0.25x (the delta shows on
 *   screen, applied on release); vertical drag on the left half sets
 *   brightness, on the right half volume; two fingers moving apart /
 *   together pinch-zooms the video (graphicsLayer scale on the
 *   PlayerView, 0.4x–3.0x, live % label while pinching), two fingers
 *   moving together vertically set the speed (exponential, 0.25x–4x).
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
 *   entry, so the tap looked dead. Entering fullscreen forces landscape
 *   (the video grows) and exiting goes back to portrait, with a toast.
 *   The system bars no longer follow the fullscreen toggle (round 17):
 *   they follow the Compose bars instead — hidden with the 3 s
 *   auto-hide while playing, shown again on tap.
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
    videoPlayerViewModel: VideoPlayerViewModel = hiltViewModel(),
    // Round 15, wave 2, C4: seek-bar style (wavy / classic).
    appearanceViewModel: AppearanceViewModel = hiltViewModel()
) {
    val appearance by appearanceViewModel.appearance.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
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
    // Round 18: a finger on the screen gates the auto-hide timer.
    // touchActive is true from the surface gesture's first pointer-down
    // until the gesture ends, and during a row/handle drag; the timer
    // only counts down once every finger is off the screen, so a long
    // drag or hold can never hide the bars mid-gesture.
    // Relative scrubbing (Search + Player polish, 2026-11-16): dragging
    // the bottom time bar moves the position at 0.5x — half the finger
    // travel — so scrubbing feels slower and finer. A tap (one big jump)
    // still seeks straight to the tapped point. The horizontal-swipe seek
    // on the video surface is 0.25x (round 17): the same finger travel
    // moves a quarter as far in time — a full-width swipe on a 2 h film
    // moves ~30 min instead of ~1 h. The seek-bar slider drag stays 0.5x.
    var scrubAnchorFraction by remember { mutableStateOf<Float?>(null) }
    var scrubAnchorMs by remember { mutableLongStateOf(0L) }
    // Pinch zoom: graphicsLayer scale on the PlayerView, 0.4x–3.0x.
    var zoomScale by remember { mutableStateOf(1f) }
    // Long-press speed mode: the speed bar just below the arrow.
    var speedMode by remember { mutableStateOf(false) }
    var speedBarVisible by remember { mutableStateOf(false) }
    // Momentary hold-to-boost (round 17, MX model): the speed from
    // before the hold, restored on release; a speed-bar chip tapped
    // while holding pins the speed permanently instead.
    var preHoldSpeed by remember { mutableStateOf(1f) }
    var holdSpeedPinned by remember { mutableStateOf(false) }
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
    // Double-tap seek (round 17): a single tap's bar toggle is deferred
    // ~350 ms — the double-tap window — so a second tap can cancel it
    // and seek instead; otherwise the bars would flicker on every
    // double-tap seek.
    var pendingSingleTap by remember { mutableStateOf(false) }
    LaunchedEffect(pendingSingleTap) {
        if (pendingSingleTap) {
            delay(350)
            if (pendingSingleTap) {
                pendingSingleTap = false
                barsVisible = !barsVisible
                poke()
            }
        }
    }

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
    // Rotation, 3-state cycle (round 17): Landscape → Portrait → Auto
    // (follow the sensor, forcedLandscape = null → UNSPECIFIED) → back to
    // Landscape. The big centre label says which mode is now active.
    fun cycleRotation() {
        forcedLandscape = when (forcedLandscape) {
            true -> false
            false -> null
            else -> true
        }
        showFeedback(
            context.getString(
                when (forcedLandscape) {
                    true -> R.string.video_rotation_landscape
                    false -> R.string.video_rotation_portrait
                    else -> R.string.video_rotation_auto
                }
            )
        )
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
    // Bottom-bar sizing, orientation-aware (round 17): portrait on a
    // narrow phone stays compact at 0.8x, but landscape grows to 1.344x
    // (round 17: 1.12x, +40% vs the old 0.8x; round 18: 1.12 x 1.2)
    // with wide 12.8 dp gaps, so the transport row uses the wide
    // screen instead of huddling in the middle.
    // Round 18: landscape icons 20% bigger again — 1.12 x 1.2 = 1.344x
    // (buttons 48 -> 64.5 dp, play 56 -> 75.3 dp, +/-15s 40 -> 53.8 dp,
    // icons 30/36/28/26 -> 40.3/48.4/37.6/34.9 dp). bottomBarScale is
    // used ONLY by the bottom bar, so changing it here is safe.
    // Width check (POCO F3): portrait centre 185.6 + 4x6 = 209.6, right
    // 76.8 + 6 = 82.8, total ~293 + 16 padding ~309 dp < 393 dp;
    // landscape lock 64.5 + centre 311.8 + 4x12.8 = 363 + right
    // 193.6 + 2x12.8 = 219.2, total ~647 + 16 ~663 dp < 873 dp.
    val configuration = LocalConfiguration.current
    val bottomBarScale = when {
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE -> 1.344f
        configuration.screenWidthDp < 470 -> 0.8f
        else -> 1f
    }

    // Reference-player redesign (2026-11-16): landscape gets the
    // quick-action row under the top bar; portrait letterboxes the
    // video and hides the row until the arrow opens it.
    val isLandscape =
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    // Bottom-bar gaps (round 17): 6 dp in portrait (up from 3.2 dp now
    // that rotation + lock left the row), 12.8 dp in landscape.
    val bottomBarGap = if (isLandscape) 12.8.dp else 6.dp
    // Rotation state, shown wherever the rotation control lives: a
    // phone icon for the locked orientation, the rotating-arrows icon
    // for Auto (sensor follow).
    val rotationIcon = when (forcedLandscape) {
        true -> Icons.Rounded.StayCurrentLandscape
        false -> Icons.Rounded.StayCurrentPortrait
        else -> Icons.Rounded.ScreenRotation
    }
    val rotationLabel = stringResource(
        when (forcedLandscape) {
            true -> R.string.video_rotation_landscape
            false -> R.string.video_rotation_portrait
            else -> R.string.video_rotation_auto
        }
    )
    // ZOOM aspect mode adds a 1.5x zoom on top of the pinch zoom.
    val aspectZoom = if (aspectMode == "ZOOM") 1.5f else 1f

    // Pixels per speed step while sliding in long-press speed mode.
    // (Computed here with LocalDensity: inside pointerInput, `density`
    // resolves to Density.density, a Float, so toPx() would not resolve.)
    val speedStepPx = with(LocalDensity.current) { 60.dp.toPx() }

    // Quick-row drag (round 17): a horizontal drag / fling on the arrow
    // handle — and on the row itself — opens / closes the row. Drag
    // left pulls the row out (open), drag right pushes it away (close):
    // the same direction the arrow points. A short drag past 28 dp or a
    // fling faster than 500 dp/s settles it; anything less snaps back
    // (no state change). The handle and the row share one accumulator —
    // only one finger can drag at a time.
    var rowDragDx by remember { mutableStateOf(0f) }
    val rowDragDistancePx = with(LocalDensity.current) { 28.dp.toPx() }
    val rowDragFlingPx = with(LocalDensity.current) { 500.dp.toPx() }
    fun settleRowDrag(velocity: Float) {
        val open = rowDragDx < -rowDragDistancePx || velocity < -rowDragFlingPx
        val close = rowDragDx > rowDragDistancePx || velocity > rowDragFlingPx
        if (open) {
            controlsExpanded = true
            barsVisible = true
            poke()
        } else if (close) {
            controlsExpanded = false
            poke()
        }
        rowDragDx = 0f
    }
    val handleDragState = rememberDraggableState { delta -> rowDragDx += delta }
    val rowDragState = rememberDraggableState { delta -> rowDragDx += delta }

    // Position / duration / playing state, polled for the custom bottom
    // bar (the built-in PlayerView controller is off now).
    LaunchedEffect(player) {
        while (currentCoroutineContext().isActive) {
            val p = player
            if (p != null) {
                if (!scrubbing) positionMs = p.currentPosition
                // Round 16, item 1b: the total time prefers the playing
                // video's MediaStore duration over the polled ExoPlayer
                // duration — p.duration has been observed wrong (e.g.
                // 24 s for a 2-hour film), which shrank the whole seek
                // bar. Same fallback the mini bar already uses. Falls
                // back to the polled duration while the lookup hasn't
                // landed yet (currentVideo null) or the item has none.
                // Applied here, at the durationMs source, so every
                // consumer (seek bar labels, fractions, rolling times)
                // gets the corrected total.
                durationMs = currentVideo?.durationMs?.takeIf { it > 0 }
                    ?: p.duration.coerceAtLeast(0L)
                isPlaying = p.isPlaying
            }
            delay(250)
        }
    }

    // Auto-hide: hide all bars 3 s after the last interaction, only while
    // playing, and never while a menu/dialog or the speed bar is open.
    // Round 18: also never while a finger is still on the screen
    // (touchActive: surface gesture from first pointer-down to gesture
    // end, or a row/handle drag) or while a seek scrub is in progress
    // (scrubbing: surface seek drag or the seek-bar slider). Both flags
    // are keys of the effect, so a new touch/scrub cancels a running
    // countdown and the 3 s only start once the last finger lifts.
    // Round 17: it also collapses the quick row (controlsExpanded =
    // false), so re-showing the bars always starts collapsed and the
    // user reopens the row via the always-visible handle. Round 18:
    // the row collapses in landscape too — its visibility condition
    // is just `controlsExpanded` in both orientations.
    val anyMenuOpen = showSpeedMenu || showGridMenu || showSubtitlePanel ||
        showAudioListDialog || showQueueDialog || showBookmarksDialog ||
        showChaptersDialog || showInfoDialog || showTutorialDialog ||
        showDisplayDialog || showAudioDialog || showCustomiseDialog
    LaunchedEffect(barsVisible, interactionTick, isPlaying, anyMenuOpen, speedMode, speedBarVisible, touchActive, scrubbing) {
        if (barsVisible && isPlaying && !anyMenuOpen && !speedMode && !speedBarVisible && !touchActive && !scrubbing) {
            delay(3_000)
            barsVisible = false
            controlsExpanded = false
        }
    }

    // Round 17, item 1: keep the screen on ONLY while the video is
    // actually playing. Ref-counted through ScreenAwakeCoordinator —
    // the same mechanism the music side's LyricsSheet uses — so
    // pausing releases it, and leaving the screen releases it too
    // (onDispose below + the reset() in the Unit onDispose further
    // down). No new setting.
    DisposableEffect(isPlaying) {
        ScreenAwakeCoordinator.request("VideoPlayer", isPlaying, view)
        onDispose { ScreenAwakeCoordinator.request("VideoPlayer", false, view) }
    }

    // Speed bar: the hold path hides it at once on release (round 17,
    // momentary hold-to-boost); this 2 s linger is only a safety net
    // for a bar left open when a gesture ends abnormally (e.g. a second
    // finger cancelling the hold into a pinch).
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
    // every landscape request immediately. The rotation control cycles
    // three states (round 17) — Landscape, Portrait, Auto (null, sensor
    // follow) — so all three branches below are reachable from the UI.
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
            // Round 17, item 1: release this screen's keep-awake request
            // (the isPlaying effect above already releases on pause and
            // on dispose; this is the belt-and-braces release on leave).
            ScreenAwakeCoordinator.reset(view)
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

    // Round 17, item 2: the system bars follow the Compose bars, not the
    // fullscreen toggle. The player auto-forces landscape on entry
    // (forcedLandscape) without touching isFullscreen, so the
    // fullscreen-only hiding above left the status bar + gesture line
    // visible on every normal entry. Now: bars hidden (3 s auto-hide
    // while playing) -> system bars hidden; tap to show the bars ->
    // system bars shown again so they stay reachable. A swipe still
    // reveals them transiently (BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE,
    // set here on every hide so it also applies on the auto-hide path,
    // where fullscreen was never toggled). Keyed on barsVisible AND
    // isFullscreen, so it re-asserts the bars rule whenever either
    // changes. (Round 18: the fullscreen button + toggle are gone —
    // the user does not need fullscreen — but isFullscreen and this
    // effect stay: the orientation effect above still keys on it.)
    // (The old fullscreen-only effect was removed
    // in the round-17 merge — it fought this one on fullscreen exit.)
    LaunchedEffect(barsVisible, isFullscreen) {
        activity?.window?.let { window ->
            val controller = window.insetsController
            if (barsVisible) {
                controller?.show(WindowInsets.Type.systemBars())
            } else {
                controller?.hide(WindowInsets.Type.systemBars())
                controller?.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
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

    // The arrow handle's icon (round 17): no rotation — the icon itself
    // swaps between KeyboardArrowLeft (closed) and KeyboardArrowRight
    // (open) with a small slide + crossfade (VideoControlsArrowIcon).
    // Rotating an up-chevron -90° ↔ +90° passed through 0° = pointing
    // straight up halfway, which looked broken.

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
                    // surface) around its centre, 0.4x–3.0x — times
                    // the ZOOM aspect mode's extra 1.5x. The % label
                    // shows the pinch value (zoomScale) only, not the
                    // combined scale.
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
            // / volume / pinch / two-finger speed cannot fight each other:
            //  * short tap (no move, < 400 ms) → toggle all bars, deferred
            //    ~350 ms so a double-tap can seek instead.
            //  * double-tap left / right third → seek ∓10 s (chevron +
            //    target time in the centre label); centre → reset the
            //    pinch zoom to 1.0x. Works even with swipe gestures off.
            //  * hold > ~400 ms without moving → speed mode: the speed bar
            //    opens just below the arrow; sliding the finger
            //    forward/back while holding steps through the 8 speeds.
            //    Momentary (round 17, MX model): on release the speed
            //    from before the hold returns and the bar hides at once —
            //    unless a speed-bar chip was tapped, which pins it.
            //  * horizontal drag → seek at 0.25x (delta shown, applied on
            //    release).
            //  * vertical drag, left half → brightness; right half → volume.
            //  * second finger down → undecided until one motion wins the
            //    slop: span change locks pinch zoom (0.4x–3.0x, live %
            //    label), joint vertical travel locks two-finger speed
            //    (exponential, 0.25x–4x, live "1.23×" label, persisted
            //    once on release). Fingers apart/together = zoom, fingers
            //    together vertically = speed.
            // Every pointer change is consumed, so the PlayerView never
            // sees these touches.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(player, swipeGesturesEnabled) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            down.consume()
                            // Round 18: the finger is down — cancel any
                            // running auto-hide countdown until the
                            // gesture ends (see the trailing poke()).
                            touchActive = true
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
                            // Read the live speed from the flow, not the
                            // composable capture: pointerInput only
                            // restarts on (player, swipeGesturesEnabled),
                            // so the captured playbackSpeed can be stale
                            // after a menu / chip speed change — and the
                            // hold must start from the current speed.
                            var speedIndex = speedOptions
                                .indexOfFirst {
                                    it == videoPlayerViewModel.playbackSpeed.value
                                }
                                .coerceAtLeast(0)
                            var pinchLastSpan = 0f
                            // Two-finger decision (round 17): the second
                            // finger does NOT commit to pinch at once —
                            // span change vs joint vertical travel are
                            // tracked until one passes the slop and locks
                            // the mode for the rest of the gesture.
                            var twoFingerStartSpan = 0f
                            var twoFingerStartMidY = 0f
                            var twoFingerStartSpeed = 1f
                            var twoFingerSpeed = 1f
                            // Two-finger speed rate (round 17): relative /
                            // exponential — one full screen-height of joint
                            // vertical travel doubles (up) or halves (down)
                            // the speed, clamped 0.25x–4x and rounded to 2
                            // decimals so fine values like 1.23x fall out.
                            fun twoFingerSpeedFor(midY: Float, height: Float): Float {
                                val ratio = 2.0.pow(
                                    (-(midY - twoFingerStartMidY) / height).toDouble()
                                ).toFloat()
                                val raw = (twoFingerStartSpeed * ratio)
                                    .coerceIn(0.25f, 4f)
                                return (raw * 100).roundToInt() / 100f
                            }
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
                                        // Momentary hold (round 17): the
                                        // pre-hold speed returns on
                                        // release. Read it live from the
                                        // flow — the composable capture
                                        // can be stale (see speedIndex).
                                        preHoldSpeed =
                                            videoPlayerViewModel.playbackSpeed.value
                                        holdSpeedPinned = false
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
                                        val p0 = pressed[0].position
                                        val p1 = pressed[1].position
                                        val span = (p0 - p1).getDistance()
                                        val midY = (p0.y + p1.y) / 2f
                                        when (mode) {
                                            GestureMode.Pinch -> {
                                                if (pinchLastSpan > 0f && span > 0f) {
                                                    zoomScale =
                                                        (zoomScale * span / pinchLastSpan)
                                                            .coerceIn(0.4f, 3f)
                                                    // Live zoom % (round
                                                    // 17, item 12): the pinch
                                                    // value only, not the
                                                    // combined scale —
                                                    // fades out on release
                                                    // via showFeedback's
                                                    // own 1.1 s timer.
                                                    showFeedback(
                                                        "${(zoomScale * 100).roundToInt()}%"
                                                    )
                                                }
                                                pinchLastSpan = span
                                            }
                                            GestureMode.TwoFingerSpeed -> {
                                                twoFingerSpeed = twoFingerSpeedFor(
                                                    midY,
                                                    size.height.toFloat()
                                                )
                                                // Live only: no DataStore
                                                // write per move; persisted
                                                // once at gesture end.
                                                videoPlayerViewModel.setSpeedLive(
                                                    twoFingerSpeed
                                                )
                                                showFeedback(
                                                    formatSpeedLabel(twoFingerSpeed)
                                                )
                                            }
                                            GestureMode.TwoFingerUndecided -> {
                                                // Lock-in: whichever motion
                                                // passes the slop first wins
                                                // the rest of the gesture.
                                                val dSpan =
                                                    abs(span - twoFingerStartSpan)
                                                val dMidY =
                                                    abs(midY - twoFingerStartMidY)
                                                if (dSpan > slopPx && dSpan >= dMidY) {
                                                    mode = GestureMode.Pinch
                                                    pinchLastSpan = span
                                                } else if (dMidY > slopPx && dMidY > dSpan) {
                                                    // Two-finger speed is
                                                    // gated behind the
                                                    // swipe-gestures
                                                    // setting like the other
                                                    // drags; pinch is not.
                                                    // Gated off: stay
                                                    // undecided, so a later
                                                    // span-dominant move can
                                                    // still lock pinch.
                                                    if (swipeGesturesEnabled) {
                                                        mode = GestureMode.TwoFingerSpeed
                                                        twoFingerSpeed = twoFingerSpeedFor(
                                                            midY,
                                                            size.height.toFloat()
                                                        )
                                                        videoPlayerViewModel.setSpeedLive(
                                                            twoFingerSpeed
                                                        )
                                                        showFeedback(
                                                            formatSpeedLabel(twoFingerSpeed)
                                                        )
                                                    }
                                                }
                                            }
                                            else -> {
                                                // Second finger down: the
                                                // single-finger drag is
                                                // abandoned — clear its
                                                // preview so no stale hint
                                                // stays on screen — and a
                                                // momentary hold-to-boost is
                                                // cancelled WITHOUT
                                                // restoring its speed: the
                                                // two-finger gesture owns
                                                // the speed now.
                                                if (mode == GestureMode.Speed) {
                                                    speedMode = false
                                                }
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
                                                mode = GestureMode.TwoFingerUndecided
                                                twoFingerStartSpan = span
                                                twoFingerStartMidY = midY
                                                twoFingerStartSpeed =
                                                    videoPlayerViewModel.playbackSpeed.value
                                                twoFingerSpeed = twoFingerStartSpeed
                                                pinchLastSpan = 0f
                                            }
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
                                                            ((dx / size.width) * dur * 0.25f).toLong()
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
                                                    ((dx / size.width) * dur * 0.25f).toLong()
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
                                        GestureMode.TwoFingerSpeed,
                                        GestureMode.TwoFingerUndecided -> {
                                            // Handled in the two-finger
                                            // branch above; a lone remaining
                                            // finger does nothing until the
                                            // gesture ends.
                                        }
                                    }
                                    change.consume()
                                }
                            // ---- Gesture end ----
                            when (mode) {
                                GestureMode.Undecided -> {
                                    if (!longPressFired && !draggedWithGesturesOff) {
                                        // Tap: a second tap within 350 ms
                                        // is a double-tap — left / right
                                        // third seeks ∓10 s, centre resets
                                        // the pinch zoom. Otherwise the
                                        // single tap's bar toggle is
                                        // deferred ~350 ms (pendingSingleTap)
                                        // so the bars don't flicker on a
                                        // double-tap seek.
                                        val now = System.currentTimeMillis()
                                        if (now - lastTapTimeMs < 350) {
                                            pendingSingleTap = false
                                            lastTapTimeMs = 0L
                                            val tapX = down.position.x
                                            when {
                                                tapX < size.width / 3f -> {
                                                    videoPlayerViewModel.seekBy(-10_000L)
                                                    val target =
                                                        (positionMs - 10_000L)
                                                            .coerceIn(0L, durationMs)
                                                    showFeedback(
                                                        "« 10s • ${formatVideoTime(target)}"
                                                    )
                                                }
                                                tapX > size.width * 2f / 3f -> {
                                                    videoPlayerViewModel.seekBy(10_000L)
                                                    val target =
                                                        (positionMs + 10_000L)
                                                            .coerceIn(0L, durationMs)
                                                    showFeedback(
                                                        "10s » • ${formatVideoTime(target)}"
                                                    )
                                                }
                                                else -> {
                                                    zoomScale = 1f
                                                }
                                            }
                                        } else {
                                            lastTapTimeMs = now
                                            pendingSingleTap = true
                                        }
                                        poke()
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
                                    // Momentary hold-to-boost (round 17,
                                    // MX model): the speed picked while
                                    // holding was only for the hold — the
                                    // pre-hold speed returns on release
                                    // (unless a speed-bar chip was tapped
                                    // while holding, which pins the speed
                                    // permanently). The bar hides at once,
                                    // no 2 s linger.
                                    speedMode = false
                                    if (!holdSpeedPinned &&
                                        videoPlayerViewModel.playbackSpeed.value !=
                                        preHoldSpeed
                                    ) {
                                        applySpeed(preHoldSpeed)
                                    }
                                    speedBarVisible = false
                                    holdSpeedPinned = false
                                }
                                GestureMode.TwoFingerSpeed -> {
                                    // Persist the live speed once, at the
                                    // end of the drag (setSpeedLive applied
                                    // it without a DataStore write).
                                    videoPlayerViewModel.setSpeed(twoFingerSpeed)
                                }
                                GestureMode.TwoFingerUndecided -> {
                                    // Never locked onto pinch or speed:
                                    // nothing to commit. A hold cancelled
                                    // by the second finger stays cancelled
                                    // — its speed is NOT restored here.
                                }
                                GestureMode.Pinch -> {
                                    // Nothing to commit.
                                }
                            }
                            // Round 18: last finger lifted — the 3 s
                            // auto-hide countdown starts from zero now.
                            touchActive = false
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
                    // speed circle, then the arrow — and it
                    // collapses like portrait (round 18: visible
                    // only when controlsExpanded, in both
                    // orientations; the auto-hide collapse hides
                    // it). Home feedback (2026-11-16): while closed
                    // the bar is COMPLETELY hidden — no peeking row,
                    // no partial buttons — only the arrow handle below
                    // shows, and tapping it opens the full row (it
                    // scrolls).
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
                            // Round 17: a horizontal drag / fling on the
                            // row itself closes it (drag right) — same
                            // settle as the handle. Round 18: enabled in
                            // landscape too, now that the row collapses
                            // there. The LazyRow still owns scrolling: it
                            // consumes drags while it can scroll, so this
                            // only fires from its resting (left-edge) spot.
                            modifier = Modifier
                                .fillMaxWidth()
                                .draggable(
                                    state = rowDragState,
                                    orientation = Orientation.Horizontal,
                                    enabled = true,
                                    // Round 18: a row drag is a touch in
                                    // progress — gate the auto-hide timer
                                    // until the drag settles.
                                    onDragStarted = { rowDragDx = 0f; touchActive = true },
                                    onDragStopped = { velocity ->
                                        touchActive = false
                                        settleRowDrag(velocity)
                                    }
                                ),
                            color = Color.Black.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(
                                bottomStart = 28.dp,
                                bottomEnd = 28.dp
                            )
                        ) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Round 18: rotation + lock were removed
                                // from the quick row — rotation lives in
                                // the bottom bar in landscape / top stack
                                // in portrait, lock in the bottom bar in
                                // landscape / top stack in portrait.
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
                                                        modifier = Modifier.size(16.dp),
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
                                // tapping it closes the row. Round 18: in
                                // both orientations now that the row
                                // collapses in landscape too. The
                                // standalone handle outside only shows
                                // while the row is closed.
                                item(key = "arrow") {
                                    VideoRowArrowButton(
                                        expanded = controlsExpanded,
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
                    // (Round 17: the arrow handle used to live here,
                    // inside the bars — it is now hoisted out of the
                    // barsVisible block, just below, so it stays
                    // visible after the bars auto-hide.)
                    // The speed bar: 8 speeds, 0.1x–4x, just below the
                    // arrow. Opens on long-press; the hold is momentary
                    // (round 17) — on release the pre-hold speed returns
                    // and the bar hides at once, unless a chip was tapped
                    // while holding, which pins that speed. The current
                    // speed is highlighted, and tapping a chip picks it
                    // directly.
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
                                        // A chip tapped while holding pins
                                        // the speed permanently (round 17):
                                        // the hold's release restore skips.
                                        onClick = {
                                            holdSpeedPinned = true
                                            applySpeed(speed)
                                        },
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

            // Arrow handle, hoisted out of the bars (round 17): while
            // the row is closed this small translucent circle stays
            // pinned at the top-right edge — just below where the top
            // bar sits (status bar + 84 dp, the same spot it had
            // inside the column) — even after the bars auto-hide, so
            // the row can always be reopened. Round 18: in both
            // orientations now that the row collapses in landscape
            // too. Tapping it toggles the row (and shows the bars
            // again if they were hidden); dragging it left pulls the
            // row out, dragging it right pushes it away.
            // Round 17 (item 15): in portrait the handle grows into a
            // top stack — arrow → rotation → lock, top-right under the
            // three-dot menu. While the row is open the stack hides
            // (round 18: rotation + lock no longer live in the row —
            // in portrait they are only reachable from this stack
            // while the row is closed, in landscape from the bottom
            // bar).
            if (!controlsExpanded) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 84.dp, end = 16.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = {
                            controlsExpanded = !controlsExpanded
                            if (controlsExpanded) barsVisible = true
                            poke()
                        },
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.45f),
                        modifier = Modifier
                            .size(40.dp)
                            .draggable(
                                state = handleDragState,
                                orientation = Orientation.Horizontal,
                                // Round 18: a handle drag is a touch in
                                // progress — gate the auto-hide timer
                                // until the drag settles.
                                onDragStarted = { rowDragDx = 0f; touchActive = true },
                                onDragStopped = { velocity ->
                                    touchActive = false
                                    settleRowDrag(velocity)
                                }
                            )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            VideoControlsArrowIcon(
                                expanded = controlsExpanded,
                                contentDescription = stringResource(
                                    if (controlsExpanded) {
                                        R.string.video_player_hide_controls
                                    } else {
                                        R.string.video_player_more_controls
                                    }
                                ),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
            // Round 18: rotation + lock live under the arrow only in
            // portrait (top stack); in landscape they live in the
            // bottom bar, so they must not show up here too.
            if (!isLandscape && !controlsExpanded) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 132.dp, end = 16.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VideoTopStackButton(
                        icon = rotationIcon,
                        contentDescription = rotationLabel,
                        isActive = forcedLandscape == true,
                        onClick = { cycleRotation() }
                    )
                    VideoTopStackButton(
                        icon = Icons.Rounded.Lock,
                        contentDescription = stringResource(
                            R.string.video_lock_controls
                        ),
                        isActive = false,
                        onClick = {
                            controlsLocked = true
                            poke()
                        }
                    )
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
                // on the right, and below it prev -15s play/pause +15s
                // next in the centre, aspect + PiP on the far right in
                // portrait, aspect + rotation + PiP in landscape (rotation
                // took the old fullscreen slot, 2nd from the right), lock
                // far left in landscape only. Round 18: fullscreen is
                // gone entirely (user: not needed); lock and rotation live
                // in this row in landscape and in the top stack under the
                // arrow in portrait. No button is cut in either
                // orientation: portrait gaps widen to 6 dp, landscape
                // to 12.8 dp with 1.344x icons.
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
                            // Round 15, wave 2, C4: seek-bar style —
                            // wavy or classic. The scrub behaviour (tap
                            // jumps straight to the tapped point, drag
                            // scrubs at 0.5x) is the same for both; only
                            // the slider component changes.
                            val seekFraction = if (durationMs > 0) {
                                displayPositionMs.toFloat() / durationMs
                            } else {
                                0f
                            }
                            val onSeekFractionChange: (Float) -> Unit = { fraction ->
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
                            }
                            val onSeekFractionFinished: () -> Unit = {
                                scrubbing = false
                                scrubAnchorFraction = null
                                if (seekPreviewMs >= 0) {
                                    videoPlayerViewModel.seekTo(seekPreviewMs)
                                    positionMs = seekPreviewMs
                                    seekPreviewMs = -1L
                                }
                                poke()
                            }
                            if (appearance.seekBarStyle == SeekBarStyleNames.CLASSIC) {
                                Slider(
                                    value = seekFraction,
                                    onValueChange = onSeekFractionChange,
                                    onValueChangeFinished = onSeekFractionFinished,
                                    enabled = durationMs > 0,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color(0xFF8FB8FF),
                                        inactiveTrackColor = Color.White.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp)
                                )
                            } else {
                                WavySliderExpressive(
                                    value = { seekFraction },
                                    onValueChange = onSeekFractionChange,
                                    onValueChangeFinished = onSeekFractionFinished,
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
                            }
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
                            // Round 18: lock moves to the bottom bar in
                            // landscape, far left (the slot was empty —
                            // the row was a weighted centre group + a
                            // right group). Portrait keeps its lock up
                            // in the top stack; nothing changes there.
                            if (isLandscape) {
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
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(
                                    bottomBarGap,
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
                                    bottomBarGap
                                )
                            ) {
                                // Aspect / resize (round 17): the freed
                                // slot of the old rotation button. Cycles
                                // FIT → FILL → ZOOM → STRETCH with the
                                // big centre feedback label, same as the
                                // quick row's aspect button. Round 18:
                                // fullscreen is GONE (user: not needed) —
                                // the right group is now aspect + PiP in
                                // portrait, aspect / ROTATION / PiP in
                                // landscape (rotation takes the old
                                // fullscreen slot, 2nd from the right;
                                // the 3-state icon shows the mode, the
                                // centre label announces it on change).
                                // Rotation lives up top in portrait
                                // (top stack under the arrow).
                                IconButton(
                                    onClick = { cycleAspect() },
                                    modifier = Modifier.size((48f * bottomBarScale).dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AspectRatio,
                                        contentDescription = stringResource(R.string.video_aspect_ratio),
                                        tint = Color.White,
                                        modifier = Modifier.size((28f * bottomBarScale).dp)
                                    )
                                }
                                if (isLandscape) {
                                    IconButton(
                                        onClick = { cycleRotation() },
                                        modifier = Modifier.size((48f * bottomBarScale).dp)
                                    ) {
                                        Icon(
                                            imageVector = rotationIcon,
                                            contentDescription = rotationLabel,
                                            tint = Color.White,
                                            modifier = Modifier.size((28f * bottomBarScale).dp)
                                        )
                                    }
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
 * icons, 10 sp labels, so more buttons fit on screen at once.
 * Round 18: 0.8x — 36.8 dp circles, 17.6 dp icons, 7 sp labels. */
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
        verticalArrangement = Arrangement.spacedBy(1.dp),
        modifier = Modifier
            .width(44.8.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
    ) {
        // Ring + circle: the ring is drawn around the 36.8 dp circle.
        // Round 18: whole button 0.8x (56 -> 44.8, 52 -> 41.6,
        // 46 -> 36.8, icons 22 -> 17.6 dp).
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(41.6.dp)
        ) {
            if (ringProgress != null) {
                Canvas(modifier = Modifier.size(41.6.dp)) {
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
                modifier = Modifier.size(36.8.dp)
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
                            fontSize = 10.4.sp,
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
                                modifier = Modifier.size(17.6.dp)
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
                            modifier = Modifier.size(17.6.dp)
                        )
                    }
                }
            }
        }
        Text(
            text = labelOverride ?: stringResource(button.labelRes),
            color = Color.White,
            fontSize = 7.sp,
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
    // Round 18: aspect removed from the quick row (it stays on the
    // bottom bar) — dropping it here also drops it from Customise
    // Items, so there is no dead toggle. cycleAspect() and the
    // onControlButtonClick "aspect" case stay for the bottom bar.
    VideoControlButton("background", Icons.Rounded.Headphones, R.string.video_background_play),
    VideoControlButton("speed", Icons.Rounded.Speed, R.string.video_speed)
)

/** One 40 dp round button of the portrait top stack under the arrow
 * handle (round 17): same dark circle as the handle itself, primary
 * filled while its state is active (rotation locked to landscape). */
@Composable
private fun VideoTopStackButton(
    icon: ImageVector,
    contentDescription: String?,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (isActive) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.Black.copy(alpha = 0.45f)
        },
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isActive) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    Color.White
                },
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * The quick-row arrow (round 17): the icon itself swaps between
 * KeyboardArrowLeft (closed) and KeyboardArrowRight (open) with a small
 * horizontal slide + crossfade — no rotation at all. The old
 * KeyboardArrowUp rotated -90° ↔ +90° passed through 0° = pointing
 * straight up halfway. Opening: the right-pointing arrow slides in
 * from the left; closing: the left-pointing arrow slides in from the
 * right.
 */
@Composable
private fun VideoControlsArrowIcon(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    AnimatedContent(
        targetState = expanded,
        transitionSpec = {
            (slideInHorizontally { if (targetState) -it / 4 else it / 4 } + fadeIn()) togetherWith
                (slideOutHorizontally { if (targetState) it / 4 else -it / 4 } + fadeOut())
        },
        label = "videoControlsArrow"
    ) { open ->
        Icon(
            imageVector = if (open) {
                Icons.Rounded.KeyboardArrowRight
            } else {
                Icons.Rounded.KeyboardArrowLeft
            },
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = modifier
        )
    }
}

/** The arrow at the end of the quick row: a round button like the others. */
@Composable
private fun VideoRowArrowButton(
    expanded: Boolean,
    onClick: () -> Unit
) {
    // Round 18: shrunk 0.8x like the other quick-row buttons.
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
        modifier = Modifier.width(44.8.dp)
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.45f),
            modifier = Modifier.size(36.8.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                VideoControlsArrowIcon(
                    expanded = expanded,
                    modifier = Modifier.size(17.6.dp)
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
            fontSize = 7.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/** Which gesture the video-surface detector is currently tracking. */
private enum class GestureMode {
    Undecided, Seek, Brightness, Volume, Speed, Pinch,
    /** Second finger down, not yet locked to pinch or two-finger speed. */
    TwoFingerUndecided,
    /** Two fingers moving together vertically: playback speed. */
    TwoFingerSpeed
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
