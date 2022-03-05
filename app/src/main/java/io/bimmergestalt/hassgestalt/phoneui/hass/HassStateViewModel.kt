package io.bimmergestalt.hassgestalt.phoneui.hass

import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import io.bimmergestalt.hassgestalt.data.ServerConfig
import io.bimmergestalt.hassgestalt.hass.HassApiLiveData
import io.bimmergestalt.hassgestalt.hass.StateTrackerLiveData

class HassStateViewModel(testServerConfig: ServerConfig? = null): ViewModel() {
	val serverConfig = testServerConfig ?: ServerConfig()

	val api = HassApiLiveData(viewModelScope, serverConfig.serverNameLive, serverConfig.authStateLive)
	val state = api.switchMap { api ->
		StateTrackerLiveData(viewModelScope, api)
	}

	override fun onCleared() {
		super.onCleared()
		state.value?.unsubscribeAll()
		api.value?.disconnect()
	}
}