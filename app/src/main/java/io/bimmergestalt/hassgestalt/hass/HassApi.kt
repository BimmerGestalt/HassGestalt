package io.bimmergestalt.hassgestalt.hass

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.runBlocking
import net.openid.appauth.AuthState
import org.json.JSONException
import org.json.JSONObject
import java.net.URI

class HassApi(val wsURI: URI, val authState: AuthState) {
	companion object {
		fun connect(httpUri: String, authState: AuthState): Deferred<HassApi?> {
			val uri = Uri.parse(httpUri)
			val uriBuilder = uri.buildUpon()
			if (uri.scheme == "http") uriBuilder.scheme("ws") else uriBuilder.scheme("wss")
			uriBuilder.encodedPath("/api/websocket")

			val api = HassApi(URI.create(uriBuilder.build().toString()), authState)
			return api.connectAsync()
		}
	}
	inner class Callback {
		fun onMessage(message: JSONObject) {
			when(message.getString("type")) {
				"auth_required" -> onAuthRequired()
				"auth_ok" -> connecting.complete(this@HassApi)
				"auth_invalid" -> connecting.complete(null)
				"event" -> onEvent(message)
				"result" -> onResult(message)
			}
		}
	}
	val client = HassWsClient(wsURI, this.Callback())
	private var connecting: CompletableDeferred<HassApi?> = CompletableDeferred()
	val isConnected: Boolean
		get() = client.connected

	private var id: Int = 1
	val subscriptions = HashMap<Int, SendChannel<JSONObject>>()
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

	fun subscribe(subscription: JSONObject): ReceiveChannel<JSONObject> {
		if (!isConnected) return Channel()
		val channel = synchronized(this) {
			val channel = Channel<JSONObject>(64)
			subscriptions[id] = channel
			subscription.put("id", id)
			id++
			channel
		}
		client.send(subscription)
		return channel
	}

	fun unsubscribe(subscriptionId: Int, unsubscription: JSONObject) {
		if (!isConnected) return
		synchronized(this) {
			subscriptions.remove(subscriptionId)?.close()
			unsubscription.put("id", id)
			id++
		}
		unsubscription.put("subscription", subscriptionId)
		client.send(unsubscription)
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
				subscription?.send(event)
			} catch (e: CancellationException) {
				unsubscribe(id, JSONObject().apply {
					put("type", "unsubscribe_events")
				})
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