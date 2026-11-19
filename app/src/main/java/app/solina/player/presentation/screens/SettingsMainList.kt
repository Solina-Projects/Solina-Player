package app.solina.player.presentation.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.solina.player.R
import app.solina.player.data.cloud.CloudFeatures
import app.solina.player.presentation.model.SettingsCategory
import app.solina.player.presentation.navigation.Screen

/**
 * The accent family a settings row is tinted with. Rows name a role of the active [ColorScheme]
 * instead of a fixed colour, so they follow the now-playing, wallpaper, preset and AMOLED schemes.
 */
internal enum class SettingsIconTone { PRIMARY, SECONDARY, TERTIARY }

/** Icon circle background paired with the icon tint for a row of the given [tone]. */
internal fun ColorScheme.settingsIconColors(tone: SettingsIconTone): Pair<Color, Color> = when (tone) {
    SettingsIconTone.PRIMARY -> primaryContainer to onPrimaryContainer
    SettingsIconTone.SECONDARY -> secondaryContainer to onSecondaryContainer
    SettingsIconTone.TERTIARY -> tertiaryContainer to onTertiaryContainer
}

/**
 * Accent for thin markers such as the settings search result bars. Container colours sit too
 * close to the card surface to read at that width, so markers use the accent role itself.
 */
internal fun ColorScheme.settingsAccentColor(tone: SettingsIconTone): Color = when (tone) {
    SettingsIconTone.PRIMARY -> primary
    SettingsIconTone.SECONDARY -> secondary
    SettingsIconTone.TERTIARY -> tertiary
}

/** Fixed per category, so a category keeps its tone in the main list and in search results. */
internal val SettingsCategory.iconTone: SettingsIconTone
    get() = when (this) {
        SettingsCategory.LIBRARY -> SettingsIconTone.PRIMARY
        SettingsCategory.APPEARANCE -> SettingsIconTone.TERTIARY
        SettingsCategory.PLAYBACK -> SettingsIconTone.SECONDARY
        SettingsCategory.VIDEO -> SettingsIconTone.SECONDARY
        SettingsCategory.BEHAVIOR -> SettingsIconTone.PRIMARY
        SettingsCategory.BACKUP_RESTORE -> SettingsIconTone.TERTIARY
        SettingsCategory.DEVELOPER -> SettingsIconTone.SECONDARY
        SettingsCategory.DEVICE_CAPABILITIES -> SettingsIconTone.SECONDARY
        SettingsCategory.ABOUT -> SettingsIconTone.SECONDARY
    }

/** A row of the main Settings list. */
internal sealed interface MainSettingsEntry {
    val iconTone: SettingsIconTone
    val route: String

    /** A row drawn from the category's own title, subtitle and icon. */
    data class Category(val category: SettingsCategory) : MainSettingsEntry {
        override val iconTone: SettingsIconTone
            get() = category.iconTone

        override val route: String
            get() = when (category) {
                SettingsCategory.DEVICE_CAPABILITIES -> Screen.DeviceCapabilities.createRoute()
                SettingsCategory.ABOUT -> Screen.About.route
                else -> Screen.SettingsCategory.createRoute(category.id)
            }
    }

    /** A row for a settings screen that is not a [SettingsCategory]. */
    enum class Destination(
        @StringRes val titleRes: Int,
        @StringRes val subtitleRes: Int,
        val icon: ImageVector,
        override val iconTone: SettingsIconTone,
        override val route: String
    ) : MainSettingsEntry {
        HOME_CUSTOMIZATION(
            titleRes = R.string.home_customize_title,
            subtitleRes = R.string.home_customize_hub_subtitle,
            icon = Icons.Rounded.Home,
            iconTone = SettingsIconTone.PRIMARY,
            route = Screen.HomeCustomization.route
        ),
        MUSIC_CUSTOMIZATION(
            titleRes = R.string.music_customize_title,
            subtitleRes = R.string.music_customize_subtitle,
            icon = Icons.Rounded.MusicNote,
            iconTone = SettingsIconTone.SECONDARY,
            route = Screen.MusicCustomization.route
        ),
        VIDEO_CUSTOMIZATION(
            titleRes = R.string.video_customize_title,
            subtitleRes = R.string.video_customize_subtitle,
            icon = Icons.Rounded.VideoLibrary,
            iconTone = SettingsIconTone.TERTIARY,
            route = Screen.VideoCustomization.route
        ),
        // Settings search groups the lyrics options under Playback, so the row shares its tone.
        LYRICS(
            titleRes = R.string.setcat_lyrics_title,
            subtitleRes = R.string.setcat_lyrics_subtitle,
            icon = Icons.Rounded.Lyrics,
            iconTone = SettingsIconTone.SECONDARY,
            route = Screen.LyricsSettings.createRoute()
        ),
        EQUALIZER(
            titleRes = R.string.settings_category_equalizer_title,
            subtitleRes = R.string.settings_category_equalizer_subtitle,
            icon = Icons.Rounded.GraphicEq,
            iconTone = SettingsIconTone.TERTIARY,
            route = Screen.Equalizer.createRoute()
        ),
        DOWNLOADS(
            titleRes = R.string.cloud_downloads_title,
            subtitleRes = R.string.cloud_downloads_settings_subtitle,
            icon = Icons.Rounded.CloudDownload,
            iconTone = SettingsIconTone.PRIMARY,
            route = Screen.CloudDownloads.route
        ),
        ACCOUNTS(
            titleRes = R.string.settings_accounts_row_title,
            subtitleRes = R.string.settings_accounts_row_subtitle,
            icon = Icons.Rounded.AccountCircle,
            iconTone = SettingsIconTone.TERTIARY,
            route = Screen.Accounts.createRoute()
        ),
        UPDATES(
            titleRes = R.string.updates_title,
            subtitleRes = R.string.updates_settings_subtitle,
            icon = Icons.Rounded.SystemUpdate,
            iconTone = SettingsIconTone.PRIMARY,
            route = Screen.Updates.route
        )
    }
}

/** A named section of related rows rendered as its own Material 3 Expressive card stack. */
internal data class MainSettingsGroup(
    @StringRes val titleRes: Int,
    val entries: List<MainSettingsEntry>
)

/**
 * Organised Settings groups:
 * 1. Personalisation: Home, Music, Video customisers + Global Theme & Appearance
 * 2. Media & Playback: Music Library, Playback, Lyrics, Equalizer, Video defaults
 * 3. App & Data: Behaviour, Backup & Restore (+ Cloud when enabled)
 * 4. System & About: Device Capabilities, Developer, Updates, About
 */
internal val mainSettingsGroups: List<MainSettingsGroup> = listOf(
    MainSettingsGroup(
        titleRes = R.string.settings_group_personalisation,
        entries = listOf(
            MainSettingsEntry.Destination.HOME_CUSTOMIZATION,
            MainSettingsEntry.Destination.MUSIC_CUSTOMIZATION,
            MainSettingsEntry.Destination.VIDEO_CUSTOMIZATION,
            MainSettingsEntry.Category(SettingsCategory.APPEARANCE)
        )
    ),
    MainSettingsGroup(
        titleRes = R.string.settings_group_media_playback,
        entries = listOf(
            MainSettingsEntry.Category(SettingsCategory.LIBRARY),
            MainSettingsEntry.Category(SettingsCategory.PLAYBACK),
            MainSettingsEntry.Destination.LYRICS,
            MainSettingsEntry.Destination.EQUALIZER,
            MainSettingsEntry.Category(SettingsCategory.VIDEO)
        )
    ),
    MainSettingsGroup(
        titleRes = R.string.settings_group_app_data,
        entries = buildList {
            add(MainSettingsEntry.Category(SettingsCategory.BEHAVIOR))
            add(MainSettingsEntry.Category(SettingsCategory.BACKUP_RESTORE))
            if (CloudFeatures.ENABLED) {
                add(MainSettingsEntry.Destination.DOWNLOADS)
                add(MainSettingsEntry.Destination.ACCOUNTS)
            }
        }
    ),
    MainSettingsGroup(
        titleRes = R.string.settings_group_system_info,
        entries = listOf(
            MainSettingsEntry.Category(SettingsCategory.DEVICE_CAPABILITIES),
            MainSettingsEntry.Category(SettingsCategory.DEVELOPER),
            MainSettingsEntry.Destination.UPDATES,
            MainSettingsEntry.Category(SettingsCategory.ABOUT)
        )
    )
)

/**
 * Every row of the main Settings list, flattened in display order. Derived directly from
 * [mainSettingsGroups] so flat lookups and group rendering can never drift out of sync.
 */
internal val mainSettingsEntries: List<MainSettingsEntry> =
    mainSettingsGroups.flatMap { it.entries }

private val SingleGroupItemShape = RoundedCornerShape(24.dp)
private val FirstGroupItemShape =
    RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
private val MiddleGroupItemShape = RoundedCornerShape(4.dp)
private val LastGroupItemShape =
    RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)

/** Shape of row [index] in a group of [count] rows: only the group's outer corners are fully rounded. */
internal fun settingsGroupItemShape(index: Int, count: Int): Shape = when {
    count == 1 -> SingleGroupItemShape
    index == 0 -> FirstGroupItemShape
    index == count - 1 -> LastGroupItemShape
    else -> MiddleGroupItemShape
}
