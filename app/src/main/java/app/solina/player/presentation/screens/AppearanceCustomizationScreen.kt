package app.solina.player.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import app.solina.player.data.preferences.GlassPresetNames
import app.solina.player.data.preferences.GlassStyleNames
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
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
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
                val cards = appearance.homeCards
                AppearanceSectionCard(
                    title = stringResource(R.string.appearance_home_cards_title),
                    onReset = { viewModel.resetHomeCards() }
                ) {
                    PercentSlider(
                        label = stringResource(R.string.appearance_size),
                        value = cards.scalePercent,
                        range = 85..115,
                        onChange = {
                            viewModel.setHomeCards(cards.copy(scalePercent = it))
                        }
                    )
                    DpSlider(
                        label = stringResource(R.string.appearance_corners),
                        value = cards.cornerDp,
                        range = 0..32,
                        onChange = {
                            viewModel.setHomeCards(cards.copy(cornerDp = it))
                        }
                    )
                    PaletteChips(
                        selected = cards.palette,
                        onSelect = {
                            viewModel.setHomeCards(cards.copy(palette = it))
                        }
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

            // ---- Floating music bar (colour only) ----
            item(key = "appearance_floating_music_bar") {
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
                    PaletteChips(
                        selected = appearance.floatingMusicBarPalette,
                        onSelect = { viewModel.setFloatingMusicBarPalette(it) }
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
                    DpSlider(
                        label = stringResource(R.string.appearance_glass_radius),
                        value = glass.radiusDp,
                        range = 0..40,
                        onChange = { viewModel.setGlass(glass.copy(radiusDp = it)) },
                        enabled = glass.enabled
                    )
                    PercentSlider(
                        label = stringResource(R.string.appearance_glass_tint),
                        value = glass.tintPercent,
                        range = 0..60,
                        onChange = { viewModel.setGlass(glass.copy(tintPercent = it)) },
                        enabled = glass.enabled
                    )
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
                }
            }
        }
    }
}

/** One settings group: a card with a title, a reset button and content. */
@Composable
private fun AppearanceSectionCard(
    title: String,
    onReset: () -> Unit,
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
                TextButton(onClick = onReset) {
                    Text(
                        text = resetLabel
                            ?: stringResource(R.string.appearance_reset_section)
                    )
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
        DpSlider(
            label = stringResource(R.string.appearance_corners),
            value = bar.cornerDp,
            range = 0..32,
            onChange = { onChange(bar.copy(cornerDp = it)) },
            enabled = bar.visible
        )
        PaletteChips(
            selected = bar.palette,
            onSelect = { onChange(bar.copy(palette = it)) },
            enabled = bar.visible
        )
    }
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
