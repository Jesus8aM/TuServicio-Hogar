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
import com.google.firebase.database.*

class ServicioEnCursoFragment : Fragment() {

    private var solicitudId: String = ""
    private var servicioListener: ValueEventListener? = null
    private var servicioRef: DatabaseReference? = null
    private var yaNavego = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_servicio_en_curso, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        solicitudId = arguments?.getString("solicitudId") ?: ""

        if (solicitudId.isEmpty()) {
            buscarServicioActivo(view)
        } else {
            escucharServicio(view, solicitudId)
        }
    }

    private fun buscarServicioActivo(view: View) {
        val sessionManager = SessionManager(requireContext())
        val uid = sessionManager.getUid()
        val role = sessionManager.getRole()

        FirebaseDatabase.getInstance()
            .getReference("solicitudes")
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener

                val servicio = snapshot.children.firstOrNull {
                    val estado = it.child("estado").getValue(String::class.java) ?: ""
                    val clienteUid = it.child("clienteUid").getValue(String::class.java) ?: ""
                    val tecnicoUid = it.child("tecnicoUid").getValue(String::class.java) ?: ""

                    (estado == "en_curso" || estado == "pendiente_evaluacion") &&
                            ((role == "tecnico" && tecnicoUid == uid) ||
                                    (role != "tecnico" && clienteUid == uid))
                }

                if (servicio != null) {
                    solicitudId = servicio.child("id").getValue(String::class.java) ?: ""
                    escucharServicio(view, solicitudId)
                } else {
                    mostrarSinServicio(view)
                }
            }
            .addOnFailureListener {
                if (!isAdded) return@addOnFailureListener
                mostrarSinServicio(view)
            }
    }

    private fun escucharServicio(view: View, id: String) {
        val sessionManager = SessionManager(requireContext())
        val role = sessionManager.getRole()

        servicioRef = FirebaseDatabase.getInstance()
            .getReference("solicitudes")
            .child(id)

        servicioListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || yaNavego) return

                val estado = snapshot.child("estado").getValue(String::class.java) ?: ""

                if (estado == "pendiente_evaluacion") {
                    val clienteYaEvaluo = snapshot.child("clienteYaEvaluo")
                        .getValue(Boolean::class.java) ?: false

                    val tecnicoYaEvaluo = snapshot.child("tecnicoYaEvaluo")
                        .getValue(Boolean::class.java) ?: false

                    if (
                        (role == "tecnico" && !tecnicoYaEvaluo) ||
                        (role != "tecnico" && !clienteYaEvaluo)
                    ) {
                        yaNavego = true

                        val bundle = Bundle().apply {
                            putString("solicitudId", id)
                        }

                        findNavController().navigate(R.id.nav_evaluacion_servicio, bundle)
                    } else {
                        mostrarEsperandoEvaluacion(view)
                    }

                    return
                }

                if (estado == "completada" || estado == "cancelada") {
                    mostrarSinServicio(view)
                    return
                }

                val clienteNombre = snapshot.child("clienteNombre").getValue(String::class.java) ?: ""
                val tecnicoNombre = snapshot.child("tecnicoNombre").getValue(String::class.java) ?: ""
                val categoria = snapshot.child("categoria").getValue(String::class.java) ?: ""
                val descripcion = snapshot.child("descripcion").getValue(String::class.java) ?: ""
                val direccion = snapshot.child("direccion").getValue(String::class.java) ?: ""

                val montoBase =
                    snapshot.child("montoBase")
                        .getValue(Double::class.java) ?: 0.0

                val montoAdicional =
                    snapshot.child("montoAdicional")
                        .getValue(Double::class.java) ?: 0.0

                val montoTotal =
                    snapshot.child("montoTotal")
                        .getValue(Double::class.java) ?: 0.0

                val comisionApp =
                    snapshot.child("comisionApp")
                        .getValue(Double::class.java) ?: 0.0

                val gananciaTecnico =
                    snapshot.child("gananciaTecnico")
                        .getValue(Double::class.java) ?: 0.0

                val montoAceptado =
                    snapshot.child("montoAceptado")
                        .getValue(Boolean::class.java) ?: false

                view.findViewById<TextView>(R.id.tvTituloServicio).text =
                    "Servicio en curso"

                view.findViewById<TextView>(R.id.tvClienteServicio).text =
                    "👤 Cliente: $clienteNombre"

                view.findViewById<TextView>(R.id.tvTecnicoServicio).text =
                    "👷 Técnico: $tecnicoNombre"

                view.findViewById<TextView>(R.id.tvCategoriaServicio).text =
                    "🔧 Categoría: $categoria"

                view.findViewById<TextView>(R.id.tvDescripcionServicio).text =
                    "📝 Descripción: $descripcion"

                view.findViewById<TextView>(R.id.tvDireccionServicio).text =
                    "📍 Dirección: $direccion"

                view.findViewById<TextView>(R.id.tvMontoBase).text =
                    "Tarifa base: $${"%.2f".format(montoBase)}"

                view.findViewById<TextView>(R.id.tvMontoAdicional).text =
                    "Monto adicional: $${"%.2f".format(montoAdicional)}"

                view.findViewById<TextView>(R.id.tvMontoTotal).text =
                    "Total: $${"%.2f".format(montoTotal)}"

                view.findViewById<TextView>(R.id.tvComisionApp).text =
                    "Comisión app: $${"%.2f".format(comisionApp)}"

                view.findViewById<TextView>(R.id.tvGananciaTecnico).text =
                    "Ganancia técnico: $${"%.2f".format(gananciaTecnico)}"

                val btnTerminar = view.findViewById<Button>(R.id.btnTerminarServicio)

                val btnAceptarMonto =
                    view.findViewById<Button>(R.id.btnAceptarMonto)

                val btnEnviarMonto =
                    view.findViewById<Button>(R.id.btnEnviarMonto)

                val layoutMontoTecnico =
                    view.findViewById<View>(R.id.layoutMontoTecnico)

                if (role == "tecnico") {
                    btnTerminar.visibility = View.VISIBLE
                    btnTerminar.setOnClickListener {
                        finalizarServicio(id)
                    }
                } else {
                    btnTerminar.visibility = View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isAdded) return

                Toast.makeText(
                    requireContext(),
                    "Error cargando servicio",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        servicioRef?.addValueEventListener(servicioListener!!)
    }

    private fun finalizarServicio(id: String) {
        FirebaseDatabase.getInstance()
            .getReference("solicitudes")
            .child(id)
            .updateChildren(
                mapOf(
                    "estado" to "pendiente_evaluacion",
                    "fechaFinalizacion" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener {
                val bundle = Bundle().apply {
                    putString("solicitudId", id)
                }

                findNavController().navigate(R.id.nav_evaluacion_servicio, bundle)
            }
    }

    private fun mostrarEsperandoEvaluacion(view: View) {
        view.findViewById<TextView>(R.id.tvTituloServicio).text =
            "Evaluación enviada"

        view.findViewById<TextView>(R.id.tvClienteServicio).text =
            "Esperando evaluación de la otra parte"

        view.findViewById<TextView>(R.id.tvTecnicoServicio).text = ""
        view.findViewById<TextView>(R.id.tvCategoriaServicio).text = ""
        view.findViewById<TextView>(R.id.tvDescripcionServicio).text = ""
        view.findViewById<TextView>(R.id.tvDireccionServicio).text = ""

        view.findViewById<Button>(R.id.btnTerminarServicio).visibility = View.GONE
        view.findViewById<TextView>(R.id.tvMontoBase).text = ""
        view.findViewById<TextView>(R.id.tvMontoAdicional).text = ""
        view.findViewById<TextView>(R.id.tvMontoTotal).text = ""
        view.findViewById<TextView>(R.id.tvComisionApp).text = ""
        view.findViewById<TextView>(R.id.tvGananciaTecnico).text = ""

        view.findViewById<View>(R.id.layoutMontoTecnico).visibility = View.GONE
        view.findViewById<Button>(R.id.btnAceptarMonto).visibility = View.GONE
        view.findViewById<Button>(R.id.btnEnviarMonto).visibility = View.GONE
    }

    private fun mostrarSinServicio(view: View) {
        view.findViewById<TextView>(R.id.tvTituloServicio).text =
            "No tienes un servicio activo"

        view.findViewById<TextView>(R.id.tvClienteServicio).text = ""
        view.findViewById<TextView>(R.id.tvTecnicoServicio).text = ""
        view.findViewById<TextView>(R.id.tvCategoriaServicio).text = ""
        view.findViewById<TextView>(R.id.tvDescripcionServicio).text = ""
        view.findViewById<TextView>(R.id.tvDireccionServicio).text = ""

        view.findViewById<Button>(R.id.btnTerminarServicio).visibility = View.GONE
        view.findViewById<TextView>(R.id.tvMontoBase).text = ""
        view.findViewById<TextView>(R.id.tvMontoAdicional).text = ""
        view.findViewById<TextView>(R.id.tvMontoTotal).text = ""
        view.findViewById<TextView>(R.id.tvComisionApp).text = ""
        view.findViewById<TextView>(R.id.tvGananciaTecnico).text = ""

        view.findViewById<View>(R.id.layoutMontoTecnico).visibility = View.GONE
        view.findViewById<Button>(R.id.btnAceptarMonto).visibility = View.GONE
        view.findViewById<Button>(R.id.btnEnviarMonto).visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()

        servicioListener?.let {
            servicioRef?.removeEventListener(it)
        }
    }
}