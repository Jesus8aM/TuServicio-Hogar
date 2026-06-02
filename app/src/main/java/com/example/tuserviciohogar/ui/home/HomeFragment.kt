package com.example.tuserviciohogar.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.tuserviciohogar.utils.SessionManager
import com.google.firebase.database.*
import com.example.tuserviciohogar.R
import com.google.android.material.card.MaterialCardView

class HomeFragment : Fragment() {

    private var historialListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val sessionManager = SessionManager(requireContext())
        // Si no está logueado redirigir a pantalla de invitado
        if (!sessionManager.isLoggedIn()) {
            findNavController().navigate(R.id.nav_home_guest)
            return
        }
        super.onViewCreated(view, savedInstanceState)



        // Saludo personalizado
        val tvSaludo = view.findViewById<TextView>(R.id.tvSaludo)
        if (sessionManager.isLoggedIn()) {
            tvSaludo.text = "¡Hola, ${sessionManager.getName()}!"
        } else {
            tvSaludo.text = "¡Bienvenido!"
        }

        // Botón principal
        view.findViewById<Button>(R.id.btnSolicitarServicio).setOnClickListener {
            findNavController().navigate(R.id.nav_map)
        }

        // Categorías
        // Categorías
        view.findViewById<MaterialCardView>(R.id.cardPlomeria).setOnClickListener {
            irAlMapaConCategoria("Plomería")
        }

        view.findViewById<MaterialCardView>(R.id.cardElectricidad).setOnClickListener {
            irAlMapaConCategoria("Electricidad")
        }

        view.findViewById<MaterialCardView>(R.id.cardCarpinteria).setOnClickListener {
            irAlMapaConCategoria("Carpintería")
        }

        view.findViewById<MaterialCardView>(R.id.cardPintura).setOnClickListener {
            irAlMapaConCategoria("Pintura")
        }

        view.findViewById<MaterialCardView>(R.id.cardCerrajeria).setOnClickListener {
            irAlMapaConCategoria("Cerrajería")
        }

        view.findViewById<MaterialCardView>(R.id.cardAire).setOnClickListener {
            irAlMapaConCategoria("Aire Acondicionado")
        }

        // Cargar historial si está logueado
        if (sessionManager.isLoggedIn()) {
            cargarHistorial(sessionManager.getUid(), view)
        }
    }

    private fun irAlMapaConCategoria(categoria: String) {
        val bundle = Bundle().apply { putString("categoria", categoria) }
        findNavController().navigate(R.id.nav_map, bundle)
    }

    private fun cargarHistorial(uid: String, view: View) {
        val layoutSinServicios = view.findViewById<LinearLayout>(R.id.layoutSinServicios)
        val layoutHistorial = view.findViewById<LinearLayout>(R.id.layoutHistorial)

        historialListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return

                layoutHistorial.removeAllViews()

                if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                    layoutSinServicios.visibility = View.VISIBLE
                    layoutHistorial.visibility = View.GONE
                    return
                }

                layoutSinServicios.visibility = View.GONE
                layoutHistorial.visibility = View.VISIBLE

                // Mostrar los últimos 5 servicios
                val servicios = snapshot.children.toList().takeLast(5).reversed()

                for (servicio in servicios) {
                    val direccion = servicio.child("direccion")
                        .getValue(String::class.java) ?: ""
                    val descripcion = servicio.child("descripcion")
                        .getValue(String::class.java) ?: ""
                    val categoria = servicio.child("categoria")
                        .getValue(String::class.java) ?: ""
                    val fecha = servicio.child("fecha")
                        .getValue(Long::class.java) ?: 0L

                    val fechaStr = java.text.SimpleDateFormat(
                        "dd/MM/yyyy HH:mm",
                        java.util.Locale.getDefault()
                    ).format(java.util.Date(fecha))

                    // Crear card de servicio
                    val card = LayoutInflater.from(requireContext())
                        .inflate(R.layout.item_servicio_reciente, layoutHistorial, false)

                    card.findViewById<TextView>(R.id.tvItemCategoria).text = "🔧 $categoria"
                    card.findViewById<TextView>(R.id.tvItemDescripcion).text = descripcion
                    card.findViewById<TextView>(R.id.tvItemDireccion).text = "📍 $direccion"
                    card.findViewById<TextView>(R.id.tvItemFecha).text = fechaStr

                    layoutHistorial.addView(card)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        FirebaseDatabase.getInstance()
            .getReference("historial").child(uid)
            .addValueEventListener(historialListener!!)
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