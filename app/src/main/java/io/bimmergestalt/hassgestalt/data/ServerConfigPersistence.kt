package io.bimmergestalt.hassgestalt.data

import android.content.Context
import androidx.core.content.edit
import io.bimmergestalt.hassgestalt.data.JsonHelpers.toList
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.debounce
import net.openid.appauth.AuthState
import org.json.JSONArray
import org.json.JSONException

class ServerConfigPersistence(context: Context, val coroutineScope: CoroutineScope) {
	companion object {
		const val SHARED_PREFERENCES_NAME = "HassGestaltServerConfig"
		const val KEY_SERVER_NAME = "ServerName"
		const val KEY_AUTH_STATE = "AuthState"
		const val KEY_STARRED_DASHBOARDS = "StarredDashboards"
	}

	private val sharedPreferences = context.getSharedPreferences(
		SHARED_PREFERENCES_NAME,
		Context.MODE_PRIVATE
	)

	private val serverConfig = ServerConfig()

	private var saveJob: Job? = null
	private var watchJob: Job? = null
	private var watchStarsJob: Job? = null

	fun startSaving() {
		watchJob?.cancel()
		watchJob = coroutineScope.launch {
			serverConfig.flow.debounce(1000).collect {
				save()
			}
		}
		watchStarsJob?.cancel()
		watchStarsJob = coroutineScope.launch {
			serverConfig.starredDashboards.debounce(1000).collect {
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
		serverConfig.starredDashboards.value = sharedPreferences.getString(KEY_STARRED_DASHBOARDS, null)?.let {
			try {
				JSONArray(it).toList().filterIsInstance<String>()
			} catch (e: JSONException) { null }
		} ?: emptyList()
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
					putString(
						KEY_STARRED_DASHBOARDS,
						JSONArray(serverConfig.starredDashboards.value).toString()
					)
				}
			}
		}
	}
}