package io.bimmergestalt.hassgestalt.phoneui.server_config

import android.net.Uri
import androidx.lifecycle.LifecycleCoroutineScope
import io.bimmergestalt.hassgestalt.OauthAccess
import io.bimmergestalt.hassgestalt.data.ServerConfig
import io.bimmergestalt.hassgestalt.hass.HassWsClient
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
			serverConfig.isValidServerName.value = null

			// debounce, will get cancelled for new input
			delay(1500)

			val changed = serverConfig.serverName != pendingServerName
			if (changed) {
				println("Pending $pendingServerName is different than ${serverConfig.serverName}")
				serverConfig.authState = null
			}
			var pendingServerName = pendingServerName ?: ""
			if (pendingServerName.isNotBlank() && !pendingServerName.contains("://")) {
				pendingServerName = "https://$pendingServerName"
			}
			serverConfig.serverName = pendingServerName

			if (pendingServerName.isNotBlank()) {
				serverConfig.isValidServerName.value = HassWsClient.testUri(HassWsClient.parseUri(pendingServerName))
			}
		}
	}

	fun startLogin() {
		oauthAccess.startAuthRequest(Uri.parse(pendingServerName ?: ""))
	}
	fun logout() {
		serverConfig.authState = null
	}
}