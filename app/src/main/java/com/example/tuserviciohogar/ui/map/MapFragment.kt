package com.example.tuserviciohogar.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.example.tuserviciohogar.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import android.widget.TextView

class MapFragment : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocationMarker: Marker? = null
    private var domicilioMarker: Marker? = null
    private var currentLocation: GeoPoint? = null
    private var domicilioSeleccionado: GeoPoint? = null
    private var etDomicilio: TextInputEditText? = null
    private var categoriaServicio: String = ""

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1001
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Configuration.getInstance().load(
            requireContext(),
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        )
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        mapView = view.findViewById(R.id.mapView)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(15.0)

        // Recibir categoría desde Home
        categoriaServicio = arguments?.getString("categoria") ?: ""
        val tvCategoria = view.findViewById<TextView>(R.id.tvCategoriaServicio)
        if (categoriaServicio.isNotEmpty()) {
            tvCategoria.text = "Servicio: $categoriaServicio"
            tvCategoria.visibility = View.VISIBLE
        }

        val tvTarifaBase = view.findViewById<TextView>(R.id.tvTarifaBase)

        if (categoriaServicio.isNotEmpty()) {
            val tarifaBase = obtenerTarifaBase(categoriaServicio)
            tvTarifaBase.text = "Tarifa base estimada: $${"%.2f".format(tarifaBase)}"
            tvTarifaBase.visibility = View.VISIBLE
        } else {
            tvTarifaBase.visibility = View.GONE
        }
        val btnConfirmar = view.findViewById<Button>(R.id.btnConfirmarDestino)
        etDomicilio = view.findViewById(R.id.etDestino)
        val etDescripcion = view.findViewById<TextInputEditText>(R.id.etDescripcion)

        val btnBuscar = view.findViewById<Button>(R.id.btnBuscarDireccion)
        btnBuscar.setOnClickListener {
            val direccion = etDomicilio?.text.toString().trim()
            if (direccion.isEmpty()) {
                Toast.makeText(requireContext(),
                    "Escribe una dirección para buscar", Toast.LENGTH_SHORT).show()
            } else {
                buscarSoloDireccion(direccion)
            }
        }
        // Toque en el mapa para marcar domicilio
        mapView.overlays.add(object : org.osmdroid.views.overlay.Overlay() {
            override fun onSingleTapConfirmed(
                e: android.view.MotionEvent?,
                mapView: MapView?
            ): Boolean {
                e?.let {
                    val geoPoint = mapView?.projection?.fromPixels(
                        e.x.toInt(), e.y.toInt()
                    ) as? GeoPoint
                    geoPoint?.let { punto ->
                        CoroutineScope(Dispatchers.IO).launch {
                            val nombre = obtenerNombreDireccion(punto)
                            withContext(Dispatchers.Main) {
                                marcarDomicilio(punto, nombre)
                                etDomicilio?.setText(nombre)
                            }
                        }
                    }
                }
                return true
            }
        })

        btnConfirmar.setOnClickListener {
            val direccion   = etDomicilio?.text.toString().trim()
            val descripcion = etDescripcion.text.toString().trim()

            if (direccion.isEmpty()) {
                Toast.makeText(requireContext(),
                    "Escribe tu dirección o toca el mapa", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (descripcion.isEmpty()) {
                Toast.makeText(requireContext(),
                    "Describe el servicio que necesitas", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (domicilioSeleccionado != null) {
                irASolicitar(direccion, descripcion, domicilioSeleccionado!!)
            } else {
                buscarDireccion(direccion, descripcion)
            }
        }

        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getCurrentLocation()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            getCurrentLocation()
        } else {
            Toast.makeText(requireContext(),
                "Permiso de ubicación necesario", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                currentLocation = GeoPoint(it.latitude, it.longitude)
                mapView.controller.animateTo(currentLocation)

                currentLocationMarker?.let { m -> mapView.overlays.remove(m) }
                currentLocationMarker = Marker(mapView).apply {
                    position = currentLocation
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "Mi ubicación actual"
                }
                mapView.overlays.add(currentLocationMarker)
                mapView.invalidate()
            } ?: Toast.makeText(requireContext(),
                "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show()
        }
    }

    private fun marcarDomicilio(punto: GeoPoint, nombre: String) {
        domicilioSeleccionado = punto

        // Quitar marcador de domicilio anterior si existe
        domicilioMarker?.let { mapView.overlays.remove(it) }

        // Quitar marcador de ubicación actual
        currentLocationMarker?.let { mapView.overlays.remove(it) }

        // Crear un solo marcador que representa el domicilio seleccionado
        domicilioMarker = Marker(mapView).apply {
            position = punto
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = nombre
        }
        currentLocationMarker = domicilioMarker
        mapView.overlays.add(domicilioMarker)
        mapView.controller.animateTo(punto)
        mapView.invalidate()

        view?.findViewById<Button>(R.id.btnConfirmarDestino)?.text = "Solicitar servicio aquí"
    }

    private suspend fun obtenerNombreDireccion(punto: GeoPoint): String {
        return try {
            val url = "https://nominatim.openstreetmap.org/reverse?lat=${punto.latitude}&lon=${punto.longitude}&format=json"
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.setRequestProperty("User-Agent", requireContext().packageName)
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val response = connection.inputStream.bufferedReader().readText()
            val json = org.json.JSONObject(response)
            json.optString("display_name", "${punto.latitude}, ${punto.longitude}")
        } catch (e: Exception) {
            "${punto.latitude}, ${punto.longitude}"
        }
    }

    private fun buscarDireccion(direccion: String, descripcion: String) {
        Toast.makeText(requireContext(), "Buscando dirección...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "https://nominatim.openstreetmap.org/search?q=${
                    java.net.URLEncoder.encode(direccion, "UTF-8")
                }&format=json&limit=1&countrycodes=mx"

                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.setRequestProperty("User-Agent", requireContext().packageName)
                connection.setRequestProperty("Accept-Language", "es")
                connection.connectTimeout = 8000
                connection.readTimeout = 8000

                val response = connection.inputStream.bufferedReader().readText()
                val json = org.json.JSONArray(response)

                withContext(Dispatchers.Main) {
                    if (json.length() > 0) {
                        val lugar = json.getJSONObject(0)
                        val lat = lugar.getDouble("lat")
                        val lon = lugar.getDouble("lon")
                        val nombre = lugar.getString("display_name")
                        val geoPoint = GeoPoint(lat, lon)

                        mapView.controller.animateTo(geoPoint)
                        marcarDomicilio(geoPoint, nombre)
                        etDomicilio?.setText(nombre)
                        irASolicitar(nombre, descripcion, geoPoint)
                    } else {
                        Toast.makeText(requireContext(),
                            "No se encontró, intenta con una dirección más específica",
                            Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(),
                        "Error de conexión al buscar dirección", Toast.LENGTH_SHORT).show()
                }
            }
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
    private fun irASolicitar(direccion: String, descripcion: String, domicilio: GeoPoint) {
        val bundle = Bundle().apply {
            putString("direccion", direccion)
            putString("descripcion", descripcion)
            putString("categoria", categoriaServicio)
            putFloat("domicilioLat", domicilio.latitude.toFloat())
            putFloat("domicilioLon", domicilio.longitude.toFloat())
        }
        findNavController().navigate(R.id.action_map_to_pago, bundle)
    }

    private fun buscarSoloDireccion(direccion: String) {
        Toast.makeText(requireContext(), "Buscando...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "https://nominatim.openstreetmap.org/search?q=${
                    java.net.URLEncoder.encode(direccion, "UTF-8")
                }&format=json&limit=1&countrycodes=mx"

                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.setRequestProperty("User-Agent", requireContext().packageName)
                connection.setRequestProperty("Accept-Language", "es")
                connection.connectTimeout = 8000
                connection.readTimeout = 8000

                val response = connection.inputStream.bufferedReader().readText()
                val json = org.json.JSONArray(response)

                withContext(Dispatchers.Main) {
                    if (json.length() > 0) {
                        val lugar = json.getJSONObject(0)
                        val lat = lugar.getDouble("lat")
                        val lon = lugar.getDouble("lon")
                        val nombre = lugar.getString("display_name")
                        val geoPoint = GeoPoint(lat, lon)

                        mapView.controller.animateTo(geoPoint)
                        marcarDomicilio(geoPoint, nombre)
                        etDomicilio?.setText(nombre)
                        Toast.makeText(requireContext(),
                            "Dirección encontrada", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(),
                            "No se encontró, intenta con una dirección más específica",
                            Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(),
                        "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}