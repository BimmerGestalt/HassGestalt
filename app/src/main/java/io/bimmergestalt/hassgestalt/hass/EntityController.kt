package io.bimmergestalt.hassgestalt.hass

import kotlinx.coroutines.flow.MutableSharedFlow
import org.json.JSONObject
import java.lang.AssertionError

class EntityController(val hassApi: HassApi, val entityRepresentation: EntityRepresentation, val pendingFlow: MutableSharedFlow<EntityRepresentation>): ()->Unit {

	companion object {
		fun create(hassApi: HassApi, entityRepresentation: EntityRepresentation, pendingFlow: MutableSharedFlow<EntityRepresentation>): EntityController? {
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
				EntityController(hassApi, entityRepresentation, pendingFlow)
			} else {
				null
			}
		}
	}

	val icon = null

	override fun invoke() {
		val domain = entityRepresentation.entityId.split('.').first()
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
		val commandDomain = if (domain == "group") "homeassistant" else domain
		pendingFlow.tryEmit(entityRepresentation.copy(stateText = "..."))
		hassApi.request(JSONObject().apply {
			put("type", "call_service")
			put("domain", commandDomain)
			put("service", service)
			put("target", JSONObject().apply {
				put("entity_id", entityRepresentation.entityId)
			})
		})
	}
}