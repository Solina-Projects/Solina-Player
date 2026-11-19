package app.solina.player.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material.icons.rounded.ViewCompact
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import app.solina.player.R
import app.solina.player.data.preferences.CarouselStyle
import app.solina.player.data.preferences.LibraryNavigationMode
import app.solina.player.data.preferences.NavBarStyle
import app.solina.player.data.preferences.SeekBarStyleNames
import app.solina.player.data.preferences.ThemePreference
import app.solina.player.presentation.components.MiniPlayerHeight
import app.solina.player.presentation.navigation.Screen
import app.solina.player.presentation.navigation.navigateSafely
import app.solina.player.presentation.viewmodel.AppearanceViewModel
import app.solina.player.presentation.viewmodel.SettingsViewModel

/**
 * Dedicated Music Customisation hub — directly accessible from the Music Library
 * top bar as well as from App Settings.
 * - Tab 0 (Visuals & Player): Music section palette, floating mini-bar, seek bar style,
 *   Now Playing theme, file info badge, album carousel style, and album-art palette style.
 * - Tab 1 (Library & Lists): Song list row corners & font size, Library navigation mode,
 *   bottom navigation bar style & compact mode, and quick links to Lyrics, Equalizer & Artists.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MusicCustomizationScreen(
    navController: NavController,
    appearanceViewModel: AppearanceViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val appearance by appearanceViewModel.appearance.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.music_customize_tab_player),
        stringResource(R.string.music_customize_tab_library)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.music_customize_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { appearanceViewModel.resetMusicAppearance() }) {
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
            item(key = "music_customize_tabs") {
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
                // Music Section Palette
                item(key = "music_section_palette") {
                    AppearanceSectionCard(
                        title = stringResource(R.string.music_customize_palette_title),
                        subtitle = stringResource(R.string.music_customize_palette_subtitle),
                        onReset = { appearanceViewModel.resetMusicSectionPalette() }
                    ) {
                        PaletteChips(
                            selected = appearance.musicSectionPalette,
                            onSelect = { appearanceViewModel.setMusicSectionPalette(it) },
                            showLabel = false
                        )
                    }
                }

                // Floating Music Mini-Player Bar
                item(key = "music_floating_bar") {
                    val floatingBar = appearance.effectiveFloatingMusicBar
                    AppearanceSectionCard(
                        title = stringResource(R.string.appearance_floating_music_bar_title),
                        subtitle = stringResource(R.string.appearance_floating_music_bar_note),
                        onReset = { appearanceViewModel.resetFloatingMusicBar() }
                    ) {
                        CornerControls(
                            corners = floatingBar.effectiveCorners,
                            onChange = {
                                appearanceViewModel.setFloatingMusicBar(floatingBar.copy(corners = it))
                            }
                        )
                        PaletteChips(
                            selected = floatingBar.palette,
                            onSelect = {
                                appearanceViewModel.setFloatingMusicBar(floatingBar.copy(palette = it))
                            }
                        )
                    }
                }

                // Seek Bar Style
                item(key = "music_seek_bar") {
                    AppearanceSectionCard(
                        title = stringResource(R.string.appearance_seek_bar_title),
                        onReset = { appearanceViewModel.resetSeekBarStyle() }
                    ) {
                        ChoiceChips(
                            options = listOf(
                                SeekBarStyleNames.WAVY to stringResource(R.string.appearance_seek_bar_wavy),
                                SeekBarStyleNames.CLASSIC to stringResource(R.string.appearance_seek_bar_classic)
                            ),
                            selected = appearance.seekBarStyle,
                            onSelect = { appearanceViewModel.setSeekBarStyle(it) },
                            label = null
                        )
                    }
                }

                // Now Playing Look
                item(key = "music_now_playing_look") {
                    AppearanceSectionCard(
                        title = stringResource(R.string.setcat_now_playing),
                        onReset = null
                    ) {
                        ChoiceChips(
                            options = listOf(
                                ThemePreference.ALBUM_ART to stringResource(R.string.setcat_player_theme_album_art),
                                ThemePreference.DYNAMIC to stringResource(R.string.setcat_player_theme_dynamic)
                            ),
                            selected = settingsState.playerThemePreference,
                            onSelect = { settingsViewModel.setPlayerThemePreference(it) },
                            label = stringResource(R.string.setcat_player_theme_label)
                        )
                        Spacer(Modifier.height(4.dp))
                        ChoiceChips(
                            options = listOf(
                                CarouselStyle.NO_PEEK to stringResource(R.string.setcat_carousel_no_peek),
                                CarouselStyle.ONE_PEEK to stringResource(R.string.setcat_carousel_one_peek)
                            ),
                            selected = settingsState.carouselStyle,
                            onSelect = { settingsViewModel.setCarouselStyle(it) },
                            label = stringResource(R.string.setcat_carousel_style_label)
                        )
                        Spacer(Modifier.height(4.dp))
                        CustomizationSwitchRow(
                            icon = Icons.Rounded.Info,
                            title = stringResource(R.string.setcat_show_player_file_info_title),
                            subtitle = stringResource(R.string.setcat_show_player_file_info_subtitle),
                            checked = settingsState.showPlayerFileInfo,
                            onCheckedChange = { settingsViewModel.setShowPlayerFileInfo(it) }
                        )
                    }
                }

                // Album art palette style link
                item(key = "music_palette_style_link") {
                    CustomizationLinkCard(
                        title = stringResource(R.string.setcat_album_art_palette_title),
                        subtitle = stringResource(
                            R.string.setcat_album_art_palette_subtitle,
                            settingsState.albumArtPaletteStyle.label
                        ),
                        icon = Icons.Rounded.Palette,
                        onClick = { navController.navigateSafely(Screen.PaletteStyle.createRoute()) }
                    )
                }
            } else {
                // Song List Rows & List Typography
                item(key = "music_list_rows") {
                    val rows = appearance.listRows
                    AppearanceSectionCard(
                        title = stringResource(R.string.appearance_list_rows_title),
                        subtitle = stringResource(R.string.appearance_list_rows_note),
                        onReset = { appearanceViewModel.resetListRows() }
                    ) {
                        DpSlider(
                            label = stringResource(R.string.appearance_corners),
                            value = rows.cornerDp,
                            range = 0..32,
                            onChange = {
                                appearanceViewModel.setListRows(rows.copy(cornerDp = it))
                            }
                        )
                        PercentSlider(
                            label = stringResource(R.string.appearance_list_font_size_title),
                            value = appearance.listFontScalePercent,
                            range = 85..125,
                            onChange = { appearanceViewModel.setListFontScalePercent(it) }
                        )
                    }
                }

                // Library & Bottom Bar Layout
                item(key = "music_library_nav_layout") {
                    AppearanceSectionCard(
                        title = stringResource(R.string.setcat_navigation_bar),
                        onReset = null
                    ) {
                        ChoiceChips(
                            options = listOf(
                                LibraryNavigationMode.TAB_ROW to stringResource(R.string.setcat_library_nav_tab_row),
                                LibraryNavigationMode.COMPACT_PILL to stringResource(R.string.setcat_library_nav_compact_pill)
                            ),
                            selected = settingsState.libraryNavigationMode,
                            onSelect = { settingsViewModel.setLibraryNavigationMode(it) },
                            label = stringResource(R.string.setcat_library_navigation_label)
                        )
                        Spacer(Modifier.height(4.dp))
                        ChoiceChips(
                            options = listOf(
                                NavBarStyle.DEFAULT to stringResource(R.string.setcat_navbar_style_default),
                                NavBarStyle.FULL_WIDTH to stringResource(R.string.setcat_navbar_style_full_width)
                            ),
                            selected = settingsState.navBarStyle,
                            onSelect = { settingsViewModel.setNavBarStyle(it) },
                            label = stringResource(R.string.setcat_navbar_style_label)
                        )
                        Spacer(Modifier.height(4.dp))
                        CustomizationSwitchRow(
                            icon = Icons.Rounded.ViewCompact,
                            title = stringResource(R.string.setcat_compact_mode_title),
                            subtitle = stringResource(R.string.setcat_compact_mode_subtitle),
                            checked = settingsState.navBarCompactMode,
                            onCheckedChange = { settingsViewModel.setNavBarCompactMode(it) }
                        )
                    }
                }

                // Related Music Personalisation shortcuts
                item(key = "music_related_links") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CustomizationGroupHeading(
                            text = stringResource(R.string.music_customize_more_heading)
                        )
                        CustomizationLinkCard(
                            title = stringResource(R.string.setcat_navbar_corner_title),
                            subtitle = stringResource(R.string.setcat_navbar_corner_subtitle),
                            icon = Icons.Rounded.RoundedCorner,
                            onClick = { navController.navigateSafely(Screen.NavBarCrRad.createRoute()) }
                        )
                        CustomizationLinkCard(
                            title = stringResource(R.string.setcat_lyrics_title),
                            subtitle = stringResource(R.string.setcat_lyrics_subtitle),
                            icon = Icons.Rounded.Lyrics,
                            iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                            onClick = { navController.navigateSafely(Screen.LyricsSettings.createRoute()) }
                        )
                        CustomizationLinkCard(
                            title = stringResource(R.string.settings_category_equalizer_title),
                            subtitle = stringResource(R.string.settings_category_equalizer_subtitle),
                            icon = Icons.Rounded.GraphicEq,
                            iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                            onClick = { navController.navigateSafely(Screen.Equalizer.createRoute()) }
                        )
                        CustomizationLinkCard(
                            title = stringResource(R.string.setcat_artists_title),
                            subtitle = stringResource(R.string.setcat_artists_subtitle),
                            icon = Icons.Rounded.Person,
                            onClick = { navController.navigateSafely(Screen.ArtistSettings.createRoute()) }
                        )
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
                }
            }
        }
    }
}
