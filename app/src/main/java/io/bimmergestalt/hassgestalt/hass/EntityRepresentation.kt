package io.bimmergestalt.hassgestalt.hass

import android.content.Context
import android.graphics.Paint
import android.graphics.drawable.Drawable
import com.mikepenz.iconics.IconicsColor
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.utils.color
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

data class EntityRepresentation(val iconName: String, val color: Int,
                                val entityId: String, val name: String, val state: String, val stateText: String,
                                val action: (() -> Unit)?,
) {
	companion object {
		fun fromEntityState(state: EntityState): EntityRepresentation {
			return EntityRepresentation(state.icon(), state.color(),
				state.entityId, state.label,
				state.state, state.stateText, null)
		}

		fun Flow<EntityState>.asRepresentation(): Flow<EntityRepresentation> = this.map {
			fromEntityState(it)
		}
		fun Flow<EntityRepresentation>.gainControl(hassApi: HassApi): Flow<EntityRepresentation> {
			val controller = EntityController(hassApi)
			val representation = this.map { representation ->
				representation.copy(action = controller.toggle(representation))
			}
			return merge(representation, controller.pendingResult)
		}
		fun Flow<EntityRepresentation>.gainControl(hassApi: HassApi, domain: String, service: String, target: Map<String, Any?>, args: Map<String, Any?>): Flow<EntityRepresentation> {
			val controller = EntityController(hassApi)
			val representation = this.map { representation ->
				representation.copy(action = controller.callService(representation, domain, service, target, args))
			}
			return merge(representation, controller.pendingResult)
		}
	}

	val icon: (Context.() -> Drawable)?
		get() {
			val iconValue = EntityIcon.iconByName(iconName)
			return if (iconValue != null) {
				{
					IconicsDrawable(this, iconValue).also {
						it.style = Paint.Style.FILL_AND_STROKE
						it.color = IconicsColor.colorInt(color)
					}
				}
			} else { null }
		}

	fun tryClick() {
		action?.invoke()
	}

	override fun toString(): String {
		return "$name $stateText"
	}
}