package io.bimmergestalt.hassgestalt.carapp

import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.switchMap
import io.bimmergestalt.hassgestalt.L
import io.bimmergestalt.hassgestalt.OauthAccess
import io.bimmergestalt.hassgestalt.data.ServerConfig
import io.bimmergestalt.hassgestalt.data.ServerConfigPersistence
import io.bimmergestalt.hassgestalt.hass.HassApiLiveData
import io.bimmergestalt.hassgestalt.hass.StateTrackerLiveData
import io.bimmergestalt.idriveconnectkit.android.CarAppAssetResources
import io.bimmergestalt.idriveconnectkit.android.IDriveConnectionReceiver
import io.bimmergestalt.idriveconnectkit.android.IDriveConnectionStatus
import io.bimmergestalt.idriveconnectkit.android.security.SecurityAccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.android.asCoroutineDispatcher

class CarAppService: LifecycleService() {

	val handler = Handler(Looper.getMainLooper())
	var thread: CarThread? = null
	var app: CarApp? = null

	val serverConfig = ServerConfig()
	val oauthAccess by lazy { OauthAccess(this, serverConfig.authState) {
		// trigger the LiveData to update with the authState, even if it's the same
		serverConfig.authState = it
	} }
	val serverConfigPersistence by lazy { ServerConfigPersistence(this, this) }

	override fun onCreate() {
		super.onCreate()
		SecurityAccess.getInstance(applicationContext).connect()

		serverConfigPersistence.load()
		serverConfig.authStateLive.observe(this) {
			oauthAccess.tryRefreshToken()
		}
	}

	/**
	 * When a car is connected, it will bind the Addon Service
	 */
	override fun onBind(intent: Intent): IBinder? {
		super.onBind(intent)
		IDriveConnectionReceiver().onReceive(applicationContext, intent)
		startThread()
		return null
	}

	/**
	 * If the thread crashes for any reason,
	 * opening the main app will trigger a Start on the Addon Services
	 * as a chance to reconnect
	 */
	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		super.onStartCommand(intent, flags, startId)
		intent ?: return START_NOT_STICKY
		IDriveConnectionReceiver().onReceive(applicationContext, intent)
		startThread()
		return START_STICKY
	}

	/**
	 * The car has disconnected, so forget the previous details
	 */
	override fun onUnbind(intent: Intent?): Boolean {
		IDriveConnectionStatus.reset()
		return super.onUnbind(intent)
	}

	/**
	 * Starts the thread for the car app, if it isn't running
	 */
	fun startThread() {
		val iDriveConnectionStatus = IDriveConnectionReceiver()
		val securityAccess = SecurityAccess.getInstance(applicationContext)
		if (iDriveConnectionStatus.isConnected &&
			securityAccess.isConnected() &&
			thread?.isAlive != true) {

			val dispatcher = handler.asCoroutineDispatcher("HassGestalt")
			val scope = CoroutineScope(dispatcher)
			val api = HassApiLiveData(scope, serverConfig.serverNameLive, serverConfig.authStateLive)
			val state = api.switchMap {
				StateTrackerLiveData(scope, it)
			}
			state.observe(this) { }

			L.loadResources(applicationContext)
			thread = CarThread("HassGestalt") {
				Log.i(TAG, "CarThread is ready, starting CarApp")

				app = CarApp(
					iDriveConnectionStatus,
					securityAccess,
					CarAppAssetResources(applicationContext, "smartthings"),
					AndroidResources(applicationContext),
					state,
				)
			}
			thread?.start()
		} else if (thread?.isAlive != true) {
			if (thread?.isAlive != true) {
				Log.i(TAG, "Not connecting to car, because: iDriveConnectionStatus.isConnected=${iDriveConnectionStatus.isConnected} securityAccess.isConnected=${securityAccess.isConnected()}")
			} else {
				Log.d(TAG, "CarThread is still running, not trying to start it again")
			}
		}
	}

	override fun onDestroy() {
		super.onDestroy()

		app?.onDestroy()
	}
}