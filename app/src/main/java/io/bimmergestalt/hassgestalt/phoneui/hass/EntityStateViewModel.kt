package io.bimmergestalt.hassgestalt.phoneui.hass

import androidx.lifecycle.*
import io.bimmergestalt.hassgestalt.hass.EntityState
import io.bimmergestalt.hassgestalt.hass.StateTracker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class EntityStateViewModel(stateLiveData: Flow<EntityState>): ViewModel() {
	@Suppress("UNCHECKED_CAST")
	class Factory(val stateTracker: Flow<StateTracker>, private val entityId: String): ViewModelProvider.Factory {
		override fun <T : ViewModel> create(modelClass: Class<T>): T {
			val stateLiveData = stateTracker.flatMapLatest {
				println("Locating flow for $entityId: ${it.flow[entityId]}")
				it.flow[entityId]
			}
			return EntityStateViewModel(stateLiveData) as T
		}
	}
	val label = stateLiveData.map { it.attributes["friendly_name"] as? String ?: it.entityId }.asLiveData()
	val value = stateLiveData.map { it.state }.asLiveData()
	val units = stateLiveData.map { it.attributes["unit_of_measurement"] as? String ?: ""}.asLiveData()
}