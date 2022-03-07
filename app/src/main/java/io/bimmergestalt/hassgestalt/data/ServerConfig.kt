package io.bimmergestalt.hassgestalt.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.openid.appauth.AuthState

class ServerConfig() {
	companion object {
		private var serverName: String = ""
			set(value) {field=value; _serverNameLive.value=value; _flow.value = ServerConfig()}
		private val _serverNameLive = MutableLiveData<String>(serverName)
		private val serverNameLive: LiveData<String> = _serverNameLive

		private var authState: AuthState? = null
			set(value) {field=value; _authStateLive.value=value}
		private val _authStateLive = MutableLiveData<AuthState?>(authState)
		private val authStateLive: LiveData<AuthState?> = _authStateLive

		private val _flow = MutableStateFlow(ServerConfig())
		private val flow: StateFlow<ServerConfig> = _flow
	}

	var serverName: String
		get() = ServerConfig.serverName
		set(value) { ServerConfig.serverName = value }
	val serverNameLive = ServerConfig.serverNameLive

	var authState: AuthState?
		get() = ServerConfig.authState
		set(value) { ServerConfig.authState = value }
	val authStateLive = ServerConfig.authStateLive

	val flow = ServerConfig.flow

	val isAuthorized
		get() = authState?.isAuthorized == true

}