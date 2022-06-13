package io.bimmergestalt.hassgestalt

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import io.bimmergestalt.hassgestalt.phoneui.MainActivity
import net.openid.appauth.*
import java.net.HttpURLConnection
import java.net.URL

class OauthAccess(private val context: Context, private val previousAuthState: AuthState? = null, val callback: (AuthState) -> Unit) {
	val TAG = "OauthAccess"
	private val authConfig = AppAuthConfiguration.Builder()
		.setConnectionBuilder { uri ->
			Log.d(TAG, "Requested to open uri $uri")
			(URL(uri.toString()).openConnection() as HttpURLConnection).apply {
				connectTimeout = 15000
				readTimeout = 10000
				instanceFollowRedirects = false
			}
		}
		.build()
	private val authService = AuthorizationService(context, authConfig)
	private var authState = previousAuthState ?: AuthState()

	fun startAuthRequest(uri: Uri) {
		val serviceConfig = AuthorizationServiceConfiguration(
			OauthConstants.authEndpoint(uri),
			OauthConstants.tokenEndpoint(uri)
		)
		val builder = AuthorizationRequest.Builder(
			serviceConfig,
			OauthConstants.CLIENT_ID,
			ResponseTypeValues.CODE,
			Uri.parse(OauthConstants.REDIRECT_URI)
		)
		builder.setState(uri.toString())
		val authRequest = builder.build()

		val pendingIntentFlags = if (Build.VERSION.SDK_INT >= 31) {
			PendingIntent.FLAG_MUTABLE
		} else {
			0
		}
		authService.performAuthorizationRequest(authRequest,
			PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), pendingIntentFlags)
		)
	}

	fun handleAuthorizationResponse(intent: Intent) {
		val response = AuthorizationResponse.fromIntent(intent)
		val ex = AuthorizationException.fromIntent(intent)
		if (response != null || ex != null) {
			Log.d(TAG, "Received auth response $response $ex")
			authState.update(response, ex)
			callback(authState)

			tryAuthToken()
		}
	}

	fun tryAuthToken() {
		authState.lastAuthorizationResponse?.let {
			authService.performTokenRequest(it.createTokenExchangeRequest()) { response, ex ->
				Log.d(TAG, "Received token response $response $ex")
				authState.update(response, ex)
				callback(authState)
			}
		}
	}

	fun tryRefreshToken() {
		if (authState.refreshToken != null && authState.needsTokenRefresh) {
			authService.performTokenRequest(authState.createTokenRefreshRequest()) { response, ex ->
				Log.d(TAG, "Received token response $response $ex")
				authState.update(response, ex)
				callback(authState)
			}
		} else {
//			Log.d(TAG, "Not refreshing token: refreshToken isNull:${authState.refreshToken == null} needsRefresh:${authState.needsTokenRefresh} accessToken isNull:${authState.accessToken == null}")
		}
	}

	fun logout() {
		authState = AuthState()
	}
	fun dispose() {
		authService.dispose()
	}
}