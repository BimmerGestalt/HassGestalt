package io.bimmergestalt.hassgestalt.phoneui.hass

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.bimmergestalt.hassgestalt.data.ServerConfig
import io.bimmergestalt.hassgestalt.hass.hassApi
import io.bimmergestalt.hassgestalt.hass.stateTracker
import kotlinx.coroutines.flow.*

class HassStateViewModel(testServerConfig: ServerConfig? = null): ViewModel() {
	val serverConfig = testServerConfig ?: ServerConfig()

	val api = serverConfig.flow.hassApi().shareIn(viewModelScope, SharingStarted.WhileSubscribed(), replay = 1)
	val state = api.stateTracker().shareIn(viewModelScope, SharingStarted.WhileSubscribed(), replay = 1)
}