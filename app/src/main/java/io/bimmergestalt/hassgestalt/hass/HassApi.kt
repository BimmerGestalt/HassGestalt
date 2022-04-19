package io.bimmergestalt.hassgestalt.hass

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import net.openid.appauth.AuthState
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.lang.Exception
import java.net.URI

interface HassApi {
	fun subscribe(subscription: JSONObject): Flow<JSONObject>
	fun request(request: JSONObject): Deferred<JSONObject>
}

class HassApiDisconnected(): HassApi {
	override fun subscribe(subscription: JSONObject): Flow<JSONObject> {
		return flowOf()
	}

	override fun request(request: JSONObject): Deferred<JSONObject> {
		val id = request.optInt("id")
		val response = JSONObject().apply {
			put("id", id)
			put("type", "result")
			put("success", true)
		}
		val type = request.optString("type")
		when(type) {
			"get_states" -> response.apply {
				put("result", JSONArray())
			}
			"get_panels" -> response.apply {
				put("result", JSONArray())
			}
			"lovelace/config" -> response.apply {
				put("result", JSONObject())
			}
			else -> response.apply {
				put("success", false)
				Log.w(TAG, "HassApiDisconnected didn't have an answer for $type request")
				put("error", JSONObject().apply {
					put("code", "unknown_request")
					put("message", "Unknown request")
				})
			}
		}
		return CompletableDeferred(response)
	}
}

class HassApiConnection(val wsURI: URI, private val authState: AuthState): HassApi {
	companion object {
		fun create(httpUri: String, authState: AuthState): HassApiConnection {
			return HassApiConnection(HassWsClient.parseUri(httpUri), authState)
		}
	}
	inner class Callback: HassWsClient.Callback {
		override fun onMessage(message: JSONObject) {
			when(message.getString("type")) {
				"auth_required" -> onAuthRequired()
				"auth_ok" -> connectingFlow.tryEmit(this@HassApiConnection)
				"auth_invalid" -> connectingFlow.tryEmit(null)
				"event" -> onEvent(message)
				"result" -> onResult(message)
			}
		}

		override fun onConnection(connected: Boolean) {
			if (!connected) {
				onDisconnected()
			}
		}

		override fun onError(ex: Exception?) {
			onDisconnected()
		}
	}
	private val client = HassWsClient(wsURI, this.Callback())
	private var clientUsed = false
	private var connectingFlow = MutableSharedFlow<HassApiConnection?>(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
	val isConnected: Boolean
		get() = client.connected

	private var id: Int = 1
	val subscriptions = HashMap<Int, MutableSharedFlow<JSONObject>>()
	val pendingCommands = HashMap<Int, CompletableDeferred<JSONObject>>()

	fun connect(): Flow<HassApiConnection?> {
		if (!isConnected) {
			if (!clientUsed) {
				clientUsed = true
				client.connect()
			} else {
				client.reconnect()
			}
		}
		return this.connectingFlow
	}

	private fun onAuthRequired() {
		if (!isConnected) return
		client.send(JSONObject().apply {
			put("type", "auth")
			put("access_token", authState.accessToken)
		})
	}

	private fun onDisconnected() {
		subscriptions.clear()
		pendingCommands.clear()
		connectingFlow.tryEmit(null)
	}

	fun disconnect() {
		client.close()
	}

	override fun subscribe(subscription: JSONObject): Flow<JSONObject> {
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

	override fun request(request: JSONObject): Deferred<JSONObject> {
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