package io.bimmergestalt.hassgestalt.hass

import io.bimmergestalt.hassgestalt.data.ServerConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest

fun Flow<ServerConfig>.hassApi(): Flow<HassApi> = flatMapLatest { serverConfig ->
	callbackFlow<HassApi> {
		val authState = serverConfig.authState
		val api = if (authState != null) {
			HassApi.connect(serverConfig.serverName, authState).await()?.also {
				send(it)
			}
		} else { null }

		awaitClose {
			api?.disconnect()
		}
	}
}