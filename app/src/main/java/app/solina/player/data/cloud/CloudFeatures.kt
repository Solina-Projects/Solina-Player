package app.solina.player.data.cloud

/**
 * Single reversible switch for the app's cloud features (2026-11-16,
 * user: the app must work fully offline for now).
 *
 * When false:
 * - All cloud UI is hidden: the cloud button in the Home top bar, the
 *   "Cloud Downloads" and "Accounts" rows in Settings, the matching
 *   Settings-search entries, the "Cloud only" toggle and the storage
 *   filter button in the Library, and the cloud filter in the song
 *   picker.
 * - No cloud code path runs: [app.solina.player.data.worker.CloudSyncCoordinator]
 *   never syncs Navidrome / Jellyfin, the library storage filter never
 *   selects the ONLINE (cloud-only) mode, and "hide local media" is a
 *   no-op.
 *
 * Local playback, the local library and local (on-device) backup are
 * unaffected. The cloud subsystems themselves (repositories, login
 * activities, dashboards, routes) are left in place, only unreachable
 * from the UI — flip this back to `true` to re-enable them.
 */
object CloudFeatures {
    const val ENABLED: Boolean = false
}
