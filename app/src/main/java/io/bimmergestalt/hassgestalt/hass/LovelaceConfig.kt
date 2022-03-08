package io.bimmergestalt.hassgestalt.hass

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
				DashboardHeader(it.optString("title"), it.optString("url_path"), it.optString("icon"))
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