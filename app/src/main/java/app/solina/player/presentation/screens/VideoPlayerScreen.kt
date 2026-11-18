package app.solina.player.presentation.screens

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.provider.OpenableColumns
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.rounded.LockOpen
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
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import app.solina.player.R
import app.solina.player.data.preferences.SeekBarStyleNames
import app.solina.player.presentation.components.WavySliderExpressive
import app.solina.player.presentation.utils.LocalIsInPictureInPicture
import app.solina.player.presentation.utils.ScreenAwakeCoordinator
import app.solina.player.presentation.utils.VideoPipContract
import app.solina.player.presentation.viewmodel.AppearanceViewModel
import app.solina.player.presentation.viewmodel.VideoPlayerViewModel
import app.solina.player.utils.PhoneOrientationPolicy
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.URLDecoder
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Modular, expressive Video Player screen built around hardware-accelerated Media3,
 * isolated 250ms seekbar recomposition, non-blocking screenshots, live scrubbing previews,
 * PiP integration, smart portrait/landscape orientation, and rich error/resume recovery.
 */
@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    videoUri: String,
    navController: NavController,
    videoQueue: List<String> = emptyList(),
    startPositionMs: Long = 0L,
    videoPlayerViewModel: VideoPlayerViewModel = hiltViewModel(),
    appearanceViewModel: AppearanceViewModel = hiltViewModel()
) {
    val appearance by appearanceViewModel.appearance.collectAsStateWithLifecycle()
    val inPip = LocalIsInPictureInPicture.current

    val player by videoPlayerViewModel.player.collectAsStateWithLifecycle()
    val currentVideo by videoPlayerViewModel.currentVideo.collectAsStateWithLifecycle()
    val playbackSpeed by videoPlayerViewModel.playbackSpeed.collectAsStateWithLifecycle()
    val isPlaying by videoPlayerViewModel.isPlaying.collectAsStateWithLifecycle()
    val isLoading by videoPlayerViewModel.isLoading.collectAsStateWithLifecycle()
    val holderDurationMs by videoPlayerViewModel.holderDurationMs.collectAsStateWithLifecycle()
    val playbackError by videoPlayerViewModel.playbackError.collectAsStateWithLifecycle()
    val pendingResumePromptMs by videoPlayerViewModel.pendingResumePromptMs.collectAsStateWithLifecycle()
    val aspectMode by videoPlayerViewModel.aspectMode.collectAsStateWithLifecycle()
    val backgroundPlayback by videoPlayerViewModel.backgroundPlayback.collectAsStateWithLifecycle()
    val swipeGesturesEnabled by videoPlayerViewModel.swipeGesturesEnabled.collectAsStateWithLifecycle()
    val nightLight by videoPlayerViewModel.nightLight.collectAsStateWithLifecycle()
    val brightness by videoPlayerViewModel.brightness.collectAsStateWithLifecycle()
    val volume by videoPlayerViewModel.volume.collectAsStateWithLifecycle()
    val sleepTimerMinutes by videoPlayerViewModel.sleepTimerMinutes.collectAsStateWithLifecycle()
    val sleepTimerRemainingSeconds by videoPlayerViewModel.sleepTimerRemainingSeconds.collectAsStateWithLifecycle()
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
    val isFullscreen by videoPlayerViewModel.isFullscreen.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val view = LocalView.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val durationMs = remember(currentVideo?.durationMs, holderDurationMs) {
        currentVideo?.durationMs?.takeIf { it > 0L } ?: holderDurationMs.coerceAtLeast(0L)
    }

    var screenshotState by remember { mutableIntStateOf(0) }
    LaunchedEffect(screenshotState) {
        if (screenshotState == 2) {
            delay(1200)
            screenshotState = 0
        }
    }

    // Dialog / sheet visibility
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showGridMenu by remember { mutableStateOf(false) }
    var showSubtitlePanel by remember { mutableStateOf(false) }
    var showQueueDialog by remember { mutableStateOf(false) }
    var showBookmarksDialog by remember { mutableStateOf(false) }
    var showJumpToDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showTutorialDialog by remember { mutableStateOf(false) }
    var showAudioDisplayDialog by remember { mutableStateOf(false) }
    var showCustomiseDialog by remember { mutableStateOf(false) }

    var controlsExpanded by rememberSaveable { mutableStateOf(false) }
    var controlsLocked by rememberSaveable { mutableStateOf(false) }
    var lockHintVisible by remember { mutableStateOf(false) }
    var isMirrored by rememberSaveable { mutableStateOf(false) }
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }

    // Smart initial orientation: portrait videos open portrait; landscape/unknown open landscape.
    var forcedLandscape by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var userOverrodeOrientation by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(currentVideo?.contentUriString, currentVideo?.width, currentVideo?.height) {
        if (!userOverrodeOrientation && currentVideo != null) {
            forcedLandscape = !currentVideo!!.isPortrait
        }
    }

    // Auto-hide bars timer
    var barsVisible by remember { mutableStateOf(true) }
    var interactionTick by remember { mutableIntStateOf(0) }
    fun poke() {
        interactionTick++
    }

    var touchActive by remember { mutableStateOf(false) }
    var scrubbing by remember { mutableStateOf(false) }
    var zoomScale by remember { mutableStateOf(1f) }
    var speedMode by remember { mutableStateOf(false) }
    var speedBarVisible by remember { mutableStateOf(false) }
    var holdSpeedPinned by remember { mutableStateOf(false) }
    var lastHoldSpeed by remember { mutableStateOf<Float?>(null) }
    var seekPreviewMs by remember { mutableLongStateOf(-1L) }
    var seekDeltaText by remember { mutableStateOf<String?>(null) }
    var gestureHint by remember { mutableStateOf<GestureHint?>(null) }
    var gestureBrightness by rememberSaveable { mutableStateOf<Float?>(null) }

    var pendingSingleTap by remember { mutableStateOf(false) }
    LaunchedEffect(pendingSingleTap) {
        if (pendingSingleTap) {
            delay(240)
            if (pendingSingleTap) {
                pendingSingleTap = false
                barsVisible = !barsVisible
                if (barsVisible) poke()
            }
        }
    }

    LaunchedEffect(lockHintVisible, controlsLocked) {
        if (controlsLocked && lockHintVisible) {
            delay(2_500)
            lockHintVisible = false
        }
    }

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

    val decodedUri = remember(videoUri) {
        runCatching { URLDecoder.decode(videoUri, "UTF-8") }.getOrElse { videoUri }
    }

    LaunchedEffect(decodedUri, videoQueue, startPositionMs) {
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

    val anyMenuOpen = showSpeedMenu || showGridMenu || showSubtitlePanel ||
        showQueueDialog || showBookmarksDialog || showJumpToDialog ||
        showInfoDialog || showTutorialDialog || showAudioDisplayDialog ||
        showCustomiseDialog

    LaunchedEffect(
        barsVisible, interactionTick, isPlaying, anyMenuOpen,
        speedMode, speedBarVisible, touchActive, scrubbing
    ) {
        if (barsVisible && isPlaying && !anyMenuOpen &&
            !speedMode && !speedBarVisible && !touchActive && !scrubbing
        ) {
            delay(4_000)
            barsVisible = false
            controlsExpanded = false
        }
    }

    LaunchedEffect(barsVisible) {
        if (!barsVisible) controlsExpanded = false
    }

    // Screen keep-awake only while playing
    DisposableEffect(isPlaying) {
        ScreenAwakeCoordinator.request("VideoPlayer", isPlaying, view)
        onDispose { ScreenAwakeCoordinator.request("VideoPlayer", false, view) }
    }

    LaunchedEffect(speedBarVisible, speedMode) {
        if (speedBarVisible || speedMode) {
            // Round 20: the hold speed bar is solo — hide the top/bottom bars,
            // the quick row and the handle stack while it is showing.
            barsVisible = false
            controlsExpanded = false
        }
        if (speedBarVisible && !speedMode) {
            delay(2_000)
            speedBarVisible = false
        }
    }

    // Periodic progress persistence every 15s while playing
    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        while (currentCoroutineContext().isActive) {
            delay(15_000L)
            videoPlayerViewModel.saveResumePosition()
        }
    }

    // Background playback toggle: skip pause if activity moved into Picture-in-Picture
    DisposableEffect(lifecycleOwner, backgroundPlayback) {
        val observer = LifecycleEventObserver { _, event ->
            val inPipNow = activity?.isInPictureInPictureMode == true
            if (event == Lifecycle.Event.ON_STOP && !backgroundPlayback && !inPipNow) {
                videoPlayerViewModel.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Screen brightness (gesture override > night light > system default)
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

    // Orientation ownership & cleanup
    LaunchedEffect(Unit) {
        PhoneOrientationPolicy.videoPlayerActive = true
    }
    LaunchedEffect(isFullscreen, forcedLandscape) {
        activity?.requestedOrientation = when {
            isFullscreen || forcedLandscape == true -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            forcedLandscape == false -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            videoPlayerViewModel.onLeavePlayer()
            PhoneOrientationPolicy.videoPlayerActive = false
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            ScreenAwakeCoordinator.reset(view)
            activity?.window?.let { window ->
                val attributes = window.attributes
                attributes.screenBrightness = -1f
                window.attributes = attributes
                window.insetsController?.show(WindowInsets.Type.systemBars())
            }
        }
    }

    // System bars sync with UI bars
    LaunchedEffect(barsVisible, controlsLocked, inPip) {
        val controller = activity?.window?.insetsController ?: return@LaunchedEffect
        if (barsVisible && !controlsLocked && !inPip) {
            controller.show(WindowInsets.Type.systemBars())
        } else {
            controller.hide(WindowInsets.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    // Subtitle document picker
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
            Toast.makeText(context, context.getString(R.string.video_subtitle_unsupported), Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        videoPlayerViewModel.addExternalSubtitle(uri, name, mime)
        poke()
    }

    // Toast feedback for ViewModel actions
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

    fun applySpeed(speed: Float) {
        videoPlayerViewModel.setSpeed(speed)
        if (holdSpeedPinned) lastHoldSpeed = speed
        showFeedback(formatSpeedLabel(speed))
        poke()
    }

    fun cycleAspect() {
        val next = videoPlayerViewModel.nextAspectMode()
        videoPlayerViewModel.cycleAspectMode()
        val label = when (next) {
            "FILL" -> context.getString(R.string.video_aspect_fill)
            "ZOOM" -> context.getString(R.string.video_aspect_zoom)
            "STRETCH" -> context.getString(R.string.video_aspect_stretch)
            else -> context.getString(R.string.video_aspect_fit)
        }
        showFeedback(label)
        poke()
    }

    fun cycleRotation() {
        userOverrodeOrientation = true
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
                Intent.createChooser(intent, context.getString(R.string.video_grid_menu_share))
            )
        }
        poke()
    }

    fun openSystemEqualizer() {
        val sessionId = player?.audioSessionId ?: 0
        val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MOVIE)
            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
        }
        runCatching { context.startActivity(intent) }.onFailure {
            Toast.makeText(context, context.getString(R.string.video_equalizer_unavailable), Toast.LENGTH_SHORT).show()
        }
    }

    fun enterPictureInPicture() {
        val act = activity ?: return
        val params = VideoPipContract.buildParams(
            context = act,
            isPlaying = isPlaying,
            videoWidth = currentVideo?.width ?: 16,
            videoHeight = currentVideo?.height ?: 9,
            autoEnter = isPlaying
        )
        runCatching {
            act.enterPictureInPictureMode(params)
        }.onFailure {
            Toast.makeText(context, context.getString(R.string.video_pip_unavailable), Toast.LENGTH_SHORT).show()
        }
    }

    val brightnessLabel = stringResource(R.string.video_brightness)
    val volumeLabel = stringResource(R.string.video_volume)
    val speedOptions = videoPlayerViewModel.availableSpeeds
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val bottomBarScale = when {
        isLandscape -> 1.24f
        configuration.screenWidthDp < 470 -> 0.85f
        else -> 1f
    }
    // Round 20: centre transport buttons (prev/next + +/-15s) are 20% larger and
    // twice as far apart ONLY in landscape; play/pause is intentionally untouched.
    val bottomBarCentreScale = if (isLandscape) 1.2f else 1f
    val bottomBarCentreGap = if (isLandscape) 24.dp else 10.dp
    // Physical right edge, whatever the layout direction is. Alignment.End on its
    // own is the LEFT wall in RTL, which is why the quick row used to open from
    // the left even though its handle sits on the right.
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val physicalRightH = if (isRtl) Alignment.Start else Alignment.End
    val physicalRightCenter = if (isRtl) Alignment.CenterStart else Alignment.CenterEnd
    val physicalRightTop = if (isRtl) Alignment.TopStart else Alignment.TopEnd
    val bottomBarGap = if (isLandscape) 10.dp else 6.dp
    val speedStepPx = with(LocalDensity.current) { 56.dp.toPx() }

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

    val allButtons = videoControlButtons
    val quickRowButtons = remember(visibleButtons) {
        if (visibleButtons.isEmpty()) allButtons
        else allButtons.filter { it.id in visibleButtons }
    }

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
            "audiofx" -> showAudioDisplayDialog = true
            "equalizer" -> openSystemEqualizer()
            "screenshot" -> {
                if (screenshotState == 0) {
                    screenshotState = 1
                    takeVideoScreenshotAsync(
                        context = context,
                        playerView = playerViewRef,
                        videoUri = currentVideo?.contentUriString ?: decodedUri,
                        positionMs = videoPlayerViewModel.currentPositionMs()
                    ) { ok ->
                        screenshotState = if (ok) 2 else 0
                    }
                }
            }
            "background" -> videoPlayerViewModel.setBackgroundPlayback(!backgroundPlayback)
            "speed" -> showSpeedMenu = true
        }
        poke()
    }

    val hint = gestureHint
    val indicatorText: String? = when {
        feedbackLabel != null -> null
        seekPreviewMs >= 0L && seekDeltaText != null ->
            "$seekDeltaText • ${formatVideoTime(seekPreviewMs)}"
        else -> null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Hardware video surface + multi-touch gestures (isolated from 250ms ticks)
        VideoPlayerSurface(
            player = player,
            playerViewRef = playerViewRef,
            onPlayerViewCreated = { playerViewRef = it },
            aspectMode = aspectMode,
            zoomScale = zoomScale,
            isMirrored = isMirrored,
            subtitleStyle = subtitleStyle,
            controlsLocked = controlsLocked,
            inPip = inPip,
            swipeGesturesEnabled = swipeGesturesEnabled,
            playbackSpeed = playbackSpeed,
            availableSpeeds = speedOptions,
            lastHoldSpeed = lastHoldSpeed,
            onLastHoldSpeedChanged = { lastHoldSpeed = it },
            gestureBrightness = gestureBrightness,
            nightLight = nightLight,
            nightLightBrightnessPercent = brightness,
            speedStepPx = speedStepPx,
            brightnessLabel = brightnessLabel,
            volumeLabel = volumeLabel,
            positionMsProvider = { videoPlayerViewModel.currentPositionMs() },
            durationMsProvider = { durationMs },
            onTouchActiveChange = { touchActive = it },
            onScrubbingChange = { scrubbing = it },
            onSeekPreview = { target, delta ->
                seekPreviewMs = target
                seekDeltaText = delta
            },
            onCommitSeek = { target -> videoPlayerViewModel.seekTo(target) },
            onSeekBy = { delta -> videoPlayerViewModel.seekBy(delta) },
            onBrightnessChange = { gestureBrightness = it },
            onGestureHint = { gestureHint = it },
            onFeedback = { showFeedback(it) },
            onZoomScaleChange = { zoomScale = it },
            onSpeedBarState = { sm, sbv ->
                speedMode = sm
                speedBarVisible = sbv
            },
            holdSpeedPinned = holdSpeedPinned,
            onHoldPinnedChange = { holdSpeedPinned = it },
            onApplySpeedLive = { videoPlayerViewModel.setSpeedLive(it) },
            onCommitSpeed = { videoPlayerViewModel.setSpeed(it) },
            onSingleTapCandidate = { pendingSingleTap = true },
            onShowBarsAndPoke = {
                barsVisible = true
                poke()
            },
            onSpeedBarShown = {
                // Round 20: the long-press speed bar is solo — do NOT show the bars.
                barsVisible = false
                controlsExpanded = false
            },
            onCancelPendingSingleTap = { pendingSingleTap = false },
            onLockedTap = { lockHintVisible = !lockHintVisible }
        )

        // Subtle warm Night Light film tint
        if (nightLight) {
            val tintAlpha = ((100 - brightness).coerceIn(0, 90) / 90f) * 0.24f + 0.06f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFF9800).copy(alpha = tintAlpha))
            )
        }

        // In Picture-in-Picture mode render ONLY the clean video surface.
        if (inPip) return@Box

        // 2. Smart transparent lock pill (zero dim scrim over the movie)
        if (controlsLocked) {
            AnimatedVisibility(
                visible = lockHintVisible,
                enter = fadeIn(tween(180)) + slideInVertically { it / 2 },
                exit = fadeOut(tween(220)) + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 32.dp)
            ) {
                Surface(
                    onClick = {
                        controlsLocked = false
                        lockHintVisible = false
                        barsVisible = true
                        poke()
                    },
                    shape = RoundedCornerShape(28.dp),
                    color = Color.Black.copy(alpha = 0.72f),
                    modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(28.dp))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LockOpen,
                            contentDescription = null,
                            tint = videoAccentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.video_tap_to_unlock),
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            return@Box
        }

        // 3. Top bar & sliding Quick-Action Row
        AnimatedVisibility(
            visible = barsVisible,
            enter = fadeIn(tween(200)) + slideInVertically { -it / 2 },
            exit = fadeOut(tween(200)) + slideOutVertically { -it / 2 },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentVideo?.displayTitle ?: stringResource(R.string.video_player_title),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        color = Color.White.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            text = decoderBadge,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Box {
                        Surface(
                            onClick = { showSpeedMenu = true; poke() },
                            shape = RoundedCornerShape(14.dp),
                            color = if (playbackSpeed != 1f) videoAccentColor else Color.White.copy(alpha = 0.16f),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = formatSpeedLabel(playbackSpeed),
                                color = if (playbackSpeed != 1f) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showSpeedMenu,
                            onDismissRequest = { showSpeedMenu = false }
                        ) {
                            speedOptions.forEach { speed ->
                                DropdownMenuItem(
                                    text = { Text(formatSpeedLabel(speed)) },
                                    onClick = {
                                        showSpeedMenu = false
                                        applySpeed(speed)
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { showAudioDisplayDialog = true; poke() }) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = stringResource(R.string.video_audio_track),
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { showSubtitlePanel = true; poke() }) {
                        Icon(
                            imageVector = Icons.Rounded.ClosedCaption,
                            contentDescription = stringResource(R.string.video_subtitles),
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { showGridMenu = true; poke() }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.video_grid_menu_more),
                            tint = Color.White
                        )
                    }
                }

                // Expandable quick-action row (natural wrap sizing, RTL/LTR safe)
                AnimatedVisibility(
                    visible = controlsExpanded,
                    enter = expandHorizontally(expandFrom = physicalRightH) + fadeIn(),
                    exit = shrinkHorizontally(shrinkTowards = physicalRightH) + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, start = if (isRtl) 8.dp else 0.dp, end = if (isRtl) 0.dp else 8.dp),
                        contentAlignment = physicalRightCenter
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.62f),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .border(
                                    1.dp,
                                    Color.White.copy(alpha = 0.12f),
                                    RoundedCornerShape(24.dp)
                                )
                                .quickRowSwipeToClose(controlsExpanded) { controlsExpanded = false; poke() }
                        ) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                items(quickRowButtons, key = { it.id }) { button ->
                                    val sleepProgress = if (button.id == "sleep" && sleepTimerMinutes > 0 && sleepTimerRemainingSeconds > 0) {
                                        (sleepTimerRemainingSeconds.toFloat() / (sleepTimerMinutes * 60f)).coerceIn(0f, 1f)
                                    } else null
                                    val sleepLabel = if (button.id == "sleep" && sleepTimerMinutes > 0) {
                                        val mins = (sleepTimerRemainingSeconds + 59) / 60
                                        stringResource(R.string.video_sleep_timer_active, mins.coerceAtLeast(1))
                                    } else null
                                    VideoRoundControlButton(
                                        button = button,
                                        isActive = when (button.id) {
                                            "night" -> nightLight
                                            "shuffle" -> shuffleEnabled
                                            "mute" -> isMuted
                                            "background" -> backgroundPlayback
                                            "loop" -> repeatMode != Player.REPEAT_MODE_OFF
                                            "sleep" -> sleepTimerMinutes > 0
                                            "ab" -> aPointMs >= 0L
                                            "speed" -> playbackSpeed != 1f
                                            else -> false
                                        },
                                        labelOverride = when (button.id) {
                                            "loop" -> when (repeatMode) {
                                                Player.REPEAT_MODE_ONE -> stringResource(R.string.video_loop_one)
                                                Player.REPEAT_MODE_ALL -> stringResource(R.string.video_loop_all)
                                                else -> stringResource(R.string.video_loop)
                                            }
                                            "sleep" -> sleepLabel ?: stringResource(R.string.video_sleep_timer)
                                            "ab" -> when {
                                                bPointMs >= 0L -> stringResource(R.string.video_ab_repeat_active)
                                                aPointMs >= 0L -> stringResource(R.string.video_ab_repeat_set_a)
                                                else -> stringResource(R.string.video_ab_repeat)
                                            }
                                            else -> null
                                        },
                                        circleText = if (button.id == "speed") formatSpeedLabel(playbackSpeed) else null,
                                        iconOverride = if (button.id == "loop" && repeatMode == Player.REPEAT_MODE_ONE) {
                                            Icons.Rounded.RepeatOne
                                        } else null,
                                        ringProgress = sleepProgress,
                                        customContent = if (button.id == "screenshot" && screenshotState != 0) {
                                            {
                                                if (screenshotState == 1) {
                                                    CircularProgressIndicator(
                                                        color = Color.White,
                                                        strokeWidth = 2.dp,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                } else {
                                                    ScreenshotDoneCheck()
                                                }
                                            }
                                        } else null,
                                        onClick = { onControlButtonClick(button) }
                                    )
                                }
                                item(key = "collapse_arrow") {
                                    VideoRowArrowButton(
                                        expanded = true,
                                        onClick = { controlsExpanded = false; poke() }
                                    )
                                }
                            }
                        }
                    }
                }

            }
        }

        // Top-End compact handle stack (expand arrow, plus rotation + lock in portrait)
        AnimatedVisibility(
            visible = barsVisible && !controlsExpanded,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180)),
            modifier = Modifier
                .align(physicalRightTop)
                .statusBarsPadding()
                .padding(top = 72.dp, start = if (isRtl) 14.dp else 0.dp, end = if (isRtl) 0.dp else 14.dp)
        ) {
            Column(
                horizontalAlignment = physicalRightH,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = {
                        controlsExpanded = true
                        barsVisible = true
                        poke()
                    },
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.52f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        VideoControlsArrowIcon(
                            expanded = false,
                            contentDescription = stringResource(R.string.video_player_more_controls),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                if (!isLandscape) {
                    VideoTopStackButton(
                        icon = rotationIcon,
                        contentDescription = rotationLabel,
                        isActive = forcedLandscape == true,
                        onClick = { cycleRotation() }
                    )
                    VideoTopStackButton(
                        icon = Icons.Rounded.Lock,
                        contentDescription = stringResource(R.string.video_lock_controls),
                        isActive = false,
                        onClick = {
                            controlsLocked = true
                            lockHintVisible = true
                        }
                    )
                }
            }
        }

        // Round 20: solo hold speed bar — a direct child of the root Box (NOT
        // inside the top bar, which hides with the other bars), compact, top
        // physical-right wall. All sizes halved vs before: chips 11sp -> 6sp.
        AnimatedVisibility(
            visible = speedBarVisible,
            enter = fadeIn(tween(150)) + slideInVertically { -it / 4 },
            exit = fadeOut(tween(150)),
            modifier = Modifier
                .align(physicalRightTop)
                .statusBarsPadding()
                .padding(top = 8.dp, start = if (isRtl) 14.dp else 0.dp, end = if (isRtl) 0.dp else 14.dp)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.72f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
                ) {
                    speedOptions.forEach { speed ->
                        val selected = abs(playbackSpeed - speed) < 0.01f
                        Surface(
                            onClick = {
                                holdSpeedPinned = true
                                applySpeed(speed)
                            },
                            shape = RoundedCornerShape(7.dp),
                            color = if (selected) videoAccentColor else Color.White.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = formatSpeedLabel(speed),
                                color = if (selected) Color.Black else Color.White,
                                fontSize = 6.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Non-destructive Resume Ask Banner
        AnimatedVisibility(
            visible = pendingResumePromptMs != null,
            enter = fadeIn(tween(220)) + slideInVertically { it / 2 },
            exit = fadeOut(tween(180)) + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (barsVisible) 136.dp else 28.dp)
        ) {
            pendingResumePromptMs?.let { resumeMs ->
                VideoResumePromptBanner(
                    resumePositionMs = resumeMs,
                    onResume = { videoPlayerViewModel.acceptResumePrompt(); poke() },
                    onStartOver = { videoPlayerViewModel.restartFromBeginning(); poke() }
                )
            }
        }

        // 5. Bottom SeekBar + Transport Bar (position ticks isolated inside VideoSeekBarSection!)
        AnimatedVisibility(
            visible = barsVisible,
            enter = fadeIn(tween(200)) + slideInVertically { it / 2 },
            exit = fadeOut(tween(200)) + slideOutVertically { it / 2 },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))
                        )
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                VideoSeekBarSection(
                    positionMsFlow = videoPlayerViewModel.positionMs,
                    durationMs = durationMs,
                    videoUri = currentVideo?.contentUriString ?: decodedUri,
                    isPlaying = isPlaying,
                    classicStyle = appearance.seekBarStyle == SeekBarStyleNames.CLASSIC,
                    externalSeekPreviewMs = seekPreviewMs,
                    onScrubStart = {
                        scrubbing = true
                        poke()
                    },
                    onScrubStop = { targetMs ->
                        scrubbing = false
                        if (targetMs >= 0L) videoPlayerViewModel.seekTo(targetMs)
                        poke()
                    }
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLandscape) {
                        IconButton(
                            onClick = {
                                controlsLocked = true
                                lockHintVisible = true
                            },
                            modifier = Modifier.size((46f * bottomBarScale).dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = stringResource(R.string.video_lock_controls),
                                tint = Color.White,
                                modifier = Modifier.size((24f * bottomBarScale).dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(bottomBarCentreGap, Alignment.CenterHorizontally),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = { videoPlayerViewModel.seekToPrevious(); poke() },
                            modifier = Modifier.size((44f * bottomBarScale * bottomBarCentreScale).dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipPrevious,
                                contentDescription = stringResource(R.string.video_previous_video),
                                tint = Color.White,
                                modifier = Modifier.size((28f * bottomBarScale * bottomBarCentreScale).dp)
                            )
                        }
                        VideoSkip15Button(
                            label = "-15",
                            contentDescription = stringResource(R.string.video_seek_back_15),
                            onClick = { videoPlayerViewModel.seekBy(-15_000L); poke() },
                            size = (38f * bottomBarScale * bottomBarCentreScale).dp,
                            fontSize = (11f * bottomBarScale * bottomBarCentreScale).sp
                        )
                        val playCorner by animateDpAsState(
                            targetValue = if (isPlaying) 14.dp else 28.dp,
                            animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                            label = "videoPlayCorner"
                        )
                        Surface(
                            onClick = { videoPlayerViewModel.togglePlayPause(); poke() },
                            shape = RoundedCornerShape(playCorner),
                            color = videoAccentColor,
                            modifier = Modifier.size((52f * bottomBarScale).dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size((30f * bottomBarScale).dp)
                                )
                            }
                        }
                        VideoSkip15Button(
                            label = "+15",
                            contentDescription = stringResource(R.string.video_seek_forward_15),
                            onClick = { videoPlayerViewModel.seekBy(15_000L); poke() },
                            size = (38f * bottomBarScale * bottomBarCentreScale).dp,
                            fontSize = (11f * bottomBarScale * bottomBarCentreScale).sp
                        )
                        IconButton(
                            onClick = { videoPlayerViewModel.seekToNext(); poke() },
                            modifier = Modifier.size((44f * bottomBarScale * bottomBarCentreScale).dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = stringResource(R.string.video_next_video),
                                tint = Color.White,
                                modifier = Modifier.size((28f * bottomBarScale * bottomBarCentreScale).dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(bottomBarGap)
                    ) {
                        IconButton(
                            onClick = { cycleAspect() },
                            modifier = Modifier.size((42f * bottomBarScale).dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AspectRatio,
                                contentDescription = stringResource(R.string.video_aspect_ratio),
                                tint = Color.White,
                                modifier = Modifier.size((24f * bottomBarScale).dp)
                            )
                        }
                        if (isLandscape) {
                            IconButton(
                                onClick = { cycleRotation() },
                                modifier = Modifier.size((42f * bottomBarScale).dp)
                            ) {
                                Icon(
                                    imageVector = rotationIcon,
                                    contentDescription = rotationLabel,
                                    tint = Color.White,
                                    modifier = Modifier.size((24f * bottomBarScale).dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = { enterPictureInPicture() },
                            modifier = Modifier.size((42f * bottomBarScale).dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PictureInPictureAlt,
                                contentDescription = stringResource(R.string.video_picture_in_picture),
                                tint = Color.White,
                                modifier = Modifier.size((24f * bottomBarScale).dp)
                            )
                        }
                    }
                }
            }
        }

        // 6. Center Seek Delta / Feedback / Edge Sliders / Buffering / Playback Error
        indicatorText?.let { text ->
            Surface(
                color = Color.Black.copy(alpha = 0.68f),
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

        AnimatedVisibility(
            visible = feedbackLabel != null,
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            feedbackLabel?.let { text ->
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.displaySmall.copy(
                        shadow = Shadow(color = Color.Black.copy(alpha = 0.7f), blurRadius = 12f)
                    )
                )
            }
        }

        hint?.let { edgeHint ->
            val isBrightnessHint = edgeHint.label == brightnessLabel
            VideoEdgeSlider(
                label = edgeHint.label,
                percent = edgeHint.percent,
                isBrightness = isBrightnessHint,
                modifier = Modifier
                    .align(if (isBrightnessHint) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 16.dp)
            )
        }

        if (isLoading && playbackError == null && indicatorText == null && feedbackLabel == null) {
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
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

        playbackError?.let { err ->
            VideoPlaybackErrorOverlay(
                error = err,
                hasNext = queueItems.size > 1,
                onRetry = { videoPlayerViewModel.retryCurrent() },
                onRetrySoftware = { videoPlayerViewModel.retryWithSoftwareDecoder() },
                onNext = {
                    videoPlayerViewModel.clearError()
                    videoPlayerViewModel.seekToNext()
                },
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // 7. Landscape Subtitle Side Panel
        if (showSubtitlePanel && isLandscape) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { showSubtitlePanel = false }
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxWidth(0.45f)
                        .fillMaxHeight()
                        .background(Color.Black.copy(alpha = 0.88f))
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
                        onEdgeType = { videoPlayerViewModel.setSubtitleEdgeType(it) },
                        onClose = { showSubtitlePanel = false },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }

    // 8. Modals & Dialogs
    if (showGridMenu) {
        VideoGridMenuSheet(
            isFavourite = isFavourite,
            isMirrored = isMirrored,
            onDismiss = { showGridMenu = false },
            onAction = { action ->
                when (action) {
                    VideoGridAction.AUDIO, VideoGridAction.DISPLAY -> showAudioDisplayDialog = true
                    VideoGridAction.SUBTITLE -> showSubtitlePanel = true
                    VideoGridAction.QUEUE -> showQueueDialog = true
                    VideoGridAction.ASPECT -> cycleAspect()
                    VideoGridAction.BOOKMARK -> showBookmarksDialog = true
                    VideoGridAction.JUMP_TO -> showJumpToDialog = true
                    VideoGridAction.MIRROR -> {
                        isMirrored = !isMirrored
                        showFeedback(
                            context.getString(
                                if (isMirrored) R.string.video_mirror_on else R.string.video_mirror_off
                            )
                        )
                        poke()
                    }
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
            onEdgeType = { videoPlayerViewModel.setSubtitleEdgeType(it) },
            onDismiss = { showSubtitlePanel = false }
        )
    }

    if (showAudioDisplayDialog) {
        VideoAudioDisplayDialog(
            audioTrackLabels = audioTrackLabels,
            selectedAudioTrack = selectedAudioTrack,
            onSelectTrack = { videoPlayerViewModel.selectAudioTrack(it) },
            playerGain = volume,
            onPlayerGain = { videoPlayerViewModel.setVolume(it) },
            nightLight = nightLight,
            brightness = brightness,
            onNightLight = { videoPlayerViewModel.setNightLight(it) },
            onBrightness = { videoPlayerViewModel.setBrightness(it) },
            onDismiss = { showAudioDisplayDialog = false }
        )
    }

    if (showQueueDialog) {
        VideoQueueDialog(
            queueItems = queueItems,
            currentIndex = player?.currentMediaItemIndex ?: 0,
            onSelect = { videoPlayerViewModel.playQueueIndex(it) },
            onMove = { from, to -> videoPlayerViewModel.moveQueueItem(from, to) },
            onRemove = { idx -> videoPlayerViewModel.removeQueueItem(idx) },
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

    if (showJumpToDialog) {
        VideoJumpToDialog(
            currentPositionMs = videoPlayerViewModel.currentPositionMs(),
            durationMs = durationMs,
            onJump = { target ->
                videoPlayerViewModel.seekTo(target)
                poke()
            },
            onDismiss = { showJumpToDialog = false }
        )
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

    if (showCustomiseDialog) {
        val selectedIds = remember(visibleButtons) {
            mutableStateOf(
                if (visibleButtons.isEmpty()) allButtons.map { it.id }.toSet()
                else visibleButtons
            )
        }
        AlertDialog(
            onDismissRequest = { showCustomiseDialog = false },
            containerColor = Color.Black.copy(alpha = 0.90f),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text(text = stringResource(R.string.video_customise_items)) },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
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
                                .padding(vertical = 2.dp)
                        ) {
                            Checkbox(
                                checked = selectedIds.value.contains(button.id),
                                onCheckedChange = { checked ->
                                    val current = selectedIds.value
                                    selectedIds.value =
                                        if (checked) current + button.id
                                        else current - button.id
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = videoAccentColor,
                                    uncheckedColor = Color.White.copy(alpha = 0.6f),
                                    checkmarkColor = Color.Black
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
                    val ids = selectedIds.value
                    videoPlayerViewModel.setVisibleControlButtons(
                        if (ids.size == allButtons.size) emptySet() else ids
                    )
                    showCustomiseDialog = false
                }) {
                    Text(text = stringResource(R.string.video_player_done), color = videoAccentColor)
                }
            }
        )
    }
}

/**
 * Isolated SeekBar + Floating Thumbnail Preview Card.
 * Collects [positionMsFlow] inside its own scope so 250ms playback ticks
 * never trigger recomposition in [VideoPlayerSurface] or top controls.
 */
@Composable
private fun VideoSeekBarSection(
    positionMsFlow: kotlinx.coroutines.flow.StateFlow<Long>,
    durationMs: Long,
    videoUri: String,
    isPlaying: Boolean,
    classicStyle: Boolean,
    externalSeekPreviewMs: Long,
    onScrubStart: () -> Unit,
    onScrubStop: (Long) -> Unit
) {
    val context = LocalContext.current
    val positionMs by positionMsFlow.collectAsStateWithLifecycle()

    var sliderScrubbing by remember { mutableStateOf(false) }
    var sliderPreviewMs by remember { mutableLongStateOf(-1L) }
    var anchorFraction by remember { mutableStateOf<Float?>(null) }
    var anchorMs by remember { mutableLongStateOf(0L) }

    val activePreviewMs = when {
        sliderScrubbing && sliderPreviewMs >= 0L -> sliderPreviewMs
        externalSeekPreviewMs >= 0L -> externalSeekPreviewMs
        else -> -1L
    }
    val displayPosMs = if (activePreviewMs >= 0L) activePreviewMs else positionMs
    val seekFraction = if (durationMs > 0L) {
        (displayPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    // Live thumbnail preview above the thumb when scrubbing
    if (sliderScrubbing && sliderPreviewMs >= 0L && durationMs > 0L) {
        val previewBucket = sliderPreviewMs / 2_000L
        val previewBitmap by produceState<Bitmap?>(initialValue = null, videoUri, previewBucket) {
            value = VideoSeekPreviewCache.loadFrame(context, videoUri, sliderPreviewMs)
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 42.dp, vertical = 6.dp)
        ) {
            val density = LocalDensity.current
            val cardWidthPx = with(density) { 144.dp.toPx() }
            val maxXPx = (constraints.maxWidth.toFloat() - cardWidthPx).coerceAtLeast(0f)
            val offsetXPx = (seekFraction * constraints.maxWidth - cardWidthPx / 2f).coerceIn(0f, maxXPx)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.85f),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .offset { IntOffset(offsetXPx.roundToInt(), 0) }
                    .size(width = 144.dp, height = 84.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (previewBitmap != null) {
                        Image(
                            bitmap = previewBitmap!!.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = formatVideoTime(sliderPreviewMs),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    val onValueChange: (Float) -> Unit = { fraction ->
        if (durationMs > 0L) {
            if (!sliderScrubbing) {
                sliderScrubbing = true
                onScrubStart()
            }
            val currentAnchor = anchorFraction
            if (currentAnchor == null) {
                val curFrac = positionMs.toFloat() / durationMs.toFloat()
                if (abs(fraction - curFrac) > 0.25f) {
                    anchorFraction = fraction
                    anchorMs = (fraction * durationMs).toLong()
                    sliderPreviewMs = anchorMs
                } else {
                    anchorFraction = fraction
                    anchorMs = positionMs
                    sliderPreviewMs = positionMs
                }
            } else {
                val delta = ((fraction - currentAnchor) * durationMs * 0.65f).toLong()
                sliderPreviewMs = (anchorMs + delta).coerceIn(0L, durationMs)
            }
        }
    }
    val onValueChangeFinished: () -> Unit = {
        val target = sliderPreviewMs
        sliderScrubbing = false
        sliderPreviewMs = -1L
        anchorFraction = null
        onScrubStop(target)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Text(
                text = formatVideoTime(displayPosMs),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        if (classicStyle) {
            Slider(
                value = seekFraction,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
                enabled = durationMs > 0L,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = videoAccentColor,
                    inactiveTrackColor = Color.White.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )
        } else {
            WavySliderExpressive(
                value = { seekFraction },
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
                enabled = durationMs > 0L,
                activeTrackColor = videoAccentColor,
                inactiveTrackColor = Color.White.copy(alpha = 0.35f),
                thumbColor = Color.White,
                isPlaying = isPlaying,
                thumbRadius = 7.dp,
                thumbLineHeightWhenInteracting = 16.dp,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Text(
                text = formatVideoTime(durationMs),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.padding(end = 4.dp)
            )
        }
    }
}

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
    VideoControlButton("background", Icons.Rounded.Headphones, R.string.video_background_play),
    VideoControlButton("speed", Icons.Rounded.Speed, R.string.video_speed)
)

@Composable
private fun VideoRoundControlButton(
    button: VideoControlButton,
    isActive: Boolean,
    labelOverride: String?,
    circleText: String? = null,
    iconOverride: ImageVector? = null,
    ringProgress: Float? = null,
    customContent: (@Composable BoxScope.() -> Unit)? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "roundButtonPress"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .width(54.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(44.dp)
        ) {
            if (ringProgress != null) {
                Canvas(modifier = Modifier.size(44.dp)) {
                    drawArc(
                        color = videoAccentColor,
                        startAngle = -90f,
                        sweepAngle = 360f * ringProgress.coerceIn(0f, 1f),
                        useCenter = false,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
            Surface(
                onClick = onClick,
                interactionSource = interactionSource,
                shape = CircleShape,
                color = if (isActive) videoAccentColor else Color.White.copy(alpha = 0.14f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (customContent != null) {
                        customContent()
                    } else if (circleText != null) {
                        Text(
                            text = circleText,
                            color = if (isActive) Color.Black else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    } else if (button.id == "night") {
                        AnimatedContent(
                            targetState = isActive,
                            transitionSpec = {
                                (fadeIn() + scaleIn(initialScale = 0.6f)) togetherWith
                                    (fadeOut() + scaleOut(targetScale = 0.6f))
                            },
                            label = "nightMorph"
                        ) { on ->
                            Icon(
                                imageVector = if (on) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay,
                                contentDescription = null,
                                tint = if (on) Color.Black else Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = iconOverride ?: button.icon,
                            contentDescription = null,
                            tint = if (isActive) Color.Black else Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
        Text(
            text = labelOverride ?: stringResource(button.labelRes),
            color = Color.White,
            fontSize = 9.5.sp,
            lineHeight = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Round 20: swipe-to-close for the quick-action row. This observes only — it
 * never consumes the drag, so the LazyRow underneath keeps scrolling normally
 * on slow drags (back/forward through its buttons). A fast fling toward the
 * physical right wall (screen +x) closes the row instead.
 */
private fun Modifier.quickRowSwipeToClose(
    enabled: Boolean,
    onClose: () -> Unit
): Modifier = if (!enabled) this else this.pointerInput(onClose) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val tracker = VelocityTracker()
        tracker.addPosition(down.uptimeMillis, down.position)
        var totalDx = 0f
        var lastPosition = down.position
        while (true) {
            val event = awaitPointerEvent()
            val pressed = event.changes.filter { it.pressed }
            if (pressed.isEmpty()) break
            val change = pressed.firstOrNull { it.id == down.id } ?: pressed.first()
            totalDx += change.position.x - lastPosition.x
            lastPosition = change.position
            tracker.addPosition(change.uptimeMillis, change.position)
        }
        val velocityX = runCatching { tracker.calculateVelocity().x }.getOrDefault(0f)
        if (velocityX > 900f && totalDx > 24f) {
            onClose()
        }
    }
}

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
            imageVector = if (open) Icons.Rounded.KeyboardArrowRight else Icons.Rounded.KeyboardArrowLeft,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = modifier
        )
    }
}

@Composable
private fun VideoRowArrowButton(
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = modifier.width(50.dp)
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.14f),
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                VideoControlsArrowIcon(expanded = expanded, modifier = Modifier.size(20.dp))
            }
        }
        Text(
            text = stringResource(
                if (expanded) R.string.video_player_hide_controls else R.string.video_player_more_controls
            ),
            color = Color.White,
            fontSize = 9.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

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
        color = if (isActive) videoAccentColor else Color.Black.copy(alpha = 0.52f),
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isActive) Color.Black else Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun ScreenshotDoneCheck() {
    val scale = remember { Animatable(0.5f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = spring(dampingRatio = 0.55f, stiffness = 300f))
    }
    Icon(
        imageVector = Icons.Rounded.Check,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier
            .size(20.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
    )
}

@Composable
private fun VideoSkip15Button(
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
    size: Dp = 38.dp,
    fontSize: TextUnit = 11.sp
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

private fun formatVideoTime(positionMs: Long): String {
    val totalSeconds = (positionMs / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)
}

private fun formatSpeedLabel(speed: Float): String {
    val text = if (speed == speed.toInt().toFloat()) speed.toInt().toString() else speed.toString()
    return "${text}×"
}
