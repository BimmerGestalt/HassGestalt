package io.bimmergestalt.hassgestalt.carapp

/**
 * Used to signal from an RHMIActionCallback that the rhmi_onActionEvent should return success=false
 *
 * TODO move to IDriveConnectKit
 */
class RHMIActionAbort: Exception()