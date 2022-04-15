package io.bimmergestalt.hassgestalt.hass

import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.OffsetDateTime
import kotlin.random.Random

class HassApiDemo(): HassApi {
	companion object {
		val DEMO_URL = "https://demo"
	}
	private data class MutableEntityState(
		val entityId: String,
		val state: String,
		val attributes: Map<String, Any?>,
	) {
		val lastChanged = OffsetDateTime.now()

		fun toJson(): JSONObject {
			return JSONObject().apply {
				put("entity_id", entityId)
				put("state", state)
				put("last_changed", lastChanged.toString())
				put("attributes", JSONObject().apply {
					attributes.forEach { k, v ->
						put(k, v)
					}
				})
			}
		}
	}
	private var MutableStateFlow<MutableEntityState>.state
		get() = this.value.state
		set(value) {
			this.value = this.value.copy(state=value)
		}
	private fun stateFactory(entityId: String, name: String, state: String, attributes: Map<String, Any?> = emptyMap()): MutableStateFlow<MutableEntityState> {
		return MutableStateFlow(
			MutableEntityState(
				entityId,
				state,
				mapOf("friendly_name" to name) + attributes
			)
		)
	}
	private val states = listOf(
		stateFactory("alarm_control_panel.partition_1", "Home Alarm", "disarmed"),
		stateFactory("light.front_door", "Front Door Light", "on", mapOf(
			"brightness" to 150
		)),
		stateFactory("lock.front_door", "Front Door Lock", "locked"),
		stateFactory("light.living_room", "Living Room Light", "off"),
		stateFactory("light.common_area", "Common Area Lights", "on", mapOf(
			"icon" to "mdi:lightbulb-group", "brightness" to 212,
		)),
		stateFactory("light.bedroom_lamp", "Bedroom Lamp", "on", mapOf(
			"brightness" to 50
		)),
		stateFactory("cover.garage_door", "Garage Door", "closed", mapOf(
			"raw_state_text" to "closed", "device_class" to "garage"
		))
	).map {
		it.value.entityId to it
	}.toMap()

	private val stateEvents = states.mapValues {
		it.value.map {
			delay(400 + Random(System.currentTimeMillis()).nextLong(100, 1000))      // simulate api delay
			JSONObject().apply {
				put("event", JSONObject().apply {
					put("data", JSONObject().apply {
						put("new_state", it.toJson())
					})
				})
			}
		}
	}
	private val eventStream = stateEvents.values.merge()

	private data class Dashboard(
		val title: String,
		val icon: String,
		val entities: List<StateFlow<MutableEntityState>>
	)
	private val dashboards = listOf(
		Dashboard("Security", "mdi:lock", listOf(
			states["alarm_control_panel.partition_1"]!!,
			states["lock.front_door"]!!,
			states["light.front_door"]!!,
			states["cover.garage_door"]!!
		)),
		Dashboard("Lights", "mdi:lightbulb", listOf(
			states["light.front_door"]!!,
			states["light.living_room"]!!,
			states["light.common_area"]!!,
			states["light.bedroom_lamp"]!!,
		))
	).map { it.title to it }
	.toMap()

	override fun subscribe(subscription: JSONObject): Flow<JSONObject> {
		if (subscription.optString("type") == "subscribe_events") {
			return eventStream
		} else if (subscription.optString("type") == "subscribe_trigger") {
			val entityId = subscription.optJSONObject("trigger")?.optString("entity_id") ?: ""
			return stateEvents[entityId] ?: flowOf()
		} else {
			return flowOf()
		}
	}

	override fun request(request: JSONObject): Deferred<JSONObject> {
		val id = request.optInt("id")
		val response = JSONObject().apply {
			put("id", id)
			put("type", "result")
			put("success", true)
		}
		val type = request.optString("type")
		when(type) {
			"get_states" -> response.apply {
				put("result", JSONArray().apply {
					states.values.forEach {
						put(it.value.toJson())
					}
				})
			}
			"get_panels" -> response.apply {
				put("result", JSONObject().apply {
					dashboards.forEach {
						put(it.key, JSONObject().apply {
							put("component_name", "lovelace")
							put("title", it.key)
							put("url_path", it.key)
							put("icon", it.value.icon)
						})
					}
				})
			}
			"lovelace/config" -> response.apply {
				put("result", JSONObject().apply {
					put("views", JSONArray().apply {
						put(JSONObject().apply {
							put("cards", JSONArray().apply {
								put(JSONObject(). apply {
									put("entities", JSONArray().apply {
										dashboards[request.optString("url_path")]?.entities?.forEach {
											put(it.value.entityId)
										}
									})
								})
							})
						})
					})
				})
			}
			"call_service" -> response.apply {
				val service = request.optString("service")
				val entityId = request.optJSONObject("target")?.optString("entity_id") ?: ""
				val entity = states[entityId]
				println("Received call_service $request against ${entity?.value}")
				if (entity != null) {
					when {
						entityId.startsWith("lock") -> entity.state = service + "ed"
						service == "toggle" && entity.state == "on" -> entity.state = "off"
						service == "toggle" && entity.state == "off" -> entity.state = "on"
						service == "alarm_disarm" -> entity.state = "disarmed"
						service == "alarm_arm_away" -> entity.state = "armed away"
						else -> println("Unknown service to call")
					}
				}
			}
			else -> response.apply {
				put("success", false)
				Log.w(TAG, "HassApiDisconnected didn't have an answer for $type request")
				put("error", JSONObject().apply {
					put("code", "unknown_request")
					put("message", "Unknown request")
				})
			}
		}
		return CompletableDeferred(response)
	}
}