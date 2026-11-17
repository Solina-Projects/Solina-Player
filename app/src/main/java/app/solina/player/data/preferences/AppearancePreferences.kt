package app.solina.player.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Per-section appearance customisation (bug-fix round 2026-11-17, item 6).
 *
 * Nearly every visible part of the player can be tuned from Settings
 * ("Customise appearance"): the Home bars, the Home cards, the video
 * bar, the floating music bar's colour, the colours of the Home /
 * Music / Video sections each separately, the font family and the font
 * size scale — plus ready-made presets and a reset per section.
 *
 * Stored as one JSON document in the shared "settings" DataStore, the
 * same way [HomeCustomizationRepository] stores the Home order, so a
 * whole appearance is saved atomically. Every value is a plain number
 * or name; the colour tables live in
 * [app.solina.player.presentation.components.AppearancePalettes] so
 * this file stays free of Compose types.
 *
 * Every section is independent: changing the Home music bar never
 * touches the video bar, and a section palette only tints that
 * section's surfaces (a per-bar palette overrides the section one
 * for that bar). "default" palette means "follow the app theme".
 */

/** Colour palette names. "default" follows the app theme scheme. */
object AppearancePaletteNames {
    const val DEFAULT = "default"
    const val BLUE = "blue"
    const val GREEN = "green"
    const val PURPLE = "purple"
    const val ORANGE = "orange"
    const val TEAL = "teal"

    val ALL = listOf(DEFAULT, BLUE, GREEN, PURPLE, ORANGE, TEAL)
}

/** Font family choices. "rounded" is the app's default face. */
object AppearanceFontFamilies {
    const val ROUNDED = "rounded"
    const val VAZIRMATN = "vazirmatn"
    const val SYSTEM = "system"

    val ALL = listOf(ROUNDED, VAZIRMATN, SYSTEM)
}

/**
 * Glass styles (blur-engine test build, 2026-11-17). "frosted" is plain
 * blur + tint; "liquid" adds the rim light / refraction glass; "clear"
 * is a tiny radius with strong refraction (glasswork's "clear glass":
 * sharp content, bent at the edges).
 */
object GlassStyleNames {
    const val FROSTED = "frosted"
    const val LIQUID = "liquid"
    const val CLEAR = "clear"

    val ALL = listOf(FROSTED, LIQUID, CLEAR)
}

/** Ready-made glass looks: a preset is just a whole [GlassAppearance]. */
object GlassPresetNames {
    const val OFF = "off"
    const val SUBTLE = "subtle"
    const val FROSTED = "frosted"
    const val LIQUID = "liquid"

    val ALL = listOf(OFF, SUBTLE, FROSTED, LIQUID)
}

/**
 * Glass & blur (blur-engine test build, 2026-11-17): frosted / liquid
 * glass surfaces, driven by blur-engine's `blurSurface` (RenderEffect
 * GPU path on API 31+). DEFAULT OFF — glass costs a capture + blur
 * per surface per frame, and the user must opt in per the test plan.
 *
 * Per-surface toggles: mini-player bars, bottom sheets, dialogs &
 * menus, and the Now Playing background (a static, one-shot blur of
 * the album art — no per-frame cost). Video-player bars are
 * deliberately NOT here: video renders on a SurfaceView, which no
 * blur library can capture, so glass over playing video would show
 * a black hole; those bars stay on the gradient scrim. Home cards
 * are skipped too: Home's background is a flat colour and glass
 * over a flat colour only looks grey.
 *
 * The tint colour is NOT stored here: glass tint comes from the
 * bar/section palette colour at [tintPercent] alpha, so the pink
 * brand look survives. Refraction is an AGSL shader that only
 * exists on API 33+; on API 31/32 blur-engine degrades gracefully
 * to blur + tint + rim light, no crash.
 */
@Serializable
data class GlassAppearance(
    val enabled: Boolean = false,
    val miniBars: Boolean = true,
    val sheets: Boolean = true,
    val dialogs: Boolean = true,
    val nowPlayingBackground: Boolean = true,
    val radiusDp: Int = 24,
    val tintPercent: Int = 20,
    val saturationPercent: Int = 130,
    val style: String = GlassStyleNames.FROSTED,
    val refractionDp: Int = 14
) {
    /** Blur radius, 0–40 dp. */
    val radius: Int get() = radiusDp.coerceIn(0, 40)
    /** Tint opacity, 0–60%. */
    val tintAlpha: Float get() = (tintPercent.coerceIn(0, 60) / 100f)
    /** Saturation, 100–160% (1.0–1.6x; glasswork's Lumno glass is 1.6–1.8x). */
    val saturation: Float get() = (saturationPercent.coerceIn(100, 160) / 100f)
    /** Refraction distance, 0–26 dp (liquid / clear glass only, API 33+). */
    val refraction: Int get() = refractionDp.coerceIn(0, 26)
}

/** Ready-made looks: a preset is just a whole [AppearanceSettings]. */
object AppearancePresetNames {
    const val DEFAULT = "default"
    const val COMPACT = "compact"
    const val BOLD = "bold"
    const val NIGHT = "night"

    val ALL = listOf(DEFAULT, COMPACT, BOLD, NIGHT)
}

/** One playback bar: show/hide, size (percent), corner radius, palette. */
@Serializable
data class BarAppearance(
    val visible: Boolean = true,
    val scalePercent: Int = 100,
    val cornerDp: Int = 32,
    val palette: String = AppearancePaletteNames.DEFAULT
) {
    /** 0.8x–1.2x, sanitised so a corrupt stored value cannot break layout. */
    val scale: Float get() = (scalePercent.coerceIn(80, 120) / 100f)
    val cornerRadiusDp: Int get() = cornerDp.coerceIn(0, 32)
}

/** One Home entry card: size (percent), corner radius, palette. */
@Serializable
data class CardAppearance(
    val scalePercent: Int = 100,
    val cornerDp: Int = 20,
    val palette: String = AppearancePaletteNames.DEFAULT
) {
    /** 0.85x–1.15x, sanitised. */
    val scale: Float get() = (scalePercent.coerceIn(85, 115) / 100f)
    val cornerRadiusDp: Int get() = cornerDp.coerceIn(0, 32)
}

@Serializable
data class AppearanceSettings(
    /** Fixed music bar under "Recently added music" on Home. */
    val homeMusicBar: BarAppearance = BarAppearance(),
    /** Fixed video bar under "Recently added videos" on Home. */
    val homeVideoBar: BarAppearance = BarAppearance(),
    /** Floating video bar above the bottom bar, in the Video section. */
    val sectionVideoBar: BarAppearance = BarAppearance(),
    /** Floating music mini-player bar (Music section): colour only — its
     * size and corners are fixed by the player sheet's layout math. */
    val floatingMusicBarPalette: String = AppearancePaletteNames.DEFAULT,
    /** The two big Music / Video entry cards on Home. */
    val homeCards: CardAppearance = CardAppearance(),
    /** Section accent palettes, each independent. */
    val homeSectionPalette: String = AppearancePaletteNames.DEFAULT,
    val musicSectionPalette: String = AppearancePaletteNames.DEFAULT,
    val videoSectionPalette: String = AppearancePaletteNames.DEFAULT,
    /** App-wide font family (applied through the theme typography). */
    val fontFamily: String = AppearanceFontFamilies.ROUNDED,
    /** Font size scale for the customisable surfaces (85–125%). */
    val fontScalePercent: Int = 100,
    /** Glass & blur (blur-engine test build): default OFF. */
    val glass: GlassAppearance = GlassAppearance()
) {
    val fontScale: Float get() = (fontScalePercent.coerceIn(85, 125) / 100f)
}

@Singleton
class AppearancePreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json
) {
    private object Keys {
        val APPEARANCE_SETTINGS = stringPreferencesKey("appearance_settings_v1")
    }

    companion object {
        fun defaultSettings(): AppearanceSettings = AppearanceSettings()

        fun presetSettings(preset: String): AppearanceSettings = when (preset) {
            AppearancePresetNames.COMPACT -> AppearanceSettings(
                homeMusicBar = BarAppearance(scalePercent = 90, cornerDp = 24),
                homeVideoBar = BarAppearance(scalePercent = 90, cornerDp = 24),
                sectionVideoBar = BarAppearance(scalePercent = 90, cornerDp = 24),
                homeCards = CardAppearance(scalePercent = 90, cornerDp = 16),
                fontScalePercent = 95
            )

            AppearancePresetNames.BOLD -> AppearanceSettings(
                homeMusicBar = BarAppearance(scalePercent = 110, cornerDp = 32),
                homeVideoBar = BarAppearance(scalePercent = 110, cornerDp = 32),
                sectionVideoBar = BarAppearance(scalePercent = 110, cornerDp = 32),
                homeCards = CardAppearance(scalePercent = 108, cornerDp = 28),
                fontScalePercent = 110
            )

            AppearancePresetNames.NIGHT -> AppearanceSettings(
                homeMusicBar = BarAppearance(palette = AppearancePaletteNames.PURPLE),
                homeVideoBar = BarAppearance(palette = AppearancePaletteNames.BLUE),
                sectionVideoBar = BarAppearance(palette = AppearancePaletteNames.BLUE),
                floatingMusicBarPalette = AppearancePaletteNames.PURPLE,
                homeSectionPalette = AppearancePaletteNames.TEAL,
                musicSectionPalette = AppearancePaletteNames.PURPLE,
                videoSectionPalette = AppearancePaletteNames.BLUE,
                homeCards = CardAppearance(palette = AppearancePaletteNames.TEAL)
            )

            else -> AppearanceSettings()
        }

        /** A glass preset touches ONLY the glass block (other settings stay). */
        fun glassPresetSettings(preset: String): GlassAppearance = when (preset) {
            GlassPresetNames.SUBTLE -> GlassAppearance(
                enabled = true,
                radiusDp = 16,
                tintPercent = 15,
                saturationPercent = 120,
                style = GlassStyleNames.FROSTED
            )

            GlassPresetNames.FROSTED -> GlassAppearance(
                enabled = true,
                radiusDp = 28,
                tintPercent = 25,
                saturationPercent = 130,
                style = GlassStyleNames.FROSTED
            )

            GlassPresetNames.LIQUID -> GlassAppearance(
                enabled = true,
                radiusDp = 28,
                tintPercent = 14,
                saturationPercent = 130,
                style = GlassStyleNames.LIQUID,
                refractionDp = 14
            )

            else -> GlassAppearance()
        }
    }

    /** Whole appearance, live. Falls back to defaults when unset or corrupt. */
    val appearanceFlow: Flow<AppearanceSettings> = dataStore.data.map { preferences ->
        val raw = preferences[Keys.APPEARANCE_SETTINGS] ?: return@map defaultSettings()
        runCatching { json.decodeFromString<AppearanceSettings>(raw) }
            .getOrElse { defaultSettings() }
    }

    suspend fun update(transform: (AppearanceSettings) -> AppearanceSettings) {
        val current = appearanceFlow.first()
        val updated = transform(current)
        dataStore.edit { preferences ->
            preferences[Keys.APPEARANCE_SETTINGS] = json.encodeToString(updated)
        }
    }

    suspend fun applyPreset(preset: String) {
        dataStore.edit { preferences ->
            preferences[Keys.APPEARANCE_SETTINGS] =
                json.encodeToString(presetSettings(preset))
        }
    }

    suspend fun resetHomeMusicBar() =
        update { it.copy(homeMusicBar = BarAppearance()) }

    suspend fun resetHomeVideoBar() =
        update { it.copy(homeVideoBar = BarAppearance()) }

    suspend fun resetSectionVideoBar() =
        update { it.copy(sectionVideoBar = BarAppearance()) }

    suspend fun resetFloatingMusicBar() =
        update { it.copy(floatingMusicBarPalette = AppearancePaletteNames.DEFAULT) }

    suspend fun resetHomeCards() =
        update { it.copy(homeCards = CardAppearance()) }

    suspend fun resetSectionColors() = update {
        it.copy(
            homeSectionPalette = AppearancePaletteNames.DEFAULT,
            musicSectionPalette = AppearancePaletteNames.DEFAULT,
            videoSectionPalette = AppearancePaletteNames.DEFAULT
        )
    }

    suspend fun resetTypography() = update {
        it.copy(
            fontFamily = AppearanceFontFamilies.ROUNDED,
            fontScalePercent = 100
        )
    }

    suspend fun resetGlass() = update { it.copy(glass = GlassAppearance()) }

    suspend fun resetAll() {
        dataStore.edit { preferences ->
            preferences[Keys.APPEARANCE_SETTINGS] =
                json.encodeToString(defaultSettings())
        }
    }
}
