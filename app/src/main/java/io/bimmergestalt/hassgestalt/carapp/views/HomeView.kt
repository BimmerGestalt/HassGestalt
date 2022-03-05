package io.bimmergestalt.hassgestalt.carapp.views

import androidx.lifecycle.LiveData
import io.bimmergestalt.hassgestalt.L
import io.bimmergestalt.hassgestalt.hass.EntityState
import io.bimmergestalt.hassgestalt.hass.StateTracker
import io.bimmergestalt.idriveconnectkit.rhmi.*

class HomeView(val state: RHMIState, val hassState: LiveData<StateTracker>, val displayedEntities: List<String>) {
	companion object {
		fun fits(state: RHMIState): Boolean {
			return state is RHMIState.PlainState &&
				state.componentsList.count { it is RHMIComponent.List } >= 5
		}
	}

	val list = state.componentsList.filterIsInstance<RHMIComponent.List>().first()
	val contents = ArrayList<EntityState>()
	val rhmiContents = object: RHMIModel.RaListModel.RHMIListAdapter<EntityState>(2, contents) {
		override fun convertRow(index: Int, item: EntityState): Array<Any> {
			return arrayOf(
				item.attributes["friendly_name"] as? String ?: item.entityId,
				item.state + (item.attributes["unit_of_measurement"] as? String ?: "")
			)
		}
	}

	fun initWidgets() {
		state.getTextModel()?.asRaDataModel()?.value = L.APP_NAME
		state.setProperty(RHMIProperty.PropertyId.HMISTATE_TABLETYPE, 3)
		state.focusCallback = FocusCallback { visible ->
			if (visible) {
				redraw()
			}
		}
		list.setVisible(true)
		list.setProperty(RHMIProperty.PropertyId.LIST_COLUMNWIDTH, "*,150")
	}

	fun redraw() {
		// cache the contents so that the underlying list can change without messing up list indices
		contents.clear()
		contents.addAll(displayedEntities.mapNotNull {
			hassState.value?.states?.get(it)
		})

		list.getModel()?.value = rhmiContents
	}
}