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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    // Night light: dim the screen while playing; restore on leave.
    LaunchedEffect(nightLight, brightness) {
        activity?.window?.let { window ->
            val attributes = window.attributes
            attributes.screenBrightness = if (nightLight) {
                brightness.coerceIn(10, 100) / 100f
            } else {
                -1f
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
                    playerViewRef = this
                }
            },
            update = { playerView ->
                playerView.player = player
                playerView.useController = !controlsLocked
                playerView.resizeMode = when (aspectMode) {
                    "FILL" -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    "ZOOM" -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize()
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
            // ---- Top bar: back, film title, subtitles, audio (dubbing),
            // three-dot menu — and nothing else. It sits below the status
            // bar (statusBarsPadding + extra margin) so the title never
            // clashes with it, in portrait and in fullscreen-landscape.
            if (!isFullscreen) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
                        .padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 4.dp)
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
                        // Subtitles: the file's subtitle tracks, or "off".
                        Box {
                            IconButton(onClick = { showSubtitleMenu = true }) {
                                Icon(
                                    imageVector = Icons.Rounded.ClosedCaption,
                                    contentDescription = stringResource(R.string.video_subtitles),
                                    tint = if (selectedSubtitleTrack >= 0) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
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
                                        MaterialTheme.colorScheme.onSurface
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
                                    contentDescription = null
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
            } else {
                IconButton(
                    onClick = { videoPlayerViewModel.toggleFullscreen() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 12.dp, end = 8.dp)
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FullscreenExit,
                        contentDescription = stringResource(R.string.video_exit_fullscreen),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // ---- Fixed rotation icon: always visible on the right edge,
            // outside the collapsible bar, so rotation is one tap away
            // whether the bar is open or not. ----
            Surface(
                onClick = {
                    forcedLandscape = if (forcedLandscape == true) false else true
                },
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.55f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
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

            // ---- Extra control bar (round buttons, like the reference):
            // hidden by default; the animated arrow handle in front of it
            // slides it in/out. ----
            val arrowRotation by animateFloatAsState(
                targetValue = if (controlsExpanded) 180f else 0f,
                label = "videoControlsArrow"
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(
                    visible = controlsExpanded,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(
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
                                        if (playbackSpeed == 1f) "1×" else "${playbackSpeed}×"
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
                                    },
                                )
                            }
                        }
                        // Speed menu, anchored to the speed round button.
                        DropdownMenu(
                            expanded = showSpeedMenu,
                            onDismissRequest = { showSpeedMenu = false }
                        ) {
                            videoPlayerViewModel.availableSpeeds.forEach { speed ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (speed == 1f) "1×" else "${speed}×"
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
                }
                // The animated arrow handle in front of the bar.
                Surface(
                    onClick = { controlsExpanded = !controlsExpanded },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f),
                    tonalElevation = 4.dp,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowUp,
                        contentDescription = stringResource(
                            if (controlsExpanded) {
                                R.string.video_player_hide_controls
                            } else {
                                R.string.video_player_more_controls
                            }
                        ),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(24.dp)
                            .rotate(arrowRotation)
                    )
                }
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
