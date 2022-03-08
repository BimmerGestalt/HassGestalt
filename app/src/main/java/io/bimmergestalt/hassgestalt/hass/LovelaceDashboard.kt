package io.bimmergestalt.hassgestalt.hass

import android.util.Log
import io.bimmergestalt.hassgestalt.data.JsonHelpers.forEach
import io.bimmergestalt.hassgestalt.data.JsonHelpers.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.util.ArrayList

class DashboardHeader(val title: String, val url_path: String, icon: String)

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

	fun flatten(stateTracker: StateTracker): List<Flow<EntityRepresentation>> {
		val results = ArrayList<Flow<EntityRepresentation>>()
		cards.forEach { card ->
			when (card) {
				is LovelaceCardEntities -> results.addAll(card.entities.map { id ->
					stateTracker.flow[id].map { EntityRepresentation.fromEntityState(it) }
				})
				is LovelaceCardSensor -> results.add(stateTracker.flow[card.entityId].map {EntityRepresentation.fromEntityState(it)})
			}
		}
		return results
	}
}

sealed class LovelaceCard {
	companion object {
		fun parse(data: JSONObject): LovelaceCard? {
			Log.d(TAG, "Parsing lovelace card $data")
			return when(data.optString("type")) {
				"entities" -> LovelaceCardEntities(data.optJSONArray("entities")?.map {
					(it as? JSONObject)?.optString("entity")
				}?.filterNotNull() ?: emptyList())
				"sensor" -> data.optString("entity")?.let { LovelaceCardSensor(it) }
				else -> null
			}
		}
	}
}

class LovelaceCardEntities(val entities: List<String>): LovelaceCard() {
	override fun toString(): String {
		return "LovelaceCardEntities($entities)"
	}
}
class LovelaceCardSensor(val entityId: String): LovelaceCard() {
	override fun toString(): String {
		return "LovelaceCardSensor($entityId)"
	}
}