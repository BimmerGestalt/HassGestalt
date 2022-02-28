package io.bimmergestalt.hassgestalt.data

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import net.openid.appauth.AuthState

class ServerConfigPersistence(context: Context, val lifecycleOwner: LifecycleOwner) {
	companion object {
		const val SHARED_PREFERENCES_NAME = "HassGestaltServerConfig"
		const val KEY_SERVER_NAME = "ServerName"
		const val KEY_AUTH_STATE = "AuthState"
	}

	private val sharedPreferences = context.getSharedPreferences(
		SHARED_PREFERENCES_NAME,
		Context.MODE_PRIVATE
	)

	private val serverConfig = ServerConfig().also {
		it.serverNameLive.observe(lifecycleOwner) {
			save()
		}
		it.authStateLive.observe(lifecycleOwner) {
			save()
		}
	}

	private var saveJob: Job? = null

	fun load() {
		serverConfig.serverName = sharedPreferences.getString(KEY_SERVER_NAME, null) ?: ""
		serverConfig.authState = sharedPreferences.getString(KEY_AUTH_STATE, null)?.let {
			AuthState.jsonDeserialize(it)
		}
	}

	fun save() {
		saveJob?.cancel()
		saveJob = lifecycleOwner.lifecycleScope.launch {
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