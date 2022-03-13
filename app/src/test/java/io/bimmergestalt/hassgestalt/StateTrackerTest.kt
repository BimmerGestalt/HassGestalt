package io.bimmergestalt.hassgestalt

import com.nhaarman.mockito_kotlin.*
import io.bimmergestalt.hassgestalt.hass.HassApi
import io.bimmergestalt.hassgestalt.hass.StateTracker
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class StateTrackerTest {
	val subscriptions = ArrayList<MutableSharedFlow<JSONObject>>()
	val api = mock<HassApi> {
		on {subscribe(any())} doAnswer { MutableSharedFlow<JSONObject>(extraBufferCapacity = 1).also{subscriptions.add(it)}}
		on {request(any())} doAnswer { CompletableDeferred()}
	}

	fun event(entityId: String, state: String): JSONObject {
		return JSONObject().apply {
			put("event", JSONObject().apply {
				put ("data", JSONObject().apply {
					put ("new_state", JSONObject().apply {
						put("entity_id", entityId)
						put("state", state)
						put("last_changed", "2022-03-06T22:47:37.089694Z")
					})
				})
			})
		}
	}

	@Test
	fun testStateGlobalSubscriptionBeforeFetch() = runTest(dispatchTimeoutMs = 1000L) {
		val subject = StateTracker(this, api)
		subject.subscribeAll()
		verify(api).subscribe(argThat{
			optString("type") == "subscribe_events" &&
			optString("event_type") == "state_changed"})
		assertEquals(1, subscriptions.size)
		launch {
			subscriptions[0].emit(event("sensor.test", "on"))
			println("Successful post")
		}
		val result = subject["sensor.test"].first()
		assertEquals("sensor.test", result.entityId)
		assertEquals("on", result.state)
		assertEquals("on", subject.lastState["sensor.test"]?.state)
		println("Successful result")
		subject.cancel()
	}

	@Test
	fun testStateGlobalSubscriptionAfterFetch() = runTest(dispatchTimeoutMs = 1000L) {
		val subject = StateTracker(this, api)
		subject.subscribeAll()
		verify(api).subscribe(argThat{
			optString("type") == "subscribe_events" &&
			optString("event_type") == "state_changed"})
		assertEquals(1, subscriptions.size)

		val job = launch {
			val result = subject["sensor.test"].first()
			assertEquals("sensor.test", result.entityId)
			assertEquals("on", result.state)
			assertEquals("on", subject.lastState["sensor.test"]?.state)
			println("Successful result")
		}
		launch {
			subscriptions[0].emit(event("sensor.test", "on"))
			println("Successful post")
		}
		job.join()
		subject.cancel()
	}

	@Test
	fun testStateSwitchesToSingle() = runTest(dispatchTimeoutMs = 1000L) {
		val subject = StateTracker(this, api)
		subject.subscribeAll()
		assertEquals(1, subscriptions.size)
		val job = launch {
			val result = subject["sensor.test"].first()
			assertEquals("sensor.test", result.entityId)
			assertEquals("on", result.state)
			assertEquals("on", subject.lastState["sensor.test"]?.state)
			println("Successful result")
		}
		advanceUntilIdle()

		// the subscription automatically switches to single stream
		subject.unsubscribeAll()
		advanceUntilIdle()
		assertEquals(2, subscriptions.size)
		verify(api).subscribe(argThat{
			optString("type") == "subscribe_trigger" &&
			getJSONObject("trigger").optString("platform") == "state" &&
			getJSONObject("trigger").optString("entity_id") == "sensor.test"})

		subscriptions[0].emit(event("sensor.test", "wrong"))
		subscriptions[1].emit(event("sensor.test", "on"))
		println("Successful post")
		job.join()
		subject.cancel()
	}

	@Test
	fun testStateSwitchesToAll() = runTest(dispatchTimeoutMs = 1000L) {
		val subject = StateTracker(this, api)
		val job = launch {
			val result = subject["sensor.test"].first()
			assertEquals("sensor.test", result.entityId)
			assertEquals("on", result.state)
			assertEquals("on", subject.lastState["sensor.test"]?.state)
			println("Successful result")
		}
		advanceUntilIdle()

		// the subscription automatically switches to global stream
		subject.subscribeAll()
		advanceUntilIdle()
		assertEquals(2, subscriptions.size)
		subscriptions[0].emit(event("sensor.test", "wrong"))
		subscriptions[1].emit(event("sensor.test", "on"))
		println("Successful post")
		job.join()
		subject.cancel()
	}

	@Test
	fun testUnsubscribe() = runTest(dispatchTimeoutMs = 1000L) {
		val subject = StateTracker(this, api)
		val job = launch {
			val result = subject["sensor.test"].first()
			assertEquals("sensor.test", result.entityId)
			assertEquals("on", result.state)
			assertEquals("on", subject.lastState["sensor.test"]?.state)
			println("Successful result")
			assertEquals(1, subscriptions[0].subscriptionCount.value)
		}
		advanceUntilIdle()
		verify(api).subscribe(argThat{
			optString("type") == "subscribe_trigger" &&
					getJSONObject("trigger").optString("platform") == "state" &&
					getJSONObject("trigger").optString("entity_id") == "sensor.test"})

		// emit the event for the consuming job, which closes after the first() event
		subscriptions[0].emit(event("sensor.test", "on"))
		advanceUntilIdle()
		assertEquals(0, subscriptions[0].subscriptionCount.value)
		job.join()
		subject.cancel()
	}
}