package io.bimmergestalt.hassgestalt.carapp.views

import android.util.SparseArray
import io.bimmergestalt.hassgestalt.L
import io.bimmergestalt.hassgestalt.carapp.IconRenderer
import io.bimmergestalt.hassgestalt.carapp.batchDataTables
import io.bimmergestalt.hassgestalt.carapp.rhmiDataTableFlow
import io.bimmergestalt.hassgestalt.hass.*
import io.bimmergestalt.idriveconnectkit.rhmi.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class DashboardView(val state: RHMIState, val iconRenderer: IconRenderer,
                    val hassApi: Flow<HassApi>, val hassState: Flow<StateTracker>, val lovelaceConfig: Flow<LovelaceConfig>) {
	companion object {
		fun fits(state: RHMIState): Boolean {
			return state is RHMIState.PlainState &&
					state.componentsList.count { it is RHMIComponent.List } >= 5
		}
	}

	private val coroutineScope = CoroutineScope(Job() + Dispatchers.IO)

	var currentDashboard: DashboardHeader? = null
	val listComponent = state.componentsList.filterIsInstance<RHMIComponent.List>().first()
	val listElements = SparseArray<EntityRepresentation>()

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
		listComponent.setProperty(RHMIProperty.PropertyId.LIST_COLUMNWIDTH, "50,*,150")
		listComponent.getAction()?.asRAAction()?.rhmiActionCallback = RHMIActionListCallback {
			listElements[it]?.tryClick()
		}
	}

	private suspend fun onShow() {
		state.getTextModel()?.asRaDataModel()?.value = currentDashboard?.title ?: "[Unknown]"

		val dashboardConfig = lovelaceConfig.map { config ->
			val currentDashboard = currentDashboard
			if (currentDashboard != null) {
				config.getDashboardConfig(currentDashboard.url_path)
			} else { LovelaceDashboard(emptyList()) }
		}
		combine(hassApi, hassState, dashboardConfig) { hassApi, hassState, dashboard ->
			dashboard.flatten(hassApi, hassState)
		}.map { list ->
			// memoize the EntityControllers for handling the row click handler
			listElements.clear()
			list.mapIndexed { index, flow ->
				flow.map { entity ->
					entity.also { listElements.put(index, it) }
				}
			}
		}.rhmiDataTableFlow { item -> arrayOf(
			item.icon?.let {iconRenderer.render(it, 46, 46)}
				?.let {iconRenderer.compress(it, 100)} ?: "",
			item.name,
			item.state
		)}.batchDataTables().collect {
			listComponent.app.setModel(listComponent.model, it)
		}
	}
}