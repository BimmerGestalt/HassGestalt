package io.bimmergestalt.hassgestalt.hass

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

fun Flow<HassApi>.stateTracker(): Flow<StateTracker> = flatMapLatest { hassApi ->
	callbackFlow {
		val stateTracker = StateTracker(hassApi)
		stateTracker.subscribeAll(this)
		send(stateTracker)
		awaitClose {
			stateTracker.unsubscribeAll()
		}
}}

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