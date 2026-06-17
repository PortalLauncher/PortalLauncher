package de.szalkowski.activitylauncher.services

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import de.szalkowski.activitylauncher.StartupItem
import de.szalkowski.activitylauncher.StartupType
import de.szalkowski.activitylauncher.workers.AppLaunchWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules startup items via [WorkManager] for delayed/conditional launches
 * and launches immediate items directly via [PortalActivityLauncherService].
 */
@Singleton
class StartupScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val portalSettingsService: PortalSettingsService,
    private val portalActivityLauncherService: PortalActivityLauncherService,
    private val settingsService: SettingsService,
) {
    /**
     * Launch all configured startup items.
     *
     * @return true if at least one immediate, zero-delay item was launched.
     */
    fun launchStartupItems(): Boolean {
        val startups = portalSettingsService.getStartupItems()
        var hasImmediate = false

        for (item in startups) {
            if (item.startupType == StartupType.IMMEDIATE && item.delaySeconds == 0) {
                launchImmediate(item)
                hasImmediate = true
                continue
            }

            scheduleWorker(item)
        }

        Log.d(TAG, "Scheduled ${startups.size} items, immediate=$hasImmediate")
        return hasImmediate
    }

    private fun launchImmediate(item: StartupItem) {
        val component = ComponentName(item.packageName, item.className)
        portalActivityLauncherService.launchActivity(
            activity = component,
            asRoot = settingsService.allowRoot,
            showToast = false,
        )
    }

    private fun scheduleWorker(item: StartupItem) {
        val data = Data.Builder()
            .putString(AppLaunchWorker.KEY_PACKAGE_NAME, item.packageName)
            .putString(AppLaunchWorker.KEY_CLASS_NAME, item.className)
            .putBoolean(AppLaunchWorker.KEY_ALLOW_ROOT, settingsService.allowRoot)
            .build()

        val workRequestBuilder = OneTimeWorkRequestBuilder<AppLaunchWorker>()
            .setInputData(data)
            .addTag(PortalSettingsService.TAG_APP_LAUNCH)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS,
            )

        when (item.startupType) {
            StartupType.IMMEDIATE -> {
                if (item.delaySeconds > 0) {
                    workRequestBuilder.setInitialDelay(
                        item.delaySeconds.toLong(),
                        TimeUnit.SECONDS,
                    )
                }
            }
            StartupType.ON_NETWORK -> {
                workRequestBuilder.setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                if (item.delaySeconds > 0) {
                    workRequestBuilder.setInitialDelay(
                        item.delaySeconds.toLong(),
                        TimeUnit.SECONDS,
                    )
                }
            }
        }

        WorkManager.getInstance(context).enqueue(workRequestBuilder.build())
    }

    companion object {
        private const val TAG = "StartupScheduler"
    }
}
