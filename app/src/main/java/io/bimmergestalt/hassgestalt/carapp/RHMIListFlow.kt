package io.bimmergestalt.hassgestalt.carapp

import de.bmw.idrive.BMWRemoting
import kotlinx.coroutines.flow.*

fun <T> List<Flow<T>>.rhmiDataTableFlow(rowMap: (T) -> Array<Any>): Flow<BMWRemoting.RHMIDataTable> {
	return rhmiDataTableFlow(flowOf(this), rowMap)
}
@JvmName("_rhmiDataTableFlowExt")
fun <T> Flow<List<Flow<T>>>.rhmiDataTableFlow(rowMap: (T) -> Array<Any>): Flow<BMWRemoting.RHMIDataTable> {
	return rhmiDataTableFlow(this, rowMap)
}
fun <T> rhmiDataTableFlow(rowData: Flow<List<Flow<T>>>, rowMap: (T) -> Array<Any>): Flow<BMWRemoting.RHMIDataTable> {
	return rowData.flatMapLatest { tableRows ->
		val rowCount = tableRows.count()
		tableRows.mapIndexed { rowIndex, rowFlow: Flow<T> ->
			rowFlow.map { row ->
				val output = rowMap(row)
				BMWRemoting.RHMIDataTable(arrayOf(output), false, rowIndex, 1, rowCount, 0, output.size, output.size)
			}
		}.merge()
	}
}