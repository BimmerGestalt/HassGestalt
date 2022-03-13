package io.bimmergestalt.hassgestalt.phoneui.hass

import androidx.lifecycle.*
import io.bimmergestalt.hassgestalt.BR
import io.bimmergestalt.hassgestalt.R
import io.bimmergestalt.hassgestalt.hass.*
import io.bimmergestalt.hassgestalt.phoneui.asObservableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import me.tatarka.bindingcollectionadapter2.ItemBinding

class DashboardListViewModel(val hassApi: Flow<HassApi>, val stateTracker: Flow<StateTracker>): ViewModel() {
	@Suppress("UNCHECKED_CAST")
	class Factory(val hassApi: Flow<HassApi>, val stateTracker: Flow<StateTracker>): ViewModelProvider.Factory {
		override fun <T : ViewModel> create(modelClass: Class<T>): T {
			return DashboardListViewModel(hassApi, stateTracker) as T
		}
	}

	val lovelaceConfig = hassApi.map { LovelaceConfig(it) }
	val dashboardItems = lovelaceConfig.map { config -> config.getDashboardList() }
		.asObservableList(viewModelScope)
	val dashboardItemsBinding = ItemBinding.of<DashboardHeader>(BR.dashboardHeader, R.layout.item_dashboard)
		.bindExtra(BR.viewModel, this)

	val currentSelection = MutableLiveData<DashboardHeader?>(null)
	val currentSelectionTitle = currentSelection.map {it?.title ?: ""}

	val dashboardConfig = lovelaceConfig.combine(currentSelection.asFlow()) { config, selected ->
		if (selected != null) {
			config.getDashboardConfig(selected.url_path)
		} else { LovelaceDashboard(emptyList()) }
	}
	val dashboardEntities = combine(hassApi, stateTracker, dashboardConfig) { hassApi, stateTracker, dashboard ->
		dashboard.flatten(hassApi, stateTracker)
			.map {it.asLiveData()}
	}.asObservableList(viewModelScope)
	val dashboardEntitiesBinding = ItemBinding.of<LiveData<EntityRepresentation>>(BR.entityRepresentation, R.layout.item_entity_representation)

	fun setCurrentSelection(value: DashboardHeader) {
		if (currentSelection.value == value) {
			currentSelection.value = null
		} else {
			currentSelection.value = value
		}
	}
}