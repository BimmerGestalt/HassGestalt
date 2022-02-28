package io.bimmergestalt.hassgestalt.phoneui.server_config

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import io.bimmergestalt.hassgestalt.data.ServerConfig

class ServerConfigViewModel(testServerConfig: ServerConfig? = null): ViewModel() {
	val serverConfig = testServerConfig ?: ServerConfig()

	val serverName: LiveData<String> = serverConfig.serverNameLive
	val authenticated: LiveData<Boolean> = serverConfig.authStateLive.map { it?.isAuthorized == true }
}