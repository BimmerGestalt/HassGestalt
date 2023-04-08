package io.bimmergestalt.hassgestalt.hass

import kotlinx.coroutines.Deferred
import org.json.JSONObject
import java.lang.AssertionError

class EntityController(val hassApi: HassApi) {
	fun toggle(entityId: String, state: String): (()->Deferred<JSONObject>)? {
		val domain = entityId.split('.').first()
		// only support some types
		return if (domain == "alarm_control_panel" ||
			domain == "automation" ||
			domain == "cover" ||
			domain == "group" ||
			domain == "button" ||
			domain == "input_button" ||
			domain == "fan" ||
			domain == "light" ||
			domain == "lock" ||
			domain == "switch") {
			val commandDomain = if (domain == "group") "homeassistant" else domain
			val service = when(domain) {
				"alarm_control_panel" -> if (state == "disarmed") "alarm_arm_away" else "alarm_disarm"
				"automation" -> "toggle"
				"cover" -> "toggle"
				"group" -> "toggle"
				"button" -> "press"
				"input_button" -> "press"
				"fan" -> "toggle"
				"light" -> "toggle"
				"lock" -> if (state == "locked") "unlock" else "lock"
				"switch" -> "toggle"
				else -> throw AssertionError("Unknown domain type $domain for entity $entityId")
			}
			callService(commandDomain, service, mapOf("entity_id" to entityId))
		} else {
			null
		}
	}

	fun callService(domain: String, service: String, target: Map<String, Any?>, data: Map<String, Any?> = emptyMap()): ()->Deferred<JSONObject> {
		return {
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