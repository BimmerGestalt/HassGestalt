package io.bimmergestalt.hassgestalt

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

object L {
	// access to the Android string resources
	var loadedResources: Resources? = null
		private set

	// all of the strings used in the car app
	// these default string values are used in tests, Android resources are used for real
	val APP_NAME by StringResourceDelegate("HassGestalt")

	val DASHBOARD_LIST by StringResourceDelegate("Dashboards")

	fun loadResources(context: Context, locale: Locale? = null) {
		val thisContext = if (locale == null) { context } else {
			val origConf = context.resources.configuration
			val localeConf = Configuration(origConf)
			localeConf.setLocale(locale)
			context.createConfigurationContext(localeConf)
		}

		loadedResources = thisContext.resources
	}
}

class StringResourceDelegate(val default: String): ReadOnlyProperty<L, String> {
	companion object {
		val pluralMatcher = Regex("([A-Z_]+)_([0-9]+)\$")
	}
	override operator fun getValue(thisRef: L, property: KProperty<*>): String {
		val resources = L.loadedResources ?: return default
		return if (property.name.matches(pluralMatcher)) {
			val nameMatch = pluralMatcher.matchEntire(property.name)
				?: throw AssertionError("Could not parse L name ${property.name}")
			val id = resources.getIdentifier(nameMatch.groupValues[1], "plurals", BuildConfig.APPLICATION_ID)
			if (id == 0) {
				throw AssertionError("Could not find Resource value for string ${property.name}")
			}
			val quantity = nameMatch.groupValues[2].toInt()
			resources.getQuantityString(id, quantity, quantity)
		} else {
			val id = resources.getIdentifier(property.name, "string", BuildConfig.APPLICATION_ID)
			if (id == 0) {
				throw AssertionError("Could not find Resource value for string ${property.name}")
			}
			resources.getString(id)
		}
	}
}