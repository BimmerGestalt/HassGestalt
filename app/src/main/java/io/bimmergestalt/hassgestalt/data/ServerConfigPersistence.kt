package io.bimmergestalt.hassgestalt.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.debounce
import net.openid.appauth.AuthState
import org.json.JSONException

class ServerConfigPersistence(context: Context, val coroutineScope: CoroutineScope) {
	companion object {
		const val SHARED_PREFERENCES_NAME = "HassGestaltServerConfig"
		const val KEY_SERVER_NAME = "ServerName"
		const val KEY_AUTH_STATE = "AuthState"
	}

	private val sharedPreferences = context.getSharedPreferences(
		SHARED_PREFERENCES_NAME,
		Context.MODE_PRIVATE
	)

	private val serverConfig = ServerConfig()

	private var saveJob: Job? = null
	private var watchJob: Job? = null

	fun startSaving() {
		watchJob?.cancel()
		watchJob = coroutineScope.launch {
			serverConfig.flow.debounce(1000).collect {
				save()
			}
		}
	}

	fun load() {
		serverConfig.serverName = sharedPreferences.getString(KEY_SERVER_NAME, null) ?: ""
		serverConfig.authState = sharedPreferences.getString(KEY_AUTH_STATE, null)?.let {
			try {
				AuthState.jsonDeserialize(it)
			} catch (e: JSONException) { null }
		}
	}

	fun save() {
		saveJob?.cancel()
		saveJob = coroutineScope.launch {
			withContext(Dispatchers.IO) {
				sharedPreferences.edit {
					putString(KEY_SERVER_NAME, serverConfig.serverName)
					putString(
						KEY_AUTH_STATE,
						serverConfig.authState?.jsonSerializeString()
					)
				}
			}
		}
	}
}