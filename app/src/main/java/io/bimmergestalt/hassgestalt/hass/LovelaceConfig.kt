package io.bimmergestalt.hassgestalt.hass

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import com.mikepenz.iconics.IconicsColor
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.utils.color
import org.json.JSONObject

class LovelaceConfig(val api: HassApi) {
	suspend fun getDashboardList(): List<DashboardHeader> {
		val panelConfig = api.request(JSONObject().apply {
			put("type", "get_panels")
		}).await()
		val result = panelConfig.optJSONObject("result") ?: return emptyList()
		return result.keys().asSequence()
			.mapNotNull { key -> result.optJSONObject(key) }
			.filter { it.optString("component_name") == "lovelace" && it.optString("title") != "null" }
			.map {
				val icon = it.optString("icon")
				val iconValue = EntityIcon.iconByName(icon ?: "")
				val iconDrawable: (Context.() -> Drawable)? = if (iconValue != null) {
					{
						IconicsDrawable(this, iconValue).apply {
							style = Paint.Style.FILL_AND_STROKE
							color = IconicsColor.colorInt(Color.parseColor("#44739e"))
						}
					}
				} else { null }
				DashboardHeader(it.optString("title"), it.optString("url_path"),
					icon, iconDrawable)
			}
			.toList()
	}

	suspend fun getDashboardConfig(urlPath: String): LovelaceDashboard {
		val panelConfig = api.request(JSONObject().apply {
			put("type", "lovelace/config")
			put("url_path", urlPath)
		}).await()
		val result = panelConfig.optJSONObject("result") ?: return LovelaceDashboard(emptyList())
		return LovelaceDashboard.parse(result)
	}
}