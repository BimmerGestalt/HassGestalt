package io.bimmergestalt.hassgestalt.phoneui

import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
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