package io.bimmergestalt.hassgestalt.hass

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.openid.appauth.AuthState

class HassApiLiveData(val coroutineScope: CoroutineScope, val serverName: LiveData<String>, val authState: LiveData<AuthState?>): MediatorLiveData<HassApi>() {
	init {
		addSource(serverName) {
			reconnect()
		}
		addSource(authState) {
			reconnect()
		}
	}

	fun reconnect() {
		value?.disconnect()

		val serverName = serverName.value ?: return
		val authState = authState.value ?: return

		coroutineScope.launch {
			Log.i(TAG, "Connecting HassApi to $serverName with authorization ${authState.isAuthorized}")
			value = HassApi.connect(serverName, authState).await() ?: return@launch
		}

	}
}