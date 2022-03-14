package io.bimmergestalt.hassgestalt.hass

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import org.java_websocket.client.WebSocketClient
import org.java_websocket.exceptions.WebsocketNotConnectedException
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONException
import org.json.JSONObject
import java.net.URI

val TAG = "HassWs"
class HassWsClient(val wsURI: URI, val callback: Callback): WebSocketClient(wsURI) {
	companion object {
		fun parseUri(httpUri: String): URI {
			val uri = Uri.parse(httpUri)
			val uriBuilder = uri.buildUpon()
			if (uri.scheme == "http" || uri.scheme == "ws") uriBuilder.scheme("ws") else uriBuilder.scheme("wss")
			uriBuilder.encodedPath("/api/websocket")
			return URI.create(uriBuilder.build().toString())
		}

		suspend fun testUri(wsURI: URI, timeout: Long = 5000L): Boolean {
			val status = CompletableDeferred<Boolean>()
			val client = HassWsClient(wsURI, object : Callback {
				override fun onMessage(message: JSONObject) {}
				override fun onConnection(connected: Boolean) {
					status.complete(connected)
				}

				override fun onError(ex: Exception?) {
					status.complete(false)
				}
			})
			return try {
				client.connect()
				withTimeoutOrNull(timeout) {
					status.await()
				} ?: false
			} finally {
				client.close()
			}
		}
	}

	interface Callback {
		fun onMessage(message: JSONObject)
		fun onConnection(connected: Boolean)
		fun onError(ex: Exception?)
	}

	var connected = false
		private set

	override fun onOpen(handshakedata: ServerHandshake?) {
		Log.i(TAG, "Opened")
		connected = true
		callback.onConnection(connected)
	}

	override fun onMessage(message: String?) {
		Log.d(TAG, "Message: $message")
		message ?: return
		val data = try {
			JSONObject(message)
		} catch (ex: JSONException) {
			Log.w(TAG, "Failed to parse", ex)
			return
		}
		callback.onMessage(data)
	}

	override fun onClose(code: Int, reason: String?, remote: Boolean) {
		Log.i(TAG, "Closed: reason=$reason remote=$remote")
		connected = false
		callback.onConnection(connected)
	}

	override fun onError(ex: Exception?) {
		Log.i(TAG, "Error", ex)
		connected = false
		callback.onError(ex)
	}

	fun send(message: JSONObject) {
		try {
			send(message.toString())
		} catch (e: WebsocketNotConnectedException) {}
	}
}