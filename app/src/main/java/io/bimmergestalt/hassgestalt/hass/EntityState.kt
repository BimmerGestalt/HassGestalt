package io.bimmergestalt.hassgestalt.hass

import java.time.OffsetDateTime

data class EntityState(
	val entityId: String,
	val state: String,
	val attributes: Map<String, Any?>,
	val lastChanged: OffsetDateTime,
	) {
	companion object {
		val EMPTY = EntityState("", "", emptyMap(), OffsetDateTime.MIN)
	}
}
