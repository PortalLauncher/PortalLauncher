package de.szalkowski.activitylauncher.ui

import android.content.ComponentName
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.StartupType
import de.szalkowski.activitylauncher.databinding.FragmentPortalBinding
import de.szalkowski.activitylauncher.services.ActivityLauncherService
import de.szalkowski.activitylauncher.services.MyActivityInfo
import de.szalkowski.activitylauncher.services.PortalSettingsService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class PortalFragment : Fragment() {

    @Inject
    lateinit var portalSettingsService: PortalSettingsService

    @Inject
    lateinit var activityLauncherService: ActivityLauncherService

    private val viewModel: PortalViewModel by viewModels()

    private var _binding: FragmentPortalBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPortalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pad bottom to clear the BottomNavigationView
        applyBottomNavPadding()

        setupSettingsControls()
        updateLauncherInfo()
        loadStartupItems()
        loadFavorites()

        // Click on launcher card → pick a favorite as default launcher
        binding.cardLauncherInfo.setOnClickListener {
            showLauncherPickerDialog()
        }

        binding.btClearLauncher.setOnClickListener {
            portalSettingsService.clearDefaultLauncherSettings()
            updateLauncherInfo()
            Toast.makeText(
                requireContext(),
                R.string.portal_no_launcher_set,
                Toast.LENGTH_SHORT,
            ).show()
        }

        binding.btAddStartup.setOnClickListener {
            showAddStartupPickerDialog()
        }

        binding.btClearAllStartups.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.startup_clear_all)
                .setMessage(getString(R.string.startup_clear_all_message))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    portalSettingsService.clearAllStartupItems()
                    loadStartupItems()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        updateLauncherInfo()
        loadStartupItems()
        loadFavorites()
    }

    /** Adds bottom padding equal to the BottomNavigationView height. */
    private fun applyBottomNavPadding() {
        val bottomNav = requireActivity().findViewById<View>(R.id.bottom_navigation)
            ?: return
        bottomNav.post {
            val scrollView = _binding?.portalScrollView ?: return@post
            scrollView.setPadding(
                scrollView.paddingLeft,
                scrollView.paddingTop,
                scrollView.paddingRight,
                bottomNav.height,
            )
        }
    }

    // ==================== Favorites ====================

    private fun loadFavorites() {
        viewLifecycleOwner.lifecycleScope.launch {
            val items = withContext(Dispatchers.Default) {
                viewModel.refresh()
                viewModel.favoriteActivities.value
            }

            val container = _binding?.favoritesContainer ?: return@launch
            container.removeAllViews()

            items.forEach { info ->
                container.addView(createFavoriteItemRow(info))
            }
        }
    }

    private fun createFavoriteItemRow(info: MyActivityInfo): View {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = resources.getDimensionPixelSize(R.dimen.portal_item_spacing)
            }
            minimumHeight = resources.getDimensionPixelSize(R.dimen.portal_item_min_height)
            isFocusable = true
            isClickable = true
            foreground = resources.getDrawable(
                android.R.drawable.list_selector_background,
                requireContext().theme,
            )
            setPadding(
                resources.getDimensionPixelSize(R.dimen.portal_item_padding_h),
                resources.getDimensionPixelSize(R.dimen.portal_item_padding_v),
                resources.getDimensionPixelSize(R.dimen.portal_item_padding_h),
                resources.getDimensionPixelSize(R.dimen.portal_item_padding_v),
            )

            setOnClickListener {
                activityLauncherService.launchActivity(
                    info.componentName,
                    asRoot = false,
                    showToast = true,
                )
            }
            setOnLongClickListener {
                showAddToStartupDialog(info.componentName)
                true
            }
        }

        val icon = ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.portal_icon_size),
                resources.getDimensionPixelSize(R.dimen.portal_icon_size),
            ).apply { marginEnd = resources.getDimensionPixelSize(R.dimen.portal_item_padding_h) }
            setImageDrawable(info.icon)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        row.addView(icon)

        val textLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f,
            )
        }

        textLayout.addView(
            TextView(requireContext()).apply {
                text = info.name
                setTextAppearance(android.R.style.TextAppearance_Medium)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            },
        )

        textLayout.addView(
            TextView(requireContext()).apply {
                text = info.componentName.flattenToShortString()
                setTextAppearance(android.R.style.TextAppearance_Small)
                setTextColor(resources.getColor(android.R.color.darker_gray, null))
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            },
        )

        row.addView(textLayout)
        return row
    }

    // ==================== Settings Controls ====================

    private fun setupSettingsControls() {
        setupLaunchModeToggle()
        setupEmergencyModeToggle()
        setupDebugSwitch()
    }

    private fun setupLaunchModeToggle() {
        val toggle = binding.toggleLaunchMode
        val currentMode = portalSettingsService.getLaunchMode()
        toggle.check(if (currentMode == "1") R.id.btLaunchRestart else R.id.btLaunchBringToFront)
        toggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                R.id.btLaunchBringToFront -> portalSettingsService.saveLaunchMode("0")
                R.id.btLaunchRestart -> portalSettingsService.saveLaunchMode("1")
            }
        }
    }

    private fun setupEmergencyModeToggle() {
        val toggle = binding.toggleEmergencyMode
        val currentCount = portalSettingsService.getEmergencyModeCount()
        val targetId = when (currentCount) {
            4 -> R.id.btEmergency4
            5 -> R.id.btEmergency5
            else -> R.id.btEmergency3
        }
        toggle.check(targetId)
        toggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val count = when (checkedId) {
                R.id.btEmergency4 -> 4
                R.id.btEmergency5 -> 5
                else -> 3
            }
            portalSettingsService.saveEmergencyModeCount(count)
        }
    }

    private fun setupDebugSwitch() {
        val sw = binding.swDebug
        sw.isChecked = portalSettingsService.getDebugMode()
        sw.setOnCheckedChangeListener { _, isChecked ->
            portalSettingsService.saveDebugMode(isChecked)
        }
    }

    // ==================== Picker Dialogs ====================

    private fun showFavoritesPicker(
        title: String,
        onPicked: (MyActivityInfo) -> Unit,
    ) {
        val favorites = viewModel.favoriteActivities.value
        if (favorites.isEmpty()) {
            Toast.makeText(
                requireContext(),
                R.string.startup_items_empty_hint,
                Toast.LENGTH_SHORT,
            ).show()
            return
        }

        val adapter = object : android.widget.ArrayAdapter<MyActivityInfo>(
            requireContext(),
            R.layout.dialog_picker_item,
            R.id.tvName,
            favorites,
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: layoutInflater.inflate(
                    R.layout.dialog_picker_item,
                    parent,
                    false,
                )
                val info = getItem(position)!!
                view.findViewById<android.widget.ImageView>(R.id.ivIcon)
                    .setImageDrawable(info.icon)
                view.findViewById<android.widget.TextView>(R.id.tvName).text = info.name
                view.findViewById<android.widget.TextView>(R.id.tvPackage).text =
                    info.componentName.packageName
                return view
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setAdapter(adapter) { _, which ->
                onPicked(favorites[which])
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showLauncherPickerDialog() {
        showFavoritesPicker(getString(R.string.portal_tab_title)) { chosen ->
            portalSettingsService.saveDefaultLauncher(
                chosen.componentName.packageName,
                chosen.componentName.className,
            )
            updateLauncherInfo()
            Toast.makeText(
                requireContext(),
                getString(R.string.portal_launcher_set, chosen.name),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun showAddStartupPickerDialog() {
        showFavoritesPicker(getString(R.string.portal_add_to_startup)) { chosen ->
            val newItem = de.szalkowski.activitylauncher.StartupItem(
                packageName = chosen.componentName.packageName,
                className = chosen.componentName.className,
            )
            showStartupItemEditDialog(newItem) { edited ->
                portalSettingsService.addStartupItem(
                    packageName = edited.packageName,
                    className = edited.className,
                    startupType = edited.startupType,
                    delaySeconds = edited.delaySeconds,
                )
                loadStartupItems()
            }
        }
    }

    private fun showAddToStartupDialog(componentName: ComponentName) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(componentName.packageName)
            .setMessage(getString(R.string.startup_add_confirm))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                portalSettingsService.addStartupItem(
                    packageName = componentName.packageName,
                    className = componentName.className,
                )
                loadStartupItems()
                Toast.makeText(
                    requireContext(),
                    getString(R.string.startup_added_toast, componentName.packageName),
                    Toast.LENGTH_SHORT,
                ).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // ==================== Launcher Info ====================

    private fun updateLauncherInfo() {
        val pkg = portalSettingsService.getDefaultLauncherPackage()
        val cls = portalSettingsService.getDefaultLauncherClass()

        if (pkg != null && cls != null) {
            binding.tvLauncherInfo.text = getString(
                R.string.portal_current_launcher,
                "$pkg/${cls.substringAfterLast(".")}",
            )
            binding.btClearLauncher.isVisible = true
        } else {
            binding.tvLauncherInfo.text = getString(R.string.portal_no_launcher_set)
            binding.btClearLauncher.isVisible = false
        }
    }

    // ==================== Startup Items ====================

    private fun loadStartupItems() {
        val container = binding.startupItemsContainer
        container.removeAllViews()

        val startups = portalSettingsService.getStartupItems()

        binding.tvStartupEmpty.isVisible = startups.isEmpty()
        binding.btClearAllStartups.isVisible = startups.isNotEmpty()

        startups.forEachIndexed { index, item ->
            val row = createStartupItemRow(item, index)
            container.addView(row)
        }
    }

    private fun createStartupItemRow(
        item: de.szalkowski.activitylauncher.StartupItem,
        index: Int,
    ): View {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = resources.getDimensionPixelSize(R.dimen.portal_item_spacing)
            }
            minimumHeight = resources.getDimensionPixelSize(R.dimen.portal_item_min_height)
            isFocusable = true
            isClickable = true
            foreground = resources.getDrawable(
                android.R.drawable.list_selector_background,
                requireContext().theme,
            )
            setPadding(
                resources.getDimensionPixelSize(R.dimen.portal_item_padding_h),
                resources.getDimensionPixelSize(R.dimen.portal_item_padding_v),
                resources.getDimensionPixelSize(R.dimen.portal_item_padding_h),
                resources.getDimensionPixelSize(R.dimen.portal_item_padding_v),
            )
            setOnClickListener {
                showStartupItemEditDialog(item) { edited ->
                    portalSettingsService.updateStartupItem(
                        index,
                        edited.startupType,
                        edited.delaySeconds,
                    )
                }
            }
        }

        val textLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f,
            )
        }

        textLayout.addView(
            TextView(requireContext()).apply {
                text = item.packageName
                setTextAppearance(android.R.style.TextAppearance_Medium)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            },
        )

        val detailText = buildString {
            append(
                if (item.startupType == StartupType.IMMEDIATE) {
                    getString(R.string.startup_item_type_normal)
                } else {
                    getString(R.string.startup_item_type_delayed)
                },
            )
            if (item.delaySeconds > 0) {
                append(" · ${item.delaySeconds}${getString(R.string.startup_delay_seconds)}")
            }
        }

        textLayout.addView(
            TextView(requireContext()).apply {
                text = detailText
                setTextAppearance(android.R.style.TextAppearance_Small)
                setTextColor(resources.getColor(android.R.color.darker_gray, null))
            },
        )

        row.addView(textLayout)

        val deleteBtn = Button(
            android.view.ContextThemeWrapper(
                requireContext(),
                android.R.style.Widget_DeviceDefault_Button_Borderless_Small,
            ),
        ).apply {
            text = getString(R.string.startup_delete_title)
            isFocusable = true
            setOnClickListener {
                portalSettingsService.deleteStartupItem(index)
                loadStartupItems()
            }
        }
        row.addView(deleteBtn)

        return row
    }

    /**
     * Shows the startup type + delay editor dialog.
     * For existing items, callers pass [onConfirm] that calls [PortalSettingsService.updateStartupItem].
     * For new items, callers pass [onConfirm] that calls [PortalSettingsService.addStartupItem].
     */
    private fun showStartupItemEditDialog(
        item: de.szalkowski.activitylauncher.StartupItem,
        onConfirm: (de.szalkowski.activitylauncher.StartupItem) -> Unit,
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_startup_edit, null)
        val rgType = dialogView.findViewById<android.widget.RadioGroup>(R.id.rgStartupType)
        val etDelay = dialogView.findViewById<android.widget.EditText>(R.id.etDelaySeconds)

        rgType.check(
            if (item.startupType == StartupType.ON_NETWORK) R.id.rbOnNetwork else R.id.rbImmediate,
        )
        etDelay.setText(item.delaySeconds.toString())

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(item.packageName)
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val newType = if (rgType.checkedRadioButtonId == R.id.rbOnNetwork) {
                    StartupType.ON_NETWORK
                } else {
                    StartupType.IMMEDIATE
                }
                val delay = etDelay.text.toString().toIntOrNull() ?: 0
                onConfirm(
                    item.copy(startupType = newType, delaySeconds = delay),
                )
                loadStartupItems()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
