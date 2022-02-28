package io.bimmergestalt.hassgestalt.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import net.openid.appauth.AuthState

class ServerConfig() {
	companion object {
		private var serverName: String = ""
			set(value) {field=value; _serverNameLive.value=value}
		private val _serverNameLive = MutableLiveData<String>(serverName)
		private val serverNameLive: LiveData<String> = _serverNameLive

		private var authState: AuthState? = null
			set(value) {field=value; _authStateLive.value=value}
		private val _authStateLive = MutableLiveData<AuthState?>(authState)
		private val authStateLive: LiveData<AuthState?> = _authStateLive
	}

	var serverName: String
		get() = ServerConfig.serverName
		set(value) { ServerConfig.serverName = value }
	val serverNameLive = ServerConfig.serverNameLive

	var authState: AuthState?
		get() = ServerConfig.authState
		set(value) { ServerConfig.authState = value }
	val authStateLive = ServerConfig.authStateLive

	val isAuthorized
		get() = authState?.isAuthorized == true

}