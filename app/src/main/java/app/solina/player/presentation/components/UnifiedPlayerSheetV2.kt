package app.solina.player.presentation.components

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.window.core.layout.WindowSizeClass
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import app.solina.player.presentation.viewmodel.AppearanceViewModel
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import app.solina.player.data.model.Song
import app.solina.player.data.preferences.NavBarStyle
import app.solina.player.data.preferences.sanitizeNavBarCornerRadius
import app.solina.player.presentation.components.scoped.PlayerAlbumNavigationEffect
import app.solina.player.presentation.components.scoped.PlayerArtistNavigationEffect
import app.solina.player.presentation.components.scoped.PlayerSheetPredictiveBackHandler
import app.solina.player.presentation.components.scoped.QueuePredictiveBackHandler
import app.solina.player.presentation.components.scoped.QueueSheetRuntimeEffects
import app.solina.player.presentation.components.scoped.SheetMotionController
import app.solina.player.presentation.components.scoped.computeShouldRenderQueueHost
import app.solina.player.presentation.components.scoped.miniPlayerHorizontalGesture
import app.solina.player.presentation.components.scoped.playerContentOffsetXPx
import app.solina.player.presentation.components.scoped.playerContentWidthPx
import app.solina.player.presentation.components.scoped.playerSheetVerticalDragGesture
import app.solina.player.presentation.components.scoped.rememberFullPlayerCompositionPolicy
import app.solina.player.presentation.components.scoped.rememberFullPlayerVisualState
import app.solina.player.presentation.components.scoped.rememberMiniPlayerDismissGestureHandler
import app.solina.player.presentation.components.scoped.rememberMiniPlayerSkipGestureHandler
import app.solina.player.presentation.components.scoped.rememberPrewarmFullPlayer
import app.solina.player.presentation.components.scoped.rememberQueueSheetState
import app.solina.player.presentation.components.scoped.rememberSheetActionHandlers
import app.solina.player.presentation.components.scoped.rememberSheetBackAndDragState
import app.solina.player.presentation.components.scoped.rememberSheetInteractionState
import app.solina.player.presentation.components.scoped.rememberSheetModalOverlayController
import app.solina.player.presentation.components.scoped.rememberSheetOverlayState
import app.solina.player.presentation.components.scoped.rememberSheetThemeState
import app.solina.player.presentation.components.scoped.rememberSheetVisualState
import app.solina.player.presentation.components.scoped.sheetHorizontalPaddingPx
import app.solina.player.presentation.viewmodel.PlayerSheetState
import app.solina.player.presentation.viewmodel.PlayerViewModel
import app.solina.player.presentation.viewmodel.StablePlayerState
import app.solina.player.ui.theme.LocalSolinaPlayerAmoledTheme
import app.solina.player.ui.theme.LocalSolinaPlayerDarkTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private data class PlayerUiSheetSliceV2(
    val currentQueueSourceName: String = "",
    val preparingSongId: String? = null
)

/**
 * V2 real host: no longer delegates to the legacy `UnifiedPlayerSheet`.
 *
 * This path keeps behavior parity, but now owns its own runtime wiring so we can
 * profile and optimize V2 independently while preserving the Experimental switch.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun UnifiedPlayerSheetV2(
    playerViewModel: PlayerViewModel,
    sheetCollapsedTargetY: Float,
    containerHeight: Dp,
    collapsedStateHorizontalPadding: Dp = 12.dp,
    collapsedStateHorizontalPaddingStart: Dp = collapsedStateHorizontalPadding,
    collapsedStateHorizontalPaddingEnd: Dp = collapsedStateHorizontalPadding,
    navController: NavHostController,
    hideMiniPlayer: Boolean = false,
    isNavBarHidden: Boolean = false,
    containerWidth: Dp = Dp.Unspecified,
    // Appearance customisation (bug-fix round 2026-11-17, item 6):
    // a chosen floating-music-bar colour; null = follow the theme.
    miniPlayerSchemeOverride: ColorScheme? = null,
    // Glass & blur (blur-engine test build, 2026-11-17): the floating
    // bar's glass settings; also drives the player-area background
    // transparency below.
    appearanceViewModel: AppearanceViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestContext by rememberUpdatedState(context)

    val writePermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        playerViewModel.onWritePermissionResult(result.resultCode == android.app.Activity.RESULT_OK)
    }

    val deletePermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        playerViewModel.onDeletePermissionResult(result.resultCode == android.app.Activity.RESULT_OK)
    }

    LaunchedEffect(playerViewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch {
                playerViewModel.toastEvents.collect { message ->
                    Toast.makeText(latestContext, message, Toast.LENGTH_SHORT).show()
                }
            }
            launch {
                playerViewModel.writePermissionRequest.collect { intentSender ->
                    writePermissionLauncher.launch(
                        androidx.activity.result.IntentSenderRequest.Builder(intentSender).build()
                    )
                }
            }
            launch {
                playerViewModel.deletePermissionRequest.collect { intentSender ->
                    deletePermissionLauncher.launch(
                        androidx.activity.result.IntentSenderRequest.Builder(intentSender).build()
                    )
                }
            }
        }
    }

    val infrequentPlayerStateReference = playerViewModel.stablePlayerState.collectAsStateWithLifecycle()
    val infrequentPlayerState = infrequentPlayerStateReference.value
    val currentBackStackEntry by navController.currentBackStackEntryAsState()

    val currentPositionState = playerViewModel.currentPlaybackPosition.collectAsStateWithLifecycle()
    val positionToDisplayProvider = remember(currentPositionState) {
        { currentPositionState.value }
    }

    val isFavorite by playerViewModel.isCurrentSongFavorite.collectAsStateWithLifecycle()

    val playerUiSheetSlice by remember {
        playerViewModel.playerUiState
            .map { state ->
                PlayerUiSheetSliceV2(
                    currentQueueSourceName = state.currentQueueSourceName,
                    preparingSongId = state.preparingSongId
                )
            }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = PlayerUiSheetSliceV2())
    val currentQueueSourceName = playerUiSheetSlice.currentQueueSourceName
    val preparingSongId = playerUiSheetSlice.preparingSongId

    val currentSheetContentState by playerViewModel.sheetState.collectAsStateWithLifecycle()
    val predictiveBackCollapseProgress by playerViewModel.predictiveBackCollapseFraction.collectAsStateWithLifecycle()
    val predictiveBackSwipeEdge by playerViewModel.predictiveBackSwipeEdge.collectAsStateWithLifecycle()
    val prewarmFullPlayer = rememberPrewarmFullPlayer(infrequentPlayerState.currentSong?.id)

    val playerConfig by playerViewModel.playerConfigSlice.collectAsStateWithLifecycle()
    val navBarCornerRadius = sanitizeNavBarCornerRadius(playerConfig.navBarCornerRadius)
    val navBarStyle = playerConfig.navBarStyle

    // Round 16, item 6: the floating music bar's corner settings.
    // The sheet's shape carries ONE top radius (both top corners)
    // and ONE bottom radius (both bottom corners), so the two top
    // sliders average into one top radius and the two bottom sliders
    // into one bottom radius — the Appearance screen's note says so.
    // The radii only shape the COLLAPSED mini bar: the sheet lerps
    // them to 0 dp as the player expands (see the provider scaling
    // below), so the expanded player stays square.
    val floatingBarAppearance by appearanceViewModel.appearance.collectAsStateWithLifecycle()
    val floatingBarCorners = floatingBarAppearance.effectiveFloatingMusicBar.effectiveCorners
    val floatingTopRadiusState = rememberUpdatedState(
        ((floatingBarCorners.topStart + floatingBarCorners.topEnd) / 2).dp
    )
    val floatingBottomRadiusState = rememberUpdatedState(
        ((floatingBarCorners.bottomStart + floatingBarCorners.bottomEnd) / 2).dp
    )
    // The sheet's own collapsed targets (mirrors
    // rememberSheetVisualState — top follows the nav-bar radius in
    // the default style, 32 dp otherwise; bottom is 10 dp in the
    // default style, 32 dp otherwise). The wrappers below scale the
    // providers against these, so a user value of 32/10 reproduces
    // today's shape exactly.
    val collapsedSheetTopRadiusState = rememberUpdatedState(
        if (isNavBarHidden) 32.dp
        else if (navBarStyle == NavBarStyle.DEFAULT) navBarCornerRadius.dp
        else if (navBarStyle == NavBarStyle.FULL_WIDTH) 32.dp
        else navBarCornerRadius.dp
    )
    val collapsedSheetBottomRadiusState = rememberUpdatedState(
        if (isNavBarHidden) 32.dp
        else if (navBarStyle == NavBarStyle.DEFAULT) 10.dp
        else if (navBarStyle == NavBarStyle.FULL_WIDTH) 32.dp
        else navBarCornerRadius.dp
    )
    val carouselStyle = playerConfig.carouselStyle
    val fullPlayerLoadingTweaks = playerConfig.fullPlayerLoadingTweaks
    val tapBackgroundClosesPlayer = playerConfig.tapBackgroundClosesPlayer
    val useSmoothCorners = playerConfig.useSmoothCorners
    val playerThemePreference = playerConfig.playerThemePreference
    // Horizontal swipes skip tracks and dismissal moves to a downward swipe.
    val miniPlayerSwipeToSkip = playerConfig.miniPlayerSwipeToSkip

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val scope = rememberCoroutineScope()

    val offsetAnimatable = remember { Animatable(0f) }
    val screenWidthPx = remember(containerWidth, configuration, density) {
        if (containerWidth != Dp.Unspecified && containerWidth > 0.dp) {
            with(density) { containerWidth.toPx() }
        } else {
            with(density) { configuration.screenWidthDp.dp.toPx() }
        }
    }
    val dismissThresholdPx = remember(screenWidthPx) { screenWidthPx * 0.4f }

    // Swipe-to-skip mode. Both live in Animatables that are only ever read inside
    // graphicsLayer, so skipping a track is a draw-phase change and never recomposes the sheet.
    val skipOffsetX = remember { Animatable(0f) }
    val skipContentAlpha = remember { Animatable(1f) }
    // Downward dismiss replaces the horizontal one while swipe-to-skip is on. Travel is the
    // distance from the collapsed card's top edge to just past the bottom of the container.
    val collapsedDismissOffsetY = remember { Animatable(0f) }
    val collapsedDismissTravelPx = remember(containerHeight, sheetCollapsedTargetY, density) {
        with(density) {
            (containerHeight.toPx() - sheetCollapsedTargetY + 24.dp.toPx())
                .coerceAtLeast(MiniPlayerHeight.toPx())
        }
    }

    // Only the dismiss tracks feed this: the corner-radius morph it drives has to be resolved at
    // composition, and swipe-to-skip is a frequent gesture that must stay out of that path.
    val swipeDismissProgress by remember(dismissThresholdPx, collapsedDismissTravelPx) {
        derivedStateOf {
            val horizontal =
                if (dismissThresholdPx == 0f) 0f
                else abs(offsetAnimatable.value) / dismissThresholdPx
            val vertical = collapsedDismissOffsetY.value / collapsedDismissTravelPx
            maxOf(horizontal, vertical).coerceIn(0f, 1f)
        }
    }

    // A preference flip mid-gesture would otherwise strand the card off-centre or faded out.
    LaunchedEffect(miniPlayerSwipeToSkip) {
        if (skipOffsetX.value != 0f) skipOffsetX.snapTo(0f)
        if (skipContentAlpha.value != 1f) skipContentAlpha.snapTo(1f)
        if (collapsedDismissOffsetY.value != 0f) collapsedDismissOffsetY.snapTo(0f)
        if (offsetAnimatable.value != 0f) offsetAnimatable.snapTo(0f)
    }

    val screenHeightPx = remember(configuration, density) {
        with(density) { configuration.screenHeightDp.dp.toPx() }
    }
    val miniPlayerContentHeightPx = remember { with(density) { MiniPlayerHeight.toPx() } }

    val isOutputConnecting = false
    val showPlayerContentArea by remember(infrequentPlayerState.currentSong) {
        derivedStateOf { infrequentPlayerState.currentSong != null }
    }

    val playerContentExpansionFraction = playerViewModel.playerContentExpansionFraction
    val visualOvershootScaleY = remember { Animatable(1f) }
    val initialFullPlayerOffsetY = remember(density) { with(density) { 24.dp.toPx() } }
    val sheetAnimationSpec = remember {
        tween<Float>(durationMillis = ANIMATION_DURATION_MS, easing = FastOutSlowInEasing)
    }
    val sheetAnimationMutex = remember { MutatorMutex() }
    val sheetExpandedTargetY = 0f
    val initialY =
        if (currentSheetContentState == PlayerSheetState.COLLAPSED) sheetCollapsedTargetY
        else sheetExpandedTargetY
    val currentSheetTranslationY = remember { Animatable(initialY) }
    val sheetMotionController = remember(
        currentSheetTranslationY,
        playerContentExpansionFraction,
        sheetAnimationMutex,
        sheetAnimationSpec
    ) {
        SheetMotionController(
            translationY = currentSheetTranslationY,
            expansionFraction = playerContentExpansionFraction,
            mutex = sheetAnimationMutex,
            defaultAnimationSpec = sheetAnimationSpec,
            expandedY = sheetExpandedTargetY
        )
    }

    PlayerArtistNavigationEffect(
        navController = navController,
        sheetCollapsedTargetY = sheetCollapsedTargetY,
        sheetMotionController = sheetMotionController,
        playerViewModel = playerViewModel
    )
    PlayerAlbumNavigationEffect(
        navController = navController,
        sheetCollapsedTargetY = sheetCollapsedTargetY,
        sheetMotionController = sheetMotionController,
        playerViewModel = playerViewModel
    )

    val fullPlayerVisualState = rememberFullPlayerVisualState(
        expansionFraction = playerContentExpansionFraction,
        initialOffsetY = initialFullPlayerOffsetY
    )
    val fullPlayerCompositionPolicy = rememberFullPlayerCompositionPolicy(
        currentSongId = infrequentPlayerState.currentSong?.id,
        currentSheetState = currentSheetContentState,
        expansionFraction = playerContentExpansionFraction
    )
    val shouldRenderFullPlayer = fullPlayerCompositionPolicy.shouldRenderFullPlayer

    DisposableEffect(shouldRenderFullPlayer) {
        playerViewModel.setSliderUiMounted(shouldRenderFullPlayer)
        onDispose { playerViewModel.setSliderUiMounted(false) }
    }

    suspend fun animatePlayerSheet(
        targetExpanded: Boolean,
        animationSpec: androidx.compose.animation.core.AnimationSpec<Float> = sheetAnimationSpec,
        initialVelocity: Float = 0f
    ) {
        sheetMotionController.animateTo(
            targetExpanded = targetExpanded,
            canExpand = showPlayerContentArea,
            collapsedY = sheetCollapsedTargetY,
            animationSpec = animationSpec,
            initialVelocity = initialVelocity
        )
    }

    LaunchedEffect(sheetCollapsedTargetY, sheetMotionController) {
        sheetMotionController.syncToExpansion(sheetCollapsedTargetY)
    }

    var previousSheetState by remember { mutableStateOf(currentSheetContentState) }
    LaunchedEffect(showPlayerContentArea, currentSheetContentState) {
        val targetExpanded = showPlayerContentArea && currentSheetContentState == PlayerSheetState.EXPANDED
        val shouldBounceCollapse =
            showPlayerContentArea &&
                previousSheetState == PlayerSheetState.EXPANDED &&
                currentSheetContentState == PlayerSheetState.COLLAPSED

        previousSheetState = currentSheetContentState
        animatePlayerSheet(targetExpanded = targetExpanded)

        if (showPlayerContentArea) {
            scope.launch {
                visualOvershootScaleY.snapTo(1f)
                if (targetExpanded) {
                    visualOvershootScaleY.animateTo(
                        targetValue = 1f,
                        animationSpec = keyframes {
                            durationMillis = 50
                            1.0f at 0
                            1.05f at 125
                            1.0f at 250
                        }
                    )
                } else if (shouldBounceCollapse) {
                    visualOvershootScaleY.snapTo(0.96f)
                    visualOvershootScaleY.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                } else {
                    visualOvershootScaleY.snapTo(1f)
                }
            }
        } else {
            scope.launch { visualOvershootScaleY.snapTo(1f) }
        }
    }

    val sheetVisualState = rememberSheetVisualState(
        showPlayerContentArea = showPlayerContentArea,
        collapsedStateHorizontalPadding = collapsedStateHorizontalPadding,
        collapsedStateHorizontalPaddingStart = collapsedStateHorizontalPaddingStart,
        collapsedStateHorizontalPaddingEnd = collapsedStateHorizontalPaddingEnd,
        predictiveBackCollapseProgress = predictiveBackCollapseProgress,
        predictiveBackSwipeEdge = predictiveBackSwipeEdge,
        currentSheetContentState = currentSheetContentState,
        playerContentExpansionFraction = playerContentExpansionFraction,
        containerHeight = containerHeight,
        currentSheetTranslationY = currentSheetTranslationY,
        sheetCollapsedTargetY = sheetCollapsedTargetY,
        navBarStyle = navBarStyle,
        navBarCornerRadiusDp = navBarCornerRadius.dp,
        isNavBarHidden = isNavBarHidden,
        isPlaying = infrequentPlayerState.isPlaying,
        hasCurrentSong = infrequentPlayerState.currentSong != null,
        swipeDismissProgress = swipeDismissProgress
    )
    val currentBottomPadding = sheetVisualState.currentBottomPadding
    val baseBottomPadding = sheetVisualState.baseBottomPadding
    val playerContentAreaHeightPxProvider = sheetVisualState.playerContentAreaHeightPxProvider
    val visualSheetTranslationYProvider = sheetVisualState.visualSheetTranslationYProvider
    // Round 16, item 6: scale the sheet's radius providers by the
    // user's floating-bar corners. Each provider lerps its collapsed
    // target to 0 dp as the player expands, so scaling the OUTPUT by
    // (user target / sheet target) keeps the lerp exact — collapsed
    // the bar shows the user's corners, expanded it is square, and
    // mid-drag it tracks the expansion. The corner states are read
    // through rememberUpdatedState, so a slider drag takes effect
    // without recomposing the sheet. When the sheet target is 0 dp
    // the output stays 0 dp: corners stay bounded by the sheet math.
    val baseTopRadiusProvider = sheetVisualState.overallSheetTopCornerRadiusProvider
    val baseBottomRadiusProvider = sheetVisualState.playerContentActualBottomRadiusProvider
    val overallSheetTopCornerRadiusProvider: () -> Dp = remember(baseTopRadiusProvider) {
        {
            scaleSheetRadius(
                base = baseTopRadiusProvider(),
                sheetTarget = collapsedSheetTopRadiusState.value,
                userTarget = floatingTopRadiusState.value
            )
        }
    }
    val playerContentActualBottomRadiusProvider: () -> Dp = remember(baseBottomRadiusProvider) {
        {
            scaleSheetRadius(
                base = baseBottomRadiusProvider(),
                sheetTarget = collapsedSheetBottomRadiusState.value,
                userTarget = floatingBottomRadiusState.value
            )
        }
    }
    val currentHorizontalPaddingStartPxProvider = sheetVisualState.currentHorizontalPaddingStartPxProvider
    val currentHorizontalPaddingEndPxProvider = sheetVisualState.currentHorizontalPaddingEndPxProvider

    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isMediumOrExpandedWidth =
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    val isCompactHeight =
        !windowSizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val canFitTwoPanes = screenWidthDp >= 660.dp
    val isFoldableTwoPane = isMediumOrExpandedWidth && !isCompactHeight && canFitTwoPanes


    val queueSheetState = rememberQueueSheetState(
        scope = scope,
        screenHeightPx = screenHeightPx,
        density = density,
        currentBottomPadding = currentBottomPadding,
        showPlayerContentArea = showPlayerContentArea,
        currentSheetContentState = currentSheetContentState
    )
    val showQueueSheet = queueSheetState.showQueueSheet
    val allowQueueSheetInteraction = queueSheetState.allowQueueSheetInteraction
    val queueSheetOffset = queueSheetState.queueSheetOffset
    val queueSheetHeightPx = queueSheetState.queueSheetHeightPx
    val queueHiddenOffsetPx = queueSheetState.queueHiddenOffsetPx
    val queueSheetController = queueSheetState.queueSheetController

    LaunchedEffect(isFoldableTwoPane) {
        if (isFoldableTwoPane && showQueueSheet) {
            queueSheetController.animateTo(false)
        }
    }
    val onQueueSheetHeightPxChange = queueSheetState.onQueueSheetHeightPxChange

    val sheetBackAndDragState = rememberSheetBackAndDragState(
        showPlayerContentArea = showPlayerContentArea,
        currentSheetContentState = currentSheetContentState
    )
    val canHandlePlayerBack by remember(
        sheetBackAndDragState.predictiveBackEnabled,
        showQueueSheet,
        isFoldableTwoPane
    ) {
        derivedStateOf {
            sheetBackAndDragState.predictiveBackEnabled &&
                (!showQueueSheet || isFoldableTwoPane)
        }
    }
    val velocityTracker = remember { VelocityTracker() }
    val sheetModalOverlayController = rememberSheetModalOverlayController(
        scope = scope,
        queueSheetController = queueSheetController,
        animationDurationMs = ANIMATION_DURATION_MS,
        onCollapsePlayerSheet = { playerViewModel.collapsePlayerSheet() }
    )
    val pendingSaveQueueOverlay = sheetModalOverlayController.pendingSaveQueueOverlay
    val selectedSongForInfo = sheetModalOverlayController.selectedSongForInfo
    val sheetActionHandlers = rememberSheetActionHandlers(
        scope = scope,
        navController = navController,
        playerViewModel = playerViewModel,
        sheetMotionController = sheetMotionController,
        queueSheetController = queueSheetController,
        sheetModalOverlayController = sheetModalOverlayController,
        sheetCollapsedTargetY = sheetCollapsedTargetY
    )

    val hapticFeedback = LocalHapticFeedback.current
    val miniDismissGestureHandler = rememberMiniPlayerDismissGestureHandler(
        scope = scope,
        density = density,
        hapticFeedback = hapticFeedback,
        offsetAnimatable = offsetAnimatable,
        screenWidthPx = screenWidthPx,
        onDismissPlaylistAndShowUndo = { playerViewModel.dismissPlaylistAndShowUndo() },
        onDismissStarted = { playerViewModel.setMiniPlayerDismissing(true) },
        onPrevious = { playerViewModel.previousSong() },
        onNext = { playerViewModel.nextSong() }
    )
    val miniSkipGestureHandler = rememberMiniPlayerSkipGestureHandler(
        scope = scope,
        hapticFeedback = hapticFeedback,
        translationXAnimatable = skipOffsetX,
        contentAlphaAnimatable = skipContentAlpha,
        screenWidthPx = screenWidthPx,
        onSkipToNext = { playerViewModel.nextSong() },
        // The card has already slid away, so restarting the current track instead of stepping
        // back would read as the gesture having failed.
        onSkipToPrevious = { playerViewModel.forcePreviousSong() }
    )

    QueueSheetRuntimeEffects(
        queueSheetController = queueSheetController,
        queueSheetOffset = queueSheetOffset,
        queueHiddenOffsetPx = queueHiddenOffsetPx,
        showQueueSheet = showQueueSheet,
        allowQueueSheetInteraction = allowQueueSheetInteraction,
        onTopEdgeReached = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    )

    PlayerSheetPredictiveBackHandler(
        enabled = canHandlePlayerBack,
        playerViewModel = playerViewModel,
        sheetCollapsedTargetY = sheetCollapsedTargetY,
        sheetExpandedTargetY = sheetExpandedTargetY,
        sheetMotionController = sheetMotionController,
        animationDurationMs = ANIMATION_DURATION_MS,
        onSwipeEdgeChanged = { playerViewModel.updatePredictiveBackSwipeEdge(it) },
        registrationKey = currentBackStackEntry?.id
    )

    val queuePredictiveBackProgress = remember { Animatable(0f) }
    val sheetOverlayState = rememberSheetOverlayState(
        density = density,
        showPlayerContentArea = showPlayerContentArea,
        hideMiniPlayer = hideMiniPlayer,
        showQueueSheet = showQueueSheet,
        queueHiddenOffsetPx = queueHiddenOffsetPx,
        screenHeightPx = screenHeightPx,
        queueSheetOffset = queueSheetOffset,
        queuePredictiveBackProgress = queuePredictiveBackProgress
    )
    val internalIsKeyboardVisible = sheetOverlayState.internalIsKeyboardVisible
    val actuallyShowSheetContent = sheetOverlayState.actuallyShowSheetContent
    val isQueueVisible = sheetOverlayState.isQueueVisible
    val queueVisualOpenFractionProvider = sheetOverlayState.queueVisualOpenFractionProvider
    val queueScrimAlphaProvider = sheetOverlayState.queueScrimAlphaProvider
    val showQueueScrim = sheetOverlayState.showQueueScrim
    val isQueueOccludingPlayer = sheetOverlayState.isQueueOccludingPlayer
    val shouldRenderQueueHost = computeShouldRenderQueueHost(
        isKeyboardVisible = internalIsKeyboardVisible,
        isQueueShown = showQueueSheet,
        isSongInfoShown = selectedSongForInfo != null
    )
    val isQueueTelemetryActive = showQueueSheet

    LaunchedEffect(showQueueSheet) {
        playerViewModel.updateQueueSheetVisibility(showQueueSheet)
    }
    // Update underlying callbacks in the same composition commit as the sheet handlers. A Flow
    // handoff would leave folder back disabled until LibraryScreen's next recomposition.
    val isPlayerSheetHandlingBack = canHandlePlayerBack || (!isFoldableTwoPane && isQueueVisible && !internalIsKeyboardVisible)
    SideEffect {
        playerViewModel.playerSheetBackCoordinator.updatePlayerSheetHandlingBack(isPlayerSheetHandlingBack)
    }
    DisposableEffect(playerViewModel) {
        onDispose {
            playerViewModel.updateQueueSheetVisibility(false)
            playerViewModel.playerSheetBackCoordinator.updatePlayerSheetHandlingBack(false)
        }
    }

    val activePlayerSchemePair by playerViewModel.activePlayerColorSchemePair.collectAsStateWithLifecycle()
    val themedAlbumArtUri by playerViewModel.currentThemedAlbumArtUri.collectAsStateWithLifecycle()
    val isDarkTheme = LocalSolinaPlayerDarkTheme.current
    val currentSong = infrequentPlayerState.currentSong
    val sheetThemeState = rememberSheetThemeState(
        activePlayerSchemePair = activePlayerSchemePair,
        isDarkTheme = isDarkTheme,
        playerThemePreference = playerThemePreference,
        currentSong = currentSong,
        themedAlbumArtUri = themedAlbumArtUri,
        preparingSongId = preparingSongId,
        systemColorScheme = MaterialTheme.colorScheme,
        isAmoledTheme = LocalSolinaPlayerAmoledTheme.current,
        miniPlayerSchemeOverride = miniPlayerSchemeOverride
    )
    val albumColorScheme = sheetThemeState.albumColorScheme
    val miniPlayerScheme = sheetThemeState.miniPlayerScheme
    val isPreparingPlayback = sheetThemeState.isPreparingPlayback
    val miniReadyAlpha = sheetThemeState.miniReadyAlpha
    val miniAppearScale = sheetThemeState.miniAppearScale
    val playerAreaBackground = sheetThemeState.playerAreaBackground
    // Glass & blur (blur-engine test build, 2026-11-17): the pill
    // background of the floating mini bar IS this player-area
    // background, so for the glass to show through it must go
    // transparent while the sheet is collapsed. The colour animates
    // (no pop) and flips back to solid as the full player expands —
    // the mini bar has already faded out by then.
    val appearance by appearanceViewModel.appearance.collectAsStateWithLifecycle()
    val floatingGlassOn = appearance.glass.enabled && appearance.glass.miniBars
    val animatedPlayerAreaBackground by animateColorAsState(
        targetValue = if (floatingGlassOn && playerContentExpansionFraction.value < 0.5f) {
            Color.Transparent
        } else {
            playerAreaBackground
        },
        label = "playerAreaBackground"
    )
    val isDragging = sheetBackAndDragState.isDragging
    val visualCardShadowElevation by remember(showQueueSheet, miniReadyAlpha, isDragging) {
        derivedStateOf {
            if (
                showQueueSheet ||
                isDragging ||
                playerContentExpansionFraction.isRunning ||
                playerContentExpansionFraction.value > 0.18f
            ) {
                0.dp
            } else {
                (3f * miniReadyAlpha).dp
            }
        }
    }

    val sheetInteractionState = rememberSheetInteractionState(
        scope = scope,
        velocityTracker = velocityTracker,
        sheetMotionController = sheetMotionController,
        playerContentExpansionFraction = playerContentExpansionFraction,
        currentSheetTranslationY = currentSheetTranslationY,
        visualOvershootScaleY = visualOvershootScaleY,
        sheetCollapsedTargetY = sheetCollapsedTargetY,
        sheetExpandedTargetY = sheetExpandedTargetY,
        miniPlayerContentHeightPx = miniPlayerContentHeightPx,
        currentSheetContentState = currentSheetContentState,
        showPlayerContentArea = showPlayerContentArea,
        overallSheetTopCornerRadiusProvider = overallSheetTopCornerRadiusProvider,
        playerContentActualBottomRadiusProvider = playerContentActualBottomRadiusProvider,
        useSmoothCorners = useSmoothCorners,
        isDragging = sheetBackAndDragState.isDragging,
        collapsedDismissOffsetY = collapsedDismissOffsetY,
        collapsedDismissEnabled = miniPlayerSwipeToSkip,
        collapsedDismissTravelPx = collapsedDismissTravelPx,
        onAnimateSheet = { targetExpanded, animationSpec, initialVelocity ->
            if (animationSpec == null) {
                animatePlayerSheet(targetExpanded = targetExpanded)
            } else {
                animatePlayerSheet(
                    targetExpanded = targetExpanded,
                    animationSpec = animationSpec,
                    initialVelocity = initialVelocity
                )
            }
        },
        onExpandSheetState = { playerViewModel.expandPlayerSheet() },
        onCollapseSheetState = { playerViewModel.collapsePlayerSheet() },
        onDraggingChange = sheetBackAndDragState.onDraggingChange,
        onDraggingPlayerAreaChange = sheetBackAndDragState.onDraggingPlayerAreaChange,
        onCollapsedDismissStarted = { playerViewModel.setMiniPlayerDismissing(true) },
        onCollapsedDismissCommitted = { playerViewModel.dismissPlaylistAndShowUndo() }
    )

    if (!actuallyShowSheetContent) return

    val sheetResources = LocalResources.current
    val currentSongTitle = infrequentPlayerState.currentSong?.title
    val playerSheetSemanticsDescription = remember(
        sheetResources,
        currentSheetContentState,
        currentSongTitle
    ) {
        playerSheetContentDescription(sheetResources, currentSheetContentState, currentSongTitle)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(0, visualSheetTranslationYProvider().roundToInt()) }
            .height(containerHeight),
        shadowElevation = 0.dp,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = currentBottomPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (showPlayerContentArea) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                // offsetAnimatable and skipOffsetX are mutually exclusive: the
                                // active horizontal gesture owns one and leaves the other at 0.
                                translationX = offsetAnimatable.value + skipOffsetX.value
                                translationY = collapsedDismissOffsetY.value
                                scaleX = miniAppearScale
                                scaleY = visualOvershootScaleY.value * miniAppearScale
                                alpha = miniReadyAlpha * skipContentAlpha.value
                                transformOrigin = TransformOrigin(0.5f, 1f)
                            }
                            .layout { measurable, constraints ->
                                val targetHeightPx = playerContentAreaHeightPxProvider()
                                    .toInt().coerceAtLeast(0)
                                val startPaddingPx = currentHorizontalPaddingStartPxProvider()
                                    .toInt().coerceAtLeast(0)
                                val endPaddingPx = currentHorizontalPaddingEndPxProvider()
                                    .toInt().coerceAtLeast(0)
                                val innerWidth = (constraints.maxWidth - startPaddingPx - endPaddingPx)
                                    .coerceAtLeast(0)
                                
                                val placeable = measurable.measure(
                                    constraints.copy(
                                        minWidth = innerWidth,
                                        maxWidth = innerWidth,
                                        minHeight = targetHeightPx,
                                        maxHeight = targetHeightPx
                                    )
                                )
                                layout(constraints.maxWidth, targetHeightPx) {
                                    placeable.placeRelative(startPaddingPx, 0)
                                }
                            }
                            .shadow(
                                elevation = visualCardShadowElevation,
                                shape = sheetInteractionState.playerShadowShape,
                                clip = false
                            )
                            .background(
                                color = animatedPlayerAreaBackground,
                                shape = sheetInteractionState.playerShadowShape
                            )
                            .clip(sheetInteractionState.playerShadowShape)
                            .miniPlayerHorizontalGesture(
                                enabled = currentSheetContentState == PlayerSheetState.COLLAPSED,
                                skipMode = miniPlayerSwipeToSkip,
                                dismissHandler = miniDismissGestureHandler,
                                skipHandler = miniSkipGestureHandler
                            )
                            .playerSheetVerticalDragGesture(
                                enabled = sheetInteractionState.canDragSheet,
                                handler = sheetInteractionState.sheetVerticalDragGestureHandler
                            )
                            .clickable(
                                enabled = tapBackgroundClosesPlayer || currentSheetContentState == PlayerSheetState.COLLAPSED,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                playerViewModel.togglePlayerSheetState()
                            }
                            .semantics {
                                contentDescription = playerSheetSemanticsDescription
                                // Language-independent marker the baseline profile benchmarks
                                // find through UiAutomator's resource id.
                                testTagsAsResourceId = true
                                testTag = playerSheetTestTag(currentSheetContentState)
                            }
                    ) {
                        // Measures the player content at the full container height and, while
                        // the player is expanding or expanded, at the sheet's full width, so the
                        // full player keeps a stable layout across the transition and the card
                        // only clips it. See PlayerSheetHorizontalLayout.
                        Box(
                            modifier = Modifier.layout { measurable, constraints ->
                                val targetContentHeightPx = containerHeight.roundToPx()
                                val fraction = playerContentExpansionFraction.value
                                val startPaddingPx =
                                    sheetHorizontalPaddingPx(currentHorizontalPaddingStartPxProvider())
                                val endPaddingPx =
                                    sheetHorizontalPaddingPx(currentHorizontalPaddingEndPxProvider())
                                val contentWidthPx = playerContentWidthPx(
                                    cardInnerWidthPx = constraints.maxWidth,
                                    startPaddingPx = startPaddingPx,
                                    endPaddingPx = endPaddingPx,
                                    expansionFraction = fraction
                                )
                                val placeable = measurable.measure(
                                    Constraints.fixed(contentWidthPx, targetContentHeightPx)
                                )
                                layout(constraints.maxWidth, constraints.maxHeight) {
                                    placeable.placeRelative(
                                        playerContentOffsetXPx(startPaddingPx, fraction),
                                        0
                                    )
                                }
                            }
                        ) {
                            UnifiedPlayerMiniAndFullLayers(
                                currentSong = infrequentPlayerState.currentSong,
                                miniPlayerScheme = miniPlayerScheme,
                                overallSheetTopCornerRadiusProvider = overallSheetTopCornerRadiusProvider,
                                infrequentPlayerState = infrequentPlayerState,
                                isOutputConnecting = isOutputConnecting,
                                isPreparingPlayback = isPreparingPlayback,
                                playerContentExpansionFraction = playerContentExpansionFraction,
                                albumColorScheme = albumColorScheme,
                                bottomSheetOpenFractionProvider = queueVisualOpenFractionProvider,
                                isQueueOccludingPlayer = isQueueOccludingPlayer,
                                fullPlayerVisualState = fullPlayerVisualState,
                                containerHeight = containerHeight,
                                currentQueueSourceName = currentQueueSourceName,
                                currentSheetContentState = currentSheetContentState,
                                carouselStyle = carouselStyle,
                                fullPlayerLoadingTweaks = fullPlayerLoadingTweaks,
                                isSheetDragGestureActive = sheetBackAndDragState.isDraggingPlayerArea,
                                playerViewModel = playerViewModel,
                                currentPositionProvider = positionToDisplayProvider,
                                isFavorite = isFavorite,
                                shouldRenderFullPlayer = shouldRenderFullPlayer,
                                currentHorizontalPaddingStartPxProvider = currentHorizontalPaddingStartPxProvider,
                                currentHorizontalPaddingEndPxProvider = currentHorizontalPaddingEndPxProvider,
                                onShowQueueClicked = sheetActionHandlers.openQueueSheet,
                                onQueueDragStart = sheetActionHandlers.beginQueueDrag,
                                onQueueDrag = sheetActionHandlers.dragQueueBy,
                                onQueueRelease = sheetActionHandlers.endQueueDrag,
                                onSongInfoClick = { song -> sheetActionHandlers.onSelectedSongForInfoChange(song) },
                                // Glass & blur (blur-engine test build): the
                                // floating bar's glass settings.
                                glass = appearance.glass,
                                // Round 15, wave 2: font scale + seek-bar style.
                                appearance = appearance
                            )
                        }
                    }
                }

                UnifiedPlayerPrewarmLayer(
                    prewarmFullPlayer = prewarmFullPlayer && !shouldRenderFullPlayer,
                    currentSong = infrequentPlayerState.currentSong,
                    containerHeight = containerHeight,
                    albumColorScheme = albumColorScheme,
                    currentQueueSourceName = currentQueueSourceName,
                    infrequentPlayerState = infrequentPlayerState,
                    carouselStyle = carouselStyle,
                    fullPlayerLoadingTweaks = fullPlayerLoadingTweaks,
                    playerViewModel = playerViewModel,
                    currentPositionProvider = positionToDisplayProvider,
                    isOutputConnecting = isOutputConnecting,
                    isFavorite = isFavorite,
                    onShowQueueClicked = sheetActionHandlers.openQueueSheet,
                    onQueueDragStart = sheetActionHandlers.beginQueueDrag,
                    onQueueDrag = sheetActionHandlers.dragQueueBy,
                    onQueueRelease = sheetActionHandlers.endQueueDrag,
                    // Glass & blur (blur-engine test build).
                    glass = appearance.glass,
                    // Round 15, wave 2: font scale + seek-bar style.
                    appearance = appearance
                )
            }

            QueuePredictiveBackHandler(
                enabled = !isFoldableTwoPane && isQueueVisible && !internalIsKeyboardVisible,
                progress = queuePredictiveBackProgress,
                animationDurationMs = ANIMATION_DURATION_MS,
                scope = scope
            ) {
                queueSheetController.animateTo(false)
            }

            UnifiedPlayerQueueAndSongInfoHost(
                shouldRenderHost = shouldRenderQueueHost,
                keepQueueSheetWarm = currentSheetContentState == PlayerSheetState.EXPANDED &&
                    !internalIsKeyboardVisible,
                isQueueTelemetryActive = isQueueTelemetryActive,
                albumColorScheme = albumColorScheme,
                queueScrimAlphaProvider = queueScrimAlphaProvider,
                showQueueScrim = showQueueScrim && !isFoldableTwoPane,
                showQueueSheet = showQueueSheet && !isFoldableTwoPane,
                queueHiddenOffsetPx = queueHiddenOffsetPx,
                queueSheetOffset = queueSheetOffset,
                queuePredictiveBackProgress = queuePredictiveBackProgress,
                queueSheetHeightPx = queueSheetHeightPx,
                onQueueSheetHeightPxChange = onQueueSheetHeightPxChange,
                configurationResetKey = configuration,
                currentQueueSourceName = currentQueueSourceName,
                infrequentPlayerState = infrequentPlayerState,
                playerViewModel = playerViewModel,
                selectedSongForInfo = selectedSongForInfo,
                onSelectedSongForInfoChange = sheetActionHandlers.onSelectedSongForInfoChange,
                onAnimateQueueSheet = sheetActionHandlers.animateQueueSheet,
                onBeginQueueDrag = sheetActionHandlers.beginQueueDrag,
                onDragQueueBy = sheetActionHandlers.dragQueueBy,
                onEndQueueDrag = sheetActionHandlers.endQueueDrag,
                onLaunchSaveQueueOverlay = sheetActionHandlers.onLaunchSaveQueueOverlay,
                onNavigateToAlbum = sheetActionHandlers.onNavigateToAlbum,
                onNavigateToArtist = sheetActionHandlers.onNavigateToArtist,
                onNavigateToGenre = sheetActionHandlers.onNavigateToGenre
            )
        }
    }

    UnifiedPlayerSaveQueueLayer(
        pendingOverlay = pendingSaveQueueOverlay,
        onDismissOverlay = { sheetModalOverlayController.dismissSaveQueueOverlay() }
    )
}

/**
 * Round 16, item 6: rescale one sheet radius by the user's floating-bar
 * corners. [base] is the sheet provider's output — its collapsed target
 * lerped towards 0 dp as the player expands — and [sheetTarget] is that
 * collapsed target. Scaling by (userTarget / sheetTarget) keeps the
 * lerp exact: collapsed the bar shows the user's corners, expanded it
 * is square. A zero target or a zero base stays zero, so the corners
 * never escape the sheet's layout math (and the transient swipe-dismiss
 * morph, which lerps between two targets, is scaled the same way).
 */
private fun scaleSheetRadius(base: Dp, sheetTarget: Dp, userTarget: Dp): Dp {
    if (base <= 0.dp || sheetTarget <= 0.dp) return 0.dp
    return (base.value / sheetTarget.value * userTarget.value).dp.coerceAtLeast(0.dp)
}
