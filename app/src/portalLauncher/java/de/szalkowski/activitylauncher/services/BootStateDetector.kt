package de.szalkowski.activitylauncher.services

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.util.Log
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects device reboot by comparing [SystemClock.elapsedRealtime] across sessions.
 *
 * On Android, [SystemClock.elapsedRealtime] resets to 0 on device reboot,
 * so comparing the current value against a previously persisted value reliably
 * detects whether the device has restarted since the last Home-key press.
 */
@Singleton
class BootStateDetector @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PortalSettingsService.PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Checks whether this is the first Home-key press after a device reboot.
     * Updates the persisted elapsed time if a reboot is detected.
     */
    fun isFirstBootAfterDeviceStartup(): Boolean {
        val currentTime = SystemClock.elapsedRealtime()
        val lastTime = prefs.getLong(PortalSettingsService.KEY_LAST_ELAPSED_TIME, -1L)

        return if (lastTime == -1L || currentTime < lastTime) {
            Log.d(
                TAG,
                "Reboot detected: ${if (lastTime == -1L) "first launch ever" else "device restarted"}",
            )
            persistElapsedTime(currentTime)
            true
        } else {
            false
        }
    }

    /**
     * Periodically updates the persisted elapsed time.
     * Only writes if at least [WRITE_INTERVAL_MS] has passed since the last write
     * to avoid excessive SharedPreferences I/O.
     */
    fun updateElapsedTimeIfNeeded() {
        val currentTime = SystemClock.elapsedRealtime()
        val lastTime = prefs.getLong(PortalSettingsService.KEY_LAST_ELAPSED_TIME, -1L)

        if (lastTime == -1L || currentTime - lastTime > WRITE_INTERVAL_MS) {
            persistElapsedTime(currentTime)
        }
    }

    private fun persistElapsedTime(time: Long) {
        prefs.edit { putLong(PortalSettingsService.KEY_LAST_ELAPSED_TIME, time) }
    }

    companion object {
        private const val TAG = "BootStateDetector"

        /** Minimum interval between writes to avoid excessive I/O (1 minute). */
        private const val WRITE_INTERVAL_MS = 60_000L
    }
}
