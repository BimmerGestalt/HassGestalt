package io.bimmergestalt.hassgestalt.hass

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class StateFlowManager(val stateTracker: StateTracker) {
	private val flows = HashMap<String, MutableStateFlow<EntityState>>()
	operator fun get(entityId: String): StateFlow<EntityState> =
		flows.getOrPut(entityId) {
			MutableStateFlow(stateTracker.states[entityId] ?: EntityState.EMPTY)
		}

	fun onState(state: EntityState) {
		flows[state.entityId]?.value = state
	}
}