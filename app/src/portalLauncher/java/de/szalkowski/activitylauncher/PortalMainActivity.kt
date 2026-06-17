package de.szalkowski.activitylauncher

import android.os.Bundle
import android.view.View
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PortalMainActivity : MainActivity() {

    private val bgScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val topLevelDestinations: Set<Int>
        get() = super.topLevelDestinations + R.id.PortalFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navController = (
            supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment_content_main) as androidx.navigation.fragment.NavHostFragment
            )
            .navController

        // Hide search bar on Portal tab
        val searchContainer = findViewById<View>(R.id.searchContainer)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            searchContainer?.visibility =
                if (destination.id == R.id.PortalFragment) View.GONE else View.VISIBLE
        }

        if (savedInstanceState == null) {
            // Navigate to Portal tab
            navController.navigate(R.id.PortalFragment)

            // Pre-load packages in background so the "All" tab is instant later.
            // This makes isLoaded=true so future navigateToAll skips LoadingFragment.
            bgScope.launch { packageListService.packages }

            // Intercept the startup auto-navigation (LoadingFragment → PackageListFragment)
            // ONCE, then remove the listener so manual "All" tab clicks work normally.
            val redirectOnce = object : NavController.OnDestinationChangedListener {
                override fun onDestinationChanged(
                    controller: NavController,
                    destination: NavDestination,
                    arguments: Bundle?,
                ) {
                    if (destination.id == R.id.PackageListFragment) {
                        controller.removeOnDestinationChangedListener(this)
                        controller.navigate(R.id.PortalFragment)
                    }
                }
            }
            navController.addOnDestinationChangedListener(redirectOnce)
        }
    }

    override fun onDestroy() {
        bgScope.cancel()
        super.onDestroy()
    }
}
