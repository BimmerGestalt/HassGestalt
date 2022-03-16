package io.bimmergestalt.hassgestalt.hass

import androidx.annotation.VisibleForTesting
import io.bimmergestalt.hassgestalt.hass.EntityStateParser.mapEntityStateStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject

class StateTracker(private val scope: CoroutineScope, private val api: HassApi, private val timeout: Long = DEFAULT_EXPIRATION) {
	companion object {
		const val DEFAULT_EXPIRATION = 5000L
		fun <K> MutableMap<K, MutableStateFlow<EntityState>>.getFlow(key: K): MutableStateFlow<EntityState> {
			return synchronized(this) {
				this.getOrPut(key, { MutableStateFlow(EntityState.EMPTY) })
			}
		}
	}

	val lastState = HashMap<String, EntityState>()
	val allEntityStates = HashMap<String, MutableStateFlow<EntityState>>()
	val singleEntityStates = HashMap<String, MutableStateFlow<EntityState>>()

	private val dataNeeded = MutableSharedFlow<String>(extraBufferCapacity = 1)
	private val dataNeededJob: Job
	private val isAllSubscribed = MutableStateFlow(false)
	private var allEventProcessor: Job? = null
	private val singleEventProcessor = HashMap<String, Job>()
	private val singleEventSubscriber = HashMap<String, Job>()
	private val singleEventUnsubscriber = HashMap<String, Job>()

	init {
		dataNeededJob = dataNeeded
			.onEach { println("Needing initial data for $it") }
			.debounce(50)
			.onEach { println("Firing fetchStates") }
			.onEach { fetchStates() }
			.launchIn(scope)
	}

	fun subscribeAll() {
		if (allEventProcessor != null) return

		val eventStream = api.subscribe(JSONObject().apply {
			put("type", "subscribe_events")
			put("event_type", "state_changed")
		})
		isAllSubscribed.tryEmit(true)
		scope.launch {
			dataNeeded.emit("*")
		}
		allEventProcessor = eventStream.mapEntityStateStream()
			.onEach {
				processState(it)
				allEntityStates.getFlow(it.entityId).emit(it)
			}
			.onCompletion { allEntityStates.forEach { it.value.value = EntityState.EMPTY } }
			.launchIn(scope)
	}

	fun subscribeSingle(entityId: String): Flow<EntityState> {
		if (singleEventProcessor[entityId]?.isActive != true) {
			singleEventProcessor[entityId] = scope.launch { singleEntityStates.getFlow(entityId)
				.subscriptionCount
				.map { it != 0 }     // isActive
				.distinctUntilChanged()
				.collect { isActive ->
					singleEventUnsubscriber[entityId]?.cancel()
					if (isActive) {
						println("Starting single subscription for $entityId for new subscriber")
						dataNeeded.emit(entityId)

						singleEventSubscriber[entityId] = launch {
							api.subscribe(JSONObject().apply {
								put("type", "subscribe_trigger")
								put("trigger", JSONObject().apply {
									put("platform", "state")
									put("entity_id", entityId)
								})
							}).mapEntityStateStream()
							.collect {
								processState(it)
								singleEntityStates.getFlow(it.entityId).emit(it)
							}
						}
					} else {
						println("Single subscription for $entityId has no subscribers")
						singleEventUnsubscriber[entityId] = launch {
							// schedule to unsubscribe after a timeout
							delay(timeout)
							singleEntityStates.getFlow(entityId).value = EntityState.EMPTY
							singleEventSubscriber[entityId]?.cancel()
						}
					}
				}
			}
		}

		// make sure both flows exist
		allEntityStates.getFlow(entityId)
		singleEntityStates.getFlow(entityId)
		return isAllSubscribed.flatMapLatest { isAllSubscribed ->
			// switch between them based on the mode
			println("Running flow for $entityId in subscribedAllMode $isAllSubscribed")
			if (isAllSubscribed) allEntityStates.getFlow(entityId)
			else singleEntityStates.getFlow(entityId)
		}
	}

	operator fun get(entityId: String) = subscribeSingle(entityId)

	fun unsubscribeAll() {
		allEventProcessor?.cancel()
		allEventProcessor = null
		isAllSubscribed.tryEmit(false)
	}

	suspend fun fetchStates() {
		val allStates = api.request(JSONObject().apply {
			put("type", "get_states")
		}).await()
		EntityStateParser.parseStateArray(allStates.getJSONArray("result")).forEach { state ->
			processState(state)
			singleEntityStates[state.entityId]?.emit(state)
			allEntityStates[state.entityId]?.emit(state)
		}
	}

	private fun processState(state: EntityState) {
		val entityId = state.entityId
//		Log.d(TAG, "New state $entityId = $state")
		lastState[entityId] = state
	}

	/**
	 * Unit tests should call this to cancel all of the background coroutines
	 * so that runBlockingTest doesn't complain
	 */
	@VisibleForTesting
	fun cancel() {
		dataNeededJob.cancel()
		allEventProcessor?.cancel()
		singleEventSubscriber.values.forEach { it.cancel() }
		singleEventProcessor.values.forEach { it.cancel() }
	}
}