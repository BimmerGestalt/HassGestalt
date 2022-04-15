package io.bimmergestalt.hassgestalt.hass

import io.bimmergestalt.hassgestalt.data.ServerConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest

fun Flow<ServerConfig>.hassApi(): Flow<HassApi> = flatMapLatest { serverConfig ->
	callbackFlow<HassApi> {
		val authState = serverConfig.authState
		val api = if (serverConfig.serverName == HassApiDemo.DEMO_URL) {
			HassApiDemo()
		} else if (authState != null) {
			HassApiConnection.connect(serverConfig.serverName, authState).await()
		} else { null } ?: HassApiDisconnected()
		send(api)

		awaitClose {
			if (api is HassApiConnection) {
				api.disconnect()
			}
		}
	}
}