package com.example.tuserviciohogar.ui.slideshow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.tuserviciohogar.R
import com.example.tuserviciohogar.utils.SessionManager

class SlideshowFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_slideshow, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionManager = SessionManager(requireContext())

        if (!sessionManager.isLoggedIn()) {
            findNavController().navigate(R.id.nav_select_role)
            return
        }

        view.findViewById<TextView>(R.id.tvNombre).text = sessionManager.getName()
        view.findViewById<TextView>(R.id.tvEmail).text = sessionManager.getEmail()
        view.findViewById<TextView>(R.id.tvTelefono).text = sessionManager.getPhone()
        view.findViewById<TextView>(R.id.tvRol).text =
            sessionManager.getRole().replaceFirstChar { it.uppercase() }

        val layoutCategoria = view.findViewById<View>(R.id.tvCategoria)
        val tvEspecialidad = view.findViewById<TextView>(R.id.tvEspecialidad)
        if (sessionManager.getRole() == "tecnico") {
            layoutCategoria.visibility = View.VISIBLE
            tvEspecialidad.text = sessionManager.getCategory()
        } else {
            layoutCategoria.visibility = View.GONE
        }

        view.findViewById<Button>(R.id.btnCerrarSesion).setOnClickListener {
            sessionManager.logout()
            Toast.makeText(requireContext(), "Sesión cerrada", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.nav_home)
        }
    }
}