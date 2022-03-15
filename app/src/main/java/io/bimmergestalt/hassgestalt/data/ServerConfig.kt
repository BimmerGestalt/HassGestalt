package io.bimmergestalt.hassgestalt.data

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.openid.appauth.AuthState

class ServerConfig() {
	companion object {
		private var serverName: String = ""
			set(value) { if (field != value) {
				field=value; _flow.value = ServerConfig()
			}}

		private var authState: AuthState? = null
			set(value) {field=value; _flow.value = ServerConfig()}

		private val _flow = MutableStateFlow(ServerConfig())
		private val flow: StateFlow<ServerConfig> = _flow
	}

	var serverName: String
		get() = ServerConfig.serverName
		set(value) { ServerConfig.serverName = value }

	var authState: AuthState?
		get() = ServerConfig.authState
		set(value) { ServerConfig.authState = value }

	val flow = ServerConfig.flow

	var isValidServerName = MutableLiveData<Boolean?>(null)
	val isAuthorized
		get() = authState?.isAuthorized == true

}