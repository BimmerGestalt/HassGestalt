package io.bimmergestalt.hassgestalt.phoneui.server_config

import android.net.Uri
import androidx.lifecycle.LifecycleCoroutineScope
import io.bimmergestalt.hassgestalt.OauthAccess
import io.bimmergestalt.hassgestalt.data.ServerConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ServerConfigController(val lifecycleScope: LifecycleCoroutineScope, val serverConfig: ServerConfig, val oauthAccess: OauthAccess) {
	private var pendingServerName: String? = serverConfig.serverName
	private var setServerNameJob: Job? = null
	fun setServerName(serverName: CharSequence) {
		pendingServerName = serverName.toString()
		setServerNameJob?.cancel()
		setServerNameJob = lifecycleScope.launch {
			// debounce, will get cancelled for new input
			delay(1500)

			val changed = serverConfig.serverName != pendingServerName
			if (changed) {
				serverConfig.authState = null
			}
			serverConfig.serverName = pendingServerName ?: ""
		}
	}

	fun startLogin() {
		oauthAccess.startAuthRequest(Uri.parse(pendingServerName ?: ""))
	}
	fun logout() {
		serverConfig.authState = null
	}
}