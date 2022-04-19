package io.bimmergestalt.hassgestalt.hass

import io.bimmergestalt.hassgestalt.data.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun Flow<ServerConfig>.hassApi(): Flow<HassApi> = flatMapLatest { serverConfig ->
	callbackFlow<HassApi> {
		val authState = serverConfig.authState
		val hassApiConnection = if (authState != null) {
			HassApiConnection.create(serverConfig.serverName, authState)
		} else {
			null
		}
		val apiFlow = when {
			serverConfig.serverName == HassApiDemo.DEMO_URL -> flowOf(HassApiDemo())
			hassApiConnection != null -> hassApiConnection.connect()
			else -> { flowOf(null) }
		}

		launch {
			apiFlow.collectLatest {
				if (it != null) {
					send(it)
				} else {
					send(HassApiDisconnected())

					// retry every so often while the UI is watching
					// hassApiConnection will send a new connection (or null) through apiFlow after each attempt
					if (hassApiConnection != null) {
						delay(5_000)
						println("Trying to reconnect hassApiConnection")
						withContext(Dispatchers.IO) {
							hassApiConnection.connect()
						}
					}
				}
			}
		}

		awaitClose {
			println("HassApi flow is out of scope, disconnecting")
			hassApiConnection?.disconnect()
		}
	}
}