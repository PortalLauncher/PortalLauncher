package de.szalkowski.activitylauncher.services

import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.regex.Pattern

/**
 * Shared activity launch utility used by both [PortalActivityLauncherService]
 * and [AppLaunchWorker] to avoid code duplication.
 */
object ActivityLaunchHelper {
    private const val TAG = "ActivityLaunchHelper"

    private val VALID_COMPONENT = Pattern.compile("^[./a-zA-Z0-9]+$")

    /**
     * Launch an activity, optionally with root privileges.
     *
     * @return true if the launch was successful, false otherwise.
     */
    fun launch(
        context: Context,
        packageName: String,
        className: String,
        allowRoot: Boolean,
    ): Boolean {
        return try {
            if (!allowRoot) {
                val intent = Intent().apply {
                    setClassName(packageName, className)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            } else {
                val component = "$packageName/$className"
                if (!VALID_COMPONENT.matcher(component).matches()) {
                    Log.e(TAG, "Invalid component name rejected: $component")
                    return false
                }
                val process = Runtime.getRuntime()
                    .exec(arrayOf("su", "-c", "am start -n $component"))
                val exitValue = process.waitFor()
                if (exitValue != 0) {
                    Log.e(TAG, "Root launch failed with exit code $exitValue for $component")
                    return false
                }
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch $packageName/$className", e)
            false
        }
    }
}
