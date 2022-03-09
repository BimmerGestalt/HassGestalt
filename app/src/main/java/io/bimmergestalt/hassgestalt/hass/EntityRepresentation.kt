package io.bimmergestalt.hassgestalt.hass

import android.graphics.drawable.Drawable
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

data class EntityRepresentation(val iconName: String, val icon: Drawable?,
                                val entityId: String, val name: String, val state: String,
                                val actionIcon: Drawable?, val action: (() -> Unit)?,
) {
	companion object {
		fun fromEntityState(state: EntityState): EntityRepresentation {
			return EntityRepresentation((state.attributes["icon"] as? String) ?: "", null,
			state.entityId, state.attributes["friendly_name"] as? String ?: state.entityId,
			state.state + " ${state.attributes["unit_of_measurement"] ?: ""}",
			null, null)
		}

		fun Flow<EntityState>.gainControl(hassApi: HassApi): Flow<EntityRepresentation> {
			val pendingResult = MutableSharedFlow<EntityRepresentation>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
			val currentState = this.map { state ->
				val controller = EntityController.create(hassApi, state, pendingResult)
				EntityRepresentation((state.attributes["icon"] as? String) ?: "", null,
					state.entityId, state.attributes["friendly_name"] as? String ?: state.entityId,
					state.state + " ${state.attributes["unit_of_measurement"] ?: ""}",
					controller?.icon, controller)
			}
			return merge(currentState, pendingResult)
		}
	}

	fun tryClick() {
		action?.invoke()
	}

	override fun toString(): String {
		return "$name $state"
	}
}