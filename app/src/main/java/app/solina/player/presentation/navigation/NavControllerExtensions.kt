package app.solina.player.presentation.navigation

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder

private fun NavController.isReadyForNavigation(): Boolean {
    return runCatching {
        currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) == true
    }.getOrDefault(false)
}

fun NavController.navigateSafely(route: String): Boolean {
    if (!isReadyForNavigation()) return false
    navigate(route) {
        launchSingleTop = true
    }
    return true
}

fun NavController.navigateSafely(
    route: String,
    builder: NavOptionsBuilder.() -> Unit
): Boolean {
    if (!isReadyForNavigation()) return false
    navigate(route) {
        launchSingleTop = true
        builder()
    }
    return true
}

fun NavController.navigateSafelyReplacing(
    route: String,
    patternToPop: String,
    builder: NavOptionsBuilder.() -> Unit = {}
): Boolean {
    if (!isReadyForNavigation()) return false
    navigate(route) {
        launchSingleTop = false
        popUpTo(patternToPop) {
            inclusive = true
        }
        builder()
    }
    return true
}

fun NavController.navigateToTopLevelSafely(route: String): Boolean {
    val startDestinationId = runCatching { graph.startDestinationId }.getOrNull() ?: return false
    navigate(route) {
        popUpTo(startDestinationId) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
    return true
}

/**
 * Bottom-bar feedback (2026-11-16): tapping Home in the floating bar must
 * always land on the main Home page, in both the music and the video
 * sections. The previous behaviour popped to the graph's start destination
 * — the user's launch tab, which can be Library or Search — not
 * necessarily Home. This pops everything above Home itself (the drawer
 * already navigates Home this way) and recreates a single Home on top.
 * If Home is not in the back stack at all (launch tab is not Home), the
 * pop is a no-op and Home is simply pushed on top — it still lands on Home.
 */
fun NavController.navigateHomeSafely(): Boolean {
    if (!isReadyForNavigation()) return false
    navigate(Screen.Home.route) {
        popUpTo(Screen.Home.route) { inclusive = true }
        launchSingleTop = true
    }
    return true
}
