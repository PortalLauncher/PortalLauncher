package de.szalkowski.activitylauncher.ui

import android.os.Bundle
import androidx.navigation.fragment.findNavController
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.StartupType
import de.szalkowski.activitylauncher.services.PackageListService
import de.szalkowski.activitylauncher.services.PortalSettingsService
import javax.inject.Inject

@AndroidEntryPoint
class PortalSettingsFragment : PreferenceFragmentCompat() {

    @Inject
    lateinit var packageListService: PackageListService

    @Inject
    lateinit var portalSettingsService: PortalSettingsService

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.portal_preferences, rootKey)
        setupPreferences()
    }

    override fun onResume() {
        super.onResume()
        refreshDefaultLauncherInfo()
        loadStartupItems()
    }

    private fun setupPreferences() {
        val enableDebug = findPreference<SwitchPreference>("enable_debug")
        val launchMode = findPreference<ListPreference>("launch_mode")

        enableDebug?.setOnPreferenceChangeListener { _, newValue ->
            portalSettingsService.saveDebugMode(newValue as Boolean)
            true
        }

        setupListPreferenceSummary(launchMode)
        launchMode?.setOnPreferenceChangeListener { _, newValue ->
            portalSettingsService.saveLaunchMode(newValue as String)
            val index = launchMode.findIndexOfValue(newValue)
            if (index >= 0) {
                launchMode.summary = launchMode.entries[index]
            }
            true
        }
    }

    private fun setupListPreferenceSummary(listPreference: ListPreference?) {
        if (listPreference == null) return
        val value = portalSettingsService.getLaunchMode()
        val index = listPreference.findIndexOfValue(value)
        if (index >= 0) {
            listPreference.summary = listPreference.entries[index]
        }
    }

    private fun refreshDefaultLauncherInfo() {
        val info = findPreference<Preference>("default_launcher_info") ?: return
        val pkg = portalSettingsService.getDefaultLauncherPackage()
        val cls = portalSettingsService.getDefaultLauncherClass()

        if (pkg != null && cls != null) {
            val displayName = packageListService.packages
                .find { it.packageName == pkg }?.name

            if (displayName != null) {
                info.title = displayName
                info.summary = cls
            } else {
                portalSettingsService.clearDefaultLauncherSettings()
                info.title = getString(R.string.pref_default_launcher_title)
                info.summary = getString(R.string.pref_default_launcher_summary)
            }
        } else {
            info.title = getString(R.string.pref_default_launcher_title)
            info.summary = getString(R.string.pref_default_launcher_summary)
        }
    }

    private fun loadStartupItems() {
        val category = findPreference<PreferenceCategory>("startup_items_category") ?: return
        category.removeAll()

        val startups = portalSettingsService.getStartupItems()

        if (startups.isEmpty()) {
            val empty = Preference(requireContext()).apply {
                title = getString(R.string.startup_items_empty)
                summary = getString(R.string.startup_items_empty_hint)
                isEnabled = false
            }
            category.addPreference(empty)
        } else {
            startups.forEachIndexed { index, item ->
                val displayName =
                    packageListService.packages.find { it.packageName == item.packageName }?.name
                        ?: item.packageName

                val typeText = when (item.startupType) {
                    StartupType.IMMEDIATE -> getString(R.string.startup_item_type_normal)
                    StartupType.ON_NETWORK -> getString(R.string.startup_item_type_delayed)
                }

                val preference = Preference(requireContext()).apply {
                    title = displayName
                    summary = buildString {
                        appendLine(
                            getString(R.string.startup_item_class_format, item.className),
                        )
                        if (item.delaySeconds > 0) {
                            appendLine(
                                getString(
                                    R.string.startup_item_startup_delayed,
                                    typeText,
                                    item.delaySeconds,
                                ),
                            )
                        } else {
                            appendLine(
                                getString(R.string.startup_item_startup_immediate, typeText),
                            )
                        }
                    }

                    setOnPreferenceClickListener {
                        val bundle = Bundle().apply {
                            putInt("startup_item_index", index)
                            putString("package_name", item.packageName)
                            putString("class_name", item.className)
                        }
                        findNavController().navigate(
                            R.id.action_settings_to_startup_edit,
                            bundle,
                        )
                        true
                    }
                }

                category.addPreference(preference)
            }
        }
    }
}
