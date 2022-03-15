package io.bimmergestalt.hassgestalt.carapp.views

import android.util.SparseArray
import io.bimmergestalt.hassgestalt.carapp.IconRenderer
import io.bimmergestalt.hassgestalt.carapp.RHMIActionAbort
import io.bimmergestalt.hassgestalt.carapp.batchDataTables
import io.bimmergestalt.hassgestalt.carapp.rhmiDataTableFlow
import io.bimmergestalt.hassgestalt.hass.EntityRepresentation
import io.bimmergestalt.idriveconnectkit.rhmi.RHMIActionListCallback
import io.bimmergestalt.idriveconnectkit.rhmi.RHMIComponent
import io.bimmergestalt.idriveconnectkit.rhmi.RHMIModel
import io.bimmergestalt.idriveconnectkit.rhmi.RHMIProperty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class DashboardListComponent(val scope: CoroutineScope, val listComponent: RHMIComponent.List, val iconRenderer: IconRenderer) {
	var updateJob: Job? = null
	val listElements = SparseArray<EntityRepresentation>()

	init {
		initWidgets()
	}

	fun initWidgets() {
		listComponent.setVisible(true)
		listComponent.setProperty(RHMIProperty.PropertyId.LIST_COLUMNWIDTH, "50,*,150")
		listComponent.getAction()?.asRAAction()?.rhmiActionCallback = RHMIActionListCallback {
			listElements[it]?.tryClick()
			throw RHMIActionAbort()
		}
	}

	fun show(entities: List<Flow<EntityRepresentation>>) {
		// reset the click handler for this instance, in case we are sharing the list with others
		initWidgets()

		// subscribe to display updates
		updateJob?.cancel()
		updateJob = scope.launch {
			// memoize the EntityControllers for handling the row click handler
			listElements.clear()
			entities
				.mapIndexed { index, flow ->
					flow.map { entity ->
						entity.also { listElements.put(index, it) }
					}
				}
				.rhmiDataTableFlow { item -> arrayOf(
					item.icon?.let { iconRenderer.render(it, 46, 46) }
						?.let { iconRenderer.compress(it, 100) } ?: "",
					item.name,
					item.state
				) }
				.batchDataTables()
				.collect {
					listComponent.app.setModel(listComponent.model, it)
				}
		}
	}

	fun hide() {
		listComponent.getModel()?.value = RHMIModel.RaListModel.RHMIListConcrete(3)
		updateJob?.cancel()
	}
}