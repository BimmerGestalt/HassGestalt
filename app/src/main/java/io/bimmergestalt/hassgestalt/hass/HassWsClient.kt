package io.bimmergestalt.hassgestalt.hass

import android.util.Log
import org.java_websocket.client.WebSocketClient
import org.java_websocket.exceptions.WebsocketNotConnectedException
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONException
import org.json.JSONObject
import java.lang.Exception
import java.net.URI

val TAG = "HassWs"
class HassWsClient(val wsURI: URI, val callback: HassApi.Callback): WebSocketClient(wsURI) {

	var connected = false
		private set

	override fun onOpen(handshakedata: ServerHandshake?) {
		Log.i(TAG, "Opened")
		connected = true
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
	}

	override fun onError(ex: Exception?) {
		Log.i(TAG, "Error", ex)
		connected = false
	}

	fun send(message: JSONObject) {
		try {
			send(message.toString())
		} catch (e: WebsocketNotConnectedException) {}
	}
}