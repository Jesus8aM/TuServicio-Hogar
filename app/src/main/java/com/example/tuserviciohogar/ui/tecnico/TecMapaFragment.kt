package com.example.tuserviciohogar.ui.tecnico

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.example.tuserviciohogar.R
import com.example.tuserviciohogar.utils.SessionManager
import com.google.android.gms.location.*
import com.google.firebase.database.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class TecMapaFragment : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var currentMarker: Marker? = null
    private var clienteMarker: Marker? = null
    private var routeOverlay: Polyline? = null
    private var currentLocation: GeoPoint? = null
    private var solicitudActiva: DataSnapshot? = null
    private var solicitudListener: ValueEventListener? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 2001
        private const val DISTANCIA_LLEGADA_METROS = 50.0
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Configuration.getInstance().load(
            requireContext(),
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        )
        return inflater.inflate(R.layout.fragment_tec_mapa, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionManager = SessionManager(requireContext())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        mapView = view.findViewById(R.id.mapViewTecnico)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(15.0)

        view.findViewById<Button>(R.id.btnLlegarCliente).setOnClickListener {
            val btn = view.findViewById<Button>(R.id.btnLlegarCliente)
            android.util.Log.d("FINALIZAR", "Botón tocado, texto: '${btn.text}'")

            val texto = btn.text.toString()
            if (texto == "TERMINAR SERVICIO") {
                val id = solicitudActiva?.child("id")?.getValue(String::class.java) ?: ""
                if (id.isNotEmpty()) {
                    finalizarServicio(id)
                }
            } else {
                marcarLlegada(sessionManager)
            }
        }

        checkLocationPermission()
        escucharSolicitudActiva(sessionManager, view)
    }

    private fun escucharSolicitudActiva(sessionManager: SessionManager, view: View) {
        val uid = sessionManager.getUid()
        val database = FirebaseDatabase.getInstance()

        solicitudListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return

                val solicitud = snapshot.children.firstOrNull {
                    it.child("tecnicoUid").getValue(String::class.java) == uid &&
                            it.child("estado").getValue(String::class.java) == "aceptada"
                }

                if (solicitud != null) {
                    solicitudActiva = solicitud
                    val lat = solicitud.child("lat").getValue(Float::class.java) ?: 0f
                    val lon = solicitud.child("lon").getValue(Float::class.java) ?: 0f
                    val clienteNombre = solicitud.child("clienteNombre")
                        .getValue(String::class.java) ?: ""
                    val direccion = solicitud.child("direccion")
                        .getValue(String::class.java) ?: ""

                    val clienteGeoPoint = GeoPoint(lat.toDouble(), lon.toDouble())

                    // Mostrar info del cliente
                    view.findViewById<View>(R.id.layoutInfoCliente).visibility = View.VISIBLE
                    view.findViewById<Button>(R.id.btnLlegarCliente).visibility = View.VISIBLE
                    view.findViewById<TextView>(R.id.tvNombreCliente).text = "👤 $clienteNombre"
                    view.findViewById<TextView>(R.id.tvDireccionCliente).text = "📍 $direccion"

                    // Marcador del cliente
                    clienteMarker?.let { mapView.overlays.remove(it) }
                    clienteMarker = Marker(mapView).apply {
                        position = clienteGeoPoint
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Domicilio: $clienteNombre"
                    }
                    mapView.overlays.add(clienteMarker)

                    // Dibujar ruta si ya tenemos ubicación actual
                    currentLocation?.let { dibujarRuta(it, clienteGeoPoint) }

                    mapView.invalidate()
                } else {
                    // No hay solicitud activa
                    view.findViewById<View>(R.id.layoutInfoCliente).visibility = View.GONE
                    view.findViewById<Button>(R.id.btnLlegarCliente).visibility = View.GONE
                    clienteMarker?.let { mapView.overlays.remove(it) }
                    routeOverlay?.let { mapView.overlays.remove(it) }
                    mapView.invalidate()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        database.getReference("solicitudes")
            .addValueEventListener(solicitudListener!!)
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates()
        } else {
            Toast.makeText(requireContext(),
                "Permiso de ubicación necesario", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 5000L
        ).setMinUpdateIntervalMillis(3000L).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    updateTecnicoLocation(location)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(request, locationCallback,
            requireActivity().mainLooper)
    }

    private fun updateTecnicoLocation(location: Location) {
        if (!isAdded) return
        val geoPoint = GeoPoint(location.latitude, location.longitude)
        currentLocation = geoPoint

        mapView.controller.animateTo(geoPoint)

        currentMarker?.let { mapView.overlays.remove(it) }
        currentMarker = Marker(mapView).apply {
            position = geoPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Mi ubicación"
        }
        mapView.overlays.add(currentMarker)

        // Actualizar ubicación del técnico en Firebase
        solicitudActiva?.let { solicitud ->
            val id = solicitud.child("id").getValue(String::class.java) ?: return
            FirebaseDatabase.getInstance()
                .getReference("solicitudes").child(id)
                .updateChildren(mapOf(
                    "tecnicoLat" to location.latitude,
                    "tecnicoLon" to location.longitude
                ))

            // Verificar si llegó al domicilio
            val clienteLat = solicitud.child("lat").getValue(Float::class.java) ?: 0f
            val clienteLon = solicitud.child("lon").getValue(Float::class.java) ?: 0f
            val clienteGeoPoint = GeoPoint(clienteLat.toDouble(), clienteLon.toDouble())

            dibujarRuta(geoPoint, clienteGeoPoint)

            val distancia = geoPoint.distanceToAsDouble(clienteGeoPoint)
            if (distancia <= DISTANCIA_LLEGADA_METROS) {
                view?.findViewById<Button>(R.id.btnLlegarCliente)?.text =
                    "HE LLEGADO AL DOMICILIO"
            }
        }

        mapView.invalidate()
    }

    private fun dibujarRuta(origen: GeoPoint, destino: GeoPoint) {
        routeOverlay?.let { mapView.overlays.remove(it) }
        routeOverlay = Polyline().apply {
            addPoint(origen)
            addPoint(destino)
            color = Color.parseColor("#1565C0")
            width = 8f
        }
        mapView.overlays.add(routeOverlay)
        mapView.invalidate()
    }

    private fun marcarLlegada(sessionManager: SessionManager) {
        val solicitud = solicitudActiva ?: return
        val id = solicitud.child("id").getValue(String::class.java) ?: return

        FirebaseDatabase.getInstance()
            .getReference("solicitudes").child(id)
            .updateChildren(mapOf("estado" to "en_curso"))
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "Has llegado al domicilio",
                    Toast.LENGTH_SHORT
                ).show()

                val bundle = Bundle().apply {
                    putString("solicitudId", id)
                }

                androidx.navigation.fragment.NavHostFragment
                    .findNavController(this)
                    .navigate(R.id.nav_servicio_en_curso, bundle)
            }
    }

    private fun finalizarServicio(solicitudId: String) {
        val solicitud = solicitudActiva ?: run {
            android.util.Log.e("FINALIZAR", "solicitudActiva es null")
            return
        }
        val database = FirebaseDatabase.getInstance()
        val sessionManager = SessionManager(requireContext())

        val clienteUid = solicitud.child("clienteUid").getValue(String::class.java) ?: run {
            android.util.Log.e("FINALIZAR", "clienteUid es null")
            return
        }

        android.util.Log.d("FINALIZAR", "clienteUid: $clienteUid")
        android.util.Log.d("FINALIZAR", "solicitudId: $solicitudId")

        val clienteNombre = solicitud.child("clienteNombre").getValue(String::class.java) ?: ""
        val direccion = solicitud.child("direccion").getValue(String::class.java) ?: ""
        val descripcion = solicitud.child("descripcion").getValue(String::class.java) ?: ""
        val categoria = solicitud.child("tecnicoCategoria").getValue(String::class.java) ?: ""
        val metodoPago = solicitud.child("metodoPago").getValue(String::class.java) ?: ""

        android.util.Log.d("FINALIZAR", "categoria: $categoria, descripcion: $descripcion")

        val servicioCompletado = mapOf(
            "id" to solicitudId,
            "direccion" to direccion,
            "descripcion" to descripcion,
            "categoria" to categoria,
            "metodoPago" to metodoPago,
            "fecha" to System.currentTimeMillis(),
            "estado" to "completada",
            "clienteNombre" to clienteNombre,
            "tecnicoNombre" to sessionManager.getName()
        )

        database.getReference("solicitudes").child(solicitudId)
            .updateChildren(mapOf("estado" to "completada"))

        database.getReference("historial").child(clienteUid).child(solicitudId)
            .setValue(servicioCompletado)
            .addOnSuccessListener {
                android.util.Log.d("FINALIZAR", "historial cliente guardado OK")
            }
            .addOnFailureListener {
                android.util.Log.e("FINALIZAR", "Error historial cliente: ${it.message}")
            }

        database.getReference("historial_tecnico")
            .child(sessionManager.getUid()).child(solicitudId)
            .setValue(servicioCompletado)
            .addOnSuccessListener {
                if (!isAdded) return@addOnSuccessListener
                android.util.Log.d("FINALIZAR", "historial tecnico guardado OK")
                Toast.makeText(requireContext(), "¡Servicio completado!", Toast.LENGTH_SHORT).show()
                androidx.navigation.fragment.NavHostFragment
                    .findNavController(this)
                    .navigate(R.id.nav_gallery)
            }
            .addOnFailureListener {
                android.util.Log.e("FINALIZAR", "Error historial tecnico: ${it.message}")
            }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        solicitudListener?.let {
            FirebaseDatabase.getInstance()
                .getReference("solicitudes")
                .removeEventListener(it)
        }
    }
}