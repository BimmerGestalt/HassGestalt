package io.bimmergestalt.hassgestalt.carapp

import android.util.Log
import de.bmw.idrive.BMWRemoting
import de.bmw.idrive.BMWRemotingServer
import de.bmw.idrive.BaseBMWRemotingClient
import io.bimmergestalt.hassgestalt.carapp.views.DashboardView
import io.bimmergestalt.hassgestalt.carapp.views.HomeView
import io.bimmergestalt.hassgestalt.hass.LovelaceConfig
import io.bimmergestalt.hassgestalt.hass.StateTracker
import io.bimmergestalt.idriveconnectkit.IDriveConnection
import io.bimmergestalt.idriveconnectkit.Utils.rhmi_setResourceCached
import io.bimmergestalt.idriveconnectkit.android.CarAppResources
import io.bimmergestalt.idriveconnectkit.android.IDriveConnectionStatus
import io.bimmergestalt.idriveconnectkit.android.security.SecurityAccess
import io.bimmergestalt.idriveconnectkit.rhmi.*
import kotlinx.coroutines.flow.Flow

const val TAG = "HassGestalt"

class CarApp(val iDriveConnectionStatus: IDriveConnectionStatus, securityAccess: SecurityAccess,
             val carAppResources: CarAppResources, val androidResources: AndroidResources,
             val state: Flow<StateTracker>, val lovelaceConfig: Flow<LovelaceConfig>
) {

    val displayedEntities = listOf("sensor.chillcat_inverter_energy", "sensor.zwave_11_w")
    val carConnection: BMWRemotingServer
    val carApp: RHMIApplication
    val homeView: HomeView
    val dashboardView: DashboardView

    init {
        Log.i(TAG, "Starting connecting to car")
        val carappListener = CarAppListener()
        carConnection = IDriveConnection.getEtchConnection(iDriveConnectionStatus.host ?: "127.0.0.1", iDriveConnectionStatus.port ?: 8003, carappListener)
        val appCert = carAppResources.getAppCertificate(iDriveConnectionStatus.brand ?: "")?.readBytes()
        val sas_challenge = carConnection.sas_certificate(appCert)
        val sas_response = securityAccess.signChallenge(challenge = sas_challenge)
        carConnection.sas_login(sas_response)

        carApp = createRhmiApp()
        homeView = HomeView(carApp.states.values.first {HomeView.fits(it)}, state, lovelaceConfig, displayedEntities)
        dashboardView = DashboardView(carApp.states.values.first {it != homeView.state && DashboardView.fits(it)}, state, lovelaceConfig)


        initWidgets()
        Log.i(TAG, "CarApp running")
    }

    private fun createRhmiApp(): RHMIApplication {
        // create the app in the car
        val rhmiHandle = carConnection.rhmi_create(null, BMWRemoting.RHMIMetaData("io.bimmergestalt.hassgestalt", BMWRemoting.VersionInfo(0, 1, 0), "io.bimmergestalt.hassgestalt", "io.bimmergestalt"))
        carConnection.rhmi_setResourceCached(rhmiHandle, BMWRemoting.RHMIResourceType.DESCRIPTION, carAppResources.getUiDescription())
        carConnection.rhmi_setResourceCached(rhmiHandle, BMWRemoting.RHMIResourceType.TEXTDB, carAppResources.getTextsDB(iDriveConnectionStatus.brand ?: "common"))
        carConnection.rhmi_setResourceCached(rhmiHandle, BMWRemoting.RHMIResourceType.IMAGEDB, carAppResources.getImagesDB(iDriveConnectionStatus.brand ?: "common"))
        carConnection.rhmi_initialize(rhmiHandle)

        // register for events from the car
        carConnection.rhmi_addActionEventHandler(rhmiHandle, "io.bimmergestalt.hassgestalt", -1)
        carConnection.rhmi_addHmiEventHandler(rhmiHandle, "io.bimmergestalt.hassgestalt", -1, -1)

        return RHMIApplicationSynchronized(
            RHMIApplicationIdempotent(
            RHMIApplicationEtch(carConnection, rhmiHandle)
        ), carConnection).apply {
            loadFromXML(carAppResources.getUiDescription()!!.readBytes())
        }
    }

    private fun initWidgets() {
        carApp.components.values.filterIsInstance<RHMIComponent.EntryButton>().forEach {
            it.getAction()?.asHMIAction()?.getTargetModel()?.asRaIntModel()?.value = homeView.state.id
        }
        homeView.initWidgets(dashboardView)
        dashboardView.initWidgets()
    }

    fun onDestroy() {
        try {
            Log.i(TAG, "Trying to shut down etch connection")
            IDriveConnection.disconnectEtchConnection(carConnection)
        } catch ( e: java.io.IOError) {
        } catch (e: RuntimeException) {}
    }

    inner class CarAppListener(): BaseBMWRemotingClient() {
        override fun rhmi_onActionEvent(handle: Int?, ident: String?, actionId: Int?, args: MutableMap<*, *>?) {
            try {
                carApp.actions[actionId]?.asRAAction()?.rhmiActionCallback?.onActionEvent(args)
                synchronized(carConnection) {
                    carConnection.rhmi_ackActionEvent(handle, actionId, 1, true)
                }
            } catch (e: RHMIActionAbort) {
                // Action handler requested that we don't claim success
                synchronized(carConnection) {
                    carConnection.rhmi_ackActionEvent(handle, actionId, 1, false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while calling onActionEvent handler!", e)
                synchronized(carConnection) {
                    carConnection.rhmi_ackActionEvent(handle, actionId, 1, true)
                }
            }
        }

        override fun rhmi_onHmiEvent(handle: Int?, ident: String?, componentId: Int?, eventId: Int?, args: MutableMap<*, *>?) {
            try {
                // generic event handler
                carApp.states[componentId]?.onHmiEvent(eventId, args)
                carApp.components[componentId]?.onHmiEvent(eventId, args)
            } catch (e: Exception) {
                Log.e(TAG, "Received exception while handling rhmi_onHmiEvent", e)
            }
        }
    }
}