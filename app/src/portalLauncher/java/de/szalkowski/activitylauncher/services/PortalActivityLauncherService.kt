package de.szalkowski.activitylauncher.services

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import dagger.hilt.android.qualifiers.ApplicationContext
import de.szalkowski.activitylauncher.R
import java.io.IOException
import java.util.regex.Pattern
import javax.inject.Inject

interface PortalActivityLauncherService {
    fun launchActivity(
        activity: ComponentName,
        asRoot: Boolean,
        showToast: Boolean,
        flags: Int? = null,
    )
}

class PortalActivityLauncherServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : PortalActivityLauncherService {

    override fun launchActivity(
        activity: ComponentName,
        asRoot: Boolean,
        showToast: Boolean,
        flags: Int?,
    ) {
        if (showToast) {
            Toast.makeText(
                context,
                "${context.getText(R.string.starting_activity)}: ${activity.flattenToShortString()}",
                Toast.LENGTH_LONG,
            ).show()
        }

        try {
            if (!asRoot) {
                val intent = Intent()
                intent.component = activity
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                flags?.let { intent.flags = intent.flags or it }
                context.startActivity(intent)
            } else {
                startRootActivity(activity, flags)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context,
                context.getText(R.string.error).toString() + ": " + e,
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    @Throws(IOException::class, InterruptedException::class, IllegalArgumentException::class)
    private fun startRootActivity(activity: ComponentName, flags: Int? = null) {
        val component = activity.flattenToShortString()
        val isValid = validateComponentName(component)
        require(isValid) {
            "${context.getString(R.string.exception_invalid_component_name)}: $component"
        }

        val command = buildAmStartCommand(component, flags)

        Runtime.getRuntime().exec(arrayOf("su", "-c", command))
    }

    private fun buildAmStartCommand(component: String, flags: Int?): String {
        val commandBuilder = StringBuilder("am start")

        flags?.let { flagValue ->
            val flagMappings = mapOf(
                Intent.FLAG_ACTIVITY_NEW_TASK to "--activity-no-animation",
                Intent.FLAG_ACTIVITY_CLEAR_TASK to "--activity-clear-task",
                Intent.FLAG_ACTIVITY_SINGLE_TOP to "--activity-single-top",
                Intent.FLAG_ACTIVITY_CLEAR_TOP to "--activity-clear-top",
                Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT to "--activity-brought-to-front",
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT to "--activity-reorder-to-front",
                Intent.FLAG_ACTIVITY_NO_HISTORY to "--activity-no-history",
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS to "--activity-exclude-from-recents",
                Intent.FLAG_ACTIVITY_NO_ANIMATION to "--activity-no-animation",
            )

            var remainingFlags = flagValue
            flagMappings.forEach { (intentFlag, amFlag) ->
                if (remainingFlags and intentFlag != 0) {
                    commandBuilder.append(" $amFlag")
                    remainingFlags = remainingFlags and intentFlag.inv()
                }
            }

            if (remainingFlags != 0) {
                commandBuilder.append(" -f 0x${remainingFlags.toString(16)}")
            }
        }

        commandBuilder.append(" -n $component")
        return commandBuilder.toString()
    }

    private fun validateComponentName(component: String): Boolean {
        val p = Pattern.compile("^[./a-zA-Z0-9]+$")
        val m = p.matcher(component)
        return m.matches()
    }
}
