package io.bimmergestalt.hassgestalt

import android.graphics.Color
import io.bimmergestalt.hassgestalt.hass.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class LovelaceDashboardTest {
	@Test
	fun testDashboard() {
		val json = JSONObject("""{"views":[{"title":"Home","cards":[
			{"type":"entity","entity":"lock.front_door_lock"}
		]}]}
		""".trimIndent())
		val lovelace = LovelaceDashboard.parse(json)
		assertNotEquals(0, lovelace.cards.size)
	}

	@Test
	fun testDashboardSections() {
		val json = JSONObject("""{"views":[{"title":"Home","sections":[
			{"type":"grid","cards":[
			{"type":"entity","entity":"lock.front_door_lock"}
		]}
		]}]}
		""".trimIndent())
		val lovelace = LovelaceDashboard.parse(json)
		assertNotEquals(0, lovelace.cards.size)
	}

	@Test
	fun testHiddenEntity() {
		val json = JSONObject("""
			{"type":"entity","entity":"lock.front_door_lock","hass_gestalt":false}
		""".trimIndent())
		val lovelace = LovelaceCard.parse(json)
		assertNull(lovelace)
	}
	@Test
	fun testHiddenEntities() {
		val json = JSONObject("""
			{"type":"entities","entities":[
			  {"entity":"lock.front_door_lock","hass_gestalt":false},
			  {"entity":"lock.garage_door_lock"}
			]}
		""".trimIndent())
		val lovelace = LovelaceCard.parse(json)
		assertTrue(lovelace is LovelaceCardEntities)
		lovelace as LovelaceCardEntities
		assertEquals(1, lovelace.entities.size)
		assertEquals("lock.garage_door_lock", lovelace.entities[0].entityId)
	}

	@Test
	fun testButtonCardSimple() {
		val json = JSONObject("""{
			"type": "button",
			"entity": "light.living_room"
		}""".trimIndent())
		val lovelace = LovelaceCard.parse(json)
		assertTrue(lovelace is LovelaceCardEntity)
		lovelace as LovelaceCardEntity
		assertEquals("light.living_room", lovelace.entityId)
	}

	@Test
	fun testButtonCardService() {
		// doesn't have an entity_id because it just triggers a service
		val json = JSONObject("""{
			"type": "button",
			"name": "Turn Off Lights",
			"show_state": false,
			"tap_action": {
			  "action": "call-service",
			  "service": "script.turn_on",
			  "data": {
			    "entity_id": "script.turn_off_lights"
			  }
			}
		}""".trimIndent())
		val lovelace = LovelaceCard.parse(json)
		assertTrue(lovelace is LovelaceCardEntity)
		lovelace as LovelaceCardEntity
		assertEquals("", lovelace.entityId)
	}

	@Test
	fun testGaugeSeverity() {
		// a gauge with colored severity bands
		val json = JSONObject("""{
			"type": "gauge",
			"name": "With Severity",
			"unit": "%",
			"entity": "sensor.cpu_usage",
			"severity": {
			  "green": 0,
			  "yellow": 45,
			  "red": 85
			}
		}""".trimMargin())
		val lovelace = LovelaceCard.parse(json)
		assertTrue(lovelace is LovelaceCardGauge)
		lovelace as LovelaceCardGauge
		assertEquals("sensor.cpu_usage", lovelace.entityId)

		val source = EntityRepresentation("", 0, "sensor.cpu_usage", "", "28.0", "28.0", null)
		val green = lovelace.apply(source)
		assertEquals(EntityColor.GAUGE_COLORS["green"], green.color)
		assertEquals("28.0 %", green.stateText)
		val yellow = lovelace.apply(source.copy(state="67.5", stateText = "67.5"))
		assertEquals(EntityColor.GAUGE_COLORS["yellow"], yellow.color)
		assertEquals("67.5 %", yellow.stateText)
		val red = lovelace.apply(source.copy(state="85.0", stateText = "85.0"))
		assertEquals(EntityColor.GAUGE_COLORS["red"], red.color)
		assertEquals("85.0 %", red.stateText)
	}

	@Test
	fun testGaugeSegments() {
		val json = JSONObject("""{
			"type": "gauge",
			"entity": "sensor.kitchen_humidity",
			"needle": true,
			"min": 20,
			"max": 80,
			"segments": [{
			  "from": 0,
			  "color": "#db4437"
			}, {
			  "from": 35,
			  "color": "#ffa600"
			}, {
			  "from": 40,
			  "color": "#43a047"
			}, {
			  "from": 60,
			  "color": "#ffa601"
			}, {
			  "from": 65,
			  "color": "#db4438"
			}]
		}""".trimMargin())
		val lovelace = LovelaceCard.parse(json)
		assertTrue(lovelace is LovelaceCardGauge)
		lovelace as LovelaceCardGauge
		assertEquals("sensor.kitchen_humidity", lovelace.entityId)

		val source = EntityRepresentation("", 0, "sensor.kitchen_humidity", "", "42", "42 %", null)
		val green = lovelace.apply(source)
		assertEquals(Color.parseColor("#43a047"), green.color)
		val redLow = lovelace.apply(source.copy(state = "10", stateText = "10 %"))
		assertEquals(Color.parseColor("#db4437"), redLow.color)
		val yellowLow = lovelace.apply(source.copy(state = "35", stateText = "35 %"))
		assertEquals(Color.parseColor("#ffa600"), yellowLow.color)
		val yellowHigh = lovelace.apply(source.copy(state = "64", stateText = "64 %"))
		assertEquals(Color.parseColor("#ffa601"), yellowHigh.color)
		val redHigh = lovelace.apply(source.copy(state = "65", stateText = "65 %"))
		assertEquals(Color.parseColor("#db4438"), redHigh.color)
	}
}