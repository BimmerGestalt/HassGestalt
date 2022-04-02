package io.bimmergestalt.hassgestalt.carapp.views

import io.bimmergestalt.hassgestalt.L
import io.bimmergestalt.hassgestalt.carapp.IconRenderer
import io.bimmergestalt.hassgestalt.hass.DashboardHeader
import io.bimmergestalt.hassgestalt.hass.Lovelace
import io.bimmergestalt.idriveconnectkit.rhmi.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class HomeView(val state: RHMIState, val iconRenderer: IconRenderer, val lovelace: Flow<Lovelace>, val dashboards: Flow<List<DashboardHeader>>) {
	companion object {
		fun fits(state: RHMIState): Boolean {
			return state is RHMIState.PlainState &&
				state.componentsList.count { it is RHMIComponent.List } >= 5
		}
	}

	private val coroutineScope = CoroutineScope(Job() + Dispatchers.IO)

	private val headings = state.componentsList.filterIsInstance<RHMIComponent.Label>().takeLast(5).take(4)
	private val lists = state.componentsList.filterIsInstance<RHMIComponent.List>().takeLast(5).take(4)
	private val dashboardListComponents = lists.map {
		DashboardListComponent(coroutineScope, it, iconRenderer)
	}

	private val dashboardLabel = state.componentsList.filterIsInstance<RHMIComponent.Label>().last()
	private val dashboardList = state.componentsList.filterIsInstance<RHMIComponent.List>().last()

	private var shownDashboards = emptyList<DashboardHeader>()

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

		dashboardLabel.setVisible(true)
		dashboardLabel.getModel()?.asRaDataModel()?.value = L.DASHBOARD_LIST
		dashboardList.setVisible(true)
		dashboardList.setProperty(RHMIProperty.PropertyId.LIST_COLUMNWIDTH, "50,*")
		dashboardList.getAction()?.asHMIAction()?.getTargetModel()?.asRaIntModel()?.value = dashboardView.state.id
		dashboardList.getAction()?.asRAAction()?.rhmiActionCallback = RHMIActionListCallback { index ->
			val dashboard = shownDashboards.getOrNull(index)
			dashboardView.currentDashboard = dashboard
		}
	}

	private suspend fun onShow() {
		coroutineScope.launch {
			val starredDashboards = dashboards.map { currentDashboards ->
				currentDashboards.filter { it.starred }.take(headings.size)
			}

			lovelace.combine(starredDashboards) { dashboardRenderer, dashboards ->
				dashboards.forEachIndexed { index, dashboard ->
					headings[index].getModel()?.asRaDataModel()?.value = dashboard.title
					headings[index].setVisible(true)

					val entities = dashboardRenderer.renderDashboard(dashboard.url_path)
					dashboardListComponents[index].show(entities)
				}
				(dashboards.size until headings.size).forEach { index ->
					headings[index].setVisible(false)
					dashboardListComponents[index].hide()
				}
			}.collect()
		}

		coroutineScope.launch {
			dashboards.collectLatest { dashboards ->
				shownDashboards = dashboards
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