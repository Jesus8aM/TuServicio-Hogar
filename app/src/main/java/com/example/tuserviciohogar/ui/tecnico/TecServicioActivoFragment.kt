package com.example.tuserviciohogar.ui.tecnico

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.tuserviciohogar.R
import com.example.tuserviciohogar.utils.SessionManager
import com.google.firebase.database.FirebaseDatabase

class TecServicioActivoFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tec_servicio_activo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvEstado = view.findViewById<TextView>(R.id.tvEstadoServicioActivo)
        val sessionManager = SessionManager(requireContext())
        val uid = sessionManager.getUid()

        FirebaseDatabase.getInstance()
            .getReference("solicitudes")
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener

                val solicitudActiva = snapshot.children.firstOrNull {
                    val tecnicoUid = it.child("tecnicoUid").getValue(String::class.java) ?: ""
                    val estado = it.child("estado").getValue(String::class.java) ?: ""

                    tecnicoUid == uid &&
                            (
                                    estado == "aceptada" ||
                                            estado == "en_curso" ||
                                            estado == "pendiente_evaluacion"
                                    )
                }

                if (solicitudActiva != null) {
                    val estado = solicitudActiva.child("estado")
                        .getValue(String::class.java) ?: ""

                    val solicitudId = solicitudActiva.child("id")
                        .getValue(String::class.java) ?: ""

                    if (estado == "en_curso" || estado == "pendiente_evaluacion") {
                        val bundle = Bundle().apply {
                            putString("solicitudId", solicitudId)
                        }

                        findNavController().navigate(R.id.nav_servicio_en_curso, bundle)
                    } else {
                        findNavController().navigate(R.id.nav_tec_mapa)
                    }
                } else {
                    tvEstado.text = "No tienes un servicio activo"
                }
            }
            .addOnFailureListener {
                if (!isAdded) return@addOnFailureListener
                tvEstado.text = "Error al buscar servicio activo"
            }
    }
}