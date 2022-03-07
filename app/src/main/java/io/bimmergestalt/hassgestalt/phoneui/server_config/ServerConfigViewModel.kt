package io.bimmergestalt.hassgestalt.phoneui.server_config

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import io.bimmergestalt.hassgestalt.data.ServerConfig
import kotlinx.coroutines.flow.map

class ServerConfigViewModel(testServerConfig: ServerConfig? = null): ViewModel() {
	val serverConfig = testServerConfig ?: ServerConfig()

	val serverName: LiveData<String> = serverConfig.flow.map { it.serverName }.asLiveData()
	val authenticated: LiveData<Boolean> = serverConfig.flow.map { it.isAuthorized }.asLiveData()
}