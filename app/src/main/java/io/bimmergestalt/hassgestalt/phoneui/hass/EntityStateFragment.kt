package io.bimmergestalt.hassgestalt.phoneui.hass

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import io.bimmergestalt.hassgestalt.EntityStateBinding
import io.bimmergestalt.hassgestalt.R
import java.lang.IllegalArgumentException

class EntityStateFragment: Fragment() {
	var entityId = ""
	val stateViewModel by activityViewModels<HassStateViewModel>()
	val viewModel by viewModels<EntityStateViewModel> { EntityStateViewModel.Factory(stateViewModel.state, entityId) }

	override fun onInflate(context: Context, attrs: AttributeSet, savedInstanceState: Bundle?) {
		super.onInflate(context, attrs, savedInstanceState)
		val ta = context.obtainStyledAttributes(attrs, R.styleable.EntityStateFragment_MembersInjector)
		val entityId = ta.getString(R.styleable.EntityStateFragment_MembersInjector_hass_entity_id)
		if (entityId != null) {
			this.entityId = entityId
		} else {
			throw IllegalArgumentException("EntityStateFragment requires an app:hass_entity_id parameter")
		}
		ta.recycle()
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		val binding = EntityStateBinding.inflate(inflater, container, false)
		binding.lifecycleOwner = viewLifecycleOwner
		binding.viewModel = viewModel
		return binding.root
	}

}