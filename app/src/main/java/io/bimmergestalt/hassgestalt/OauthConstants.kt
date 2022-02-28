package io.bimmergestalt.hassgestalt

import android.net.Uri

/**
 *  IndieAuth is a clever way to use ownership of a webpage as an Oauth Client
 *  By passing an HTTP(S) link as a Client ID, the Oauth service provider will trust a Redirect Uri
 *  on the same scheme and domain as the Client ID.
 *  The service provider will also scan the HTML at the Client ID for some extra metadata, such as
 *  the name and icon of the relying party application, and any custom Redirect URIs to use
 */
object OauthConstants {
	/* Hass looks at this page for app metadata and the custom redirect_uri */
	const val CLIENT_ID = "https://bimmergestalt.github.io/HassGestalt/"
	/* The Redirect URI to tell Hass to redirect back to
	   This will trigger Android to open up our app with the response given in an Intent
	   The Oauth library automatically adds this scheme to our AndroidManifest
	   Because of its custom scheme, it must be referenced in the above Client ID HTML:
	     <head>
	       <link rel="redirect_uri" href="io.bimmergestalt.hassgestalt://oauth_callback" />
	     </head>
	 */
	const val REDIRECT_URI = "${BuildConfig.APPLICATION_ID}://oauth_callback"

	fun authEndpoint(instanceUri: Uri) = instanceUri.buildUpon().encodedPath("/auth/authorize").build()
	fun tokenEndpoint(instanceUri: Uri) = instanceUri.buildUpon().encodedPath("/auth/token").build()
}