package de.szalkowski.activitylauncher

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.services.BootStateDetector
import de.szalkowski.activitylauncher.services.EmergencyModeDetector
import de.szalkowski.activitylauncher.services.PortalSettingsService
import de.szalkowski.activitylauncher.services.StartupScheduler
import javax.inject.Inject

/**
 * Home-screen replacement activity for Portal Launcher.
 *
 * Intercepts the Home key (via CATEGORY_HOME intent filter) and redirects
 * to either the user-configured default launcher app or the Portal settings.
 *
 * Responsibilities (delegated to injected components):
 * - [BootStateDetector]: detect device reboots
 * - [EmergencyModeDetector]: detect rapid Home-key presses (emergency recovery)
 * - [StartupScheduler]: launch startup items after boot
 */
@AndroidEntryPoint
class PortalHomeActivity : ComponentActivity() {

    @Inject
    lateinit var bootStateDetector: BootStateDetector

    @Inject
    lateinit var emergencyModeDetector: EmergencyModeDetector

    @Inject
    lateinit var startupScheduler: StartupScheduler

    @Inject
    lateinit var portalSettingsService: PortalSettingsService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (emergencyModeDetector.checkEmergencyMode()) {
            Log.w(TAG, "Emergency mode — opening Portal settings")
            openPortalMainActivity()
            return
        }

        if (bootStateDetector.isFirstBootAfterDeviceStartup()) {
            val hasImmediate = startupScheduler.launchStartupItems()
            if (!hasImmediate) {
                navigateToDefaultLauncherOrSettings()
            }
            // If there were immediate startups, stay on the launched activity
        } else {
            navigateToDefaultLauncherOrSettings()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Emergency mode is only checked in onCreate to avoid double-counting.
        navigateToDefaultLauncherOrSettings()
    }

    override fun onPause() {
        super.onPause()
        bootStateDetector.updateElapsedTimeIfNeeded()
    }

    private fun navigateToDefaultLauncherOrSettings() {
        val pkg = portalSettingsService.getDefaultLauncherPackage()
        val cls = portalSettingsService.getDefaultLauncherClass()

        if (!pkg.isNullOrEmpty() && !cls.isNullOrEmpty()) {
            val exists = runCatching {
                packageManager.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES)
            }.isSuccess

            if (exists) {
                val launched = runCatching {
                    startActivity(buildLauncherIntent(pkg, cls))
                }.isSuccess

                if (launched) return
            }
        }

        // Fallback: no default launcher configured or launch failed
        openPortalMainActivity()
    }

    private fun buildLauncherIntent(packageName: String, className: String): Intent {
        val intent = Intent().apply {
            setClassName(packageName, className)
        }
        when (portalSettingsService.getLaunchMode()) {
            "1" -> intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK,
            )
            else -> intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )
        }
        return intent
    }

    private fun openPortalMainActivity() {
        val intent = Intent(this, PortalMainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    companion object {
        private const val TAG = "PortalHomeActivity"
    }
}
