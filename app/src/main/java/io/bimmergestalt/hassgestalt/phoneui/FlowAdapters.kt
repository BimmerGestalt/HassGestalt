package io.bimmergestalt.hassgestalt.phoneui

import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

fun <T> Flow<List<T>>.asObservableList(coroutineScope: CoroutineScope): ObservableList<T> {
	val upstream = this
	val result = ObservableArrayList<T>()
	coroutineScope.launch {
		upstream.collect {
			result.clear()
			result.addAll(it)
		}
	}
	return result
}