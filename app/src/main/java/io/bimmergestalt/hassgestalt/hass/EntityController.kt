package io.bimmergestalt.hassgestalt.hass

import kotlinx.coroutines.flow.MutableSharedFlow
import org.json.JSONObject
import java.lang.AssertionError

class EntityController(val hassApi: HassApi, val entityState: EntityState, val pendingFlow: MutableSharedFlow<EntityRepresentation>): ()->Unit {

	companion object {
		fun create(hassApi: HassApi, entityState: EntityState, pendingFlow: MutableSharedFlow<EntityRepresentation>): EntityController? {
			val domain = entityState.entityId.split('.').first()
			// only support some types
			return if (domain == "group" ||
				domain == "button" ||
				domain == "input_button" ||
				domain == "fan" ||
				domain == "light" ||
				domain == "lock" ||
				domain == "switch") {
				EntityController(hassApi, entityState, pendingFlow)
			} else {
				null
			}
		}
	}

	val icon = null

	override fun invoke() {
		val domain = entityState.entityId.split('.').first()
		val service = when(domain) {
			"group" -> "toggle"
			"button" -> "press"
			"input_button" -> "press"
			"fan" -> "toggle"
			"light" -> "toggle"
			"lock" -> if (entityState.state == "locked") "unlock" else "lock"
			"switch" -> "toggle"
			else -> throw AssertionError("Unknown domain type $domain for entity ${entityState.entityId}")
		}
		val commandDomain = if (domain == "group") "homeassistant" else domain
		pendingFlow.tryEmit(EntityRepresentation.fromEntityState(entityState, null).copy(state = "..."))
		hassApi.request(JSONObject().apply {
			put("type", "call_service")
			put("domain", commandDomain)
			put("service", service)
			put("target", JSONObject().apply {
				put("entity_id", entityState.entityId)
			})
		})
	}
}