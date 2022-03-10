package io.bimmergestalt.hassgestalt.hass

import android.graphics.Color
import java.time.OffsetDateTime
import kotlin.math.max
import kotlin.math.min

data class EntityState(
	val entityId: String,
	val state: String,
	val attributes: Map<String, Any?>,
	val lastChanged: OffsetDateTime,
	) {
	companion object {
		val EMPTY = EntityState("", "", emptyMap(), OffsetDateTime.MIN)
	}

	val label = attributes["friendly_name"] as? String ?: entityId
	val stateText = (attributes["raw_state_text"] as? String) ?: state +
	((attributes["unit_of_measurement"] as? String)?.let { " $it" } ?: "")
	val domain = entityId.split('.').first()

	fun icon(): String {
		if (attributes.containsKey("icon")) {
			return (attributes["icon"] as? String) ?: ""
		}
		return EntityIcon.defaultIconName(domain, attributes, state)
	}

	fun color(): Int {
		val colorAttr = attributes["rgb_color"] as? List<*>
		return if (colorAttr != null) {
			val r = colorAttr[0] as? Int ?: 100
			val g = colorAttr[1] as? Int ?: 100
			val b = colorAttr[2] as? Int ?: 100
			val brightness = attributes["brightness"] as? Int
			adjustColorBrightness(r, g, b, brightness)
		} else {
			if (domain == "light" && state == "on") {
				adjustColorBrightness(0xfd, 0xd8, 0x35, attributes["brightness"] as? Int)
			} else if (state == "unavailable") {
				Color.parseColor("#6f6f6f")
			} else {
				Color.parseColor("#44739e")
			}
		}
	}

	private fun adjustColorBrightness(r: Int, g: Int, b: Int, brightness: Int?): Int {
		return if (brightness != null) {
			val factor = brightness / 255.0 * 0.5 + 0.5
			Color.rgb((r*factor).toInt(), (g*factor).toInt(), (b*factor).toInt())
		} else {
			Color.rgb(r,g,b)
		}
	}
}
