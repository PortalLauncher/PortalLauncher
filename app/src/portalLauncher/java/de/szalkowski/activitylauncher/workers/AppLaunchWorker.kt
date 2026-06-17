package de.szalkowski.activitylauncher.workers

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import de.szalkowski.activitylauncher.services.ActivityLaunchHelper

/**
 * WorkManager Worker for delayed or conditional app launches.
 *
 * Uses [ActivityLaunchHelper] for the actual launch logic, which is shared
 * with [de.szalkowski.activitylauncher.services.PortalActivityLauncherService].
 */
class AppLaunchWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val packageName = inputData.getString(KEY_PACKAGE_NAME)
        val className = inputData.getString(KEY_CLASS_NAME)
        val allowRoot = inputData.getBoolean(KEY_ALLOW_ROOT, false)

        if (packageName.isNullOrEmpty() || className.isNullOrEmpty()) {
            Log.e(TAG, "Missing packageName or className in input data")
            return Result.failure()
        }

        // Verify the target package exists before attempting launch
        val packageExists = runCatching {
            applicationContext.packageManager.getPackageInfo(packageName, 0)
        }.isSuccess

        if (!packageExists) {
            Log.w(TAG, "Package not installed: $packageName — skipping")
            return Result.failure()
        }

        val success = ActivityLaunchHelper.launch(
            context = applicationContext,
            packageName = packageName,
            className = className,
            allowRoot = allowRoot,
        )

        return if (success) {
            Log.d(TAG, "Successfully launched $packageName/$className")
            Result.success()
        } else {
            Log.e(TAG, "Failed to launch $packageName/$className (allowRoot=$allowRoot)")
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "AppLaunchWorker"

        /** Input data key for the target package name. */
        const val KEY_PACKAGE_NAME = "packageName"

        /** Input data key for the target activity class name. */
        const val KEY_CLASS_NAME = "className"

        /** Input data key for whether to use root privileges. */
        const val KEY_ALLOW_ROOT = "allowRoot"
    }
}
