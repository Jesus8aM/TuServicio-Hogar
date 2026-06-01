package com.example.tuserviciohogar.ui.map

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
import com.google.firebase.database.FirebaseDatabase

class PagoFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pago, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionManager = SessionManager(requireContext())

        val direccion    = arguments?.getString("direccion") ?: ""
        val descripcion  = arguments?.getString("descripcion") ?: ""
        val categoria = arguments?.getString("categoria") ?: ""
        val domicilioLat = arguments?.getFloat("domicilioLat") ?: 0f
        val domicilioLon = arguments?.getFloat("domicilioLon") ?: 0f

        view.findViewById<TextView>(R.id.tvDireccion).text = "📍 $direccion"
        view.findViewById<TextView>(R.id.tvDescripcion).text = "🔧 $descripcion"

        view.findViewById<Button>(R.id.btnEfectivo).setOnClickListener {
            enviarSolicitud(
                sessionManager, "Efectivo",
                direccion, descripcion,categoria,
                domicilioLat, domicilioLon
            )
        }

        view.findViewById<Button>(R.id.btnTarjeta).setOnClickListener {
            Toast.makeText(requireContext(),
                "Pago con tarjeta próximamente", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<Button>(R.id.btnCancelar).setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun obtenerTarifaBase(categoria: String): Double {
        return when (categoria) {
            "Electricidad" -> 250.0
            "Plomería" -> 250.0
            "Carpintería" -> 300.0
            "Pintura" -> 300.0
            "Cerrajería" -> 350.0
            "Aire Acondicionado" -> 400.0
            else -> 250.0
        }
    }

    private fun enviarSolicitud(
        sessionManager: SessionManager,
        metodoPago: String,
        direccion: String,
        descripcion: String,
        categoria: String,
        lat: Float,
        lon: Float
    ) {
        Toast.makeText(requireContext(), "Enviando solicitud...", Toast.LENGTH_SHORT).show()

        val database = FirebaseDatabase.getInstance()
        val solicitudRef = database.getReference("solicitudes").push()
        val solicitudId = solicitudRef.key ?: return

        val montoBase = obtenerTarifaBase(categoria)
        val montoAdicional = 0.0
        val montoTotal = montoBase + montoAdicional
        val comisionApp = montoTotal * 0.10
        val gananciaTecnico = montoTotal - comisionApp

        val solicitud = mapOf(
            "id"          to solicitudId,
            "clienteUid"  to sessionManager.getUid(),
            "clienteNombre" to sessionManager.getName(),
            "descripcion" to descripcion,
            "direccion"   to direccion,
            "metodoPago"  to metodoPago,
            "lat"         to lat,
            "lon"         to lon,
            "estado"      to "pendiente",
            "categoria" to categoria,
            "timestamp"   to System.currentTimeMillis(),
            "montoBase" to montoBase,
            "montoAdicional" to montoAdicional,
            "montoTotal" to montoTotal,
            "comisionApp" to comisionApp,
            "gananciaTecnico" to gananciaTecnico,
            "montoAceptado" to false,
        )

        solicitudRef.setValue(solicitud)
            .addOnSuccessListener {
                val bundle = Bundle().apply {
                    putString("solicitudId", solicitudId)
                    putString("metodoPago", metodoPago)
                    putString("descripcion", descripcion)
                    putFloat("domicilioLat", lat)
                    putFloat("domicilioLon", lon)
                }
                findNavController().navigate(R.id.action_pago_to_viaje, bundle)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(),
                    "Error enviando solicitud", Toast.LENGTH_SHORT).show()
            }
    }
}