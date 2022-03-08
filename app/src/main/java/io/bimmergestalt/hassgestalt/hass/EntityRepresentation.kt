package io.bimmergestalt.hassgestalt.hass

import android.graphics.drawable.Drawable

data class EntityRepresentation(val iconName: String, val icon: Drawable?,
                                val entityId: String, val name: String, val state: String,
                                val actionIcon: Drawable?, val action: ((entityId: String) -> Unit)?,
) {
	companion object {
		fun fromEntityState(state: EntityState): EntityRepresentation {
			return EntityRepresentation("", null,
			state.entityId, state.attributes["friendly_name"] as? String ?: state.entityId,
			state.state + " ${state.attributes["unit_of_measurement"] ?: ""}",
			null, null)
		}
	}
	override fun toString(): String {
		return "$name $state"
	}
}