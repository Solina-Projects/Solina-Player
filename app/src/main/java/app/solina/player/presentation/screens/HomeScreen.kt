package app.solina.player.presentation.screens

import app.solina.player.presentation.navigation.navigateSafely
import app.solina.player.presentation.navigation.navigateSafelyReplacing
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.window.core.layout.WindowSizeClass
import app.solina.player.R
import app.solina.player.data.cloud.CloudFeatures
import app.solina.player.data.model.Song
import app.solina.player.data.model.VideoItem
import app.solina.player.data.preferences.HomeSectionId
import app.solina.player.presentation.components.BetaInfoBottomSheet
import app.solina.player.presentation.components.ChangelogBottomSheet
import app.solina.player.presentation.components.DailyMixSection
import app.solina.player.presentation.components.EqBars
import app.solina.player.presentation.components.GlassModalBottomSheet
import app.solina.player.presentation.components.HomeGradientTopBar
import app.solina.player.presentation.components.HomeMusicPlaybackBar
import app.solina.player.presentation.components.HomeOptionsBottomSheet
import app.solina.player.presentation.components.HomeQuickShortcutsRow
import app.solina.player.presentation.components.MediaEntryCards
import app.solina.player.presentation.components.RecentlyAddedMusicRow
import app.solina.player.presentation.components.RecentlyAddedVideosRow
import app.solina.player.presentation.components.RecentlyPlayedSection
import app.solina.player.presentation.components.RecentlyPlayedSectionMinSongsToShow
import app.solina.player.presentation.components.RecentlyPlayedSongOptionsSheet
import app.solina.player.presentation.components.SongArtworkTile
import app.solina.player.presentation.components.StatsOverviewCard
import app.solina.player.presentation.components.StreamingProviderSheet
import app.solina.player.presentation.components.VideoMiniPlayerBar
import app.solina.player.presentation.components.VideoMiniPlayerBarKind
import app.solina.player.presentation.components.effectivePalette
import app.solina.player.presentation.components.rememberModalSheetState
import app.solina.player.presentation.components.subcomps.PlayingEqIcon
import app.solina.player.presentation.components.toShape
import app.solina.player.presentation.components.withPalette
import app.solina.player.presentation.model.collectRecentlyPlayedSongIds
import app.solina.player.presentation.model.mapRecentlyPlayedSongs
import app.solina.player.presentation.model.withUpdatedSongs
import app.solina.player.presentation.model.withoutSong
import app.solina.player.presentation.navigation.Screen
import app.solina.player.presentation.recap.RecapHomeCard
import app.solina.player.presentation.utils.HomeMediaFilter
import app.solina.player.presentation.viewmodel.AppearanceViewModel
import app.solina.player.presentation.viewmodel.HomeCustomizationViewModel
import app.solina.player.presentation.viewmodel.HomeMediaViewModel
import app.solina.player.presentation.viewmodel.PlayerViewModel
import app.solina.player.presentation.viewmodel.RecapEntryViewModel
import app.solina.player.presentation.viewmodel.SettingsViewModel
import app.solina.player.presentation.viewmodel.StatsViewModel
import app.solina.player.presentation.viewmodel.StreamingProviderLoginViewModel
import app.solina.player.presentation.viewmodel.VideoViewModel
import app.solina.player.presentation.viewmodel.sharedPlayerViewModel
import app.solina.player.ui.theme.RoundedSans
import app.solina.player.ui.theme.ShapeCache
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

private const val HomeLoadingPlaceholderMinDurationMillis = 1200L
private const val HomeScrollRestoreGraceMillis = 1_500L

/** A Home song a long press opened options for, with the queue that playing it from there uses. */
private data class HomeSongOptions(
    val song: Song,
    val queueSongs: ImmutableList<Song>,
    val queueName: String,
    /** Play builds the full library queue, as tapping a Home library preview does. */
    val playFromLibrary: Boolean = false
)

/**
 * Search pill for Home: read-only tap target that opens unified music + video Search.
 * Clean opaque surface so scrolling content never peeks behind its rounded corners.
 */
@Composable
private fun HomeSearchBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(50.dp),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f)
            .compositeOver(MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = stringResource(R.string.cd_search_icon),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = stringResource(R.string.home_search_placeholder),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    paddingValuesParent: PaddingValues,
    playerViewModel: PlayerViewModel = sharedPlayerViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    streamingLoginViewModel: StreamingProviderLoginViewModel = hiltViewModel(),
    onOpenSidebar: () -> Unit
) {
    val context = LocalContext.current
    val isBenchmarkMode = remember {
        (context as? android.app.Activity)?.intent?.getBooleanExtra("is_benchmark", false) ?: false
    }
    val statsViewModel: StatsViewModel = hiltViewModel()
    val recapEntryViewModel: RecapEntryViewModel = hiltViewModel()
    val homeMediaViewModel: HomeMediaViewModel = hiltViewModel()
    val homeCustomizationViewModel: HomeCustomizationViewModel = hiltViewModel()
    val appearanceViewModel: AppearanceViewModel = hiltViewModel()
    val videoViewModel: VideoViewModel = hiltViewModel()

    val homeSections by homeCustomizationViewModel.sections.collectAsStateWithLifecycle()
    val layoutPrefs by homeCustomizationViewModel.layoutPrefs.collectAsStateWithLifecycle()
    val appearance by appearanceViewModel.appearance.collectAsStateWithLifecycle()

    val rawRecentMusic by homeMediaViewModel.recentMusic.collectAsStateWithLifecycle()
    val audioCount by homeMediaViewModel.audioCount.collectAsStateWithLifecycle()
    val albumCount by homeMediaViewModel.albumCount.collectAsStateWithLifecycle()
    val rawRecentVideos by homeMediaViewModel.recentVideos.collectAsStateWithLifecycle()
    val videoCount by homeMediaViewModel.videoCount.collectAsStateWithLifecycle()
    val videoFolderCount by homeMediaViewModel.videoFolderCount.collectAsStateWithLifecycle()

    val videoProgressMap by videoViewModel.videoProgressMap.collectAsStateWithLifecycle()
    val showVideoMiniBar by videoViewModel.showVideoMiniBar.collectAsStateWithLifecycle()
    val videoResumeUri by videoViewModel.videoResumeUri.collectAsStateWithLifecycle()
    val videoResumePos by videoViewModel.videoResumePositionMs.collectAsStateWithLifecycle()

    val isSectionVisible: (HomeSectionId) -> Boolean = { id ->
        homeSections.firstOrNull { it.id == id.name }?.visible ?: true
    }

    val dailyMixSongs by playerViewModel.dailyMixSongs.collectAsStateWithLifecycle()
    val curatedYourMixSongs by playerViewModel.yourMixSongs.collectAsStateWithLifecycle()
    val homeMixPreviewSongs by playerViewModel.homeMixPreviewSongs.collectAsStateWithLifecycle()
    val playbackHistory by playerViewModel.playbackHistory.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    val usesFallbackHomeMix = remember(curatedYourMixSongs, dailyMixSongs) {
        curatedYourMixSongs.isEmpty() && dailyMixSongs.isEmpty()
    }
    val rawYourMixSongs = remember(curatedYourMixSongs, dailyMixSongs, homeMixPreviewSongs) {
        when {
            curatedYourMixSongs.isNotEmpty() -> curatedYourMixSongs
            dailyMixSongs.isNotEmpty() -> dailyMixSongs
            else -> homeMixPreviewSongs
        }
    }

    // Apply user-toggleable smart filter to exclude ringtones, call recordings & short clips.
    val yourMixSongs = remember(rawYourMixSongs, layoutPrefs.filterShortAudioInMix) {
        HomeMediaFilter.filterMusicForHome(rawYourMixSongs, layoutPrefs.filterShortAudioInMix)
            .toImmutableList()
    }
    val displayRecentMusic = remember(
        rawRecentMusic,
        layoutPrefs.filterShortAudioInMix,
        layoutPrefs.effectiveRecentLimit
    ) {
        HomeMediaFilter.filterMusicForHome(rawRecentMusic, layoutPrefs.filterShortAudioInMix)
            .take(layoutPrefs.effectiveRecentLimit)
            .toImmutableList()
    }
    val displayRecentVideos = remember(rawRecentVideos, layoutPrefs.effectiveRecentLimit) {
        rawRecentVideos.take(layoutPrefs.effectiveRecentLimit)
    }

    var homePlaceholderRefreshGeneration by rememberSaveable { mutableIntStateOf(0) }
    var hasHomeLoadingMinimumElapsed by rememberSaveable(homePlaceholderRefreshGeneration) {
        mutableStateOf(false)
    }
    LaunchedEffect(homePlaceholderRefreshGeneration, yourMixSongs.isEmpty()) {
        if (yourMixSongs.isEmpty()) {
            hasHomeLoadingMinimumElapsed = false
            delay(HomeLoadingPlaceholderMinDurationMillis)
            hasHomeLoadingMinimumElapsed = true
        } else {
            hasHomeLoadingMinimumElapsed = true
        }
    }
    val shouldShowYourMixLoadingPlaceholder = yourMixSongs.isEmpty() && !hasHomeLoadingMinimumElapsed

    val recentSongIds = remember(playbackHistory) {
        collectRecentlyPlayedSongIds(
            playbackHistory = playbackHistory,
            maxItems = 64
        )
    }
    val recentlyPlayedSourceSongsInitialValue = remember(recentSongIds) {
        if (recentSongIds.isEmpty()) persistentListOf<Song>() else null
    }
    val recentlyPlayedSourceSongs by remember(recentSongIds, playerViewModel) {
        playerViewModel.observeSongs(recentSongIds)
            .map<List<Song>, List<Song>?> { it }
    }.collectAsStateWithLifecycle(initialValue = recentlyPlayedSourceSongsInitialValue)
    val latestRecentlyPlayedSongs = remember(playbackHistory, recentlyPlayedSourceSongs) {
        val sourceSongs = recentlyPlayedSourceSongs ?: return@remember emptyList()
        mapRecentlyPlayedSongs(
            playbackHistory = playbackHistory,
            songs = sourceSongs,
            maxItems = 64
        )
    }
    var recentlyPlayedSongs by rememberSaveable { mutableStateOf(latestRecentlyPlayedSongs) }
    val latestRecentlyPlayedSongsState = rememberUpdatedState(latestRecentlyPlayedSongs)
    LaunchedEffect(latestRecentlyPlayedSongs, lifecycleOwner) {
        val isHomeVisible = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        recentlyPlayedSongs = if (recentlyPlayedSongs.isEmpty() || !isHomeVisible) {
            latestRecentlyPlayedSongs
        } else {
            recentlyPlayedSongs.withUpdatedSongs(latestRecentlyPlayedSongs)
        }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                recentlyPlayedSongs = latestRecentlyPlayedSongsState.value
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    val recentlyPlayedQueue = remember(recentlyPlayedSongs) {
        recentlyPlayedSongs.map { it.song }.toImmutableList()
    }

    ReportDrawnWhen {
        yourMixSongs.isNotEmpty() || hasHomeLoadingMinimumElapsed || isBenchmarkMode
    }

    val yourMixSubtitle = stringResource(R.string.home_your_mix_subtitle)
    val yourMixQueueName = stringResource(R.string.home_your_mix_queue_name)
    val recentMusicQueueName = stringResource(R.string.home_recent_music_title)
    val recentlyPlayedQueueName = stringResource(R.string.presentation_batch_b_queue_recently_played)

    val initialCurrentSong = remember(playerViewModel) {
        playerViewModel.stablePlayerState.value.currentSong
    }
    val currentSong by remember(playerViewModel.stablePlayerState) {
        playerViewModel.stablePlayerState.map { it.currentSong }
    }.collectAsStateWithLifecycle(initialValue = initialCurrentSong)
    val isShuffleEnabled by remember(playerViewModel.stablePlayerState) {
        playerViewModel.stablePlayerState
            .map { it.isShuffleEnabled }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = false)
    val isPlaying by remember(playerViewModel.stablePlayerState) {
        playerViewModel.stablePlayerState
            .map { it.isPlaying }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = false)

    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val useNavigationRail = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomGradientHeight = navBarBottom + 18.dp

    var showOptionsBottomSheet by remember { mutableStateOf(false) }
    var showChangelogBottomSheet by remember { mutableStateOf(false) }
    var showBetaInfoBottomSheet by remember { mutableStateOf(false) }
    var showStreamingProviderSheet by remember { mutableStateOf(false) }
    var showYourMixFullSheet by remember { mutableStateOf(false) }
    var songForOptions by remember { mutableStateOf<HomeSongOptions?>(null) }
    var videoForOptions by remember { mutableStateOf<VideoItem?>(null) }
    // Round 22, item 5: inline playback inside a recent-video card.
    // The holder is a @Singleton, so the player survives Home leaving the
    // composition — the URI survives config changes via rememberSaveable.
    var inlineVideoUri by rememberSaveable { mutableStateOf<String?>(null) }
    val openingFullPlayer = remember { mutableStateOf(false) }
    val inlinePlayer by videoViewModel.videoPlaybackHolder.player.collectAsStateWithLifecycle()
    val inlineIsPlaying by videoViewModel.videoPlaybackHolder.isPlaying.collectAsStateWithLifecycle()
    DisposableEffect(Unit) {
        onDispose {
            // Leaving Home stops the inline playback in the card — unless
            // we are opening the full player, which takes over the same
            // shared player.
            if (!openingFullPlayer.value) {
                videoViewModel.videoPlaybackHolder.pause()
            }
        }
    }

    val sheetState = rememberModalSheetState()
    val betaSheetState = rememberModalSheetState()
    val mixSheetState = rememberModalSheetState()
    val videoOptionsSheetState = rememberModalSheetState()
    val scope = rememberCoroutineScope()

    val homeStatsOverviewState by statsViewModel.homeOverviewState.collectAsStateWithLifecycle()
    val recapHomeCard by recapEntryViewModel.homeCard.collectAsStateWithLifecycle()

    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val density = LocalDensity.current
    val scrollThresholdPx = remember(density) { with(density) { 8.dp.toPx() } }
    val isScrolledPastThreshold by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > scrollThresholdPx
        }
    }
    // Show compact search button in TopBar whenever inline search scrolled away or search bar is hidden.
    val showTopBarSearchAction by remember(layoutPrefs.showSearchBar, layoutPrefs.pinSearchBar) {
        derivedStateOf {
            when {
                !layoutPrefs.showSearchBar -> true
                layoutPrefs.pinSearchBar -> false
                else -> listState.firstVisibleItemIndex > 0
            }
        }
    }

    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    var savedScrollIndex by rememberSaveable { mutableIntStateOf(0) }
    var savedScrollOffset by rememberSaveable { mutableIntStateOf(0) }
    var needsScrollRestore by rememberSaveable { mutableStateOf(false) }
    val lifecycleState = lifecycleOwner.lifecycle.currentStateAsState()
    val isHomeResumed by remember(lifecycleState) {
        derivedStateOf { lifecycleState.value.isAtLeast(Lifecycle.State.RESUMED) }
    }

    DisposableEffect(lifecycleOwner, listState) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                savedScrollIndex = listState.firstVisibleItemIndex
                savedScrollOffset = listState.firstVisibleItemScrollOffset
                needsScrollRestore = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(
        needsScrollRestore,
        isHomeResumed,
        yourMixSongs.isNotEmpty(),
        dailyMixSongs.isNotEmpty(),
        recentlyPlayedSongs.size,
        homeStatsOverviewState,
        recapHomeCard
    ) {
        if (!needsScrollRestore || !isHomeResumed) return@LaunchedEffect
        val totalItems = listState.layoutInfo.totalItemsCount
        if (totalItems == 0) return@LaunchedEffect
        val targetIndex = savedScrollIndex.coerceIn(0, (totalItems - 1).coerceAtLeast(0))
        listState.scrollToItem(targetIndex, savedScrollOffset)
        if (listState.firstVisibleItemIndex == savedScrollIndex &&
            listState.firstVisibleItemScrollOffset == savedScrollOffset
        ) {
            needsScrollRestore = false
        }
    }

    LaunchedEffect(needsScrollRestore, isHomeResumed) {
        if (!needsScrollRestore || !isHomeResumed) return@LaunchedEffect
        launch {
            listState.interactionSource.interactions
                .filterIsInstance<DragInteraction.Start>()
                .first()
            needsScrollRestore = false
        }
        delay(HomeScrollRestoreGraceMillis)
        needsScrollRestore = false
    }

    val headerContainerColor by animateColorAsState(
        targetValue = if (isScrolledPastThreshold) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            MaterialTheme.colorScheme.background
        },
        animationSpec = tween(durationMillis = 220),
        label = "home_header_bg"
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                // Unified opaque header surface eliminates content bleeding behind SearchBar corners.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerContainerColor)
                ) {
                    HomeGradientTopBar(
                        onNavigationIconClick = {
                            navController.navigateSafely(Screen.Settings.route)
                        },
                        onMoreOptionsClick = {
                            showChangelogBottomSheet = true
                        },
                        onBetaClick = {
                            showBetaInfoBottomSheet = true
                        },
                        onStreamingClick = {
                            showStreamingProviderSheet = true
                        },
                        onMenuClick = onOpenSidebar,
                        onCustomizeClick = {
                            navController.navigateSafely(Screen.HomeCustomization.route)
                        },
                        onSearchClick = {
                            navController.navigateSafely(Screen.Search.route)
                        },
                        showSearchAction = showTopBarSearchAction,
                        showGreetingHeader = layoutPrefs.showGreetingHeader,
                        showChangelogButton = layoutPrefs.showChangelogInTopBar,
                        showBuildBadge = layoutPrefs.showBuildBadgeInTopBar,
                        isScrolled = isScrolledPastThreshold
                    )
                    if (layoutPrefs.showSearchBar && layoutPrefs.pinSearchBar) {
                        HomeSearchBar(
                            onClick = { navController.navigateSafely(Screen.Search.route) }
                        )
                    }
                    if (isScrolledPastThreshold) {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                        )
                    }
                }
            }
        ) { innerPadding ->
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        settingsViewModel.refreshLibrary()
                        playerViewModel.forceUpdateDailyMix()
                        homeMediaViewModel.refreshVideos()
                        delay(700L)
                        isRefreshing = false
                    }
                },
                state = pullToRefreshState,
                modifier = Modifier.fillMaxSize(),
                indicator = {
                    PullToRefreshDefaults.LoadingIndicator(
                        state = pullToRefreshState,
                        isRefreshing = isRefreshing,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = innerPadding.calculateTopPadding() + 4.dp)
                    )
                }
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding() + 6.dp,
                        bottom = paddingValuesParent.calculateBottomPadding() + navBarBottom + 36.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(layoutPrefs.effectiveSectionSpacingDp.dp)
                ) {
                    // Collapsible inline search bar (default): scrolls naturally and yields to TopBar search icon.
                    if (layoutPrefs.showSearchBar && !layoutPrefs.pinSearchBar) {
                        item(
                            key = "home_inline_search_bar",
                            contentType = "home_inline_search_bar"
                        ) {
                            HomeSearchBar(
                                onClick = { navController.navigateSafely(Screen.Search.route) }
                            )
                        }
                    }

                    var dashboardRowEmitted = false
                    for (section in homeSections) {
                        when (section.id) {
                            HomeSectionId.ENTRY_CARDS.name -> {
                                if (isSectionVisible(HomeSectionId.ENTRY_CARDS)) {
                                    item(
                                        key = "home_entry_cards",
                                        contentType = "home_entry_cards"
                                    ) {
                                        MediaEntryCards(
                                            musicCount = audioCount,
                                            videoCount = videoCount,
                                            albumCount = albumCount,
                                            videoFolderCount = videoFolderCount,
                                            showQuickActions = layoutPrefs.showQuickActionsOnEntryCards,
                                            onMusicClick = {
                                                navController.navigateSafely(Screen.Library.route)
                                            },
                                            onVideoClick = {
                                                navController.navigateSafely(Screen.VideoFolders.route)
                                            },
                                            onQuickPlayMusic = {
                                                playerViewModel.shuffleAllSongs(queueName = yourMixQueueName)
                                            },
                                            onQuickPlayVideo = {
                                                val targetUri = videoResumeUri
                                                    ?: displayRecentVideos.firstOrNull()?.contentUriString
                                                if (targetUri != null) {
                                                    val pos = if (targetUri == videoResumeUri) videoResumePos else 0L
                                                    navController.navigateSafely(
                                                        Screen.VideoPlayer.createRoute(
                                                            startUri = targetUri,
                                                            queueUris = displayRecentVideos.map { it.contentUriString },
                                                            startPositionMs = pos
                                                        )
                                                    )
                                                } else {
                                                    navController.navigateSafely(Screen.VideoFolders.route)
                                                }
                                            },
                                            onCustomizeClick = {
                                                navController.navigateSafely(Screen.HomeCustomization.route)
                                            }
                                        )
                                    }
                                }
                            }
                            HomeSectionId.QUICK_SHORTCUTS.name -> {
                                if (isSectionVisible(HomeSectionId.QUICK_SHORTCUTS)) {
                                    item(
                                        key = "home_quick_shortcuts",
                                        contentType = "home_quick_shortcuts"
                                    ) {
                                        HomeQuickShortcutsRow(
                                            onRecentlyPlayedClick = {
                                                navController.navigateSafely(Screen.RecentlyPlayed.route)
                                            },
                                            onVideoFoldersClick = {
                                                navController.navigateSafely(Screen.VideoFolders.route)
                                            },
                                            onEqualizerClick = {
                                                navController.navigateSafely(Screen.Equalizer.createRoute())
                                            },
                                            onDailyMixClick = {
                                                navController.navigateSafely(Screen.DailyMixScreen.route)
                                            },
                                            onAppearanceClick = {
                                                navController.navigateSafely(Screen.AppearanceCustomization.route)
                                            }
                                        )
                                    }
                                }
                            }
                            HomeSectionId.RECENT_MUSIC.name -> {
                                if (isSectionVisible(HomeSectionId.RECENT_MUSIC)) {
                                    if (displayRecentMusic.isNotEmpty()) {
                                        item(
                                            key = "recent_music_section",
                                            contentType = "recent_music_section"
                                        ) {
                                            RecentlyAddedMusicRow(
                                                songs = displayRecentMusic,
                                                onSongClick = { song ->
                                                    // Round 22, item 4: first tap plays,
                                                    // second tap (same song, already
                                                    // current) opens the full player —
                                                    // navigate FIRST, then expand the
                                                    // sheet (Round 21 mechanism).
                                                    if (currentSong?.id == song.id) {
                                                        if (navController.navigateSafely(Screen.Library.route)) {
                                                            playerViewModel.requestExpandPlayerSheetAfterNavigation()
                                                        }
                                                    } else {
                                                        playerViewModel.playSongs(
                                                            songsToPlay = displayRecentMusic,
                                                            startSong = song,
                                                            queueName = recentMusicQueueName
                                                        )
                                                    }
                                                },
                                                onSongLongClick = { song ->
                                                    songForOptions = HomeSongOptions(
                                                        song = song,
                                                        queueSongs = displayRecentMusic,
                                                        queueName = recentMusicQueueName,
                                                        playFromLibrary = false
                                                    )
                                                },
                                                onPlayAllClick = {
                                                    displayRecentMusic.firstOrNull()?.let { first ->
                                                        playerViewModel.playSongs(
                                                            songsToPlay = displayRecentMusic,
                                                            startSong = first,
                                                            queueName = recentMusicQueueName
                                                        )
                                                    }
                                                },
                                                onOpenAllClick = {
                                                    navController.navigateSafely(Screen.Library.route)
                                                }
                                            )
                                        }
                                    }
                                    // Emit playback bar ONLY when both active and visible — zero empty gaps.
                                    if (currentSong != null && appearance.homeMusicBar.visible) {
                                        item(
                                            key = "home_music_playback_bar",
                                            contentType = "home_music_playback_bar"
                                        ) {
                                            HomeMusicPlaybackBar(
                                                playerViewModel = playerViewModel,
                                                onOpenFullPlayer = {
                                                    if (navController.navigateSafely(Screen.Library.route)) {
                                                        playerViewModel.requestExpandPlayerSheetAfterNavigation()
                                                    }
                                                },
                                                modifier = Modifier.padding(horizontal = 16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            HomeSectionId.RECENT_VIDEOS.name -> {
                                if (isSectionVisible(HomeSectionId.RECENT_VIDEOS)) {
                                    if (displayRecentVideos.isNotEmpty()) {
                                        item(
                                            key = "recent_videos_section",
                                            contentType = "recent_videos_section"
                                        ) {
                                            val recentVideoQueue = remember(displayRecentVideos) {
                                                displayRecentVideos.map { it.contentUriString }
                                            }
                                            RecentlyAddedVideosRow(
                                                videos = displayRecentVideos,
                                                progressMap = videoProgressMap,
                                                showProgressStrip = layoutPrefs.showVideoProgressOnHome,
                                                // Round 22, item 5: inline playback
                                                // inside the tapped card.
                                                inlinePlayingUri = inlineVideoUri,
                                                inlinePlayer = inlinePlayer,
                                                inlineIsPlaying = inlineIsPlaying,
                                                onInlinePlayPause = {
                                                    videoViewModel.videoPlaybackHolder.togglePlayPause()
                                                },
                                                onVideoClick = { video ->
                                                    if (inlineVideoUri == video.contentUriString) {
                                                        // Second tap on the same video:
                                                        // open the full player at the
                                                        // current inline position (restart
                                                        // from 0 if within 1.5 s of the end).
                                                        val position = videoViewModel.videoPlaybackHolder.positionMs.value
                                                        val duration = videoViewModel.videoPlaybackHolder.durationMs.value
                                                        val startPosition = if (duration > 0L && duration - position <= 1500L) {
                                                            0L
                                                        } else {
                                                            position
                                                        }
                                                        openingFullPlayer.value = true
                                                        inlineVideoUri = null
                                                        navController.navigateSafely(
                                                            Screen.VideoPlayer.createRoute(
                                                                startUri = video.contentUriString,
                                                                queueUris = recentVideoQueue,
                                                                startPositionMs = startPosition
                                                            )
                                                        )
                                                    } else {
                                                        // First tap: play inside this
                                                        // card (tapping another video
                                                        // switches the inline playback).
                                                        val savedPos = videoProgressMap[video.contentUriString]?.positionMs ?: 0L
                                                        videoViewModel.videoPlaybackHolder.play(
                                                            video.contentUriString,
                                                            savedPos
                                                        )
                                                        inlineVideoUri = video.contentUriString
                                                    }
                                                },
                                                onVideoLongClick = { video ->
                                                    videoForOptions = video
                                                },
                                                onOpenAllClick = {
                                                    navController.navigateSafely(Screen.VideoFolders.route)
                                                }
                                            )
                                        }
                                    }
                                    // Emit video playback bar ONLY when both active and visible — zero empty gaps.
                                    if (showVideoMiniBar && appearance.homeVideoBar.visible) {
                                        item(
                                            key = "home_video_playback_bar",
                                            contentType = "home_video_playback_bar"
                                        ) {
                                            VideoMiniPlayerBar(
                                                navController = navController,
                                                kind = VideoMiniPlayerBarKind.HOME,
                                                modifier = Modifier.padding(horizontal = 16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            HomeSectionId.YOUR_MIX.name -> {
                                if (isSectionVisible(HomeSectionId.YOUR_MIX)) {
                                    if (yourMixSongs.isEmpty()) {
                                        item(
                                            key = "your_mix_placeholder",
                                            contentType = "your_mix_placeholder"
                                        ) {
                                            if (shouldShowYourMixLoadingPlaceholder) {
                                                YourMixLoadingPlaceholder()
                                            } else {
                                                YourMixEmptyPlaceholder(
                                                    onRefresh = {
                                                        homePlaceholderRefreshGeneration++
                                                        settingsViewModel.refreshLibrary()
                                                        playerViewModel.forceUpdateDailyMix()
                                                    }
                                                )
                                            }
                                        }
                                    } else {
                                        item(
                                            key = "your_mix_hero",
                                            contentType = "your_mix_hero"
                                        ) {
                                            YourMixHeroCard(
                                                songs = yourMixSongs,
                                                subtitle = yourMixSubtitle,
                                                wide = useNavigationRail,
                                                isShuffleEnabled = isShuffleEnabled,
                                                currentSongId = currentSong?.id,
                                                isPlaying = isPlaying,
                                                onPlayShuffled = {
                                                    if (usesFallbackHomeMix) {
                                                        playerViewModel.shuffleAllSongs(queueName = yourMixQueueName)
                                                    } else {
                                                        playerViewModel.playSongsShuffled(
                                                            songsToPlay = yourMixSongs,
                                                            queueName = yourMixQueueName,
                                                            startAtZero = true,
                                                        )
                                                    }
                                                },
                                                onRefreshMix = {
                                                    playerViewModel.forceUpdateDailyMix()
                                                },
                                                onOpenAllMix = {
                                                    showYourMixFullSheet = true
                                                },
                                                onSongClick = { song ->
                                                    if (usesFallbackHomeMix) {
                                                        playerViewModel.showAndPlaySongFromLibrary(song, queueName = yourMixQueueName)
                                                    } else {
                                                        playerViewModel.showAndPlaySong(song, yourMixSongs, yourMixQueueName)
                                                    }
                                                },
                                                onSongLongClick = { song ->
                                                    songForOptions = HomeSongOptions(
                                                        song, yourMixSongs, yourMixQueueName, playFromLibrary = usesFallbackHomeMix
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            HomeSectionId.RECAP.name -> {
                                recapHomeCard?.let { entry ->
                                    item(
                                        key = "recap_home_card",
                                        contentType = "recap_home_card"
                                    ) {
                                        RecapHomeCard(
                                            entry = entry,
                                            onOpen = { navController.navigateSafely(Screen.Recap.createRoute(entry.year)) },
                                            onDismiss = { recapEntryViewModel.dismissHomeCard(entry.year) },
                                            modifier = Modifier.animateItem()
                                        )
                                    }
                                }
                            }
                            HomeSectionId.DAILY_MIX.name -> {
                                if (isSectionVisible(HomeSectionId.DAILY_MIX) && dailyMixSongs.isNotEmpty()) {
                                    if (useNavigationRail) {
                                        if (!dashboardRowEmitted) {
                                            dashboardRowEmitted = true
                                            item(
                                                key = "foldable_dashboard_row",
                                                contentType = "foldable_dashboard_row"
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    Box(modifier = Modifier.weight(1f)) {
                                                        DailyMixSection(
                                                            songs = dailyMixSongs,
                                                            onClickOpen = {
                                                                navController.navigateSafely(Screen.DailyMixScreen.route)
                                                            },
                                                            onNavigateToAlbum = { song ->
                                                                navController.navigateSafelyReplacing(
                                                                    route = Screen.AlbumDetail.createRoute(song.albumId),
                                                                    patternToPop = Screen.AlbumDetail.route
                                                                )
                                                            },
                                                            onNavigateToArtist = { song ->
                                                                navController.navigateSafelyReplacing(
                                                                    route = Screen.ArtistDetail.createRoute(song.artistId),
                                                                    patternToPop = Screen.ArtistDetail.route
                                                                )
                                                            },
                                                            onNavigateToGenre = { song ->
                                                                song.genre?.let {
                                                                    navController.navigateSafely(
                                                                        Screen.GenreDetail.createRoute(
                                                                            java.net.URLEncoder.encode(it, "UTF-8")
                                                                        )
                                                                    )
                                                                }
                                                            },
                                                            playerViewModel = playerViewModel,
                                                            topSpacing = 0.dp
                                                        )
                                                    }
                                                    Box(modifier = Modifier.weight(1f)) {
                                                        StatsOverviewCard(
                                                            state = homeStatsOverviewState,
                                                            onClick = { navController.navigateSafely(Screen.Stats.route) },
                                                            videoCount = videoCount,
                                                            inProgressVideosCount = videoProgressMap.size,
                                                            showVideoSummary = layoutPrefs.showVideoSummaryInStats
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        item(
                                            key = "daily_mix_section",
                                            contentType = "daily_mix_section"
                                        ) {
                                            DailyMixSection(
                                                songs = dailyMixSongs,
                                                onClickOpen = {
                                                    navController.navigateSafely(Screen.DailyMixScreen.route)
                                                },
                                                onNavigateToAlbum = { song ->
                                                    navController.navigateSafelyReplacing(
                                                        route = Screen.AlbumDetail.createRoute(song.albumId),
                                                        patternToPop = Screen.AlbumDetail.route
                                                    )
                                                },
                                                onNavigateToArtist = { song ->
                                                    navController.navigateSafelyReplacing(
                                                        route = Screen.ArtistDetail.createRoute(song.artistId),
                                                        patternToPop = Screen.ArtistDetail.route
                                                    )
                                                },
                                                onNavigateToGenre = { song ->
                                                    song.genre?.let {
                                                        navController.navigateSafely(
                                                            Screen.GenreDetail.createRoute(
                                                                java.net.URLEncoder.encode(it, "UTF-8")
                                                            )
                                                        )
                                                    }
                                                },
                                                playerViewModel = playerViewModel
                                            )
                                        }
                                    }
                                }
                            }
                            HomeSectionId.RECENTLY_PLAYED.name -> {
                                if (isSectionVisible(HomeSectionId.RECENTLY_PLAYED) &&
                                    recentlyPlayedSongs.size >= RecentlyPlayedSectionMinSongsToShow
                                ) {
                                    item(
                                        key = "recently_played_section",
                                        contentType = "recently_played_section"
                                    ) {
                                        RecentlyPlayedSection(
                                            songs = remember(recentlyPlayedSongs) { recentlyPlayedSongs.toImmutableList() },
                                            onSongClick = { song ->
                                                if (recentlyPlayedQueue.isNotEmpty()) {
                                                    playerViewModel.playSongs(
                                                        songsToPlay = recentlyPlayedQueue,
                                                        startSong = song,
                                                        queueName = recentlyPlayedQueueName
                                                    )
                                                }
                                            },
                                            onSongLongClick = { song ->
                                                songForOptions = HomeSongOptions(song, recentlyPlayedQueue, recentlyPlayedQueueName)
                                            },
                                            onOpenAllClick = {
                                                navController.navigateSafely(Screen.RecentlyPlayed.route)
                                            },
                                            themeStateHolder = playerViewModel.themeStateHolder,
                                            currentSongId = currentSong?.id,
                                            contentPadding = PaddingValues(start = 8.dp, end = 24.dp)
                                        )
                                    }
                                }
                            }
                            HomeSectionId.STATS.name -> {
                                if (isSectionVisible(HomeSectionId.STATS)) {
                                    if (useNavigationRail) {
                                        if (!dashboardRowEmitted && dailyMixSongs.isNotEmpty()) {
                                            dashboardRowEmitted = true
                                            item(
                                                key = "foldable_dashboard_row",
                                                contentType = "foldable_dashboard_row"
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    Box(modifier = Modifier.weight(1f)) {
                                                        DailyMixSection(
                                                            songs = dailyMixSongs,
                                                            onClickOpen = {
                                                                navController.navigateSafely(Screen.DailyMixScreen.route)
                                                            },
                                                            onNavigateToAlbum = { song ->
                                                                navController.navigateSafelyReplacing(
                                                                    route = Screen.AlbumDetail.createRoute(song.albumId),
                                                                    patternToPop = Screen.AlbumDetail.route
                                                                )
                                                            },
                                                            onNavigateToArtist = { song ->
                                                                navController.navigateSafelyReplacing(
                                                                    route = Screen.ArtistDetail.createRoute(song.artistId),
                                                                    patternToPop = Screen.ArtistDetail.route
                                                                )
                                                            },
                                                            onNavigateToGenre = { song ->
                                                                song.genre?.let {
                                                                    navController.navigateSafely(
                                                                        Screen.GenreDetail.createRoute(
                                                                            java.net.URLEncoder.encode(it, "UTF-8")
                                                                        )
                                                                    )
                                                                }
                                                            },
                                                            playerViewModel = playerViewModel,
                                                            topSpacing = 0.dp
                                                        )
                                                    }
                                                    Box(modifier = Modifier.weight(1f)) {
                                                        StatsOverviewCard(
                                                            state = homeStatsOverviewState,
                                                            onClick = { navController.navigateSafely(Screen.Stats.route) },
                                                            videoCount = videoCount,
                                                            inProgressVideosCount = videoProgressMap.size,
                                                            showVideoSummary = layoutPrefs.showVideoSummaryInStats
                                                        )
                                                    }
                                                }
                                            }
                                        } else if (dailyMixSongs.isEmpty()) {
                                            item(
                                                key = "listening_stats_preview",
                                                contentType = "listening_stats_preview"
                                            ) {
                                                StatsOverviewCard(
                                                    state = homeStatsOverviewState,
                                                    onClick = { navController.navigateSafely(Screen.Stats.route) },
                                                    videoCount = videoCount,
                                                    inProgressVideosCount = videoProgressMap.size,
                                                    showVideoSummary = layoutPrefs.showVideoSummaryInStats
                                                )
                                            }
                                        }
                                    } else {
                                        item(
                                            key = "listening_stats_preview",
                                            contentType = "listening_stats_preview"
                                        ) {
                                            StatsOverviewCard(
                                                state = homeStatsOverviewState,
                                                onClick = { navController.navigateSafely(Screen.Stats.route) },
                                                videoCount = videoCount,
                                                inProgressVideosCount = videoProgressMap.size,
                                                showVideoSummary = layoutPrefs.showVideoSummaryInStats
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Subtle protective fade behind the Android bottom gesture bar so text never clashes with the pill.
        if (bottomGradientHeight > 0.dp) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(bottomGradientHeight)
                    .background(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                0.45f to MaterialTheme.colorScheme.background.copy(alpha = 0.65f),
                                1.0f to MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )
        }
    }

    if (showOptionsBottomSheet) {
        GlassModalBottomSheet(
            onDismissRequest = { showOptionsBottomSheet = false },
            sheetState = sheetState
        ) {
            HomeOptionsBottomSheet(
                onNavigateToMashup = {
                    scope.launch {
                        sheetState.hide()
                    }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            showOptionsBottomSheet = false
                            navController.navigateSafely(Screen.DJSpace.route)
                        }
                    }
                }
            )
        }
    }
    if (showChangelogBottomSheet) {
        GlassModalBottomSheet(
            onDismissRequest = { showChangelogBottomSheet = false },
            sheetState = sheetState
        ) {
            ChangelogBottomSheet()
        }
    }
    if (showBetaInfoBottomSheet) {
        GlassModalBottomSheet(
            onDismissRequest = { showBetaInfoBottomSheet = false },
            sheetState = betaSheetState,
        ) {
            BetaInfoBottomSheet()
        }
    }
    if (showYourMixFullSheet && yourMixSongs.isNotEmpty()) {
        GlassModalBottomSheet(
            onDismissRequest = { showYourMixFullSheet = false },
            sheetState = mixSheetState
        ) {
            YourMixFullListSheetContent(
                songs = yourMixSongs,
                currentSongId = currentSong?.id,
                isPlaying = isPlaying,
                onPlayShuffled = {
                    showYourMixFullSheet = false
                    if (usesFallbackHomeMix) {
                        playerViewModel.shuffleAllSongs(queueName = yourMixQueueName)
                    } else {
                        playerViewModel.playSongsShuffled(
                            songsToPlay = yourMixSongs,
                            queueName = yourMixQueueName,
                            startAtZero = true
                        )
                    }
                },
                onSongClick = { song ->
                    showYourMixFullSheet = false
                    if (usesFallbackHomeMix) {
                        playerViewModel.showAndPlaySongFromLibrary(song, queueName = yourMixQueueName)
                    } else {
                        playerViewModel.showAndPlaySong(song, yourMixSongs, yourMixQueueName)
                    }
                },
                onSongLongClick = { song ->
                    showYourMixFullSheet = false
                    songForOptions = HomeSongOptions(
                        song, yourMixSongs, yourMixQueueName, playFromLibrary = usesFallbackHomeMix
                    )
                }
            )
        }
    }
    videoForOptions?.let { selectedVideo ->
        val savedPos = videoProgressMap[selectedVideo.contentUriString]?.positionMs ?: 0L
        val recentQueue = displayRecentVideos.map { it.contentUriString }
        GlassModalBottomSheet(
            onDismissRequest = { videoForOptions = null },
            sheetState = videoOptionsSheetState
        ) {
            HomeVideoOptionsSheetContent(
                video = selectedVideo,
                savedPositionMs = savedPos,
                onResumeOrPlay = {
                    videoForOptions = null
                    navController.navigateSafely(
                        Screen.VideoPlayer.createRoute(
                            startUri = selectedVideo.contentUriString,
                            queueUris = recentQueue,
                            startPositionMs = savedPos
                        )
                    )
                },
                onPlayFromStart = {
                    videoForOptions = null
                    navController.navigateSafely(
                        Screen.VideoPlayer.createRoute(
                            startUri = selectedVideo.contentUriString,
                            queueUris = recentQueue,
                            startPositionMs = 0L
                        )
                    )
                },
                onOpenFolders = {
                    videoForOptions = null
                    navController.navigateSafely(Screen.VideoFolders.route)
                }
            )
        }
    }
    if (showStreamingProviderSheet && CloudFeatures.ENABLED) {
        val isNavidromeLoggedIn by streamingLoginViewModel.isNavidromeLoggedIn.collectAsStateWithLifecycle()
        val isJellyfinLoggedIn by streamingLoginViewModel.isJellyfinLoggedIn.collectAsStateWithLifecycle()
        StreamingProviderSheet(
            onDismissRequest = { showStreamingProviderSheet = false },
            isNavidromeLoggedIn = isNavidromeLoggedIn,
            onNavigateToNavidromeDashboard = {
                navController.navigateSafely(Screen.NavidromeDashboard.route)
            },
            isJellyfinLoggedIn = isJellyfinLoggedIn,
            onNavigateToJellyfinDashboard = {
                navController.navigateSafely(Screen.JellyfinDashboard.route)
            }
        )
    }
    songForOptions?.let { (song, queueSongs, queueName, playFromLibrary) ->
        RecentlyPlayedSongOptionsSheet(
            song = song,
            queueSongs = queueSongs,
            queueName = queueName,
            playFromLibrary = playFromLibrary,
            playerViewModel = playerViewModel,
            onDismiss = { songForOptions = null },
            onSongDeleted = { deletedSong ->
                recentlyPlayedSongs = recentlyPlayedSongs.withoutSong(deletedSong.id)
            },
            onNavigateToAlbum = { selectedSong ->
                navController.navigateSafelyReplacing(
                    route = Screen.AlbumDetail.createRoute(selectedSong.albumId),
                    patternToPop = Screen.AlbumDetail.route
                )
            },
            onNavigateToArtist = { artistId ->
                navController.navigateSafelyReplacing(
                    route = Screen.ArtistDetail.createRoute(artistId),
                    patternToPop = Screen.ArtistDetail.route
                )
            },
            onNavigateToGenre = { selectedSong ->
                selectedSong.genre?.let {
                    navController.navigateSafely(
                        Screen.GenreDetail.createRoute(
                            java.net.URLEncoder.encode(it, "UTF-8")
                        )
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun YourMixLoadingPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(256.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator(
            modifier = Modifier.size(128.dp),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun YourMixEmptyPlaceholder(
    onRefresh: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 256.dp)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(76.dp),
                shape = ShapeCache.expressiveHero,
                color = colors.secondaryContainer,
                contentColor = colors.onSecondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_empty_placeholder_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.home_empty_placeholder_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            FilledTonalButton(
                onClick = onRefresh,
                shape = AbsoluteSmoothCornerShape(
                    cornerRadiusTL = 22.dp,
                    smoothnessAsPercentTR = 60,
                    cornerRadiusBR = 22.dp,
                    smoothnessAsPercentTL = 60,
                    cornerRadiusBL = 22.dp,
                    smoothnessAsPercentBR = 60,
                    cornerRadiusTR = 22.dp,
                    smoothnessAsPercentBL = 60,
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.home_empty_placeholder_refresh))
            }
        }
    }
}

/**
 * Redesigned "Your Mix" hero card:
 * - Single-line crisp title that never breaks mid-word ("You / r / Mix")
 * - Respects user Appearance customisation (`yourMixCard` corners & palette)
 * - Refresh button to regenerate the mix + "View all" sheet for the full queue
 * - Artwork strip prioritizes songs with actual album art & renders vivid tonal fallbacks
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun YourMixHeroCard(
    songs: List<Song>,
    subtitle: String,
    wide: Boolean,
    isShuffleEnabled: Boolean,
    currentSongId: String?,
    isPlaying: Boolean,
    onPlayShuffled: () -> Unit,
    onRefreshMix: () -> Unit,
    onOpenAllMix: () -> Unit,
    onSongClick: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
    appearanceViewModel: AppearanceViewModel = hiltViewModel()
) {
    val appearance by appearanceViewModel.appearance.collectAsStateWithLifecycle()
    val mixCardAppearance = appearance.yourMixCard
    val mixPalette = effectivePalette(mixCardAppearance.palette, appearance.homeSectionPalette)
    val colors = mixPalette?.let { MaterialTheme.colorScheme.withPalette(it) }
        ?: MaterialTheme.colorScheme
    val cardShape = mixCardAppearance.effectiveCorners.toShape()

    val totalDurationMs = remember(songs) { songs.sumOf { it.duration } }
    val previewSongs = remember(songs) { songs.take(3) }
    val artSongs = remember(songs) { HomeMediaFilter.prioritizeArtworkSongs(songs, limit = 5) }

    val headerRow: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onOpenAllMix),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_your_mix_queue_name),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = RoundedSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 28.sp,
                        lineHeight = 32.sp
                    ),
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface.copy(alpha = 0.74f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onRefreshMix,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = colors.surface.copy(alpha = 0.55f),
                    contentColor = colors.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = stringResource(R.string.home_mix_refresh_cd),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    val artStrip: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .width((38 + 24 * (artSongs.size - 1).coerceAtLeast(0)).dp)
                ) {
                    artSongs.forEachIndexed { index, song ->
                        SongArtworkTile(
                            songId = song.id,
                            albumArtUrl = song.albumArtUriString,
                            contentDescription = null,
                            shape = CircleShape,
                            iconSize = 18.dp,
                            modifier = Modifier
                                .size(38.dp)
                                .align(Alignment.CenterStart)
                                .offset(x = (24 * index).dp)
                                .border(2.dp, colors.surface, CircleShape)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                EqBars(
                    isPlaying = isPlaying,
                    color = colors.primary,
                    modifier = Modifier.size(width = 22.dp, height = 20.dp)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MixStatChip(
                    text = stringResource(R.string.home_music_count_label, songs.size),
                    onClick = onOpenAllMix
                )
                MixStatChip(
                    text = HomeMediaFilter.formatCompactMixDuration(totalDurationMs),
                    onClick = onOpenAllMix
                )
            }
        }
    }

    val playButton: @Composable () -> Unit = {
        FilledTonalButton(
            onClick = onPlayShuffled,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = if (isShuffleEnabled) colors.primary else colors.tertiaryContainer,
                contentColor = if (isShuffleEnabled) colors.onPrimary else colors.onTertiaryContainer
            )
        ) {
            Icon(
                painter = painterResource(R.drawable.rounded_shuffle_24),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.home_your_mix_play),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    val previewList: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            previewSongs.forEach { song ->
                SongListItemFavs(
                    cardCorners = 16.dp,
                    songId = song.id,
                    title = song.title,
                    artist = song.displayArtist,
                    albumArtUrl = song.albumArtUriString,
                    isPlaying = isPlaying,
                    isCurrentSong = song.id == currentSongId,
                    onClick = { onSongClick(song) },
                    onLongClick = { onSongLongClick(song) }
                )
            }
            if (songs.size > previewSongs.size) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onOpenAllMix,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.home_mix_view_all, songs.size),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = colors.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(cardShape)
            .background(
                brush = Brush.linearGradient(
                    listOf(
                        colors.primaryContainer,
                        colors.secondaryContainer,
                        colors.tertiaryContainer
                    )
                ),
                shape = cardShape
            )
            .padding(18.dp)
    ) {
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(
                    modifier = Modifier.weight(0.46f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    headerRow()
                    artStrip()
                    playButton()
                }
                Column(
                    modifier = Modifier.weight(0.54f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    previewList()
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                headerRow()
                artStrip()
                playButton()
                previewList()
            }
        }
    }
}

@Composable
private fun MixStatChip(
    text: String,
    onClick: () -> Unit = {}
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongListItemFavs(
    modifier: Modifier = Modifier,
    cardCorners: Dp = 12.dp,
    songId: String = "",
    title: String,
    artist: String,
    albumArtUrl: String?,
    isPlaying: Boolean,
    isCurrentSong: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    val containerColor = if (isCurrentSong) colors.primaryContainer.copy(alpha = 0.56f) else colors.surfaceContainer
    val contentColor = if (isCurrentSong) colors.primary else colors.onSurface
    val unknownArtist = stringResource(R.string.home_unknown_artist)
    val cleanArtist = remember(artist, unknownArtist) {
        HomeMediaFilter.cleanArtistDisplay(artist, unknownArtist)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cardCorners))
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
                } else {
                    Modifier.clickable(onClick = onClick)
                }
            ),
        shape = RoundedCornerShape(cardCorners),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(11.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(0.9f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SongArtworkTile(
                    songId = songId.ifEmpty { title },
                    albumArtUrl = albumArtUrl,
                    contentDescription = stringResource(R.string.cd_album_art_for_title, title),
                    shape = RoundedCornerShape(10.dp),
                    iconSize = 22.dp,
                    modifier = Modifier.size(46.dp)
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                        fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = cleanArtist,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                        color = contentColor.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (isCurrentSong) {
                Spacer(Modifier.width(12.dp))
                PlayingEqIcon(
                    modifier = Modifier
                        .weight(0.1f)
                        .size(width = 18.dp, height = 16.dp),
                    color = colors.primary,
                    isPlaying = isPlaying
                )
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun SongListItemFavsWrapper(
    song: Song,
    playerViewModel: PlayerViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stablePlayerState by playerViewModel.stablePlayerState.collectAsStateWithLifecycle()

    SongListItemFavs(
        modifier = modifier,
        cardCorners = 0.dp,
        songId = song.id,
        title = song.title,
        artist = song.displayArtist,
        albumArtUrl = song.albumArtUriString,
        isPlaying = stablePlayerState.isPlaying,
        isCurrentSong = song.id == stablePlayerState.currentSong?.id,
        onClick = onClick
    )
}
