package io.bimmergestalt.hassgestalt.hass

import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope

class StateTrackerLiveData(val coroutineScope: CoroutineScope, api: HassApi): LiveData<StateTracker>(StateTracker(api)) {
	override fun onActive() {
		super.onActive()
		value?.subscribeAll(coroutineScope)
	}

	override fun onInactive() {
		super.onInactive()
		value?.unsubscribeAll()
	}
}