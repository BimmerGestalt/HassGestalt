package io.bimmergestalt.hassgestalt.phoneui

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import io.bimmergestalt.hassgestalt.R
import io.bimmergestalt.hassgestalt.hass.StateTracker
import io.bimmergestalt.hassgestalt.data.ServerConfig
import io.bimmergestalt.hassgestalt.hass.HassApiConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.InputStream
import java.net.URL
import java.nio.charset.Charset

class MainActivity : FragmentActivity() {
	val serverConfig = ServerConfig()

	private val coroutineScope = CoroutineScope(Job() + Dispatchers.IO)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		/*
		findViewById<Button>(R.id.btn_click3).setOnClickListener {
			tryWebsocket()
		}
		*/
	}

	fun tryApi() {
		val token = serverConfig.authState?.accessToken
		if (token == null) {
			println("No access token loaded")
		}

		val enteredUri = Uri.parse(serverConfig.serverName).buildUpon()
		val uri = enteredUri.encodedPath("/api/states").build()
//		val uri = enteredUri.encodedPath("/api/config").build()
		Thread {
			val connection = URL(uri.toString()).openConnection().apply {
				addRequestProperty("Authorization", "Bearer $token")
			}
			val stream = connection.content
			if (stream is InputStream) {
				println(stream.readBytes().toString(Charset.forName("UTF-8")))
			} else {
				println("Unknown content type $stream ${connection.contentType}")
			}
		}.start()
	}

	fun tryWebsocket() {
		coroutineScope.launch {
			val apiFlow = HassApiConnection.create(serverConfig.serverName, serverConfig.authState!!).connect()
			apiFlow.collectLatest { api ->
				if (api != null) {
					val states = api.request(JSONObject().apply {
						put("type", "get_states")
					})
					val panels = api.request(JSONObject().apply {
						put("type", "get_panels")
					})
					val panelConfig = api.request(JSONObject().apply {
						put("type", "lovelace/config")
						put("url_path", "lovelace-cooper")
					})
					println("States: ${states.await()}")
					println("Panels: ${panels.await()}")
					println("Cooper Panel: ${panelConfig.await()}")
					val state = StateTracker(coroutineScope, api)
				}
			}
		}
	}

	override fun onPause() {
		super.onPause()
		coroutineScope.coroutineContext.cancelChildren()
	}
}