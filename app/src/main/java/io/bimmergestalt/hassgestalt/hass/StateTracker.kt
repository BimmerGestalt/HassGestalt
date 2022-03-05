package io.bimmergestalt.hassgestalt.hass

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import org.json.JSONObject

class StateTracker(val api: HassApi) {
	val states = HashMap<String, EntityState>()
	val liveData = StateLiveDataManager(this)

	private var allEventStream: ReceiveChannel<JSONObject>? = null

	fun subscribeAll(scope: CoroutineScope) {
		val eventStream = api.subscribe(JSONObject().apply {
			put("type", "subscribe_events")
			put("event_type", "state_changed")
		})
		this.allEventStream = eventStream
		scope.launch {
			fetchStates()
		}
		scope.launch {
			for (state in EntityStateParser().filterEventStream(this, eventStream)) {
				processState(state)
			}
		}
	}

	fun unsubscribeAll() {
		allEventStream?.cancel()
	}

	suspend fun fetchStates() {
		val allStates = api.request(JSONObject().apply {
			put("type", "get_states")
		}).await()
		EntityStateParser.parseStateArray(allStates.getJSONArray("result")).forEach { state ->
			processState(state)
		}
	}

	private fun processState(state: EntityState) {
		val entityId = state.entityId
		states[entityId] = state
		liveData.onState(state)

		Log.d(TAG, "New state $entityId = $state")
	}
}