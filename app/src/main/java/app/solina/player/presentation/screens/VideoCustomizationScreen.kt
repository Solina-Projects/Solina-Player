package app.solina.player.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import app.solina.player.R
import app.solina.player.presentation.components.MiniPlayerHeight
import app.solina.player.presentation.navigation.Screen
import app.solina.player.presentation.navigation.navigateSafely
import app.solina.player.presentation.viewmodel.AppearanceViewModel
import app.solina.player.presentation.viewmodel.HomeCustomizationViewModel
import app.solina.player.presentation.viewmodel.SettingsViewModel

/**
 * Dedicated Video Customisation hub — directly accessible from Video Folders &
 * Video Library top bars as well as from App Settings.
 * - Tab 0 (Visuals & Cards): Video section palette, video section mini-bar,
 *   Home recent video cards, and watch-progress strip toggle.
 * - Tab 1 (Player & Subtitles): Default aspect ratio, playback speed, decoder,
 *   resume mode, swipe gestures, background playback, and subtitle typography/colour/background.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VideoCustomizationScreen(
    navController: NavController,
    appearanceViewModel: AppearanceViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    homeViewModel: HomeCustomizationViewModel = hiltViewModel()
) {
    val appearance by appearanceViewModel.appearance.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val homeLayoutPrefs by homeViewModel.layoutPrefs.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.video_customize_tab_visuals),
        stringResource(R.string.video_customize_tab_player)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.video_customize_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { appearanceViewModel.resetVideoAppearance() }) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(text = stringResource(R.string.home_customize_reset))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = MiniPlayerHeight +
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                    24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "video_customize_tabs") {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    tabs.forEachIndexed { index, label ->
                        SegmentedButton(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = tabs.size
                            )
                        ) {
                            Text(text = label, maxLines = 1)
                        }
                    }
                }
            }

            if (selectedTab == 0) {
                // Video Section Palette
                item(key = "video_section_palette") {
                    AppearanceSectionCard(
                        title = stringResource(R.string.video_customize_palette_title),
                        subtitle = stringResource(R.string.video_customize_palette_subtitle),
                        onReset = { appearanceViewModel.resetVideoSectionPalette() }
                    ) {
                        PaletteChips(
                            selected = appearance.videoSectionPalette,
                            onSelect = { appearanceViewModel.setVideoSectionPalette(it) },
                            showLabel = false
                        )
                    }
                }

                // Video Section Mini-Bar
                item(key = "video_section_bar") {
                    BarSection(
                        title = stringResource(R.string.appearance_section_video_bar_title),
                        bar = appearance.sectionVideoBar,
                        onChange = { appearanceViewModel.setSectionVideoBar(it) },
                        onReset = { appearanceViewModel.resetSectionVideoBar() }
                    )
                }

                // Video Recent Cards on Home
                item(key = "video_recent_cards") {
                    RecentCardsSection(
                        title = stringResource(R.string.appearance_recent_video_cards_title),
                        recent = appearance.effectiveRecentVideoCards,
                        onChange = { appearanceViewModel.setRecentVideoCards(it) },
                        onReset = { appearanceViewModel.resetRecentVideoCards() }
                    )
                }

                // Watch Progress Strip Toggle
                item(key = "video_progress_strip") {
                    AppearanceSectionCard(
                        title = stringResource(R.string.video_customize_thumbnails_title),
                        onReset = null
                    ) {
                        CustomizationSwitchRow(
                            icon = Icons.Rounded.PlayCircle,
                            title = stringResource(R.string.home_pref_video_progress_title),
                            subtitle = stringResource(R.string.home_pref_video_progress_subtitle),
                            checked = homeLayoutPrefs.showVideoProgressOnHome,
                            onCheckedChange = homeViewModel::setShowVideoProgressOnHome
                        )
                    }
                }

                item(key = "video_global_appearance_link") {
                    CustomizationLinkCard(
                        title = stringResource(R.string.customize_global_appearance_title),
                        subtitle = stringResource(R.string.customize_global_appearance_subtitle),
                        icon = Icons.Rounded.AutoAwesome,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                        iconContainerColor = MaterialTheme.colorScheme.secondary,
                        iconTint = MaterialTheme.colorScheme.onSecondary,
                        onClick = { navController.navigateSafely(Screen.AppearanceCustomization.route) }
                    )
                }
            } else {
                // Playback & Display Defaults
                item(key = "video_player_defaults") {
                    AppearanceSectionCard(
                        title = stringResource(R.string.setcat_video_playback_section),
                        onReset = null
                    ) {
                        ChoiceChips(
                            options = listOf(
                                "FIT" to stringResource(R.string.video_aspect_fit),
                                "FILL" to stringResource(R.string.video_aspect_fill),
                                "ZOOM" to stringResource(R.string.video_aspect_zoom),
                                "STRETCH" to stringResource(R.string.video_aspect_stretch)
                            ),
                            selected = settingsState.videoAspectMode,
                            onSelect = { settingsViewModel.setVideoAspectMode(it) },
                            label = stringResource(R.string.setcat_video_aspect_title)
                        )
                        Spacer(Modifier.height(4.dp))
                        val speedKey = when {
                            settingsState.videoPlaybackSpeed <= 0.625f -> "0.5"
                            settingsState.videoPlaybackSpeed <= 0.875f -> "0.75"
                            settingsState.videoPlaybackSpeed <= 1.125f -> "1.0"
                            settingsState.videoPlaybackSpeed <= 1.375f -> "1.25"
                            settingsState.videoPlaybackSpeed <= 1.75f -> "1.5"
                            else -> "2.0"
                        }
                        ChoiceChips(
                            options = listOf(
                                "0.5" to "0.5×",
                                "0.75" to "0.75×",
                                "1.0" to "1×",
                                "1.25" to "1.25×",
                                "1.5" to "1.5×",
                                "2.0" to "2×"
                            ),
                            selected = speedKey,
                            onSelect = { settingsViewModel.setVideoPlaybackSpeed(it.toFloat()) },
                            label = stringResource(R.string.setcat_video_default_speed_title)
                        )
                        Spacer(Modifier.height(4.dp))
                        ChoiceChips(
                            options = listOf(
                                "always" to stringResource(R.string.setcat_video_resume_always),
                                "ask" to stringResource(R.string.setcat_video_resume_ask),
                                "start_over" to stringResource(R.string.setcat_video_resume_start_over)
                            ),
                            selected = settingsState.videoResumeBehaviour,
                            onSelect = { settingsViewModel.setVideoResumeBehaviour(it) },
                            label = stringResource(R.string.setcat_video_resume_title)
                        )
                        Spacer(Modifier.height(4.dp))
                        ChoiceChips(
                            options = listOf(
                                "auto" to stringResource(R.string.setcat_video_decoder_auto),
                                "hardware" to stringResource(R.string.setcat_video_decoder_hardware),
                                "software" to stringResource(R.string.setcat_video_decoder_software)
                            ),
                            selected = settingsState.videoDecoderMode,
                            onSelect = { settingsViewModel.setVideoDecoderMode(it) },
                            label = stringResource(R.string.setcat_video_decoder_title)
                        )
                    }
                }

                // Controls & Gestures
                item(key = "video_player_controls") {
                    AppearanceSectionCard(
                        title = stringResource(R.string.video_customize_controls_title),
                        onReset = null
                    ) {
                        CustomizationSwitchRow(
                            icon = Icons.Rounded.Swipe,
                            title = stringResource(R.string.setcat_video_gestures_title),
                            subtitle = stringResource(R.string.setcat_video_gestures_desc),
                            checked = settingsState.videoSwipeGesturesEnabled,
                            onCheckedChange = { settingsViewModel.setVideoSwipeGesturesEnabled(it) }
                        )
                        CustomizationSwitchRow(
                            icon = Icons.Rounded.MusicNote,
                            title = stringResource(R.string.setcat_video_background_title),
                            subtitle = stringResource(R.string.setcat_video_background_desc),
                            checked = settingsState.videoBackgroundPlayback,
                            onCheckedChange = { settingsViewModel.setVideoBackgroundPlayback(it) }
                        )
                    }
                }

                // Subtitle Appearance
                item(key = "video_subtitles_style") {
                    AppearanceSectionCard(
                        title = stringResource(R.string.setcat_video_subtitles_section),
                        onReset = null
                    ) {
                        ChoiceChips(
                            options = listOf(
                                "16" to stringResource(R.string.video_subtitle_small),
                                "20" to stringResource(R.string.video_subtitle_medium),
                                "26" to stringResource(R.string.video_subtitle_large)
                            ),
                            selected = settingsState.videoSubtitleTextSizeSp.toInt().toString(),
                            onSelect = { settingsViewModel.setVideoSubtitleTextSize(it.toFloat()) },
                            label = stringResource(R.string.setcat_video_subtitle_size_title)
                        )
                        Spacer(Modifier.height(4.dp))
                        ChoiceChips(
                            options = listOf(
                                Color.White.toArgb().toString() to stringResource(R.string.setcat_video_subtitle_white),
                                Color.Yellow.toArgb().toString() to stringResource(R.string.setcat_video_subtitle_yellow),
                                Color(0xFF00E5FF).toArgb().toString() to stringResource(R.string.setcat_video_subtitle_cyan)
                            ),
                            selected = settingsState.videoSubtitleTextColorArgb.toString(),
                            onSelect = { settingsViewModel.setVideoSubtitleTextColor(it.toInt()) },
                            label = stringResource(R.string.setcat_video_subtitle_color_title)
                        )
                        Spacer(Modifier.height(4.dp))
                        ChoiceChips(
                            options = listOf(
                                "0" to stringResource(R.string.video_subtitle_none),
                                "1" to stringResource(R.string.video_subtitle_dark),
                                "2" to stringResource(R.string.video_subtitle_solid)
                            ),
                            selected = settingsState.videoSubtitleBackgroundMode.toString(),
                            onSelect = { settingsViewModel.setVideoSubtitleBackground(it.toInt()) },
                            label = stringResource(R.string.setcat_video_subtitle_bg_title)
                        )
                    }
                }
            }
        }
    }
}
