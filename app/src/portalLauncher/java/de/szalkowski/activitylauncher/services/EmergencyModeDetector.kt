package de.szalkowski.activitylauncher.services

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.util.Log
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects emergency mode: when the user rapidly presses the Home key
 * multiple times within [EMERGENCY_WINDOW_MS] (10 seconds), the app opens
 * its own settings instead of the configured default launcher.
 *
 * This prevents lock-out situations where a misconfigured default launcher
 * would make it impossible to exit the target app.
 */
@Singleton
class EmergencyModeDetector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val portalSettingsService: PortalSettingsService,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PortalSettingsService.PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Records a Home-key press and checks whether emergency mode should activate.
     *
     * @return true if emergency mode is triggered (user should be taken to the Portal settings).
     */
    fun checkEmergencyMode(): Boolean {
        val requiredCount = portalSettingsService.getEmergencyModeCount()
        val currentTime = SystemClock.elapsedRealtime()

        val history = getLaunchHistory().toMutableList()
        history.add(currentTime)
        saveLaunchHistory(history)

        if (history.size < requiredCount) {
            return false
        }

        val recentWindow = history.takeLast(requiredCount)
        val timeSpan = recentWindow.last() - recentWindow.first()

        return if (timeSpan <= EMERGENCY_WINDOW_MS) {
            Log.w(TAG, "Emergency mode triggered: $requiredCount presses in ${timeSpan}ms")
            clearLaunchHistory()
            true
        } else {
            false
        }
    }

    private fun getLaunchHistory(): List<Long> {
        val json = prefs.getString(PortalSettingsService.KEY_LAUNCH_HISTORY, "[]") ?: "[]"
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { array.getLong(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse launch history, resetting", e)
            emptyList()
        }
    }

    private fun saveLaunchHistory(history: List<Long>) {
        val latestTime = history.lastOrNull() ?: return
        // Only retain entries within the emergency window to keep data minimal
        val cleaned = history.filter { latestTime - it <= EMERGENCY_WINDOW_MS }
        val array = JSONArray()
        cleaned.forEach { array.put(it) }
        prefs.edit {
            putString(PortalSettingsService.KEY_LAUNCH_HISTORY, array.toString())
        }
    }

    private fun clearLaunchHistory() {
        prefs.edit { remove(PortalSettingsService.KEY_LAUNCH_HISTORY) }
    }

    companion object {
        private const val TAG = "EmergencyModeDetector"
        private const val EMERGENCY_WINDOW_MS = 10_000L
    }
}
