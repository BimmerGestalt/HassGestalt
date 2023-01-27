package io.bimmergestalt.hassgestalt.hass

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.json.JSONObject
import java.lang.AssertionError

class EntityController(val hassApi: HassApi) {
	val pendingResult = MutableSharedFlow<EntityRepresentation>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

	fun toggle(entityRepresentation: EntityRepresentation): (()->Unit)? {
		val domain = entityRepresentation.entityId.split('.').first()
		// only support some types
		return if (domain == "alarm_control_panel" ||
			domain == "automation" ||
			domain == "group" ||
			domain == "button" ||
			domain == "input_button" ||
			domain == "fan" ||
			domain == "light" ||
			domain == "lock" ||
			domain == "switch") {
			val commandDomain = if (domain == "group") "homeassistant" else domain
			val service = when(domain) {
				"alarm_control_panel" -> if (entityRepresentation.state == "disarmed") "alarm_arm_away" else "alarm_disarm"
				"automation" -> "toggle"
				"group" -> "toggle"
				"button" -> "press"
				"input_button" -> "press"
				"fan" -> "toggle"
				"light" -> "toggle"
				"lock" -> if (entityRepresentation.state == "locked") "unlock" else "lock"
				"switch" -> "toggle"
				else -> throw AssertionError("Unknown domain type $domain for entity ${entityRepresentation.entityId}")
			}
			callService(entityRepresentation, commandDomain, service, mapOf("entity_id" to entityRepresentation.entityId))
		} else {
			null
		}
	}

	fun callService(entityRepresentation: EntityRepresentation, domain: String, service: String, target: Map<String, Any?>, data: Map<String, Any?> = emptyMap()): ()->Unit {
		return {
//			println("call_service ($domain.$service) on $target $data")
			pendingResult.tryEmit(entityRepresentation.copy(stateText = "..."))
			hassApi.request(JSONObject().apply {
				put("type", "call_service")
				put("domain", domain)
				put("service", service)
				put("target", JSONObject().apply {
					target.forEach { (key, value) ->
						put(key, value)
					}
				})
				put("service_data", JSONObject().apply {
					data.forEach { (key, value) ->
						put(key, value)
					}
				})
			})
		}
	}
}