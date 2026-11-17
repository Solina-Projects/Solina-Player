package app.solina.player.presentation.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.solina.player.data.preferences.GlassAppearance
import app.solina.player.data.preferences.GlassStyleNames
import app.solina.player.presentation.viewmodel.AppearanceViewModel
import dev.blurengine.mrsluffy.compose.blurSurface

/**
 * Glass & blur surfaces (blur-engine test build, 2026-11-17).
 *
 * blur-engine (`io.github.mrsluffy:blur-compose:1.0.0`, Apache-2.0)
 * blurs whatever the window draws *behind* the element, on the GPU
 * RenderEffect path (API 31+, our minSdk). Its liquid-glass
 * refraction is an AGSL shader that only exists on API 33+; on
 * API 31/32 the library degrades gracefully to blur + tint + rim
 * light, so no SDK check is needed here — just don't promise
 * refraction in the UI copy below API 33.
 *
 * Rules this file enforces:
 * - Glass is DEFAULT OFF and every surface checks the master switch
 *   plus its own per-surface toggle; when off, NO blurSurface
 *   modifier is applied at all (zero capture cost).
 * - The tint colour is the bar/section palette colour at the chosen
 *   alpha — the pink brand look survives; there is no separate
 *   tint-colour picker.
 * - Settings are read INSIDE the `blurSurface { }` block, as the
 *   library docs require (the block runs during the frame, so a
 *   slider drag does not recompose every frame).
 * - NEVER over video: PlayerView is a SurfaceView and no blur
 *   library can capture it — glass over playing video would show a
 *   black hole. Video-player bars stay on the gradient scrim.
 * - Home cards are skipped: Home's background is a flat colour and
 *   glass over a flat colour only looks grey.
 *
 * Style mapping (glasswork's numbers, implemented natively):
 * - Frosted: blur + tint + saturation.
 * - Liquid glass: + rim light / refraction (14 dp rim, 20 dp edge,
 *   0.35 dispersion, 0.8 highlight, 0.16 gloss).
 * - Clear glass: tiny radius (3 dp) + strong refraction (26 dp,
 *   40 dp edge) — sharp content, bent at the edges.
 */

/** The activity's decor view, for blurring what's behind a dialog window. */
@Composable
private fun activityDecorView(): View? {
    val context = LocalContext.current
    var ctx: Context? = context
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx.window?.decorView
        ctx = ctx.baseContext
    }
    return null
}

/**
 * One glass surface: a bar, a sheet, or a dialog. [shape] clips the
 * blur; [rimCornerDp] is the corner radius the liquid-glass rim
 * follows (for sheets the rim is 28 dp while the clip is only
 * rounded on top — the sheet's bottom corners sit on the screen
 * edge, so rounding them would cut a notch out of the blur).
 *
 * When [source] is set, the blur captures that view instead of the
 * element's own window — needed for sheets/dialogs, which live in a
 * dialog window while the content behind them lives in the activity
 * window. Both windows are full-screen, so the coordinates line up;
 * if they ever don't, the library falls back to showing the tint.
 */
@Composable
fun Modifier.glassSurface(
    glass: GlassAppearance,
    tintColor: Color,
    enabled: Boolean,
    shape: Shape,
    rimCornerDp: Dp,
    source: View? = null,
    // R15 F7: minimum tint for this surface (0 = the user's slider
    // value as-is). Bars use 0.25 so text over a busy blurred list
    // never sits on a near-invisible pill.
    minTintAlpha: Float = 0f,
    // Round 15, wave 2, C5: mini-bars and sheets/dialogs have their
    // own blur radius + tint. Bars pass false, sheets/dialogs true.
    useSheetValues: Boolean = false
): Modifier {
    if (!enabled || !glass.enabled) return this
    // Read inside the block: slider drags then update the blur
    // without recomposing every animation frame.
    return this.blurSurface {
        // Per-surface values (C5): the bar sliders drive the mini
        // bars, the sheet sliders drive sheets/dialogs; saturation
        // and refraction stay global.
        val groupRadius = if (useSheetValues) glass.sheetRadius else glass.miniBarRadius
        val groupTintAlpha = if (useSheetValues) glass.sheetTintAlpha else glass.miniBarTintAlpha
        val effectiveTintAlpha = maxOf(groupTintAlpha, minTintAlpha)
        radius = groupRadius.dp
        tint(tintColor.copy(alpha = effectiveTintAlpha))
        saturation = glass.saturation
        when (glass.style) {
            GlassStyleNames.CLEAR -> {
                radius = 3.dp
                tint(tintColor.copy(alpha = minOf(effectiveTintAlpha, 0.08f)))
                glass(
                    refraction = 26.dp,
                    edgeWidth = 40.dp,
                    dispersion = 0.35f,
                    highlight = 0.8f,
                    gloss = 0.16f
                )
                cornerRadius(rimCornerDp)
                this.shape = shape
            }

            GlassStyleNames.LIQUID -> {
                glass(
                    refraction = glass.refraction.dp,
                    edgeWidth = 20.dp,
                    dispersion = 0.35f,
                    highlight = 0.8f,
                    gloss = 0.16f
                )
                cornerRadius(rimCornerDp)
                this.shape = shape
            }

            else -> {
                this.shape = shape
            }
        }
        source?.let { this.source = it }
    }
}

/** Glass for a playback bar: stadium pill, rim follows the bar's own corners.
 * R15 F7: bars floor the tint at 25% so the pill never goes
 * near-invisible over a busy blurred list (the user's slider can
 * still raise it; Clear style keeps its own 8% cap). */
@Composable
fun Modifier.glassBar(
    glass: GlassAppearance,
    tintColor: Color,
    cornerDp: Int,
    enabled: Boolean
): Modifier = glassSurface(
    glass = glass,
    tintColor = tintColor,
    enabled = enabled,
    shape = RoundedCornerShape(cornerDp.dp),
    rimCornerDp = cornerDp.dp,
    minTintAlpha = 0.25f
)

/** Glass for a playback bar with per-corner radii (round 15, wave 2, C1). */
@Composable
fun Modifier.glassBar(
    glass: GlassAppearance,
    tintColor: Color,
    corners: app.solina.player.data.preferences.CornerRadii,
    enabled: Boolean
): Modifier = glassSurface(
    glass = glass,
    tintColor = tintColor,
    enabled = enabled,
    shape = corners.toShape(),
    rimCornerDp = maxOf(
        corners.topStart,
        corners.topEnd,
        corners.bottomStart,
        corners.bottomEnd
    ).dp,
    minTintAlpha = 0.25f
)

/** Glass for a bottom sheet: rounded on top only, capturing the activity behind it. */
@Composable
fun Modifier.glassSheet(
    glass: GlassAppearance,
    tintColor: Color,
    enabled: Boolean
): Modifier {
    val activityView = activityDecorView()
    return glassSurface(
        glass = glass,
        tintColor = tintColor,
        enabled = enabled,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        rimCornerDp = 28.dp,
        source = activityView,
        // Round 15, wave 2, C5: sheets use the sheet radius/tint group.
        useSheetValues = true
    )
}

/** Glass for a dialog card: fully rounded (M3 dialog shape). */
@Composable
fun Modifier.glassDialog(
    glass: GlassAppearance,
    tintColor: Color,
    enabled: Boolean
): Modifier {
    val activityView = activityDecorView()
    return glassSurface(
        glass = glass,
        tintColor = tintColor,
        enabled = enabled,
        shape = RoundedCornerShape(28.dp),
        rimCornerDp = 28.dp,
        source = activityView,
        // Round 15, wave 2, C5: dialogs use the sheet radius/tint group.
        useSheetValues = true
    )
}

/**
 * A [ModalBottomSheet] with optional glass. Drop-in replacement:
 * same parameters as Material 3's sheet. When glass is on (master
 * switch + the "Bottom sheets" toggle), the container goes
 * transparent, the tonal elevation is dropped (it would paint a
 * solid overlay over the blur), and the content gets a glass
 * surface that captures the activity window behind the dialog.
 * When glass is off it behaves exactly like [ModalBottomSheet].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color? = null,
    contentColor: Color? = null,
    tonalElevation: Dp? = null,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    contentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.windowInsets },
    appearanceViewModel: AppearanceViewModel = hiltViewModel(),
    content: @Composable ColumnScope.() -> Unit
) {
    val appearance by appearanceViewModel.appearance.collectAsStateWithLifecycle()
    val glass = appearance.glass
    val glassOn = glass.enabled && glass.sheets
    val effectiveContainer = containerColor ?: BottomSheetDefaults.ContainerColor
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = shape,
        containerColor = if (glassOn) Color.Transparent else effectiveContainer,
        contentColor = contentColor ?: contentColorFor(effectiveContainer),
        tonalElevation = if (glassOn) 0.dp else (tonalElevation ?: BottomSheetDefaults.Elevation),
        scrimColor = scrimColor,
        dragHandle = dragHandle,
        contentWindowInsets = contentWindowInsets
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassSheet(glass = glass, tintColor = effectiveContainer, enabled = glassOn)
        ) {
            content()
        }
    }
}
