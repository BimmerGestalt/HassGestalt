package io.bimmergestalt.hassgestalt.phoneui.hass

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import io.bimmergestalt.hassgestalt.DashboardViewBinding

class DashboardViewFragment: Fragment() {
	val stateViewModel by activityViewModels<HassStateViewModel>()
	val viewModel by activityViewModels<DashboardListViewModel> {DashboardListViewModel.Factory(stateViewModel.api, stateViewModel.state)}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		val binding = DashboardViewBinding.inflate(inflater, container, false)
		binding.lifecycleOwner = viewLifecycleOwner
		binding.viewModel = viewModel
		return binding.root
	}
}