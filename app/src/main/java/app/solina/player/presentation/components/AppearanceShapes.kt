package app.solina.player.presentation.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import app.solina.player.data.preferences.CornerRadii

/**
 * Per-corner radii (round 15, wave 2, C1): turn a [CornerRadii] into
 * the Compose shape the bars and cards actually draw with. Kept here
 * (not in the data layer) so AppearancePreferences stays free of
 * Compose types.
 */
fun CornerRadii.toShape(): RoundedCornerShape = RoundedCornerShape(
    topStart = topStart.dp,
    topEnd = topEnd.dp,
    bottomEnd = bottomEnd.dp,
    bottomStart = bottomStart.dp
)
