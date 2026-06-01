package com.example.tuserviciohogar.ui.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.tuserviciohogar.utils.SessionManager
import com.google.firebase.database.*
import com.example.tuserviciohogar.R

class GalleryFragment : Fragment() {

    private var historialListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_gallery, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionManager = SessionManager(requireContext())
        val uid = sessionManager.getUid()
        val rol = sessionManager.getRole()

        android.util.Log.d("HISTORIAL", "UID: '$uid' ROL: '$rol'")
        android.util.Log.d("HISTORIAL", "isLoggedIn: ${sessionManager.isLoggedIn()}")

        cargarHistorial(uid, rol, view)
    }

    private fun cargarHistorial(uid: String, rol: String, view: View) {
        val layoutLista = view.findViewById<LinearLayout>(R.id.layoutListaServicios)
        val tvVacio = view.findViewById<TextView>(R.id.tvSinServicios)

        android.util.Log.d("HISTORIAL", "Cargando historial para uid: $uid, rol: $rol")

        val ref = if (rol == "tecnico") {
            FirebaseDatabase.getInstance()
                .getReference("historial_tecnico").child(uid)
        } else {
            FirebaseDatabase.getInstance()
                .getReference("historial").child(uid)
        }

        historialListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return

                android.util.Log.d("HISTORIAL", "Datos recibidos: ${snapshot.childrenCount} servicios")
                android.util.Log.d("HISTORIAL", "Existe: ${snapshot.exists()}")

                layoutLista.removeAllViews()

                if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                    tvVacio.visibility = View.VISIBLE
                    layoutLista.visibility = View.GONE
                    return
                }

                tvVacio.visibility = View.GONE
                layoutLista.visibility = View.VISIBLE

                val servicios = snapshot.children.toList().reversed()

                for (servicio in servicios) {
                    android.util.Log.d("HISTORIAL", "Servicio: ${servicio.key} -> ${servicio.value}")
                    val direccion = servicio.child("direccion")
                        .getValue(String::class.java) ?: ""
                    val descripcion = servicio.child("descripcion")
                        .getValue(String::class.java) ?: ""
                    val categoria = servicio.child("categoria")
                        .getValue(String::class.java) ?: ""
                    val fecha = servicio.child("fecha")
                        .getValue(Long::class.java) ?: 0L
                    val clienteNombre = servicio.child("clienteNombre")
                        .getValue(String::class.java) ?: ""
                    val tecnicoNombre = servicio.child("tecnicoNombre")
                        .getValue(String::class.java) ?: ""

                    val fechaStr = java.text.SimpleDateFormat(
                        "dd/MM/yyyy HH:mm",
                        java.util.Locale.getDefault()
                    ).format(java.util.Date(fecha))

                    val card = LayoutInflater.from(requireContext())
                        .inflate(R.layout.item_servicio_reciente, layoutLista, false)

                    card.findViewById<TextView>(R.id.tvItemCategoria).text = "🔧 $categoria"
                    card.findViewById<TextView>(R.id.tvItemDescripcion).text = descripcion
                    card.findViewById<TextView>(R.id.tvItemDireccion).text = "📍 $direccion"
                    card.findViewById<TextView>(R.id.tvItemFecha).text = fechaStr

                    val tvPersona = card.findViewById<TextView>(R.id.tvItemPersona)
                    if (rol == "tecnico") {
                        tvPersona.text = "👤 Cliente: $clienteNombre"
                    } else {
                        tvPersona.text = "🔧 Técnico: $tecnicoNombre"
                    }

                    layoutLista.addView(card)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("HISTORIAL", "Error: ${error.message}")
            }
        }

        ref.addValueEventListener(historialListener!!)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        historialListener?.let {
            FirebaseDatabase.getInstance()
                .getReference("historial")
                .removeEventListener(it)
        }
    }
}