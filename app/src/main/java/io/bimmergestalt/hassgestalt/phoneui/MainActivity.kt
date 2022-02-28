package io.bimmergestalt.hassgestalt.phoneui

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import com.android.volley.toolbox.Volley
import io.bimmergestalt.hassgestalt.R
import io.bimmergestalt.hassgestalt.data.ServerConfig
import java.io.InputStream
import java.net.URL
import java.nio.charset.Charset

class MainActivity : AppCompatActivity() {
	val requestQueue by lazy { Volley.newRequestQueue(this) }
	val serverConfig = ServerConfig()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		findViewById<Button>(R.id.btn_click3).setOnClickListener {
			tryApi()
		}
	}

	fun tryApi() {
		val token = serverConfig.authState?.accessToken
		if (token == null) {
			println("No access token loaded")
		}

		val inputField = findViewById<EditText>(R.id.txt_instance_url)
		val enteredUri = Uri.parse(inputField.text.toString()).buildUpon()
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
}