package io.bimmergestalt.hassgestalt.phoneui.hass

import androidx.lifecycle.*
import io.bimmergestalt.hassgestalt.hass.EntityState
import io.bimmergestalt.hassgestalt.hass.StateTracker

class EntityStateViewModel(stateLiveData: LiveData<EntityState>): ViewModel() {
	class Factory(val stateTracker: LiveData<StateTracker>, private val entityId: String): ViewModelProvider.Factory {
		@Suppress("UNCHECKED_CAST")
		override fun <T : ViewModel> create(modelClass: Class<T>): T {
			val stateLiveData = stateTracker.switchMap {
				it.liveData[entityId]
			}
			return EntityStateViewModel(stateLiveData) as T
		}
	}
	val label = stateLiveData.map { it.attributes["friendly_name"] as? String ?: it.entityId }
	val value = stateLiveData.map { it.state }
	val units = stateLiveData.map { it.attributes["unit_of_measurement"] as? String ?: ""}
}