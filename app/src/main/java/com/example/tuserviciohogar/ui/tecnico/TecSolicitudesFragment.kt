package com.example.tuserviciohogar.ui.tecnico

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.tuserviciohogar.R
import com.example.tuserviciohogar.utils.SessionManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import androidx.navigation.fragment.findNavController

class TecSolicitudesFragment : Fragment() {

    private var solicitudesListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tec_solicitudes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionManager = SessionManager(requireContext())
        val layoutSinSolicitudes = view.findViewById<LinearLayout>(R.id.layoutSinSolicitudes)
        val contenedor = view.findViewById<LinearLayout>(R.id.contenedorSolicitudes)

        val database = FirebaseDatabase.getInstance()
        val ref = database.getReference("solicitudes")

        solicitudesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return
                contenedor.removeAllViews()

                val categoriaTecnico = sessionManager.getCategory()

                val solicitudesPendientes = snapshot.children.filter {
                    val estado = it.child("estado").getValue(String::class.java) ?: ""
                    val categoriaSolicitud = it.child("categoria").getValue(String::class.java) ?: ""

                    estado == "pendiente" && categoriaSolicitud == categoriaTecnico
                }

                if (solicitudesPendientes.isEmpty()) {
                    layoutSinSolicitudes.visibility = View.VISIBLE
                    contenedor.visibility = View.GONE
                } else {
                    layoutSinSolicitudes.visibility = View.GONE
                    contenedor.visibility = View.VISIBLE

                    solicitudesPendientes.forEach { solicitud ->
                        val id           = solicitud.child("id").getValue(String::class.java) ?: ""
                        val clienteNombre = solicitud.child("clienteNombre").getValue(String::class.java) ?: ""
                        val descripcion  = solicitud.child("descripcion").getValue(String::class.java) ?: ""
                        val direccion    = solicitud.child("direccion").getValue(String::class.java) ?: ""
                        val metodoPago   = solicitud.child("metodoPago").getValue(String::class.java) ?: ""

                        val itemView = LayoutInflater.from(requireContext())
                            .inflate(R.layout.item_solicitud, contenedor, false)

                        itemView.findViewById<TextView>(R.id.tvClienteNombre).text = "👤 $clienteNombre"
                        itemView.findViewById<TextView>(R.id.tvDescripcion).text = "🔧 $descripcion"
                        itemView.findViewById<TextView>(R.id.tvDireccion).text = "📍 $direccion"
                        itemView.findViewById<TextView>(R.id.tvMetodoPago).text = "💳 $metodoPago"

                        itemView.findViewById<Button>(R.id.btnAceptar).setOnClickListener {
                            aceptarSolicitud(id, sessionManager)
                        }

                        itemView.findViewById<Button>(R.id.btnRechazar).setOnClickListener {
                            rechazarSolicitud(id)
                        }

                        contenedor.addView(itemView)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        ref.addValueEventListener(solicitudesListener!!)
    }

    private fun aceptarSolicitud(solicitudId: String, sessionManager: SessionManager) {
        val database = FirebaseDatabase.getInstance()
        val ref = database.getReference("solicitudes").child(solicitudId)

        val updates = mapOf(
            "estado"           to "aceptada",
            "tecnicoUid"       to sessionManager.getUid(),
            "tecnicoNombre"    to sessionManager.getName(),
            "tecnicoCategoria" to sessionManager.getCategory()
        )

        ref.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(),
                    "Solicitud aceptada", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.nav_tec_mapa)
            }

            .addOnFailureListener {
                Toast.makeText(requireContext(),
                    "Error al aceptar", Toast.LENGTH_SHORT).show()
            }

    }

    private fun rechazarSolicitud(solicitudId: String) {
        FirebaseDatabase.getInstance()
            .getReference("solicitudes")
            .child(solicitudId)
            .child("estado")
            .setValue("cancelada")
            .addOnSuccessListener {
                Toast.makeText(requireContext(),
                    "Solicitud rechazada", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        solicitudesListener?.let {
            FirebaseDatabase.getInstance()
                .getReference("solicitudes")
                .removeEventListener(it)
        }
    }
}