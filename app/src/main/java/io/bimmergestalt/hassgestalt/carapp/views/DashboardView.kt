package io.bimmergestalt.hassgestalt.carapp.views

import io.bimmergestalt.hassgestalt.L
import io.bimmergestalt.hassgestalt.carapp.rhmiDataTableFlow
import io.bimmergestalt.hassgestalt.hass.DashboardHeader
import io.bimmergestalt.hassgestalt.hass.LovelaceConfig
import io.bimmergestalt.hassgestalt.hass.LovelaceDashboard
import io.bimmergestalt.hassgestalt.hass.StateTracker
import io.bimmergestalt.idriveconnectkit.rhmi.FocusCallback
import io.bimmergestalt.idriveconnectkit.rhmi.RHMIComponent
import io.bimmergestalt.idriveconnectkit.rhmi.RHMIProperty
import io.bimmergestalt.idriveconnectkit.rhmi.RHMIState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class DashboardView(val state: RHMIState, val hassState: Flow<StateTracker>, val lovelaceConfig: Flow<LovelaceConfig>) {
	companion object {
		fun fits(state: RHMIState): Boolean {
			return state is RHMIState.PlainState &&
					state.componentsList.count { it is RHMIComponent.List } >= 5
		}
	}

	private val coroutineScope = CoroutineScope(Job() + Dispatchers.IO)

	var currentDashboard: DashboardHeader? = null
	val listComponent = state.componentsList.filterIsInstance<RHMIComponent.List>().first()

	fun initWidgets() {
		state.getTextModel()?.asRaDataModel()?.value = L.APP_NAME
		state.setProperty(RHMIProperty.PropertyId.HMISTATE_TABLETYPE, 3)
		state.focusCallback = FocusCallback { focused ->
			if (focused) {
				coroutineScope.launch { onShow() }
			} else {
				coroutineScope.coroutineContext.cancelChildren()
			}
		}

		listComponent.setVisible(true)
		listComponent.setProperty(RHMIProperty.PropertyId.LIST_COLUMNWIDTH, "*,150")
	}

	private suspend fun onShow() {
		state.getTextModel()?.asRaDataModel()?.value = currentDashboard?.title ?: "[Unknown]"

		lovelaceConfig.map { config ->
			val currentDashboard = currentDashboard
			if (currentDashboard != null) {
				config.getDashboardConfig(currentDashboard.url_path)
			} else { LovelaceDashboard(emptyList()) }
		}.combine(hassState) { dashboard, stateTracker ->
			dashboard.flatten(stateTracker)
		}.rhmiDataTableFlow { item -> arrayOf(
			item.name,
			item.state
		)}.collect {
			listComponent.app.setModel(listComponent.model, it)
		}
	}
}