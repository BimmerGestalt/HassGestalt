package io.bimmergestalt.hassgestalt.data

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject


/* Kotlin ort of https://gist.github.com/hugodotlau/4201429 */
object JsonHelpers {
	@Throws(JSONException::class)
	fun toJSON(obj: Any?): Any? {
		return if (obj is Map<*, *>) {
			val json = JSONObject()
			for (key in obj.keys) {
				json.put(key.toString(), toJSON(obj[key]))
			}
			json
		} else if (obj is Iterable<*>) {
			val json = JSONArray()
			for (value in obj) {
				json.put(value)
			}
			json
		} else {
			obj
		}
	}

	fun isEmptyObject(obj: JSONObject): Boolean {
		return obj.names() == null
	}

	@Throws(JSONException::class)
	fun getMap(obj: JSONObject, key: String): Map<String, Any?> {
		return obj.getJSONObject(key).toMap()
	}

	@Throws(JSONException::class)
	fun JSONObject.toMap(): Map<String, Any?> {
		val map: MutableMap<String, Any?> = HashMap()
		for (key in this.keys()) {
			map[key] = fromJson(this[key])
		}
		return map
	}

	@Throws(JSONException::class)
	fun JSONArray.toList(): List<Any?> {
		val list: MutableList<Any?> = ArrayList(this.length())
		for (i in 0 until this.length()) {
			list.add(fromJson(this[i]))
		}
		return list
	}

	fun JSONArray.forEach(body: (Any?) -> Unit) {
		for (i in 0 until this.length()) {
			body(this[i])
		}
	}

	fun <R> JSONArray.map(body: (Any?) -> R): List<R> {
		val result = ArrayList<R>()
		for (i in 0 until this.length()) {
			result.add(body(this[i]))
		}
		return result
	}

	@Throws(JSONException::class)
	private fun fromJson(json: Any): Any? {
		return when {
			json === JSONObject.NULL -> null
			json is JSONObject -> json.toMap()
			json is JSONArray -> json.toList()
			else -> json
		}
	}
}