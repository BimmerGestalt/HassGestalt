package io.bimmergestalt.hassgestalt.hass

import android.util.Log
import io.bimmergestalt.hassgestalt.data.JsonHelpers.map
import io.bimmergestalt.hassgestalt.data.JsonHelpers.toMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.OffsetDateTime

class EntityStateParser {
	companion object {
		fun parseStateArray(states: JSONArray): List<EntityState> {
			return states.map { stateData ->
				if (stateData is JSONObject) {
					parseStateData(stateData)
				} else null
			}.filterNotNull()
		}
		fun parseStateData(stateData: JSONObject): EntityState? {
			return try {
				val entityId = stateData.getString("entity_id")
				val state = stateData.getString("state")
				val attributes =
					stateData.optJSONObject("attributes")?.let { it.toMap() } ?: emptyMap()
				val lastChanged = OffsetDateTime.parse(stateData.getString("last_changed"))
				EntityState(entityId, state, attributes, lastChanged)
			} catch (e: JSONException) {
				Log.w(TAG, "Unexpected state structure: $stateData", e)
				null
			}
		}
		private fun parseStateEvent(eventData: JSONObject): EntityState? {
			return try {
				val stateData = eventData.getJSONObject("event")
					.getJSONObject("data")
					.getJSONObject("new_state")
				parseStateData(stateData)
			} catch (e: JSONException) {
				Log.w(TAG, "Unexpected event structure: $eventData", e)
				 null
			}
		}
	}

	@ExperimentalCoroutinesApi
	suspend fun filterEventStream(coroutineScope: CoroutineScope, events: ReceiveChannel<JSONObject>): ReceiveChannel<EntityState> = coroutineScope.produce {
		for (eventData in events) {
			Log.d(TAG, "New eventData $eventData")
			parseStateEvent(eventData)?.let {
				send(it)
			}
		}
	}
}