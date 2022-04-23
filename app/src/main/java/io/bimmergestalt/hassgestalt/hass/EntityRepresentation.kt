package io.bimmergestalt.hassgestalt.hass

import android.content.Context
import android.graphics.Paint
import android.graphics.drawable.Drawable
import com.mikepenz.iconics.IconicsColor
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.utils.color
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*

data class EntityRepresentation(val iconName: String, val icon: (Context.() -> Drawable)?,
                                val entityId: String, val name: String, val state: String, val loading: Boolean,
                                val actionIcon: Drawable?, val action: (() -> Unit)?,
) {
	companion object {
		fun fromEntityState(state: EntityState, controller: EntityController?): EntityRepresentation {
			// figure out the icon
			val iconName = state.icon()
			val iconValue = EntityIcon.iconByName(iconName)
			val iconDrawable: (Context.() -> Drawable)? = if (iconValue != null) {
				{
					IconicsDrawable(this, iconValue).apply {
						style = Paint.Style.FILL_AND_STROKE
						color = IconicsColor.colorInt(state.color())
					}
				}
			} else { null }

			return EntityRepresentation(state.icon(), iconDrawable,
				state.entityId, state.label,
				state.stateText, state == EntityState.EMPTY,
				controller?.icon, controller)
		}

		fun Flow<EntityState>.gainControl(hassApi: HassApi): Flow<EntityRepresentation> {
			val pendingResult = MutableSharedFlow<EntityRepresentation>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
			val currentState = this.map { state ->
				val controller = EntityController.create(hassApi, state, pendingResult)
				fromEntityState(state, controller)
			}
			return merge(currentState, pendingResult)
		}
	}

	fun tryClick() {
		action?.invoke()
	}

	override fun toString(): String {
		return "$name $state"
	}
}

fun List<Flow<EntityRepresentation>>.isLoading(includeControlled: Boolean): Flow<Boolean> = combine(this) { entities ->
	entities.any { it.loading || (includeControlled && it.state == "...") }
}

fun Flow<List<Flow<EntityRepresentation>>>.isLoading(includeControlled: Boolean): Flow<Boolean> = flatMapLatest { currentEntities ->
	currentEntities.isLoading(includeControlled)
}