package com.example.tuserviciohogar.ui.tecnico

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.tuserviciohogar.R
import com.example.tuserviciohogar.utils.SessionManager
import com.google.android.material.switchmaterial.SwitchMaterial

class TecHomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tec_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionManager = SessionManager(requireContext())

        view.findViewById<TextView>(R.id.tvTecSaludo).text =
            "¡Hola, ${sessionManager.getName()}!"

        view.findViewById<TextView>(R.id.tvTecCategoria).text =
            "Especialidad: ${sessionManager.getCategory()}"

        val tvEstado = view.findViewById<TextView>(R.id.tvEstadoDisponibilidad)
        val switch = view.findViewById<SwitchMaterial>(R.id.switchDisponible)

        // Estado inicial desde preferencias
        val disponible = sessionManager.isDisponible()
        switch.isChecked = disponible
        tvEstado.text = if (disponible) "🟢 Disponible" else "🔴 No disponible"

        switch.setOnCheckedChangeListener { _, isChecked ->
            sessionManager.setDisponible(isChecked)
            tvEstado.text = if (isChecked) "🟢 Disponible" else "🔴 No disponible"
        }
    }
}