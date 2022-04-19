package io.bimmergestalt.hassgestalt.hass

import io.bimmergestalt.hassgestalt.hass.StateTracker.Companion.DEFAULT_EXPIRATION
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

fun Flow<HassApi>.stateTracker(timeout: Long = DEFAULT_EXPIRATION): Flow<StateTracker> = flatMapLatest { hassApi ->
	callbackFlow {
		val stateTracker = StateTracker(this, hassApi, timeout)
		send(stateTracker)
		awaitClose {
			println("HassApi connection is out of scope, unsubscribing")
			stateTracker.unsubscribeAll()
		}
}}

class StateFlowManager(private val scope: CoroutineScope, val stateTracker: StateTracker) {
	private val receivedData = HashMap<String, Channel<EntityState>>()
	private val flows = HashMap<String, Flow<EntityState>>()

	private val dataNeeded = MutableSharedFlow<String>(extraBufferCapacity = 1)
	init {
		dataNeeded
			.onEach { println("Needing initial data for $it") }
			.debounce(100)
			.onEach { println("Firing fetchStates") }
			.onEach { stateTracker.fetchStates() }
			.launchIn(scope)
	}

	operator fun get(entityId: String): Flow<EntityState> {
		return flows.getOrPut(entityId) {
			println("Creating new flow for $entityId")
			dataNeeded.tryEmit(entityId)
			receivedData.remove(entityId)
			merge(
				receivedData.getOrPut(entityId, { Channel(1, BufferOverflow.DROP_OLDEST) }).receiveAsFlow(),
				stateTracker.subscribeSingle(entityId)
					.onCompletion {
						println("Shutting down flow for $entityId")
						flows.remove(entityId)
						receivedData[entityId]?.close()
					}
			).shareIn(scope, SharingStarted.WhileSubscribed(30000, replayExpirationMillis = 30000), replay=1)
		}
	}
	suspend fun onState(state: EntityState) {
		try {
			receivedData[state.entityId]?.send(state)
		} catch (e: ClosedSendChannelException) {
			throw CancellationException()
		}
	}
}