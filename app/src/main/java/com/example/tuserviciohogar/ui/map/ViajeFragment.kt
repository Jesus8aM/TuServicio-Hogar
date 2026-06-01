package com.example.tuserviciohogar.ui.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.example.tuserviciohogar.R
import com.example.tuserviciohogar.utils.SessionManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class ViajeFragment : Fragment() {

    private lateinit var mapView: MapView
    private var solicitudListener: ValueEventListener? = null
    private var solicitudId: String = ""
    private var tecnicoMarker: Marker? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Configuration.getInstance().load(
            requireContext(),
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        )
        return inflater.inflate(R.layout.fragment_viaje, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionManager = SessionManager(requireContext())

        // Si no hay solicitudId en argumentos, buscar la solicitud activa del cliente
        solicitudId = arguments?.getString("solicitudId") ?: ""
        val metodoPago  = arguments?.getString("metodoPago") ?: "Efectivo"
        val descripcion = arguments?.getString("descripcion") ?: ""
        val domicilioLat = arguments?.getFloat("domicilioLat") ?: 0f
        val domicilioLon = arguments?.getFloat("domicilioLon") ?: 0f

        val domicilio = GeoPoint(domicilioLat.toDouble(), domicilioLon.toDouble())

        mapView = view.findViewById(R.id.mapViewViaje)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(15.0)
        mapView.controller.animateTo(domicilio)

        Marker(mapView).apply {
            position = domicilio
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Tu domicilio"
            mapView.overlays.add(this)
        }
        mapView.invalidate()

        view.findViewById<TextView>(R.id.tvEstado).text = "⏳ Buscando técnico..."
        view.findViewById<TextView>(R.id.tvDescripcionServicio).text = "🔧 $descripcion"
        view.findViewById<TextView>(R.id.tvMetodoPago).text = "💳 Pago: $metodoPago"

        if (solicitudId.isNotEmpty()) {
            escucharSolicitud(view)
        } else {
            // Buscar solicitud activa del cliente en Firebase
            buscarSolicitudActiva(sessionManager.getUid(), view)
        }

        view.findViewById<Button>(R.id.btnCancelarServicio).setOnClickListener {
            cancelarSolicitud()
            findNavController().navigate(R.id.nav_home)
        }
    }

    private fun buscarSolicitudActiva(uid: String, view: View) {
        FirebaseDatabase.getInstance()
            .getReference("solicitudes")
            .orderByChild("clienteUid")
            .equalTo(uid)
            .get()
            .addOnSuccessListener { snapshot ->

                val solicitudActiva = snapshot.children
                    .filter { solicitud ->
                        val estado = solicitud.child("estado").getValue(String::class.java) ?: ""
                        val clienteVioResultado = solicitud.child("clienteVioResultado")
                            .getValue(Boolean::class.java) ?: false

                        estado == "pendiente" ||
                                estado == "aceptada" ||
                                estado == "en_curso" ||
                                estado == "pendiente_evaluacion" ||
                                ((estado == "cancelada" || estado == "completada") && !clienteVioResultado)
                    }
                    .sortedByDescending {
                        it.child("timestamp").getValue(Long::class.java) ?: 0L
                    }
                    .firstOrNull()

                if (solicitudActiva != null) {
                    solicitudId = solicitudActiva.child("id").getValue(String::class.java) ?: ""

                    val estado = solicitudActiva.child("estado")
                        .getValue(String::class.java) ?: ""

                    if (estado == "pendiente_evaluacion") {
                        val clienteYaEvaluo = solicitudActiva.child("clienteYaEvaluo")
                            .getValue(Boolean::class.java) ?: false

                        if (!clienteYaEvaluo) {
                            val bundle = Bundle().apply {
                                putString("solicitudId", solicitudId)
                            }
                            findNavController().navigate(R.id.nav_evaluacion_servicio, bundle)
                            return@addOnSuccessListener
                        }
                    }

                    val descripcion = solicitudActiva.child("descripcion")
                        .getValue(String::class.java) ?: ""

                    val metodoPago = solicitudActiva.child("metodoPago")
                        .getValue(String::class.java) ?: "Efectivo"

                    val lat = solicitudActiva.child("lat")
                        .getValue(Double::class.java) ?: 0.0

                    val lon = solicitudActiva.child("lon")
                        .getValue(Double::class.java) ?: 0.0

                    val domicilio = GeoPoint(lat, lon)

                    view.findViewById<TextView>(R.id.tvDescripcionServicio).text = "🔧 $descripcion"
                    view.findViewById<TextView>(R.id.tvMetodoPago).text = "💳 Pago: $metodoPago"

                    mapView.controller.setZoom(15.0)
                    mapView.controller.animateTo(domicilio)

                    Marker(mapView).apply {
                        position = domicilio
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Tu domicilio"
                        mapView.overlays.add(this)
                    }

                    mapView.invalidate()

                    if (solicitudId.isNotEmpty()) {
                        escucharSolicitud(view)
                    }
                } else {
                    view.findViewById<TextView>(R.id.tvEstado).text =
                        "🔎\nNo tienes un servicio activo"

                    view.findViewById<TextView>(R.id.tvTecnico).visibility = View.GONE
                    view.findViewById<TextView>(R.id.tvDescripcionServicio).text = ""
                    view.findViewById<TextView>(R.id.tvMetodoPago).text = ""
                    view.findViewById<Button>(R.id.btnCancelarServicio).visibility = View.GONE

                    mapView.overlays.clear()
                    mapView.invalidate()
                }
            }
    }

    private fun escucharSolicitud(view: View) {
        val ref = FirebaseDatabase.getInstance()
            .getReference("solicitudes").child(solicitudId)

        solicitudListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return
                val estado = snapshot.child("estado")
                    .getValue(String::class.java) ?: return
                val tvEstado = view.findViewById<TextView>(R.id.tvEstado)
                val tvTecnico = view.findViewById<TextView>(R.id.tvTecnico)
                val btnCancelar = view.findViewById<Button>(R.id.btnCancelarServicio)

                when (estado) {
                    "pendiente" -> {
                        tvEstado.text = "⏳ Buscando técnico..."
                        tvTecnico.visibility = View.GONE
                        btnCancelar.text = "Cancelar servicio"
                    }
                    "pendiente_evaluacion" -> {
                        val clienteYaEvaluo = snapshot.child("clienteYaEvaluo")
                            .getValue(Boolean::class.java) ?: false

                        if (!clienteYaEvaluo) {
                            val bundle = Bundle().apply {
                                putString("solicitudId", solicitudId)
                            }

                            findNavController().navigate(R.id.nav_evaluacion_servicio, bundle)
                        } else {
                            tvEstado.text = "Evaluación enviada"
                            btnCancelar.visibility = View.GONE
                        }
                    }
                    "aceptada" -> {
                        val tecNombre = snapshot.child("tecnicoNombre")
                            .getValue(String::class.java) ?: "Técnico"
                        val tecCategoria = snapshot.child("tecnicoCategoria")
                            .getValue(String::class.java) ?: ""
                        tvEstado.text = "✅ Técnico en camino"
                        tvTecnico.visibility = View.VISIBLE
                        tvTecnico.text = "👷 $tecNombre · $tecCategoria"
                        btnCancelar.visibility = View.GONE
                    }
                    "en_curso" -> {
                        val bundle = Bundle().apply {
                            putString("solicitudId", solicitudId)
                        }

                        findNavController().navigate(R.id.nav_servicio_en_curso, bundle)
                    }
                    "completada" -> {
                        tvEstado.text = "🔎\nNo tienes un servicio activo"
                        tvTecnico.visibility = View.GONE
                        view.findViewById<TextView>(R.id.tvDescripcionServicio).text = ""
                        view.findViewById<TextView>(R.id.tvMetodoPago).text = ""
                        btnCancelar.visibility = View.GONE

                        mapView.overlays.clear()
                        mapView.invalidate()
                    }
                    "cancelada" -> {
                        tvEstado.text = "❌ Solicitud rechazada"
                        btnCancelar.text = "Ver mis servicios"
                        btnCancelar.setOnClickListener {
                            guardarResultadoCliente("rechazada")
                            findNavController().navigate(R.id.nav_gallery)
                        }
                    }
                }

                // Actualizar posición del técnico en el mapa
                val tecLat = snapshot.child("tecnicoLat").getValue(Double::class.java)
                val tecLon = snapshot.child("tecnicoLon").getValue(Double::class.java)
                if (tecLat != null && tecLon != null) {
                    actualizarMarcadorTecnico(GeoPoint(tecLat, tecLon))
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        ref.addValueEventListener(solicitudListener!!)
    }

    private fun actualizarMarcadorTecnico(geoPoint: GeoPoint) {
        if (!isAdded) return
        tecnicoMarker?.let { mapView.overlays.remove(it) }
        tecnicoMarker = Marker(mapView).apply {
            position = geoPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Técnico"
        }
        mapView.overlays.add(tecnicoMarker)
        mapView.invalidate()
    }

    private fun cancelarSolicitud() {
        if (solicitudId.isEmpty()) return
        FirebaseDatabase.getInstance()
            .getReference("solicitudes")
            .child(solicitudId)
            .child("estado")
            .setValue("cancelada")
    }

    private fun guardarResultadoCliente(estadoFinal: String) {
        if (solicitudId.isEmpty()) return

        val database = FirebaseDatabase.getInstance()
        val ref = database.getReference("solicitudes").child(solicitudId)
        val sessionManager = SessionManager(requireContext())

        ref.get().addOnSuccessListener { snapshot ->
            val direccion = snapshot.child("direccion").getValue(String::class.java) ?: ""
            val descripcion = snapshot.child("descripcion").getValue(String::class.java) ?: ""
            val categoria = snapshot.child("categoria").getValue(String::class.java) ?: ""
            val metodoPago = snapshot.child("metodoPago").getValue(String::class.java) ?: ""
            val tecnicoNombre = snapshot.child("tecnicoNombre").getValue(String::class.java) ?: ""

            val servicio = mapOf(
                "id" to solicitudId,
                "direccion" to direccion,
                "descripcion" to descripcion,
                "categoria" to categoria,
                "metodoPago" to metodoPago,
                "fecha" to System.currentTimeMillis(),
                "estado" to estadoFinal,
                "tecnicoNombre" to tecnicoNombre
            )

            database.getReference("historial")
                .child(sessionManager.getUid())
                .child(solicitudId)
                .setValue(servicio)

            ref.child("clienteVioResultado").setValue(true)
        }
    }

    private fun marcarResultadoVisto() {
        if (solicitudId.isEmpty()) return

        FirebaseDatabase.getInstance()
            .getReference("solicitudes")
            .child(solicitudId)
            .child("clienteVioResultado")
            .setValue(true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // NO cancelamos el servicio al salir, solo removemos el listener
        solicitudListener?.let {
            if (solicitudId.isNotEmpty()) {
                FirebaseDatabase.getInstance()
                    .getReference("solicitudes")
                    .child(solicitudId)
                    .removeEventListener(it)
            }
        }
        if (::mapView.isInitialized) mapView.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (::mapView.isInitialized) mapView.onResume()
    }
}