package app.solina.player.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import app.solina.player.R
import app.solina.player.data.preferences.AppearanceFontFamilies
import app.solina.player.data.preferences.AppearancePaletteNames
import app.solina.player.data.preferences.AppearancePresetNames
import app.solina.player.data.preferences.AppThemeMode
import app.solina.player.data.preferences.BarAppearance
import app.solina.player.data.preferences.CardAppearance
import app.solina.player.data.preferences.CornerRadii
import app.solina.player.data.preferences.GlassPresetNames
import app.solina.player.data.preferences.GlassStyleNames
import app.solina.player.data.preferences.RecentCardsAppearance
import app.solina.player.data.preferences.SeekBarStyleNames
import app.solina.player.presentation.components.MiniPlayerHeight
import app.solina.player.presentation.components.appearancePaletteColors
import app.solina.player.presentation.viewmodel.AppearanceViewModel

/**
 * Appearance customisation (bug-fix round 2026-11-17, item 6): nearly
 * every visible part of the player, tunable from Settings, each section
 * independent — Home cards, the Home music/video bars, the Video
 * section bar, the floating music bar's colour, the Home / Music /
 * Video section colours each separately, the font family and size, and
 * the app theme. Ready-made looks and a reset per section are included.
 * Everything is stored in DataStore and applies live, no restart.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppearanceCustomizationScreen(
    navController: NavController,
    viewModel: AppearanceViewModel = hiltViewModel()
) {
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val appThemeMode by viewModel.appThemeMode.collectAsStateWithLifecycle()
    // Round 15, wave 2, C7: user-saved looks.
    val savedPresets by viewModel.savedPresets.collectAsStateWithLifecycle()
    val presetNoticeRes by viewModel.presetNoticeRes.collectAsStateWithLifecycle()
    var showSaveLookDialog by remember { mutableStateOf(false) }

    if (showSaveLookDialog) {
        SaveLookDialog(
            onDismiss = { showSaveLookDialog = false },
            onSave = { name ->
                viewModel.saveCurrentLook(name)
                showSaveLookDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.appearance_customize_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.resetAll() }) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(text = stringResource(R.string.appearance_reset_all))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            // Round 15, E1: the old all-round 16 dp left the floating
            // mini-player bar covering the last rows (e.g. the font-size
            // slider). Bottom = bar height + system nav bar + 16 dp, the
            // same formula as the main settings list.
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = MiniPlayerHeight +
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                    16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "appearance_intro") {
                Text(
                    text = stringResource(R.string.appearance_customize_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ---- Theme + ready-made looks ----
            item(key = "appearance_theme") {
                AppearanceSectionCard(
                    title = stringResource(R.string.appearance_theme_title),
                    onReset = { viewModel.resetAll() },
                    resetLabel = stringResource(R.string.appearance_reset_all)
                ) {
                    ChoiceChips(
                        options = listOf(
                            AppearancePresetNames.DEFAULT to stringResource(R.string.appearance_preset_default),
                            AppearancePresetNames.COMPACT to stringResource(R.string.appearance_preset_compact),
                            AppearancePresetNames.BOLD to stringResource(R.string.appearance_preset_bold),
                            AppearancePresetNames.NIGHT to stringResource(R.string.appearance_preset_night)
                        ),
                        selected = null,
                        onSelect = { viewModel.applyPreset(it) },
                        label = stringResource(R.string.appearance_preset_title)
                    )
                    Spacer(Modifier.height(8.dp))
                    ChoiceChips(
                        options = listOf(
                            AppThemeMode.FOLLOW_SYSTEM to stringResource(R.string.appearance_theme_system),
                            AppThemeMode.LIGHT to stringResource(R.string.appearance_theme_light),
                            AppThemeMode.DARK to stringResource(R.string.appearance_theme_dark)
                        ),
                        selected = appThemeMode,
                        onSelect = { viewModel.setAppThemeMode(it) },
                        label = null
                    )
                }
            }

            // ---- Home cards ----
            item(key = "appearance_home_cards") {
                CardSection(
                    title = stringResource(R.string.appearance_home_cards_title),
                    card = appearance.homeCards,
                    onChange = { viewModel.setHomeCards(it) },
                    onReset = { viewModel.resetHomeCards() }
                )
            }

            // ---- Recently added cards (round 15, wave 2, C3; split
            // music / video in round 16) ----
            item(key = "appearance_recent_music_cards") {
                RecentCardsSection(
                    title = stringResource(R.string.appearance_recent_music_cards_title),
                    recent = appearance.effectiveRecentMusicCards,
                    onChange = { viewModel.setRecentMusicCards(it) },
                    onReset = { viewModel.resetRecentMusicCards() }
                )
            }

            item(key = "appearance_recent_video_cards") {
                RecentCardsSection(
                    title = stringResource(R.string.appearance_recent_video_cards_title),
                    recent = appearance.effectiveRecentVideoCards,
                    onChange = { viewModel.setRecentVideoCards(it) },
                    onReset = { viewModel.resetRecentVideoCards() }
                )
            }

            // ---- Your Mix card (round 15, wave 2, C4) ----
            item(key = "appearance_your_mix_card") {
                CardSection(
                    title = stringResource(R.string.appearance_your_mix_title),
                    card = appearance.yourMixCard,
                    onChange = { viewModel.setYourMixCard(it) },
                    onReset = { viewModel.resetYourMixCard() }
                )
            }

            // ---- List rows (round 15, wave 2, C4) ----
            item(key = "appearance_list_rows") {
                val rows = appearance.listRows
                AppearanceSectionCard(
                    title = stringResource(R.string.appearance_list_rows_title),
                    onReset = { viewModel.resetListRows() }
                ) {
                    DpSlider(
                        label = stringResource(R.string.appearance_corners),
                        value = rows.cornerDp,
                        range = 0..32,
                        onChange = {
                            viewModel.setListRows(rows.copy(cornerDp = it))
                        }
                    )
                    Text(
                        text = stringResource(R.string.appearance_list_rows_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ---- Seek bar style (round 15, wave 2, C4) ----
            item(key = "appearance_seek_bar") {
                AppearanceSectionCard(
                    title = stringResource(R.string.appearance_seek_bar_title),
                    onReset = { viewModel.resetSeekBarStyle() }
                ) {
                    ChoiceChips(
                        options = listOf(
                            SeekBarStyleNames.WAVY to stringResource(R.string.appearance_seek_bar_wavy),
                            SeekBarStyleNames.CLASSIC to stringResource(R.string.appearance_seek_bar_classic)
                        ),
                        selected = appearance.seekBarStyle,
                        onSelect = { viewModel.setSeekBarStyle(it) },
                        label = null
                    )
                }
            }

            // ---- Home music bar ----
            item(key = "appearance_home_music_bar") {
                BarSection(
                    title = stringResource(R.string.appearance_home_music_bar_title),
                    bar = appearance.homeMusicBar,
                    onChange = { viewModel.setHomeMusicBar(it) },
                    onReset = { viewModel.resetHomeMusicBar() }
                )
            }

            // ---- Home video bar ----
            item(key = "appearance_home_video_bar") {
                BarSection(
                    title = stringResource(R.string.appearance_home_video_bar_title),
                    bar = appearance.homeVideoBar,
                    onChange = { viewModel.setHomeVideoBar(it) },
                    onReset = { viewModel.resetHomeVideoBar() }
                )
            }

            // ---- Video section bar ----
            item(key = "appearance_section_video_bar") {
                BarSection(
                    title = stringResource(R.string.appearance_section_video_bar_title),
                    bar = appearance.sectionVideoBar,
                    onChange = { viewModel.setSectionVideoBar(it) },
                    onReset = { viewModel.resetSectionVideoBar() }
                )
            }

            // ---- Floating music bar (round 16): corners now tunable
            // like the other bars — no show/hide switch (hiding it
            // would hide the full player) and no size slider (its size
            // is fixed by the player sheet's layout math). ----
            item(key = "appearance_floating_music_bar") {
                val floatingBar = appearance.effectiveFloatingMusicBar
                AppearanceSectionCard(
                    title = stringResource(R.string.appearance_floating_music_bar_title),
                    onReset = { viewModel.resetFloatingMusicBar() }
                ) {
                    Text(
                        text = stringResource(R.string.appearance_floating_music_bar_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    CornerControls(
                        corners = floatingBar.effectiveCorners,
                        onChange = {
                            viewModel.setFloatingMusicBar(floatingBar.copy(corners = it))
                        }
                    )
                    PaletteChips(
                        selected = floatingBar.palette,
                        onSelect = {
                            viewModel.setFloatingMusicBar(floatingBar.copy(palette = it))
                        }
                    )
                }
            }

            // ---- Glass & blur (blur-engine test build, 2026-11-17) ----
            item(key = "appearance_glass") {
                val glass = appearance.glass
                AppearanceSectionCard(
                    title = stringResource(R.string.appearance_glass_title),
                    onReset = { viewModel.resetGlass() }
                ) {
                    Text(
                        text = stringResource(R.string.appearance_glass_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.appearance_glass_enable),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = glass.enabled,
                            onCheckedChange = {
                                viewModel.setGlass(glass.copy(enabled = it))
                            }
                        )
                    }
                    ChoiceChips(
                        options = listOf(
                            GlassPresetNames.OFF to stringResource(R.string.appearance_glass_preset_off),
                            GlassPresetNames.SUBTLE to stringResource(R.string.appearance_glass_preset_subtle),
                            GlassPresetNames.FROSTED to stringResource(R.string.appearance_glass_preset_frosted),
                            GlassPresetNames.LIQUID to stringResource(R.string.appearance_glass_preset_liquid)
                        ),
                        selected = null,
                        onSelect = { viewModel.applyGlassPreset(it) },
                        label = stringResource(R.string.appearance_glass_preset_title)
                    )
                    Spacer(Modifier.height(8.dp))
                    GlassToggleRow(
                        label = stringResource(R.string.appearance_glass_mini_bars),
                        checked = glass.miniBars,
                        enabled = glass.enabled,
                        onChange = { viewModel.setGlass(glass.copy(miniBars = it)) }
                    )
                    GlassToggleRow(
                        label = stringResource(R.string.appearance_glass_sheets),
                        checked = glass.sheets,
                        enabled = glass.enabled,
                        onChange = { viewModel.setGlass(glass.copy(sheets = it)) }
                    )
                    GlassToggleRow(
                        label = stringResource(R.string.appearance_glass_dialogs),
                        checked = glass.dialogs,
                        enabled = glass.enabled,
                        onChange = { viewModel.setGlass(glass.copy(dialogs = it)) }
                    )
                    GlassToggleRow(
                        label = stringResource(R.string.appearance_glass_now_playing),
                        checked = glass.nowPlayingBackground,
                        enabled = glass.enabled,
                        onChange = {
                            viewModel.setGlass(glass.copy(nowPlayingBackground = it))
                        }
                    )
                    ChoiceChips(
                        options = listOf(
                            GlassStyleNames.FROSTED to stringResource(R.string.appearance_glass_style_frosted),
                            GlassStyleNames.LIQUID to stringResource(R.string.appearance_glass_style_liquid),
                            GlassStyleNames.CLEAR to stringResource(R.string.appearance_glass_style_clear)
                        ),
                        selected = glass.style,
                        onSelect = { viewModel.setGlass(glass.copy(style = it)) },
                        label = stringResource(R.string.appearance_glass_style)
                    )
                    Spacer(Modifier.height(8.dp))
                    // Round 15, wave 2, C5: mini-bars and sheets/dialogs
                    // have their own blur radius + tint. Saturation
                    // stays one global value.
                    Text(
                        text = stringResource(R.string.appearance_glass_mini_bars),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    DpSlider(
                        label = stringResource(R.string.appearance_glass_radius),
                        value = glass.miniBarRadius,
                        range = 0..40,
                        onChange = {
                            viewModel.setGlass(glass.copy(miniBarRadiusDp = it))
                        },
                        enabled = glass.enabled
                    )
                    PercentSlider(
                        label = stringResource(R.string.appearance_glass_tint),
                        value = (glass.miniBarTintAlpha * 100).toInt(),
                        range = 0..60,
                        onChange = {
                            viewModel.setGlass(glass.copy(miniBarTintPercent = it))
                        },
                        enabled = glass.enabled
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.appearance_glass_sheets_group),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    DpSlider(
                        label = stringResource(R.string.appearance_glass_radius),
                        value = glass.sheetRadius,
                        range = 0..40,
                        onChange = {
                            viewModel.setGlass(glass.copy(sheetRadiusDp = it))
                        },
                        enabled = glass.enabled
                    )
                    PercentSlider(
                        label = stringResource(R.string.appearance_glass_tint),
                        value = (glass.sheetTintAlpha * 100).toInt(),
                        range = 0..60,
                        onChange = {
                            viewModel.setGlass(glass.copy(sheetTintPercent = it))
                        },
                        enabled = glass.enabled
                    )
                    Spacer(Modifier.height(8.dp))
                    PercentSlider(
                        label = stringResource(R.string.appearance_glass_saturation),
                        value = glass.saturationPercent,
                        range = 100..160,
                        onChange = {
                            viewModel.setGlass(glass.copy(saturationPercent = it))
                        },
                        enabled = glass.enabled
                    )
                    DpSlider(
                        label = stringResource(R.string.appearance_glass_refraction),
                        value = glass.refractionDp,
                        range = 0..26,
                        onChange = { viewModel.setGlass(glass.copy(refractionDp = it)) },
                        enabled = glass.enabled && glass.style == GlassStyleNames.LIQUID
                    )
                    Text(
                        text = stringResource(R.string.appearance_glass_refraction_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.appearance_glass_video_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ---- Section colours ----
            item(key = "appearance_section_colors") {
                AppearanceSectionCard(
                    title = stringResource(R.string.appearance_section_colors_title),
                    onReset = { viewModel.resetSectionColors() }
                ) {
                    PaletteRow(
                        label = stringResource(R.string.appearance_section_home),
                        selected = appearance.homeSectionPalette,
                        onSelect = { viewModel.setHomeSectionPalette(it) }
                    )
                    Spacer(Modifier.height(8.dp))
                    PaletteRow(
                        label = stringResource(R.string.appearance_section_music),
                        selected = appearance.musicSectionPalette,
                        onSelect = { viewModel.setMusicSectionPalette(it) }
                    )
                    Spacer(Modifier.height(8.dp))
                    PaletteRow(
                        label = stringResource(R.string.appearance_section_video),
                        selected = appearance.videoSectionPalette,
                        onSelect = { viewModel.setVideoSectionPalette(it) }
                    )
                }
            }

            // ---- Fonts ----
            item(key = "appearance_typography") {
                AppearanceSectionCard(
                    title = stringResource(R.string.appearance_typography_title),
                    onReset = { viewModel.resetTypography() }
                ) {
                    ChoiceChips(
                        options = listOf(
                            AppearanceFontFamilies.ROUNDED to stringResource(R.string.appearance_font_rounded),
                            AppearanceFontFamilies.VAZIRMATN to stringResource(R.string.appearance_font_vazirmatn),
                            AppearanceFontFamilies.SYSTEM to stringResource(R.string.appearance_font_system)
                        ),
                        selected = appearance.fontFamily,
                        onSelect = { viewModel.setFontFamily(it) },
                        label = stringResource(R.string.appearance_font_family_title)
                    )
                    Spacer(Modifier.height(8.dp))
                    PercentSlider(
                        label = stringResource(R.string.appearance_font_size_title),
                        value = appearance.fontScalePercent,
                        range = 85..125,
                        onChange = { viewModel.setFontScalePercent(it) }
                    )
                    PercentSlider(
                        label = stringResource(R.string.appearance_list_font_size_title),
                        value = appearance.listFontScalePercent,
                        range = 85..125,
                        onChange = { viewModel.setListFontScalePercent(it) }
                    )
                }
            }

            // ---- Saved looks (round 15, wave 2, C7) ----
            item(key = "appearance_saved_presets") {
                AppearanceSectionCard(
                    title = stringResource(R.string.appearance_saved_presets_title),
                    onReset = null
                ) {
                    Text(
                        text = stringResource(R.string.appearance_saved_preset_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.clearPresetNotice()
                            showSaveLookDialog = true
                        }
                    ) {
                        Text(text = stringResource(R.string.appearance_save_current_look))
                    }
                    if (presetNoticeRes != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(presetNoticeRes!!),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (savedPresets.isEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.appearance_saved_preset_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Spacer(Modifier.height(8.dp))
                        for (preset in savedPresets) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.applySavedPreset(preset.name)
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = preset.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.deleteSavedPreset(preset.name) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = stringResource(
                                            R.string.appearance_saved_preset_delete
                                        )
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

/** One settings group: a card with a title, an optional reset button and content. */
@Composable
private fun AppearanceSectionCard(
    title: String,
    onReset: (() -> Unit)?,
    resetLabel: String? = null,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                if (onReset != null) {
                    TextButton(onClick = onReset) {
                        Text(
                            text = resetLabel
                                ?: stringResource(R.string.appearance_reset_section)
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}

/** One glass per-surface toggle: label + switch. */
@Composable
private fun GlassToggleRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled
        )
    }
}

/** One bar's controls: show/hide, size, corners, colour. */
@Composable
private fun BarSection(
    title: String,
    bar: BarAppearance,
    onChange: (BarAppearance) -> Unit,
    onReset: () -> Unit
) {
    AppearanceSectionCard(title = title, onReset = onReset) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.appearance_show),
                style = MaterialTheme.typography.bodyMedium
            )
            Switch(
                checked = bar.visible,
                onCheckedChange = { onChange(bar.copy(visible = it)) }
            )
        }
        PercentSlider(
            label = stringResource(R.string.appearance_size),
            value = bar.scalePercent,
            range = 80..120,
            onChange = { onChange(bar.copy(scalePercent = it)) },
            enabled = bar.visible
        )
        CornerControls(
            corners = bar.effectiveCorners,
            onChange = { onChange(bar.copy(corners = it)) },
            enabled = bar.visible
        )
        PaletteChips(
            selected = bar.palette,
            onSelect = { onChange(bar.copy(palette = it)) },
            enabled = bar.visible
        )
    }
}

/** One card's controls: size, corners, colour. */
@Composable
private fun CardSection(
    title: String,
    card: CardAppearance,
    onChange: (CardAppearance) -> Unit,
    onReset: () -> Unit
) {
    AppearanceSectionCard(title = title, onReset = onReset) {
        PercentSlider(
            label = stringResource(R.string.appearance_size),
            value = card.scalePercent,
            range = 85..115,
            onChange = { onChange(card.copy(scalePercent = it)) }
        )
        CornerControls(
            corners = card.effectiveCorners,
            onChange = { onChange(card.copy(corners = it)) }
        )
        PaletteChips(
            selected = card.palette,
            onSelect = { onChange(card.copy(palette = it)) }
        )
    }
}

/**
 * One "recently added" row's controls (round 15, wave 2, C3; split
 * music / video in round 16): size, per-corner radii, font size and
 * gap. The music and video sections share this layout but write to
 * their own [RecentCardsAppearance] blocks.
 */
@Composable
private fun RecentCardsSection(
    title: String,
    recent: RecentCardsAppearance,
    onChange: (RecentCardsAppearance) -> Unit,
    onReset: () -> Unit
) {
    AppearanceSectionCard(title = title, onReset = onReset) {
        PercentSlider(
            label = stringResource(R.string.appearance_size),
            value = recent.scalePercent,
            range = 80..130,
            onChange = { onChange(recent.copy(scalePercent = it)) }
        )
        CornerControls(
            corners = recent.effectiveCorners,
            onChange = { onChange(recent.copy(corners = it)) }
        )
        PercentSlider(
            label = stringResource(R.string.appearance_font_size_title),
            value = recent.fontScalePercent,
            range = 85..125,
            onChange = { onChange(recent.copy(fontScalePercent = it)) }
        )
        DpSlider(
            label = stringResource(R.string.appearance_gap),
            value = recent.gapDp,
            range = 0..16,
            onChange = { onChange(recent.copy(gapDp = it)) }
        )
    }
}

/**
 * Per-corner radius controls (round 15, wave 2, C1): a "link all
 * corners" switch — one slider when linked, four sliders (top start /
 * top end / bottom start / bottom end) when unlinked. Turning the link
 * back on collapses the four values into the top-start value.
 */
@Composable
private fun CornerControls(
    corners: CornerRadii,
    onChange: (CornerRadii) -> Unit,
    enabled: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.appearance_corners_link_all),
                style = MaterialTheme.typography.bodyMedium
            )
            Switch(
                checked = corners.linked,
                onCheckedChange = { linked ->
                    onChange(
                        if (linked) corners.withAll(corners.topStart).copy(linked = true)
                        else corners.copy(linked = false)
                    )
                },
                enabled = enabled
            )
        }
        if (corners.linked) {
            DpSlider(
                label = stringResource(R.string.appearance_corners),
                value = corners.topStart,
                range = 0..32,
                onChange = { onChange(corners.withAll(it)) },
                enabled = enabled
            )
        } else {
            DpSlider(
                label = stringResource(R.string.appearance_corner_top_start),
                value = corners.topStart,
                range = 0..32,
                onChange = { onChange(corners.copy(topStartDp = it)) },
                enabled = enabled
            )
            DpSlider(
                label = stringResource(R.string.appearance_corner_top_end),
                value = corners.topEnd,
                range = 0..32,
                onChange = { onChange(corners.copy(topEndDp = it)) },
                enabled = enabled
            )
            DpSlider(
                label = stringResource(R.string.appearance_corner_bottom_start),
                value = corners.bottomStart,
                range = 0..32,
                onChange = { onChange(corners.copy(bottomStartDp = it)) },
                enabled = enabled
            )
            DpSlider(
                label = stringResource(R.string.appearance_corner_bottom_end),
                value = corners.bottomEnd,
                range = 0..32,
                onChange = { onChange(corners.copy(bottomEndDp = it)) },
                enabled = enabled
            )
        }
    }
}

/**
 * "Save current look" dialog (round 15, wave 2, C7): the same
 * AlertDialog + OutlinedTextField pattern as the equalizer's
 * [SavePresetDialog], with appearance strings. An empty name is
 * refused in the dialog; a full list (10 looks) is refused by the
 * ViewModel with a notice.
 */
@Composable
private fun SaveLookDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.appearance_save_look_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.appearance_save_look_body),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        isError = false
                    },
                    label = { Text(stringResource(R.string.appearance_preset_name_label)) },
                    singleLine = true,
                    isError = isError,
                    modifier = Modifier.fillMaxWidth()
                )
                if (isError) {
                    Text(
                        text = stringResource(R.string.error_name_empty),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        isError = true
                    } else {
                        onSave(name.trim())
                        onDismiss()
                    }
                }
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceChips(
    options: List<Pair<String, String>>,
    selected: String?,
    onSelect: (String) -> Unit,
    label: String?
) {
    Column {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(6.dp))
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for ((value, text) in options) {
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    label = { Text(text = text) }
                )
            }
        }
    }
}

/** Palette picker: a colour dot + name per palette. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaletteChips(
    selected: String,
    onSelect: (String) -> Unit,
    enabled: Boolean = true
) {
    Column {
        Text(
            text = stringResource(R.string.appearance_colour),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (name in AppearancePaletteNames.ALL) {
                val colors = appearancePaletteColors(name)
                FilterChip(
                    selected = selected == name,
                    onClick = { onSelect(name) },
                    enabled = enabled,
                    label = { Text(text = paletteName(name)) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(
                                    colors?.container
                                        ?: MaterialTheme.colorScheme.primaryContainer
                                )
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun paletteName(name: String): String = when (name) {
    AppearancePaletteNames.BLUE -> stringResource(R.string.appearance_palette_blue)
    AppearancePaletteNames.GREEN -> stringResource(R.string.appearance_palette_green)
    AppearancePaletteNames.PURPLE -> stringResource(R.string.appearance_palette_purple)
    AppearancePaletteNames.ORANGE -> stringResource(R.string.appearance_palette_orange)
    AppearancePaletteNames.TEAL -> stringResource(R.string.appearance_palette_teal)
    else -> stringResource(R.string.appearance_palette_default)
}

/** One palette row inside the section-colours group. */
@Composable
private fun PaletteRow(
    label: String,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(6.dp))
        PaletteChips(selected = selected, onSelect = onSelect)
    }
}

@Composable
private fun PercentSlider(
    label: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
    enabled: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "$value%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            enabled = enabled
        )
    }
}

@Composable
private fun DpSlider(
    label: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
    enabled: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "${value}dp",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            enabled = enabled
        )
    }
}
