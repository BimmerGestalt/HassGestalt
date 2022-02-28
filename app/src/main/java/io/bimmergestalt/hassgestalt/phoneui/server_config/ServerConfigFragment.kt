package io.bimmergestalt.hassgestalt.phoneui.server_config

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import io.bimmergestalt.hassgestalt.OauthAccess
import io.bimmergestalt.hassgestalt.ServerConfigBinding
import io.bimmergestalt.hassgestalt.data.ServerConfigPersistence

class ServerConfigFragment: Fragment() {
	val viewModel by activityViewModels<ServerConfigViewModel>()
	val oauthAccess by lazy { OauthAccess(requireContext(), viewModel.serverConfig.authState) {
		// trigger the LiveData to update with the authState, even if it's the same
		viewModel.serverConfig.authState = it
	} }
	val serverConfigPersistence by lazy { ServerConfigPersistence(requireContext(), viewLifecycleOwner) }
	val controller by lazy {ServerConfigController(lifecycleScope, viewModel.serverConfig, oauthAccess)}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		serverConfigPersistence.load()
		viewModel.authenticated.observe(viewLifecycleOwner) {
			oauthAccess.tryRefreshToken()
		}

		val binding = ServerConfigBinding.inflate(inflater, container, false)
		binding.lifecycleOwner = viewLifecycleOwner
		binding.controller = controller
		binding.viewModel = viewModel
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		oauthAccess.handleAuthorizationResponse(requireActivity().intent)
	}

}