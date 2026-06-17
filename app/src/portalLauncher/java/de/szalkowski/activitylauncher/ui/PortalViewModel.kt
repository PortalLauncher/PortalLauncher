package de.szalkowski.activitylauncher.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.szalkowski.activitylauncher.services.ActivityListService
import de.szalkowski.activitylauncher.services.FavoritesService
import de.szalkowski.activitylauncher.services.MyActivityInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PortalViewModel @Inject constructor(
    private val favoritesService: FavoritesService,
    private val activityListService: ActivityListService,
) : ViewModel() {

    private val _favoriteActivities = MutableStateFlow<List<MyActivityInfo>>(emptyList())
    val favoriteActivities: StateFlow<List<MyActivityInfo>> = _favoriteActivities.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val items = withContext(Dispatchers.Default) {
                favoritesService.getFavorites()
                    .mapNotNull { componentName ->
                        runCatching {
                            activityListService.getActivity(componentName)
                        }.getOrNull()
                    }
                    .sortedBy { it.componentName.flattenToShortString() }
            }
            _favoriteActivities.value = items
        }
    }
}
