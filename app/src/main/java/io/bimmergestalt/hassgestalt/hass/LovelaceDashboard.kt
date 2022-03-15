package io.bimmergestalt.hassgestalt.hass

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import io.bimmergestalt.hassgestalt.data.JsonHelpers.forEach
import io.bimmergestalt.hassgestalt.data.JsonHelpers.map
import io.bimmergestalt.hassgestalt.hass.EntityRepresentation.Companion.gainControl
import kotlinx.coroutines.flow.Flow
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
					stateTracker[id].gainControl(hassApi)
				})
				is LovelaceCardSensor -> results.add(stateTracker[card.entityId].gainControl(hassApi))
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