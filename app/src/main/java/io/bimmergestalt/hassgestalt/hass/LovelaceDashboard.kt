package io.bimmergestalt.hassgestalt.hass

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.Log
import io.bimmergestalt.hassgestalt.data.JsonHelpers.forEach
import io.bimmergestalt.hassgestalt.data.JsonHelpers.map
import io.bimmergestalt.hassgestalt.data.JsonHelpers.toMap
import io.bimmergestalt.hassgestalt.hass.EntityRepresentation.Companion.asRepresentation
import io.bimmergestalt.hassgestalt.hass.EntityRepresentation.Companion.gainControl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.util.ArrayList

data class DashboardHeader(val starred: Boolean, val title: String, val url_path: String, val icon: String, val iconDrawable: (Context.() -> Drawable?)?)

class LovelaceDashboard(val cards: List<LovelaceCard>) {
	companion object {
		fun parse(data: JSONObject): LovelaceDashboard {
			val cards = ArrayList<LovelaceCard>()
			data.optJSONArray("views")?.forEach { tab ->
				if (tab is JSONObject) {
					tab.optJSONArray("cards")?.map { cardData ->
						if (cardData is JSONObject) {
							cards.addAll(parseCards(cardData))
						}
					}
				}
			}
			return LovelaceDashboard(cards)
		}
		private fun parseCards(data: JSONObject): List<LovelaceCard> {
			val type = data.optString("type")
			val result = ArrayList<LovelaceCard>()
			// flatten stacks
			when (type) {
				"vertical-stack" -> data.optJSONArray("cards")?.map {
					if (it is JSONObject) LovelaceCard.parse(it) else null
				}?.filterNotNull()?.also {
					result.addAll(it)
				}
				"horizontal-stack" -> data.optJSONArray("cards")?.map {
					if (it is JSONObject) LovelaceCard.parse(it) else null
				}?.filterNotNull()?.also {
					result.addAll(it)
				}
				else -> LovelaceCard.parse(data)?.let { result.add(it) }
			}
			return result
		}
	}

	fun flatten(hassApi: HassApi, stateTracker: StateTracker): List<Flow<EntityRepresentation>> {
		val results = ArrayList<Flow<EntityRepresentation>>()
		cards.forEach { card ->
			when (card) {
				is LovelaceCardEntities -> results.addAll(card.entities.map { id ->
					stateTracker[id].asRepresentation().map{card.apply(it)}.gainControl(hassApi)
				})
				is LovelaceCardSingle -> results.add(stateTracker[card.entityId].asRepresentation().map{card.apply(it)}.gainControl(hassApi))
			}
		}
		return results
	}
}

sealed class LovelaceCard {
	companion object {
		/** Parse the Lovelace dashboard card config into a list of entities
		 * Future development could use different subclasses to enable different UI features
		 */
		fun parse(data: JSONObject): LovelaceCard? {
			Log.d(TAG, "Parsing lovelace card $data")
			val type = data.optString("type")
			return when {
				type == "map" -> null       // doesn't translate well to a text string
				data.has("entity") && data.optString("type") == "gauge" -> {
					LovelaceCardGauge(data.optString("entity"), data.toMap())
				}
				data.has("entity") -> {
					LovelaceCardSingle(data.optString("entity"), data.toMap())
				}
				data.optJSONArray("entities") != null -> {
					LovelaceCardEntities(data.optJSONArray("entities")?.map {
						// statistics-graph just has a list of strings
						// but most others have a list of objects
						when (it) {
							is JSONObject -> it.optString("entity")
							is String -> it
							else -> null
						}
					}?.filterNotNull() ?: emptyList(), data.toMap())
				}
				else -> null
			}
		}
	}

	open fun apply(representation: EntityRepresentation): EntityRepresentation {
		return representation
	}
}

class LovelaceCardEntities(val entities: List<String>, val attributes: Map<String, Any?>): LovelaceCard() {
	override fun toString(): String {
		return "LovelaceCardEntities($entities)"
	}
	override fun apply(representation: EntityRepresentation): EntityRepresentation {
		var output = representation
		if ((attributes["state_color"] as? Boolean) == true) {
			if (representation.color == EntityColor.OFF && representation.state == "on") {
				output = output.copy(color = EntityColor.ON)
			}
		}
		return output
	}
}
open class LovelaceCardSingle(val entityId: String, val attributes: Map<String, Any?>): LovelaceCard() {
	override fun toString(): String {
		return "LovelaceCardSingle($entityId)"
	}

	override fun apply(representation: EntityRepresentation): EntityRepresentation {
		var output = representation
		val forcedName = attributes["name"] as? String
		val forcedIcon = attributes["icon"] as? String
		if (forcedName?.isNotBlank() == true) {
			output = output.copy(name = forcedName)
		}
		if (forcedIcon?.isNotBlank() == true) {
			output = output.copy(iconName = forcedIcon)
		}
		return output
	}
}
class LovelaceCardGauge(entityId: String, attributes: Map<String, Any?>): LovelaceCardSingle(entityId, attributes) {
	override fun toString(): String {
		return "LovelaceCardGauge($entityId)"
	}

	data class SegmentBandConfig(val from: Int, val color: Int, val label: String?) {
		companion object {
			fun fromMap(input: Map<*, *>): SegmentBandConfig? {
				val from = input["from"] as? Int
				val color = (input["color"] as? String)?.let { try {
					Color.parseColor(it)
				} catch (_: Exception) { null } }
				val label = input["label"] as? String
				return if (from != null && color != null) {
					SegmentBandConfig(from, color, label)
				} else {
					null
				}
			}
		}
	}

	fun <T> bandValues(value: Int, bands: Map<Int, T>): T? {
		val band = bands.keys.sorted().firstOrNull { it < value }
		return bands[band]
	}

	override fun apply(representation: EntityRepresentation): EntityRepresentation {
		var output = representation
		val forcedName = attributes["name"] as? String
		if (forcedName != null) {
			output = output.copy(name = forcedName)
		}
		val value = representation.state.toIntOrNull()

		val severityDefs = attributes["severity"] as? Map<*, *>
		if (value != null && severityDefs != null) {
			val severityBands = severityDefs.mapNotNull { entry ->
				val color = EntityColor.GAUGE_COLORS[entry.key]
				val bandValue = entry.value as? Int
				if (color != null && bandValue != null) {
					Pair(bandValue, color)
				} else { null }
			}.toMap()
			val forcedColor = bandValues(value, severityBands) ?: EntityColor.GAUGE_COLORS["min"]!!
			output = output.copy(color = forcedColor)
		}

		val segmentDefs = attributes["segments"] as? List<*>
		if (value != null && segmentDefs != null) {
			val segmentBands = segmentDefs.mapNotNull { entry ->
				(entry as? Map<*, *>)?.let { SegmentBandConfig.fromMap(entry) }
			}.associateBy { it.from }
			val band = bandValues(value, segmentBands)
			val forcedColor = band?.color ?: EntityColor.GAUGE_COLORS["min"]!!
			output = output.copy(color = forcedColor)
			if (band?.label != null) {
				output = output.copy(stateText = band.label)
			}
		}
		return output
	}
}