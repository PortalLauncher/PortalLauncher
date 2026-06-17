package de.szalkowski.activitylauncher.services

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import de.szalkowski.activitylauncher.StartupItem
import de.szalkowski.activitylauncher.StartupType
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PortalSettingsService @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    // ==================== Default Launcher ====================

    fun saveDefaultLauncher(packageName: String, className: String) {
        prefs.edit()
            .putString(KEY_DEFAULT_LAUNCHER_PACKAGE, packageName)
            .putString(KEY_DEFAULT_LAUNCHER_CLASS, className)
            .apply()
    }

    fun clearDefaultLauncherSettings() {
        prefs.edit()
            .remove(KEY_DEFAULT_LAUNCHER_PACKAGE)
            .remove(KEY_DEFAULT_LAUNCHER_CLASS)
            .apply()
    }

    fun getDefaultLauncherPackage(): String? =
        prefs.getString(KEY_DEFAULT_LAUNCHER_PACKAGE, null)

    fun getDefaultLauncherClass(): String? =
        prefs.getString(KEY_DEFAULT_LAUNCHER_CLASS, null)

    // ==================== Emergency Mode ====================

    /**
     * Reads the emergency mode trigger count.
     * Uses [getString] because [androidx.preference.ListPreference] persists as String.
     */
    fun getEmergencyModeCount(): Int {
        return prefs.getString(KEY_EMERGENCY_MODE, DEFAULT_EMERGENCY_MODE.toString())
            ?.toIntOrNull()
            ?: DEFAULT_EMERGENCY_MODE
    }

    fun saveEmergencyModeCount(count: Int) {
        prefs.edit().putString(KEY_EMERGENCY_MODE, count.toString()).apply()
    }

    // ==================== Launch Mode ====================

    fun saveLaunchMode(mode: String) {
        prefs.edit().putString(KEY_LAUNCH_MODE, mode).apply()
    }

    fun getLaunchMode(): String =
        prefs.getString(KEY_LAUNCH_MODE, DEFAULT_LAUNCH_MODE)!!

    // ==================== Debug Mode ====================

    fun saveDebugMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLE_DEBUG, enabled).apply()
    }

    fun getDebugMode(): Boolean =
        prefs.getBoolean(KEY_ENABLE_DEBUG, false)

    // ==================== Startup Items ====================

    fun addStartupItem(
        packageName: String,
        className: String,
        startupType: StartupType = StartupType.IMMEDIATE,
        delaySeconds: Int = 0,
    ) {
        val startups = getStartupItems().toMutableList()
        startups.add(
            StartupItem(
                packageName = packageName,
                className = className,
                startupType = startupType,
                delaySeconds = delaySeconds,
            ),
        )
        saveStartupItems(startups)
    }

    fun getStartupItems(): List<StartupItem> {
        val json = prefs.getString(KEY_STARTUPS, "[]") ?: "[]"
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                StartupItem(
                    packageName = obj.getString("package"),
                    className = obj.getString("class"),
                    startupType = StartupType.fromValue(obj.optInt("type", 0)),
                    delaySeconds = obj.optInt("delay", 0),
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveStartupItems(items: List<StartupItem>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("package", item.packageName)
                    put("class", item.className)
                    put("type", item.startupType.value)
                    put("delay", item.delaySeconds)
                },
            )
        }
        prefs.edit().putString(KEY_STARTUPS, array.toString()).apply()
    }

    fun updateStartupItem(index: Int, startupType: StartupType, delaySeconds: Int) {
        val startups = getStartupItems().toMutableList()
        if (index in startups.indices) {
            startups[index] = startups[index].copy(
                startupType = startupType,
                delaySeconds = delaySeconds,
            )
            saveStartupItems(startups)
        }
    }

    fun deleteStartupItem(index: Int) {
        val startups = getStartupItems().toMutableList()
        if (index in startups.indices) {
            startups.removeAt(index)
            saveStartupItems(startups)
        }
    }

    fun clearAllStartupItems() {
        prefs.edit().remove(KEY_STARTUPS).apply()
    }

    companion object {
        // SharedPreferences file name (portal-specific, separate from main app's DefaultSharedPreferences)
        const val PREFS_NAME = "portal_launcher_prefs"

        // Preference keys
        const val KEY_DEFAULT_LAUNCHER_PACKAGE = "default_launcher_package"
        const val KEY_DEFAULT_LAUNCHER_CLASS = "default_launcher_class"
        const val KEY_EMERGENCY_MODE = "emergency_mode"
        const val KEY_LAUNCH_MODE = "launch_mode"
        const val KEY_ENABLE_DEBUG = "enable_debug"
        const val KEY_STARTUPS = "startups"
        const val KEY_LAST_ELAPSED_TIME = "last_elapsed_time"
        const val KEY_LAUNCH_HISTORY = "launch_history"

        // WorkManager tags
        const val TAG_APP_LAUNCH = "app-launch"
        const val TAG_UPDATE_TIME = "update-time"

        // Defaults
        const val DEFAULT_EMERGENCY_MODE = 3
        const val DEFAULT_LAUNCH_MODE = "0"
    }
}
