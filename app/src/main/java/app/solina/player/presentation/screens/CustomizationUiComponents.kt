package app.solina.player.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.solina.player.R
import app.solina.player.data.preferences.AppearancePaletteNames
import app.solina.player.data.preferences.BarAppearance
import app.solina.player.data.preferences.CardAppearance
import app.solina.player.data.preferences.CornerRadii
import app.solina.player.data.preferences.RecentCardsAppearance
import app.solina.player.presentation.components.appearancePaletteColors

/** Shared section heading across Home, Music, Video and Global customisation screens. */
@Composable
internal fun CustomizationGroupHeading(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 6.dp, top = 4.dp, bottom = 2.dp)
    )
}

/** One settings card group with a title, optional subtitle, optional reset action and content. */
@Composable
internal fun AppearanceSectionCard(
    title: String,
    onReset: (() -> Unit)?,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (onReset != null) {
                    IconButton(onClick = onReset) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = stringResource(R.string.appearance_reset_section),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            content()
        }
    }
}

/** Clean navigation tile used to jump to related customisation or settings screens. */
@Composable
internal fun CustomizationLinkCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    iconContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    iconTint: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Standard switch row with leading icon, title and optional subtitle. */
@Composable
internal fun CustomizationSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 0.85f else 0.5f)
            )
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}

/** One glass per-surface toggle: label + switch. */
@Composable
internal fun GlassToggleRow(
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
internal fun BarSection(
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
internal fun CardSection(
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

/** One "recently added" row's controls: size, per-corner radii, font size and gap. */
@Composable
internal fun RecentCardsSection(
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

/** Per-corner radius controls: linked single slider or four independent sliders. */
@Composable
internal fun CornerControls(
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

/** "Save current look" dialog. */
@Composable
internal fun SaveLookDialog(
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
internal fun ChoiceChips(
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
internal fun PaletteChips(
    selected: String,
    onSelect: (String) -> Unit,
    enabled: Boolean = true,
    showLabel: Boolean = true
) {
    Column {
        if (showLabel) {
            Text(
                text = stringResource(R.string.appearance_colour),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(6.dp))
        }
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
internal fun paletteName(name: String): String = when (name) {
    AppearancePaletteNames.BLUE -> stringResource(R.string.appearance_palette_blue)
    AppearancePaletteNames.GREEN -> stringResource(R.string.appearance_palette_green)
    AppearancePaletteNames.PURPLE -> stringResource(R.string.appearance_palette_purple)
    AppearancePaletteNames.ORANGE -> stringResource(R.string.appearance_palette_orange)
    AppearancePaletteNames.TEAL -> stringResource(R.string.appearance_palette_teal)
    else -> stringResource(R.string.appearance_palette_default)
}

@Composable
internal fun PaletteRow(
    label: String,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(6.dp))
        PaletteChips(selected = selected, onSelect = onSelect, showLabel = false)
    }
}

@Composable
internal fun PercentSlider(
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
internal fun DpSlider(
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
