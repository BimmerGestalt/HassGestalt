package io.bimmergestalt.hassgestalt.phoneui.hass

import androidx.lifecycle.*
import io.bimmergestalt.hassgestalt.BR
import io.bimmergestalt.hassgestalt.R
import io.bimmergestalt.hassgestalt.data.ServerConfig
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

	val serverConfig = ServerConfig()

	// needs to be before, because dashboardItems refers to it
	val currentSelection: MutableLiveData<DashboardHeader?> = MutableLiveData<DashboardHeader?>(null)

	val lovelace = hassApi.combine(stateTracker) { api, state -> Lovelace(api, state) }
	val lovelaceDashboards = lovelace.map { config ->
		config.getDashboardList().also {
			val currentSelection = currentSelection.value
			if (currentSelection != null && !it.contains(currentSelection)) {
				this.currentSelection.postValue(null)
			}
		}
	}

	val dashboardItems = lovelaceDashboards.combine(serverConfig.starredDashboards) { dashboardList, starredDashboards ->
		val starred = HashSet(starredDashboards)
		dashboardList.map { it.copy(starred = starred.contains(it.url_path)) }
	}.asObservableList(viewModelScope)
	val dashboardItemsBinding = ItemBinding.of<DashboardHeader>(BR.dashboardHeader, R.layout.item_dashboard)
		.bindExtra(BR.viewModel, this)

	val dashboardEntities = lovelace.combine(currentSelection.asFlow()) { config, selected ->
		if (selected != null) {
			config.renderDashboard(selected.url_path)
				.map {it.asLiveData()}
		} else { emptyList() }
	}.asObservableList(viewModelScope)

	val dashboardEntitiesBinding = ItemBinding.of<LiveData<EntityRepresentation>>(BR.entityRepresentation, R.layout.item_entity_representation)

	fun toggleDashboardStar(urlPath: String) {
		val starred = ArrayList(serverConfig.starredDashboards.value)
		if (starred.contains(urlPath)) {
			starred.remove(urlPath)
		} else {
			starred.add(urlPath)
		}
		serverConfig.starredDashboards.value = starred
	}
	fun setCurrentSelection(value: DashboardHeader) {
		if (currentSelection.value == value) {
			currentSelection.value = null
		} else {
			currentSelection.value = value
		}
	}
}