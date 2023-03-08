package io.bimmergestalt.hassgestalt.hass

import android.util.Log
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import java.lang.IllegalArgumentException
import kotlin.math.max
import kotlin.math.min

object EntityIcon {
	/**
	 * Given an icon name like mdi:lightbulb, return the IIcon parameter for IconicsDrawable to use
	 */
	fun iconByName(name: String): IIcon? {
		val iconValueName = "cmd_" + name.split(":").last().replace('-', '_')
		return try {
			CommunityMaterial.Icon.valueOf(iconValueName)
		} catch (e: IllegalArgumentException) {
			try {
				CommunityMaterial.Icon2.valueOf(iconValueName)
			} catch (e: IllegalArgumentException) {
				try {
					CommunityMaterial.Icon3.valueOf(iconValueName)
				} catch (e: IllegalArgumentException) {
					Log.i(TAG, "Could not find CMDI icon for $name")
					null
				}
			}
		}
	}

	/**
	 * Guesses the icon for an entity
	 * domain is the first part of the entity ID, such as light or sensor
	 * attributes comes from the entity state, and it primarily uses device_class and maybe source_type
	 */
	fun defaultIconName(domain: String, attributes: Map<String, Any?>, state: String): String {
		// Copied logic from https://github.com/home-assistant/android/blob/4544c3473bf882a882bbb54874e548de31247e13/wear/src/main/java/io/homeassistant/companion/android/util/CommonFunctions.kt#L26
		// with additional reference from https://github.com/home-assistant/frontend/blob/dev/src/common/entity/domain_icon.ts
		val deviceClass = attributes["device_class"] as? String
		return when(domain) {
			"button" -> when (deviceClass) {
				"restart" -> "mdi:restart"
				"update" -> "mdi:package-up"
				else -> "mdi:gesture-tap-button"
			}
			"binary_sensor" -> binarySensorIcon(domain, deviceClass, state)
			"cover" -> binarySensorIcon(domain, deviceClass, state)
			"device_tracker" -> when (attributes["source_type"]) {
				"router" -> if (state == "home") "mdi:lan-connect" else "mdi:lan-disconnect"
				"bluetooth", "bluetooth_le" -> if (state == "home") "mdi:bluetooth-connect" else "mdi:bluetooth"
				else -> if (state == "home") "mdi:account" else "mdi:account-arrow-right"
			}
			"fan" -> if (state == "on") "mdi:fan" else "mdi:fan-off"
			"group" -> "mdi:google-circles-communities"
			"humidifier" -> if (state == "on") "mdi:air-humidifier" else "mdi:air-humidifier-off"
			"input_boolean" -> if (state == "on") "mdi:check-circle-outline" else "mdi:close-circle-outline"
			"input_button" -> "mdi:gesture-tap-button"
			"light" -> "mdi:lightbulb"
			"lock" -> when (state) {
				"unlocked" -> "mdi:lock-open"
				"jammed" -> "mdi:lock-alert"
				"locking", "unlocking" -> "mdi:lock-clock"
				else -> "mdi:lock"
			}
			"media_player" -> if (state == "playing") "mdi:cast-connected" else "mdi:cast"
			"sensor" -> sensorIcon(deviceClass, state)
			"script" -> "mdi:script-text-outline"
			"scene" -> "mdi:palette-outline"
			"switch" -> when(deviceClass) {
				"outlet" -> if (state == "on") "mdi:power-plug" else "mdi:power-plug-off"
				"switch" -> if (state == "on") "mdi:toggle-switch" else "mdi:toggle-switch-off"
				else -> "mdi:flash"
			}
			else -> ""
		}
	}

	private fun binarySensorIcon(domain: String, deviceClass: String?, state: String): String {
		return when (deviceClass) {
			"battery" -> batteryIcon(state)
			"battery_charging" -> if (state == "on") "battery-charging" else "mdi:battery"
			"blind", "shade" -> when(state) {
				"opening" -> "mdi:arrow-split-vertical"
				"closing" -> "mdi:arrow-collapse-horizontal"
				"closed" -> "mdi:blinds"
				"off" -> "mdi:blinds"
				else ->  "mdi:blinds-open"
			}
			"cold" -> if (state == "on") "mdi:snowflake" else "mdi:thermometer"
			"connectivity" -> if (state == "on") "mdi:check-network-outline" else "mdi:close-network-outline"
			"curtain" -> when(state) {
				"opening" -> "mdi:arrow-split-vertical"
				"closing" -> "mdi:arrow-collapse-horizontal"
				"closed" -> "mdi:curtains-closed"
				"off" -> "mdi:curtains-closed"
				else ->  "mdi:curtains"
			}
			"door" -> if (state == "open" || state == "on") "mdi:door-open" else "mdi:door-closed"
			"damper" -> if (state == "open" || state == "on") "mdi:circle" else "mdi:circle-slice-8"
			"garage", "garage_door" -> when(state) {
				"opening" -> "mdi:arrow-up-box"
				"closing" -> "mdi:arrow-down-box"
				"closed" -> "mdi:garage"
				"off" -> "mdi-garage"
				else -> "mdi:garage-open"
			}
			"gate" -> when(state) {
				"opening", "closing" -> "mdi:gate-arrow-right"
				"closed" -> "mdi:gate"
				"off" -> "mdi:gate"
				else -> "mdi:gate-open"
			}
			"gas", "problem", "safety", "tamper" -> if (state == "on") "mdi:alert-circle" else "mdi:check-circle"
			"heat" -> if (state == "on") "mdi:fire" else "mdi:thermometer"
			"light" -> if (state == "on") "mdi:brightness-7" else "mdi:brightness-5"
			"lock" -> if (state == "on") "mdi:lock-open" else "mdi:lock"
			"moisture" -> if (state == "on") "mdi:water" else "mdi:water-off"
			"motion" -> if (state == "on") "mdi:motion-sensor" else "mdi:motion-sensor-off"
			"occupancy", "presence" -> if (state == "on") "mdi:home" else "mdi:home-outline"
			"opening" -> if (state == "on") "mdi:square" else "mdi:square-outline"
			"power" -> if (state == "on") "mdi:power-plug" else "mdi:power-plug-off"
			"running" -> if (state == "on") "mdi:play" else "mdi:stop"
			"shutter" -> when(state) {
				"opening" -> "mdi:arrow-up-box"
				"closing" -> "mdi:arrow-down-box"
				"closed" -> "mdi:window-shutter-closed"
				"off" -> "mdi:window-shutter-closed"
				else ->  "mdi:window-shutter-open"
			}
			"smoke" -> if (state == "on") "mdi:smoke" else "mdi:check-circle"
			"sound" -> if (state == "on") "mdi:music-note" else "mdi:music-note-off"
			"update" -> if (state == "on") "mdi:package-up" else "mdi:package"
			"vibration" -> if (state == "on") "mdi:vibrate" else "mdi:crop-portrait"
			"window" -> if (state == "on") "mdi:window-open" else "mdi:window-closed"
			else -> when(state) {
				"opening" -> "mdi:arrow-up-box"
				"closing" -> "mdi:arrow-down-box"
				"closed" -> "mdi:window-closed"
				else -> if (domain == "cover") "mdi:window-open" else {
					// binary sensor
					if (state == "on") "mdi:checkbox-marked-circle" else "mdi:radiobox-blank"
				}
			}
		}
	}

	private fun batteryIcon(state: String): String {
		var numericState = state.toIntOrNull()
		return if (numericState != null) {
			numericState = max(0, min(100, numericState)).div(10).times(10)
			if (numericState == 100) {
				"mdi:battery"
			} else {
				"mdi:battery-$numericState"
			}
		} else {
			if (state == "on") "mdi:battery-alert-variant-outline" else "mdi:battery"
		}
	}

	private fun sensorIcon(deviceClass: String?, state: String): String {
		// from https://github.com/home-assistant/frontend/blob/9f1e9b43fef706718628efd5acfca89e1c5c96c5/src/common/const.ts#L114
		return when (deviceClass) {
			"apparent_power" -> "mdi:flash"
			"aqi" -> "mdi:air-filter"
			"battery" -> batteryIcon(state)
			"carbon_dioxide" -> "mdi:molecule-co2"
			"carbon_monoxide" -> "mdi:molecule-co"
			"current" -> "mdi:current-ac"
			"date" -> "mdi:calendar"
			"energy" -> "mdi:lightning-bolt"
			"frequency" -> "mdi:sine-wave"
			"gas" -> "mdi:gas-cylinder"
			"humidity" -> "mdi:water-percent"
			"illuminance" -> "mdi:brightness-5"
			"monetary" -> "mdi:cash"
			"nitrogen_dioxide" -> "mdi:molecule"
			"nitrogen_monoxide" -> "mdi:molecule"
			"nitrous_oxide" -> "mdi:molecule"
			"ozone" -> "mdi:molecule"
			"pm1", "pm10", "pm25" -> "mdi:molecule"
			"power" -> "mdi:flash"
			"power_factor" -> "mdi:angle-acute"
			"pressure" -> "mdi:gauge"
			"reactive_power" -> "mdi:flash"
			"signal_strength" -> "mdi:wifi"
			"sulphur_dioxide" -> "mdi:molecule"
			"temperature" -> "mdi:thermometer"
			"timestamp" -> "mdi:clock"
			"volatile_organic_compounds" -> "mdi:molecule"
			"voltage" -> "mdi:sine-wave"
			else -> ""
		}
	}
}