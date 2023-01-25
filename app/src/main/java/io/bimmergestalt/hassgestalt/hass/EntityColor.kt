package io.bimmergestalt.hassgestalt.hass

import android.graphics.Color

object EntityColor {
	val OFF = Color.parseColor("#44739e")
	val ON = Color.parseColor("#fdd835")
	val UNAVAIL = Color.parseColor("#6f6f6f")
	val ON_R = 0xfd
	val ON_G = 0xd8
	val ON_B = 0x35

	val GAUGE_COLORS = mapOf(
		"min" to Color.parseColor("#039be5"),
		"red" to Color.parseColor("#df4c1e"),
		"yellow" to Color.parseColor("#f4b400"),
		"green" to Color.parseColor("#2fa034")
	)
}