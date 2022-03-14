package io.bimmergestalt.hassgestalt.hass

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.runBlocking
import net.openid.appauth.AuthState
import org.json.JSONException
import org.json.JSONObject
import java.lang.Exception
import java.net.URI

class HassApi(val wsURI: URI, val authState: AuthState) {
	companion object {
		fun connect(httpUri: String, authState: AuthState): Deferred<HassApi?> {
			val api = HassApi(HassWsClient.parseUri(httpUri), authState)
			return api.connectAsync()
		}
	}
	inner class Callback: HassWsClient.Callback {
		override fun onMessage(message: JSONObject) {
			when(message.getString("type")) {
				"auth_required" -> onAuthRequired()
				"auth_ok" -> connecting.complete(this@HassApi)
				"auth_invalid" -> connecting.complete(null)
				"event" -> onEvent(message)
				"result" -> onResult(message)
			}
		}

		override fun onConnection(connected: Boolean) {
		}

		override fun onError(ex: Exception?) {
		}
	}
	val client = HassWsClient(wsURI, this.Callback())
	private var connecting: CompletableDeferred<HassApi?> = CompletableDeferred()
	val isConnected: Boolean
		get() = client.connected

	private var id: Int = 1
	val subscriptions = HashMap<Int, MutableSharedFlow<JSONObject>>()
	val pendingCommands = HashMap<Int, CompletableDeferred<JSONObject>>()

	fun connectAsync(): Deferred<HassApi?> {
		if (!isConnected) {
			this.connecting = CompletableDeferred()
			client.connect()
		}
		return this.connecting
	}

	private fun onAuthRequired() {
		if (!isConnected) return
		client.send(JSONObject().apply {
			put("type", "auth")
			put("access_token", authState.accessToken)
		})
	}

	fun disconnect() {
		client.close()
	}

	fun subscribe(subscription: JSONObject): Flow<JSONObject> {
		if (!isConnected) return MutableSharedFlow()
		return synchronized(this) {
			val subscriptionId = id
			val result = MutableSharedFlow<JSONObject>(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
			subscriptions[subscriptionId] = result
			subscription.put("id", subscriptionId)
			id++
			client.send(subscription)
			result.onCompletion {
				println("Unsubscribing $subscriptionId")
				unsubscribe(subscriptionId)
			}
		}
	}

	fun unsubscribe(subscriptionId: Int) {
		if (!isConnected) return
		synchronized(this) {
			val requestId = id
			id++

			val unsubscription = JSONObject().apply {
				put("id", requestId)
				put("type", "unsubscribe_events")
				put("subscription", subscriptionId)
			}
			client.send(unsubscription)
		}
	}

	fun onEvent(event: JSONObject) {
		val id = try {
			event.getInt("id")
		} catch (ex: JSONException) {
			Log.w(TAG, "Failed to parse $event", ex)
			return
		}
		val subscription = synchronized(this) {
			subscriptions[id]
		}
		runBlocking {
			try {
				subscription?.emit(event)
			} catch (e: CancellationException) {
				Log.d(TAG, "Unsubscribing from subscription $id")
				unsubscribe(id)
			}
		}
	}

	fun request(request: JSONObject): Deferred<JSONObject> {
		if (!isConnected) return CompletableDeferred()
		val deferred = synchronized(this) {
			val deferred = CompletableDeferred<JSONObject>()
			pendingCommands[id] = deferred
			request.put("id", id)
			id++
			deferred
		}
		client.send(request)
		return deferred
	}

	private fun onResult(message: JSONObject) {
		// each message should have an `id` that matches up with a previous command or request
		// each message should have a boolean `success` and an optional `error` string
		// each message should have a `result` with extra information
		val id = try {
			message.getInt("id")
		} catch (ex: JSONException) {
			Log.w(TAG, "Failed to parse $message", ex)
			return
		}
		val deferred = synchronized(this) {
			pendingCommands.remove(id)
		}
		deferred?.complete(message)
	}
}