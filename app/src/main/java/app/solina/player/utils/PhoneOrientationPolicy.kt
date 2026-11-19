package app.solina.player.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.pm.ActivityInfo
import android.os.Bundle

/**
 * Lets the app follow the device's rotation (2026-11-16, user: the app
 * must rotate with the phone when auto-rotate is on). Every screen uses
 * [ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED], so the system decides:
 * portrait while auto-rotate is off, sensor-driven rotation while it is
 * on. No screen is locked to portrait any more.
 *
 * The video player is the one exception: while it is open it owns the
 * orientation itself (forces landscape on entry, rotation button in the
 * bottom bar), so [apply] returns early while [videoPlayerActive] is
 * set and never fights it.
 *
 * The manifest carries no screenOrientation attribute; this policy is
 * the only place orientation was ever forced.
 */
object PhoneOrientationPolicy : Application.ActivityLifecycleCallbacks {

    /**
     * While the video player is open, landscape is allowed: the player
     * forces landscape on entry and the user can rotate. Without this flag
     * [apply] (called again from MainActivity.onConfigurationChanged) forced
     * the phone straight back to portrait the moment the player asked for
     * landscape — that was why the player's rotation button did nothing.
     * The player sets this true on entry and false on leave.
     */
    @Volatile
    var videoPlayerActive: Boolean = false

    @SuppressLint("SourceLockedOrientationActivity")
    fun apply(activity: Activity) {
        // The video player owns the orientation while it is open.
        if (videoPlayerActive) return
        // Follow the system: UNSPECIFIED means the system auto-rotate
        // setting decides (portrait when off, sensor rotation when on).
        // The old code forced SCREEN_ORIENTATION_PORTRAIT on phones here.
        if (activity.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Applied before onCreate so a phone held sideways never lays out in landscape first.
    override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) = apply(activity)

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
