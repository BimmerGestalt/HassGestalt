package io.bimmergestalt.hassgestalt.phoneui

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import io.bimmergestalt.hassgestalt.R
import io.bimmergestalt.hassgestalt.hass.StateTracker
import io.bimmergestalt.hassgestalt.hass.HassApi
import io.bimmergestalt.hassgestalt.data.ServerConfig
import io.bimmergestalt.hassgestalt.hass.HassApiConnection
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.InputStream
import java.net.URL
import java.nio.charset.Charset

class MainActivity : AppCompatActivity() {
	val serverConfig = ServerConfig()

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
		lifecycleScope.launch {
			val api = HassApiConnection.connect(serverConfig.serverName, serverConfig.authState!!).await()
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
				val state = StateTracker(lifecycleScope, api)
			}
		}
	}
}