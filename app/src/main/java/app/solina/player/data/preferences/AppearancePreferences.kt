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
    val refractionDp: Int = 14,
    /**
     * Per-surface glass (round 15, wave 2, C5): mini-bars and
     * sheets/dialogs have their own blur radius + tint. Null means
     * "not set yet" — [migrated] fills it from the old single
     * [radiusDp]/[tintPercent], so a stored look keeps its look.
     * Saturation stays one global value.
     */
    val miniBarRadiusDp: Int? = null,
    val miniBarTintPercent: Int? = null,
    val sheetRadiusDp: Int? = null,
    val sheetTintPercent: Int? = null
) {
    /** Blur radius, 0–40 dp. */
    val radius: Int get() = radiusDp.coerceIn(0, 40)
    /** Tint opacity, 0–60%. */
    val tintAlpha: Float get() = (tintPercent.coerceIn(0, 60) / 100f)
    /** Saturation, 100–160% (1.0–1.6x; glasswork's Lumno glass is 1.6–1.8x). */
    val saturation: Float get() = (saturationPercent.coerceIn(100, 160) / 100f)
    /** Refraction distance, 0–26 dp (liquid / clear glass only, API 33+). */
    val refraction: Int get() = refractionDp.coerceIn(0, 26)

    /** Mini-player bars: their own radius, or the old single value. */
    val miniBarRadius: Int get() = (miniBarRadiusDp ?: radiusDp).coerceIn(0, 40)
    /** Mini-player bars: their own tint, or the old single value. */
    val miniBarTintAlpha: Float get() = ((miniBarTintPercent ?: tintPercent).coerceIn(0, 60) / 100f)
    /** Sheets, dialogs & the Now Playing backdrop: their own radius, or the old single value. */
    val sheetRadius: Int get() = (sheetRadiusDp ?: radiusDp).coerceIn(0, 40)
    /** Sheets, dialogs & the Now Playing backdrop: their own tint, or the old single value. */
    val sheetTintAlpha: Float get() = ((sheetTintPercent ?: tintPercent).coerceIn(0, 60) / 100f)

    /** Fill unset per-surface values from the old single radius/tint. */
    fun migrated(): GlassAppearance = copy(
        miniBarRadiusDp = miniBarRadiusDp ?: radiusDp,
        miniBarTintPercent = miniBarTintPercent ?: tintPercent,
        sheetRadiusDp = sheetRadiusDp ?: radiusDp,
        sheetTintPercent = sheetTintPercent ?: tintPercent
    )
}

/**
 * Four corner radii (dp) + a "link all corners" toggle (round 15,
 * wave 2, C1). A bar or card used to have one radius for all four
 * corners; now each corner is its own slider, or one slider when
 * [linked]. Stored values migrate: the old single radius becomes all
 * four corners (see [BarAppearance.migrated] / [CardAppearance.migrated]).
 */
@Serializable
data class CornerRadii(
    val topStartDp: Int = 32,
    val topEndDp: Int = 32,
    val bottomStartDp: Int = 32,
    val bottomEndDp: Int = 32,
    val linked: Boolean = true
) {
    companion object {
        fun all(radiusDp: Int, linked: Boolean = true) =
            CornerRadii(radiusDp, radiusDp, radiusDp, radiusDp, linked)
    }

    /** All four corners to the same radius (keeps the link state). */
    fun withAll(radiusDp: Int) = copy(
        topStartDp = radiusDp,
        topEndDp = radiusDp,
        bottomStartDp = radiusDp,
        bottomEndDp = radiusDp
    )

    /** Each corner pulled in by [insetDp] (never below 0) — for an inner thumb inside a card. */
    fun insetBy(insetDp: Int) = copy(
        topStartDp = maxOf(0, topStartDp - insetDp),
        topEndDp = maxOf(0, topEndDp - insetDp),
        bottomStartDp = maxOf(0, bottomStartDp - insetDp),
        bottomEndDp = maxOf(0, bottomEndDp - insetDp)
    )

    val topStart: Int get() = topStartDp.coerceIn(0, 32)
    val topEnd: Int get() = topEndDp.coerceIn(0, 32)
    val bottomStart: Int get() = bottomStartDp.coerceIn(0, 32)
    val bottomEnd: Int get() = bottomEndDp.coerceIn(0, 32)
}

/** Seek-bar look (round 15, wave 2, C4): the wavy expressive slider or a plain M3 slider. */
object SeekBarStyleNames {
    const val WAVY = "wavy"
    const val CLASSIC = "classic"

    val ALL = listOf(WAVY, CLASSIC)
}

/** One user-saved look (round 15, wave 2, C7): a name + a whole [AppearanceSettings]. */
@Serializable
data class SavedAppearancePreset(
    val name: String,
    val settings: AppearanceSettings
)

/** Cap on user-saved looks (round 15, wave 2, C7). */
const val MAX_SAVED_APPEARANCE_PRESETS = 10

/** Result of saving a look: saved, or refused because the list is full. */
enum class SavePresetResult { SAVED, LIMIT_REACHED }

/** Ready-made looks: a preset is just a whole [AppearanceSettings]. */
object AppearancePresetNames {
    const val DEFAULT = "default"
    const val COMPACT = "compact"
    const val BOLD = "bold"
    const val NIGHT = "night"

    val ALL = listOf(DEFAULT, COMPACT, BOLD, NIGHT)
}

/** One playback bar: show/hide, size (percent), corner radii, palette. */
@Serializable
data class BarAppearance(
    val visible: Boolean = true,
    val scalePercent: Int = 100,
    /**
     * Legacy single corner radius (round 15, wave 2, C1): kept so an
     * old stored look still decodes. [corners] wins when set;
     * [migrated] copies this value into all four corners.
     */
    val cornerDp: Int = 32,
    val corners: CornerRadii? = null,
    val palette: String = AppearancePaletteNames.DEFAULT
) {
    /** 0.8x–1.2x, sanitised so a corrupt stored value cannot break layout. */
    val scale: Float get() = (scalePercent.coerceIn(80, 120) / 100f)

    /** The four corner radii: the stored per-corner values, or the old single value on all four. */
    val effectiveCorners: CornerRadii get() = corners ?: CornerRadii.all(cornerDp)

    /** Fill unset per-corner values from the old single radius. */
    fun migrated(): BarAppearance =
        if (corners == null) copy(corners = CornerRadii.all(cornerDp)) else this
}

/** One Home entry card: size (percent), corner radii, palette. */
@Serializable
data class CardAppearance(
    val scalePercent: Int = 100,
    /** Legacy single corner radius — see [BarAppearance.cornerDp]. */
    val cornerDp: Int = 20,
    val corners: CornerRadii? = null,
    val palette: String = AppearancePaletteNames.DEFAULT
) {
    /** 0.85x–1.15x, sanitised. */
    val scale: Float get() = (scalePercent.coerceIn(85, 115) / 100f)

    /** The four corner radii: the stored per-corner values, or the old single value on all four. */
    val effectiveCorners: CornerRadii get() = corners ?: CornerRadii.all(cornerDp)

    /** Fill unset per-corner values from the old single radius. */
    fun migrated(): CardAppearance =
        if (corners == null) copy(corners = CornerRadii.all(cornerDp)) else this
}

/**
 * Home "Recently added" cards (round 15, wave 2, C3): until now their
 * width, thumb, corners, fonts and gaps were all hardcoded. Card
 * width scale 80–130% of the old 104/118 dp, own font scale 85–125%
 * (on top of the hardcoded 11/10/9 sp bases), gap between cards
 * 0–16 dp, per-corner radii (old 14 dp, inner thumb 10 dp).
 */
@Serializable
data class RecentCardsAppearance(
    val scalePercent: Int = 100,
    /** Legacy single corner radius — see [BarAppearance.cornerDp]. */
    val cornerDp: Int = 14,
    val corners: CornerRadii? = null,
    val fontScalePercent: Int = 100,
    val gapDp: Int = 8
) {
    /** 0.8x–1.3x card width, sanitised. */
    val scale: Float get() = (scalePercent.coerceIn(80, 130) / 100f)
    /** 0.85x–1.25x, sanitised. */
    val fontScale: Float get() = (fontScalePercent.coerceIn(85, 125) / 100f)
    /** Gap between cards, 0–16 dp. */
    val gap: Int get() = gapDp.coerceIn(0, 16)

    /** The four corner radii: the stored per-corner values, or the old single value on all four. */
    val effectiveCorners: CornerRadii get() = corners ?: CornerRadii.all(cornerDp)

    /** Fill unset per-corner values from the old single radius. */
    fun migrated(): RecentCardsAppearance =
        if (corners == null) copy(corners = CornerRadii.all(cornerDp)) else this
}

/**
 * Library / search / video-list rows (round 15, wave 2, C4): the row
 * corner radius. The song row's highlight morph still animates from
 * this radius to the 50 dp pill — this only moves its resting
 * radius. Row fonts come from [AppearanceSettings.listFontScalePercent].
 */
@Serializable
data class ListRowsAppearance(
    val cornerDp: Int = 24
) {
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
    /**
     * List font size (round 15, wave 2, C2): a second, separate scale
     * for library / search / video-list rows (85–125%). The main
     * scale stays on entry cards, bars and the full-player metadata;
     * lists get their own slider so a big list font does not blow up
     * the player.
     */
    val listFontScalePercent: Int = 100,
    /** Home "Recently added" cards (round 15, wave 2, C3). */
    val recentCards: RecentCardsAppearance = RecentCardsAppearance(),
    /** Library / search / video-list row corners (round 15, wave 2, C4). */
    val listRows: ListRowsAppearance = ListRowsAppearance(),
    /**
     * Your Mix card (round 15, wave 2, C4): a [CardAppearance] whose
     * default corner (30 dp) matches the card's old hardcoded radius.
     * Its size/palette are tunable like the Home cards; its fonts
     * follow the main [fontScalePercent].
     */
    val yourMixCard: CardAppearance = CardAppearance(cornerDp = 30),
    /** Seek-bar look (round 15, wave 2, C4): wavy or classic. */
    val seekBarStyle: String = SeekBarStyleNames.WAVY,
    /** Glass & blur (blur-engine test build): default OFF. */
    val glass: GlassAppearance = GlassAppearance()
) {
    val fontScale: Float get() = (fontScalePercent.coerceIn(85, 125) / 100f)
    val listFontScale: Float get() = (listFontScalePercent.coerceIn(85, 125) / 100f)

    /**
     * Migrate a stored look to the current model (round 15, wave 2):
     * any per-corner block that is still null gets the old single
     * radius on all four corners, and glass gets its single values on
     * both surface groups. Idempotent — running it twice changes nothing.
     */
    fun migrated(): AppearanceSettings = copy(
        homeMusicBar = homeMusicBar.migrated(),
        homeVideoBar = homeVideoBar.migrated(),
        sectionVideoBar = sectionVideoBar.migrated(),
        homeCards = homeCards.migrated(),
        recentCards = recentCards.migrated(),
        yourMixCard = yourMixCard.migrated(),
        glass = glass.migrated()
    )
}

@Singleton
class AppearancePreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json
) {
    private object Keys {
        val APPEARANCE_SETTINGS = stringPreferencesKey("appearance_settings_v1")
        val SAVED_APPEARANCE_PRESETS = stringPreferencesKey("saved_appearance_presets_v1")
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
        }.migrated()

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
        }.migrated()
    }

    /** Whole appearance, live. Falls back to defaults when unset or corrupt. */
    val appearanceFlow: Flow<AppearanceSettings> = dataStore.data.map { preferences ->
        val raw = preferences[Keys.APPEARANCE_SETTINGS] ?: return@map defaultSettings()
        runCatching { json.decodeFromString<AppearanceSettings>(raw) }
            .getOrElse { defaultSettings() }
            // Round 15, wave 2: migrate old stored looks (single corner
            // radius, single glass radius/tint) to the new per-corner /
            // per-surface model, so the user's look never changes.
            .migrated()
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
            fontScalePercent = 100,
            listFontScalePercent = 100
        )
    }

    suspend fun resetRecentCards() =
        update { it.copy(recentCards = RecentCardsAppearance()) }

    suspend fun resetListRows() =
        update { it.copy(listRows = ListRowsAppearance()) }

    suspend fun resetYourMixCard() =
        update { it.copy(yourMixCard = CardAppearance(cornerDp = 30)) }

    suspend fun resetSeekBarStyle() =
        update { it.copy(seekBarStyle = SeekBarStyleNames.WAVY) }

    suspend fun resetGlass() = update { it.copy(glass = GlassAppearance()) }

    // ---- User-saved looks (round 15, wave 2, C7) ----
    // Stored as a JSON list under its own key, separate from the live
    // appearance, so saving a look never touches the current look.
    // Saving a name that already exists overwrites that look; saving a
    // new name when the list is full is refused (LIMIT_REACHED) rather
    // than silently dropping the oldest look.

    /** Saved looks, live. Falls back to an empty list when unset or corrupt. */
    val savedPresetsFlow: Flow<List<SavedAppearancePreset>> =
        dataStore.data.map { preferences ->
            val raw = preferences[Keys.SAVED_APPEARANCE_PRESETS] ?: return@map emptyList()
            runCatching { json.decodeFromString<List<SavedAppearancePreset>>(raw) }
                .getOrElse { emptyList() }
        }

    suspend fun savePreset(name: String, settings: AppearanceSettings): SavePresetResult {
        val trimmed = name.trim()
        val current = savedPresetsFlow.first()
        val existingIndex = current.indexOfFirst { it.name.equals(trimmed, ignoreCase = true) }
        if (existingIndex < 0 && current.size >= MAX_SAVED_APPEARANCE_PRESETS) {
            return SavePresetResult.LIMIT_REACHED
        }
        val updated = if (existingIndex >= 0) {
            current.toMutableList().also {
                it[existingIndex] = SavedAppearancePreset(trimmed, settings)
            }
        } else {
            current + SavedAppearancePreset(trimmed, settings)
        }
        dataStore.edit { preferences ->
            preferences[Keys.SAVED_APPEARANCE_PRESETS] = json.encodeToString(updated)
        }
        return SavePresetResult.SAVED
    }

    suspend fun applySavedPreset(name: String) {
        val preset = savedPresetsFlow.first()
            .firstOrNull { it.name.equals(name.trim(), ignoreCase = true) } ?: return
        dataStore.edit { preferences ->
            preferences[Keys.APPEARANCE_SETTINGS] =
                json.encodeToString(preset.settings.migrated())
        }
    }

    suspend fun deleteSavedPreset(name: String) {
        val updated = savedPresetsFlow.first()
            .filterNot { it.name.equals(name.trim(), ignoreCase = true) }
        dataStore.edit { preferences ->
            preferences[Keys.SAVED_APPEARANCE_PRESETS] = json.encodeToString(updated)
        }
    }

    suspend fun resetAll() {
        dataStore.edit { preferences ->
            preferences[Keys.APPEARANCE_SETTINGS] =
                json.encodeToString(defaultSettings())
        }
    }
}
