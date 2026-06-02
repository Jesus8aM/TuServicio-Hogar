package com.example.tuserviciohogar.ui.tecnico

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.tuserviciohogar.R
import com.example.tuserviciohogar.utils.SessionManager
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.database.FirebaseDatabase

class TecHomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_tec_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionManager = SessionManager(requireContext())
        val uid = sessionManager.getUid()

        view.findViewById<TextView>(R.id.tvTecSaludo).text =
            "¡Hola, ${sessionManager.getName()}!"

        view.findViewById<TextView>(R.id.tvTecCategoria).text =
            "Especialidad: ${sessionManager.getCategory()}"

        val tvEstado =
            view.findViewById<TextView>(R.id.tvEstadoDisponibilidad)

        val switchDisponible =
            view.findViewById<SwitchMaterial>(R.id.switchDisponible)

        val disponible = sessionManager.isDisponible()

        switchDisponible.isChecked = disponible

        tvEstado.text =
            if (disponible)
                "🟢 Disponible"
            else
                "🔴 No disponible"

        switchDisponible.setOnCheckedChangeListener { _, isChecked ->

            sessionManager.setDisponible(isChecked)

            tvEstado.text =
                if (isChecked)
                    "🟢 Disponible"
                else
                    "🔴 No disponible"

            FirebaseDatabase.getInstance()
                .getReference("usuarios")
                .child(uid)
                .child("isAvailable")
                .setValue(isChecked)

            Toast.makeText(
                requireContext(),
                if (isChecked)
                    "Ahora estás disponible"
                else
                    "Ahora no estás disponible",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Solicitudes
        view.findViewById<MaterialCardView>(R.id.cardTecSolicitudes)
            .setOnClickListener {

                findNavController()
                    .navigate(R.id.nav_tec_solicitudes)
            }

        // Servicio activo
        view.findViewById<MaterialCardView>(R.id.cardTecServicioActivo)
            .setOnClickListener {

                findNavController()
                    .navigate(R.id.nav_tec_servicios)
            }

        // Historial
        view.findViewById<MaterialCardView>(R.id.cardTecServicios)
            .setOnClickListener {

                findNavController()
                    .navigate(R.id.nav_gallery)
            }

        // Perfil
        view.findViewById<MaterialCardView>(R.id.cardTecPerfil)
            .setOnClickListener {

                findNavController()
                    .navigate(R.id.nav_slideshow)
            }
    }
}