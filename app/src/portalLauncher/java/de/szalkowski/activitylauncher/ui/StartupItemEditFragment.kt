package de.szalkowski.activitylauncher.ui

import android.os.Bundle
import androidx.navigation.fragment.findNavController
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.StartupType
import de.szalkowski.activitylauncher.services.PackageListService
import de.szalkowski.activitylauncher.services.PortalSettingsService
import javax.inject.Inject

@AndroidEntryPoint
class StartupItemEditFragment : PreferenceFragmentCompat() {

    @Inject
    lateinit var packageListService: PackageListService

    @Inject
    lateinit var portalSettingsService: PortalSettingsService

    private var startupItemIndex: Int = -1
    private var packageName: String = ""
    private var className: String = ""

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.startup_item_preferences, rootKey)

        startupItemIndex = requireArguments().getInt("startup_item_index", -1)
        packageName = requireArguments().getString("package_name") ?: ""
        className = requireArguments().getString("class_name") ?: ""

        setupPreferences()
    }

    private fun setupPreferences() {
        val infoPreference = findPreference<Preference>("startup_info")
        val displayName = getPackageDisplayName() ?: packageName
        infoPreference?.title = displayName
        infoPreference?.summary = className

        val typePreference = findPreference<ListPreference>("startup_type")
        val delayPreference = findPreference<EditTextPreference>("startup_delay")

        val startups = portalSettingsService.getStartupItems()
        if (startupItemIndex !in startups.indices) return

        val currentItem = startups[startupItemIndex]
        typePreference?.value = currentItem.startupType.value.toString()
        typePreference?.summary = getTypeDisplayName(currentItem.startupType)

        delayPreference?.text = currentItem.delaySeconds.toString()
        delayPreference?.summary = "${currentItem.delaySeconds}${getString(R.string.startup_delay_seconds)}"

        typePreference?.setOnPreferenceChangeListener { _, newValue ->
            val newType = StartupType.fromValue(newValue.toString().toInt())
            val delay = delayPreference?.text?.toIntOrNull() ?: 0
            portalSettingsService.updateStartupItem(startupItemIndex, newType, delay)
            typePreference.summary = getTypeDisplayName(newType)
            true
        }

        delayPreference?.setOnPreferenceChangeListener { _, newValue ->
            val delayStr = newValue as String
            val delay = delayStr.toIntOrNull()
            if (delay == null || delay < 0) {
                // Revert to current value on invalid input
                delayPreference.text = currentItem.delaySeconds.toString()
                false
            } else {
                val type = StartupType.fromValue(
                    typePreference?.value?.toIntOrNull() ?: 0,
                )
                portalSettingsService.updateStartupItem(startupItemIndex, type, delay)
                delayPreference.summary = "${delay}${getString(R.string.startup_delay_seconds)}"
                true
            }
        }

        val deletePreference = findPreference<Preference>("startup_delete")
        deletePreference?.setOnPreferenceClickListener {
            portalSettingsService.deleteStartupItem(startupItemIndex)
            findNavController().popBackStack()
            true
        }
    }

    private fun getPackageDisplayName(): String? {
        return packageListService.packages.find { it.packageName == packageName }?.name
    }

    private fun getTypeDisplayName(type: StartupType): String {
        return when (type) {
            StartupType.IMMEDIATE -> getString(R.string.startup_item_type_normal)
            StartupType.ON_NETWORK -> getString(R.string.startup_item_type_delayed)
        }
    }
}
