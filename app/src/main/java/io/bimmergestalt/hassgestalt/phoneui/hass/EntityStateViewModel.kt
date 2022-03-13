package io.bimmergestalt.hassgestalt.phoneui.hass

import androidx.lifecycle.*
import io.bimmergestalt.hassgestalt.hass.EntityState
import io.bimmergestalt.hassgestalt.hass.StateTracker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class EntityStateViewModel(stateFlow: Flow<EntityState>): ViewModel() {
	@Suppress("UNCHECKED_CAST")
	class Factory(val stateTracker: Flow<StateTracker>, private val entityId: String): ViewModelProvider.Factory {
		override fun <T : ViewModel> create(modelClass: Class<T>): T {
			val stateLiveData = stateTracker.flatMapLatest {
				it[entityId]
			}
			return EntityStateViewModel(stateLiveData) as T
		}
	}
	val liveData = stateFlow.asLiveData()
	val label = liveData.map { it.attributes["friendly_name"] as? String ?: it.entityId }
	val value = liveData.map { it.state }
	val units = liveData.map { it.attributes["unit_of_measurement"] as? String ?: ""}
}