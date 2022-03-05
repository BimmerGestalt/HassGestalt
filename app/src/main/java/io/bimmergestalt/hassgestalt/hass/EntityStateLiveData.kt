package io.bimmergestalt.hassgestalt.hass

import androidx.lifecycle.LiveData

class StateLiveDataManager(val stateTracker: StateTracker) {
	val liveDatas = HashMap<String, EntityStateLiveData>()
	operator fun get(entityId: String): LiveData<EntityState> =
		liveDatas.getOrPut(entityId) {
			EntityStateLiveData(stateTracker.states[entityId])
		}

	fun onState(state: EntityState) {
		liveDatas[state.entityId]?.onState(state)
	}
}

class EntityStateLiveData(initialValue: EntityState?): LiveData<EntityState>() {
	init {
		initialValue?.let {
			value = it
		}
	}
	fun onState(state: EntityState) {
		value = state
	}
}

