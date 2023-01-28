package io.bimmergestalt.hassgestalt.hass

import android.content.Context
import android.graphics.Paint
import android.graphics.drawable.Drawable
import com.mikepenz.iconics.IconicsColor
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.utils.color
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*

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
		@OptIn(ExperimentalCoroutinesApi::class)
		fun Flow<EntityRepresentation>.gainControl(hassApi: HassApi): Flow<EntityRepresentation> {
			val controller = EntityController(hassApi)
			return this.flatMapLatest { representation ->
				val command = controller.toggle(representation.entityId, representation.state)
				if (command != null) {
					addClickHandler(representation, command)
				} else {
					flowOf(representation)
				}
			}
		}
		@OptIn(ExperimentalCoroutinesApi::class)
		fun Flow<EntityRepresentation>.gainControl(hassApi: HassApi, domain: String, service: String, target: Map<String, Any?>, args: Map<String, Any?>): Flow<EntityRepresentation> {
			val controller = EntityController(hassApi)
			val command = controller.callService(domain, service, target, args)
			return this.flatMapLatest { representation ->
				addClickHandler(representation, command)
			}
		}
		private fun addClickHandler(representation: EntityRepresentation, command: () -> Unit): Flow<EntityRepresentation> {
			return channelFlow {
				val clickInput = MutableSharedFlow<Boolean>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
				val action: ()->Unit = { clickInput.tryEmit(true) }
				// send the new EntityRepresentation with our click handler attached
				val output = representation.copy(action=action)
				send(output)

				// wait for clicks and send updated answers
				// the delay will be cancelled by flatMapLatest if the original Representation is updated
				clickInput.collectLatest {
					send(representation.copy(stateText = "..."))
					command.invoke()
					delay(3000)
					send(output)
				}
			}
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