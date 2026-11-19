package app.solina.player.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import app.solina.player.R
import app.solina.player.data.preferences.HomeSectionDensity
import app.solina.player.data.preferences.HomeSectionId
import app.solina.player.presentation.components.MiniPlayerHeight
import app.solina.player.presentation.navigation.Screen
import app.solina.player.presentation.navigation.navigateSafely
import app.solina.player.presentation.viewmodel.AppearanceViewModel
import app.solina.player.presentation.viewmodel.HomeCustomizationViewModel

/**
 * Dedicated Home Customisation hub:
 * - Tab 0 (Layout & Sections): header/search behaviour, smart filters, counts, density, and section order.
 * - Tab 1 (Visuals & Style): Home palette, entry cards, recent cards, Your Mix card, and Home mini-bars.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeCustomizationScreen(
    navController: NavController,
    viewModel: HomeCustomizationViewModel = hiltViewModel(),
    appearanceViewModel: AppearanceViewModel = hiltViewModel()
) {
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    val layoutPrefs by viewModel.layoutPrefs.collectAsStateWithLifecycle()
    val appearance by appearanceViewModel.appearance.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.customize_tab_layout_sections),
        stringResource(R.string.customize_tab_visuals_style)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.home_customize_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (selectedTab == 0) {
                                viewModel.resetToDefaults()
                            } else {
                                appearanceViewModel.resetHomeAppearance()
                            }
                        }
                    ) {
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
            item(key = "home_customize_tabs") {
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
                item(key = "home_customize_header_group") {
                    CustomizationGroupHeading(text = stringResource(R.string.home_customize_group_header))
                    Spacer(Modifier.height(6.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            CustomizationSwitchRow(
                                icon = Icons.Rounded.WbSunny,
                                title = stringResource(R.string.home_pref_greeting_title),
                                subtitle = stringResource(R.string.home_pref_greeting_subtitle),
                                checked = layoutPrefs.showGreeting,
                                onCheckedChange = viewModel::setShowGreeting
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            CustomizationSwitchRow(
                                icon = Icons.Rounded.Search,
                                title = stringResource(R.string.home_pref_search_bar_title),
                                subtitle = stringResource(R.string.home_pref_search_bar_subtitle),
                                checked = layoutPrefs.showSearchBar,
                                onCheckedChange = viewModel::setShowSearchBar
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            CustomizationSwitchRow(
                                icon = Icons.Rounded.PushPin,
                                title = stringResource(R.string.home_pref_pin_search_title),
                                subtitle = stringResource(R.string.home_pref_pin_search_subtitle),
                                checked = layoutPrefs.pinSearchBarOnScroll,
                                enabled = layoutPrefs.showSearchBar,
                                onCheckedChange = viewModel::setPinSearchBarOnScroll
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            CustomizationSwitchRow(
                                icon = Icons.Rounded.NewReleases,
                                title = stringResource(R.string.home_pref_changelog_btn_title),
                                subtitle = stringResource(R.string.home_pref_changelog_btn_subtitle),
                                checked = layoutPrefs.showChangelogButton,
                                onCheckedChange = viewModel::setShowChangelogButton
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            CustomizationSwitchRow(
                                icon = Icons.Rounded.Verified,
                                title = stringResource(R.string.home_pref_build_badge_title),
                                subtitle = stringResource(R.string.home_pref_build_badge_subtitle),
                                checked = layoutPrefs.showBuildBadge,
                                onCheckedChange = viewModel::setShowBuildBadge
                            )
                        }
                    }
                }

                item(key = "home_customize_content_group") {
                    CustomizationGroupHeading(text = stringResource(R.string.home_customize_group_content))
                    Spacer(Modifier.height(6.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CustomizationSwitchRow(
                                icon = Icons.Rounded.FilterAlt,
                                title = stringResource(R.string.home_pref_smart_filter_title),
                                subtitle = stringResource(R.string.home_pref_smart_filter_subtitle),
                                checked = layoutPrefs.smartMusicFilter,
                                onCheckedChange = viewModel::setSmartMusicFilter
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            CustomizationSwitchRow(
                                icon = Icons.Rounded.Bolt,
                                title = stringResource(R.string.home_pref_entry_quick_actions_title),
                                subtitle = stringResource(R.string.home_pref_entry_quick_actions_subtitle),
                                checked = layoutPrefs.showEntryQuickActions,
                                onCheckedChange = viewModel::setShowEntryQuickActions
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            CustomizationSwitchRow(
                                icon = Icons.Rounded.PlayCircle,
                                title = stringResource(R.string.home_pref_video_progress_title),
                                subtitle = stringResource(R.string.home_pref_video_progress_subtitle),
                                checked = layoutPrefs.showVideoProgressOnHome,
                                onCheckedChange = viewModel::setShowVideoProgressOnHome
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            CustomizationSwitchRow(
                                icon = Icons.Rounded.BarChart,
                                title = stringResource(R.string.home_pref_video_stats_title),
                                subtitle = stringResource(R.string.home_pref_video_stats_subtitle),
                                checked = layoutPrefs.showVideoSummaryInStats,
                                onCheckedChange = viewModel::setShowVideoSummaryInStats
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.home_pref_recent_limit_title),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(6, 12, 20).forEach { count ->
                                    FilterChip(
                                        selected = layoutPrefs.recentItemsLimit == count,
                                        onClick = { viewModel.setRecentItemsLimit(count) },
                                        label = { Text(text = "$count") }
                                    )
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.home_pref_density_title),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    HomeSectionDensity.COMPACT to R.string.home_pref_density_compact,
                                    HomeSectionDensity.COMFORTABLE to R.string.home_pref_density_comfortable,
                                    HomeSectionDensity.SPACIOUS to R.string.home_pref_density_spacious
                                ).forEach { (density, labelRes) ->
                                    FilterChip(
                                        selected = layoutPrefs.sectionDensity == density,
                                        onClick = { viewModel.setSectionDensity(density) },
                                        label = { Text(text = stringResource(labelRes)) }
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "home_customize_sections_heading") {
                    CustomizationGroupHeading(text = stringResource(R.string.home_customize_group_sections))
                    Text(
                        text = stringResource(R.string.home_customize_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                items(sections, key = { it.id }) { section ->
                    val sectionId = runCatching { HomeSectionId.valueOf(section.id) }.getOrNull()
                    if (sectionId != null && sectionId != HomeSectionId.COUNTS) {
                        val index = sections.indexOf(section)
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (section.visible) {
                                                MaterialTheme.colorScheme.secondaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = sectionId.iconVector,
                                        contentDescription = null,
                                        tint = if (section.visible) {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(sectionId.titleRes),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = if (section.visible) {
                                            stringResource(R.string.home_customize_show)
                                        } else {
                                            stringResource(R.string.home_customize_hidden)
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.moveUp(sectionId) },
                                    enabled = index > 0
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.KeyboardArrowUp,
                                        contentDescription = stringResource(R.string.home_customize_move_up)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.moveDown(sectionId) },
                                    enabled = index < sections.lastIndex
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = stringResource(R.string.home_customize_move_down)
                                    )
                                }
                                Switch(
                                    checked = section.visible,
                                    onCheckedChange = { checked ->
                                        viewModel.setVisible(sectionId, checked)
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                // Tab 1: Home Visuals & Style
                item(key = "home_visual_palette") {
                    AppearanceSectionCard(
                        title = stringResource(R.string.home_customize_palette_title),
                        subtitle = stringResource(R.string.home_customize_palette_subtitle),
                        onReset = { appearanceViewModel.resetHomeSectionPalette() }
                    ) {
                        PaletteChips(
                            selected = appearance.homeSectionPalette,
                            onSelect = { appearanceViewModel.setHomeSectionPalette(it) },
                            showLabel = false
                        )
                    }
                }

                item(key = "home_visual_entry_cards") {
                    CardSection(
                        title = stringResource(R.string.appearance_home_cards_title),
                        card = appearance.homeCards,
                        onChange = { appearanceViewModel.setHomeCards(it) },
                        onReset = { appearanceViewModel.resetHomeCards() }
                    )
                }

                item(key = "home_visual_recent_music") {
                    RecentCardsSection(
                        title = stringResource(R.string.appearance_recent_music_cards_title),
                        recent = appearance.effectiveRecentMusicCards,
                        onChange = { appearanceViewModel.setRecentMusicCards(it) },
                        onReset = { appearanceViewModel.resetRecentMusicCards() }
                    )
                }

                item(key = "home_visual_recent_video") {
                    RecentCardsSection(
                        title = stringResource(R.string.appearance_recent_video_cards_title),
                        recent = appearance.effectiveRecentVideoCards,
                        onChange = { appearanceViewModel.setRecentVideoCards(it) },
                        onReset = { appearanceViewModel.resetRecentVideoCards() }
                    )
                }

                item(key = "home_visual_your_mix") {
                    CardSection(
                        title = stringResource(R.string.appearance_your_mix_title),
                        card = appearance.yourMixCard,
                        onChange = { appearanceViewModel.setYourMixCard(it) },
                        onReset = { appearanceViewModel.resetYourMixCard() }
                    )
                }

                item(key = "home_visual_music_bar") {
                    BarSection(
                        title = stringResource(R.string.appearance_home_music_bar_title),
                        bar = appearance.homeMusicBar,
                        onChange = { appearanceViewModel.setHomeMusicBar(it) },
                        onReset = { appearanceViewModel.resetHomeMusicBar() }
                    )
                }

                item(key = "home_visual_video_bar") {
                    BarSection(
                        title = stringResource(R.string.appearance_home_video_bar_title),
                        bar = appearance.homeVideoBar,
                        onChange = { appearanceViewModel.setHomeVideoBar(it) },
                        onReset = { appearanceViewModel.resetHomeVideoBar() }
                    )
                }

                item(key = "home_visual_global_link") {
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

private val HomeSectionId.titleRes: Int
    get() = when (this) {
        HomeSectionId.ENTRY_CARDS -> R.string.home_section_entry_cards
        HomeSectionId.QUICK_SHORTCUTS -> R.string.home_section_quick_shortcuts
        HomeSectionId.COUNTS -> R.string.home_section_counts
        HomeSectionId.RECENT_MUSIC -> R.string.home_section_recent_music
        HomeSectionId.RECENT_VIDEOS -> R.string.home_section_recent_videos
        HomeSectionId.YOUR_MIX -> R.string.home_section_your_mix
        HomeSectionId.DAILY_MIX -> R.string.home_section_daily_mix
        HomeSectionId.RECENTLY_PLAYED -> R.string.home_section_recently_played
        HomeSectionId.STATS -> R.string.home_section_stats
        HomeSectionId.RECAP -> R.string.home_section_recap
    }

private val HomeSectionId.iconVector: ImageVector
    get() = when (this) {
        HomeSectionId.ENTRY_CARDS -> Icons.Rounded.GridView
        HomeSectionId.QUICK_SHORTCUTS -> Icons.Rounded.Bolt
        HomeSectionId.COUNTS -> Icons.Rounded.GridView
        HomeSectionId.RECENT_MUSIC -> Icons.Rounded.LibraryMusic
        HomeSectionId.RECENT_VIDEOS -> Icons.Rounded.VideoLibrary
        HomeSectionId.YOUR_MIX -> Icons.Rounded.QueueMusic
        HomeSectionId.DAILY_MIX -> Icons.Rounded.AutoAwesome
        HomeSectionId.RECENTLY_PLAYED -> Icons.Rounded.History
        HomeSectionId.STATS -> Icons.Rounded.BarChart
        HomeSectionId.RECAP -> Icons.Rounded.CalendarMonth
    }
