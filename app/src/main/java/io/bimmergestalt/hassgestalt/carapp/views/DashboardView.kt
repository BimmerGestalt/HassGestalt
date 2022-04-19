package io.bimmergestalt.hassgestalt.carapp.views

import io.bimmergestalt.hassgestalt.L
import io.bimmergestalt.hassgestalt.carapp.IconRenderer
import io.bimmergestalt.hassgestalt.hass.*
import io.bimmergestalt.idriveconnectkit.rhmi.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class DashboardView(val state: RHMIState, val iconRenderer: IconRenderer,
                    val lovelace: Flow<Lovelace>) {
	companion object {
		fun fits(state: RHMIState): Boolean {
			return state is RHMIState.PlainState &&
					state.componentsList.count { it is RHMIComponent.List } >= 5
		}
	}

	private val coroutineScope = CoroutineScope(Job() + Dispatchers.IO)

	var currentDashboard: DashboardHeader? = null
	val labelComponent = state.componentsList.filterIsInstance<RHMIComponent.Label>().first()
	val listComponent = state.componentsList.filterIsInstance<RHMIComponent.List>().first()
	val dashboardListComponent = DashboardListComponent(coroutineScope, listComponent, iconRenderer)

	fun initWidgets() {
		state.getTextModel()?.asRaDataModel()?.value = L.APP_NAME
		state.setProperty(RHMIProperty.PropertyId.HMISTATE_TABLETYPE, 3)
		state.focusCallback = FocusCallback { focused ->
			if (focused) {
				coroutineScope.launch { onShow() }
			} else {
				coroutineScope.coroutineContext.cancelChildren()
				dashboardListComponent.hide()
			}
		}
		labelComponent.setVisible(true)
	}

	private suspend fun onShow() {
		state.getTextModel()?.asRaDataModel()?.value = currentDashboard?.title ?: "[Unknown]"
		labelComponent.getModel()?.asRaDataModel()?.value = currentDashboard?.title ?: "[Unknown]"

		val dashboardEntries = lovelace.map { dashboardRenderer ->
			val currentDashboard = currentDashboard
			if (currentDashboard != null) {
				labelComponent.setProperty(RHMIProperty.PropertyId.LABEL_WAITINGANIMATION, true)
				dashboardRenderer.renderDashboard(currentDashboard.url_path)
			} else { emptyList() }
		}
		dashboardEntries.collectLatest {
			labelComponent.setProperty(RHMIProperty.PropertyId.LABEL_WAITINGANIMATION, false)
			dashboardListComponent.show(it)
		}
	}
}