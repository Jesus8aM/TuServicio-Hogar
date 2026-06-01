package com.example.tuserviciohogar.ui.tecnico

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.tuserviciohogar.R
import com.example.tuserviciohogar.utils.SessionManager
import com.google.firebase.database.*

class TecServiciosFragment : Fragment() {

    private var historialListener: ValueEventListener? = null
    private lateinit var historialRef: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tec_servicios, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionManager = SessionManager(requireContext())
        val uid = sessionManager.getUid()

        val layoutSinServicios = view.findViewById<LinearLayout>(R.id.layoutSinServicios)

        historialRef = FirebaseDatabase.getInstance()
            .getReference("historial_tecnico")
            .child(uid)

        historialListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return

                layoutSinServicios.removeAllViews()

                if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                    layoutSinServicios.gravity = android.view.Gravity.CENTER
                    layoutSinServicios.addView(crearTexto("🔧", 48f, 0.5f))
                    layoutSinServicios.addView(crearTexto("Aún no tienes servicios realizados", 16f, 0.5f))
                    return
                }

                layoutSinServicios.gravity = android.view.Gravity.NO_GRAVITY
                layoutSinServicios.orientation = LinearLayout.VERTICAL
                layoutSinServicios.setPadding(24, 24, 24, 24)

                val servicios = snapshot.children.toList().reversed()

                for (servicio in servicios) {
                    val direccion = servicio.child("direccion").getValue(String::class.java) ?: ""
                    val descripcion = servicio.child("descripcion").getValue(String::class.java) ?: ""
                    val categoria = servicio.child("categoria").getValue(String::class.java) ?: ""
                    val metodoPago = servicio.child("metodoPago").getValue(String::class.java) ?: ""
                    val clienteNombre = servicio.child("clienteNombre").getValue(String::class.java) ?: ""
                    val fecha = servicio.child("fecha").getValue(Long::class.java) ?: 0L

                    val fechaStr = java.text.SimpleDateFormat(
                        "dd/MM/yyyy HH:mm",
                        java.util.Locale.getDefault()
                    ).format(java.util.Date(fecha))

                    val card = LayoutInflater.from(requireContext())
                        .inflate(R.layout.item_servicio_reciente, layoutSinServicios, false)

                    card.findViewById<TextView>(R.id.tvItemCategoria).text = "🔧 $categoria"
                    card.findViewById<TextView>(R.id.tvItemDescripcion).text = descripcion
                    card.findViewById<TextView>(R.id.tvItemDireccion).text = "📍 $direccion"
                    card.findViewById<TextView>(R.id.tvItemFecha).text = fechaStr

                    card.findViewById<TextView>(R.id.tvItemPersona).text =
                        "👤 Cliente: $clienteNombre"

                    layoutSinServicios.addView(card)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        historialRef.addValueEventListener(historialListener!!)
    }

    private fun crearTexto(texto: String, size: Float, alphaValue: Float): TextView {
        return TextView(requireContext()).apply {
            text = texto
            textSize = size
            alpha = alphaValue
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        historialListener?.let {
            historialRef.removeEventListener(it)
        }
    }
}