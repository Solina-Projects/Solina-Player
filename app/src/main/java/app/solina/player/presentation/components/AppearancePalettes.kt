package app.solina.player.presentation.components

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import app.solina.player.data.preferences.AppearancePaletteNames

/**
 * Colour tables for the per-section appearance customisation (bug-fix
 * round 2026-11-17, item 6). A palette is a small set of roles; the
 * repository stores only the palette NAME, this file owns the colours,
 * so a palette can be retuned without touching stored settings.
 *
 * "default" has no table (null) — the caller keeps the app theme
 * scheme. Every other palette is explicit and works in light and dark
 * themes because the surfaces it tints use these roles directly.
 */
data class PaletteColors(
    /** Main surface (bar pill, card). */
    val container: Color,
    /** Second surface (the Video card next to the Music card). */
    val containerAlt: Color,
    /** Text/icons on the surfaces. */
    val onContainer: Color,
    /** Accent: play circle, EQ bars, progress. */
    val primary: Color,
    /** Icons/text on the accent. */
    val onPrimary: Color
)

/** Colour table for a palette name, or null for "default" (theme). */
fun appearancePaletteColors(name: String): PaletteColors? = when (name) {
    AppearancePaletteNames.BLUE -> PaletteColors(
        container = Color(0xFFDCE9FF),
        containerAlt = Color(0xFFC4D7FF),
        onContainer = Color(0xFF1B3A6B),
        primary = Color(0xFF2F6FED),
        onPrimary = Color.White
    )

    AppearancePaletteNames.GREEN -> PaletteColors(
        container = Color(0xFFD9F2DF),
        containerAlt = Color(0xFFBDE8C8),
        onContainer = Color(0xFF1D4D2B),
        primary = Color(0xFF2E8B47),
        onPrimary = Color.White
    )

    AppearancePaletteNames.PURPLE -> PaletteColors(
        container = Color(0xFFE9DCFF),
        containerAlt = Color(0xFFD7C2FF),
        onContainer = Color(0xFF3D1E6B),
        primary = Color(0xFF7A3FF0),
        onPrimary = Color.White
    )

    AppearancePaletteNames.ORANGE -> PaletteColors(
        container = Color(0xFFFFE3C7),
        containerAlt = Color(0xFFFFD1A1),
        onContainer = Color(0xFF5C3200),
        primary = Color(0xFFD97A1F),
        onPrimary = Color.White
    )

    AppearancePaletteNames.TEAL -> PaletteColors(
        container = Color(0xFFC8F0EC),
        containerAlt = Color(0xFFA5E4DE),
        onContainer = Color(0xFF0B3D3A),
        primary = Color(0xFF0E7C74),
        onPrimary = Color.White
    )

    else -> null
}

/**
 * Effective palette for one surface: its own palette wins when set,
 * otherwise its section palette, otherwise the theme (null).
 */
fun effectivePalette(barPalette: String, sectionPalette: String): PaletteColors? =
    appearancePaletteColors(barPalette) ?: appearancePaletteColors(sectionPalette)

/** A [ColorScheme] with the palette roles swapped in (theme otherwise). */
fun ColorScheme.withPalette(palette: PaletteColors): ColorScheme = copy(
    primaryContainer = palette.container,
    onPrimaryContainer = palette.onContainer,
    secondaryContainer = palette.containerAlt,
    onSecondaryContainer = palette.onContainer,
    primary = palette.primary,
    onPrimary = palette.onPrimary
)
