package com.example.tuserviciohogar.ui.home

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.tuserviciohogar.R
import com.example.tuserviciohogar.utils.SessionManager

class SplashFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionManager = SessionManager(requireContext())

        Handler(Looper.getMainLooper()).postDelayed({
            if (!isAdded) return@postDelayed

            when {
                !sessionManager.isLoggedIn() -> {
                    findNavController().navigate(R.id.nav_home_guest)
                }

                sessionManager.getRole() == "tecnico" -> {
                    findNavController().navigate(R.id.nav_tec_home)
                }

                else -> {
                    findNavController().navigate(R.id.nav_home)
                }
            }
        }, 1800)
    }
}