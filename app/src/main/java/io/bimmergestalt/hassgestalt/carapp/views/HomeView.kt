package io.bimmergestalt.hassgestalt.carapp.views

import io.bimmergestalt.hassgestalt.L
import io.bimmergestalt.hassgestalt.carapp.IconRenderer
import io.bimmergestalt.hassgestalt.carapp.batchDataTables
import io.bimmergestalt.hassgestalt.carapp.rhmiDataTableFlow
import io.bimmergestalt.hassgestalt.hass.DashboardHeader
import io.bimmergestalt.hassgestalt.hass.LovelaceConfig
import io.bimmergestalt.hassgestalt.hass.StateTracker
import io.bimmergestalt.idriveconnectkit.rhmi.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class HomeView(val state: RHMIState, val iconRenderer: IconRenderer, val hassState: Flow<StateTracker>, val lovelaceConfig: Flow<LovelaceConfig>, val displayedEntities: List<String>) {
	companion object {
		fun fits(state: RHMIState): Boolean {
			return state is RHMIState.PlainState &&
				state.componentsList.count { it is RHMIComponent.List } >= 5
		}
	}

	private val coroutineScope = CoroutineScope(Job() + Dispatchers.IO)

	private val list = state.componentsList.filterIsInstance<RHMIComponent.List>().first()
	private val dashboardLabel = state.componentsList.filterIsInstance<RHMIComponent.Label>().last()
	private val dashboardList = state.componentsList.filterIsInstance<RHMIComponent.List>().last()

	private var dashboards = emptyList<DashboardHeader>()

	fun initWidgets(dashboardView: DashboardView) {
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

		dashboardLabel.setVisible(true)
		dashboardLabel.getModel()?.asRaDataModel()?.value = L.DASHBOARD_LIST
		dashboardList.setVisible(true)
		dashboardList.setProperty(RHMIProperty.PropertyId.LIST_COLUMNWIDTH, "50,*")
		dashboardList.getAction()?.asHMIAction()?.getTargetModel()?.asRaIntModel()?.value = dashboardView.state.id
		dashboardList.getAction()?.asRAAction()?.rhmiActionCallback = RHMIActionListCallback { index ->
			val dashboard = dashboards.getOrNull(index)
			dashboardView.currentDashboard = dashboard
		}
	}

	private suspend fun onShow() {
		coroutineScope.launch {
			hassState.collectLatest { states: StateTracker ->
				displayedEntities
					.map { id: String -> states[id] }
					.rhmiDataTableFlow { item ->
						arrayOf(
							item.attributes["friendly_name"] as? String ?: item.entityId,
							item.state + (item.attributes["unit_of_measurement"] as? String ?: "")
						)
					}
					.batchDataTables()
					.collect {
						list.app.setModel(list.model, it)
					}
			}
		}
		coroutineScope.launch {
			lovelaceConfig.collectLatest { lovelaceConfig ->
				dashboards = lovelaceConfig.getDashboardList()
				dashboardList.getModel()?.value = object : RHMIModel.RaListModel.RHMIListAdapter<DashboardHeader>(2, dashboards) {
					override fun convertRow(index: Int, item: DashboardHeader): Array<Any> =
						arrayOf(
							item.iconDrawable?.let {iconRenderer.render(it, 46, 46)}
								?.let {iconRenderer.compress(it, 100)} ?: "",
							item.title,
						)
				}
			}
		}
	}
}