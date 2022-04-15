package io.bimmergestalt.hassgestalt.phoneui.server_config

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import io.bimmergestalt.hassgestalt.data.ServerConfig
import kotlinx.coroutines.flow.map

class ServerConfigViewModel(testServerConfig: ServerConfig? = null): ViewModel() {
	val serverConfig = testServerConfig ?: ServerConfig()

	val serverName: LiveData<String> = serverConfig.flow.map { it.serverName }.asLiveData()
	val emptyServerName: LiveData<Boolean> = serverConfig.flow.map { it.serverName.isBlank() }.asLiveData()
	val isAuthorized: LiveData<Boolean> = serverConfig.flow.map { it.isAuthorized }.asLiveData()
	val canLogout = serverConfig.flow.map { it.canLogout }.asLiveData()
	val isValid: LiveData<Boolean?> = serverConfig.isValidServerName
}