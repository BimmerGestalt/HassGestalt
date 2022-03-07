package io.bimmergestalt.hassgestalt.carapp.views

import io.bimmergestalt.hassgestalt.L
import io.bimmergestalt.hassgestalt.carapp.rhmiDataTableFlow
import io.bimmergestalt.hassgestalt.hass.StateTracker
import io.bimmergestalt.idriveconnectkit.rhmi.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class HomeView(val state: RHMIState, val hassState: Flow<StateTracker>, val displayedEntities: List<String>) {
	companion object {
		fun fits(state: RHMIState): Boolean {
			return state is RHMIState.PlainState &&
				state.componentsList.count { it is RHMIComponent.List } >= 5
		}
	}

	private val coroutineScope = CoroutineScope(Job() + Dispatchers.IO)

	private val list = state.componentsList.filterIsInstance<RHMIComponent.List>().first()

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
		list.setVisible(true)
		list.setProperty(RHMIProperty.PropertyId.LIST_COLUMNWIDTH, "*,150")
	}

	private suspend fun onShow() {
		hassState.collectLatest { state: StateTracker ->
			displayedEntities
				.map { id: String -> state.flow[id] }
				.rhmiDataTableFlow { item ->
					arrayOf(
						item.attributes["friendly_name"] as? String ?: item.entityId,
						item.state + (item.attributes["unit_of_measurement"] as? String ?: "")
					)
				}
				.collect {
					list.app.setModel(list.model, it)
				}
		}
	}
}