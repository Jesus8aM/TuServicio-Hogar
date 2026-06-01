package com.example.tuserviciohogar.ui.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.tuserviciohogar.R
import com.example.tuserviciohogar.utils.SessionManager
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.FirebaseDatabase

class EvaluacionServicioFragment : Fragment() {

    private var solicitudId = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_evaluacion_servicio,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        solicitudId = arguments?.getString("solicitudId") ?: ""

        val session = SessionManager(requireContext())
        val role = session.getRole()

        val tvTitulo = view.findViewById<TextView>(R.id.tvSubtituloEvaluacion)

        tvTitulo.text =
            if (role == "tecnico")
                "¿Cómo fue tu experiencia con el cliente?"
            else
                "¿Cómo fue tu experiencia con el técnico?"

        view.findViewById<Button>(R.id.btnEnviarEvaluacion)
            .setOnClickListener {

                val rating = view.findViewById<RatingBar>(
                    R.id.ratingServicio
                ).rating.toInt()

                val comentario = view.findViewById<TextInputEditText>(
                    R.id.etComentarioEvaluacion
                ).text.toString().trim()

                guardarEvaluacion(
                    role,
                    rating,
                    comentario
                )
            }
    }

    private fun guardarEvaluacion(
        role: String,
        rating: Int,
        comentario: String
    ) {

        val ref = FirebaseDatabase.getInstance()
            .getReference("solicitudes")
            .child(solicitudId)

        ref.get().addOnSuccessListener { snapshot ->

            if (role == "cliente") {

                val tecnicoUid =
                    snapshot.child("tecnicoUid")
                        .getValue(String::class.java) ?: ""

                ref.updateChildren(
                    mapOf(
                        "clienteYaEvaluo" to true,
                        "calificacionTecnico" to rating,
                        "comentarioCliente" to comentario
                    )
                )

                actualizarRatingUsuario(
                    tecnicoUid,
                    rating.toFloat()
                )

            } else {

                val clienteUid =
                    snapshot.child("clienteUid")
                        .getValue(String::class.java) ?: ""

                ref.updateChildren(
                    mapOf(
                        "tecnicoYaEvaluo" to true,
                        "calificacionCliente" to rating,
                        "comentarioTecnico" to comentario
                    )
                )

                actualizarRatingUsuario(
                    clienteUid,
                    rating.toFloat()
                )
            }

            verificarFinalizacion()
        }
    }

    private fun actualizarRatingUsuario(
        uid: String,
        nuevaCalificacion: Float
    ) {

        val userRef = FirebaseDatabase.getInstance()
            .getReference("usuarios")
            .child(uid)

        userRef.get().addOnSuccessListener { snapshot ->

            val ratingSum =
                snapshot.child("ratingSum")
                    .getValue(Double::class.java) ?: 0.0

            val ratingCount =
                snapshot.child("ratingCount")
                    .getValue(Int::class.java) ?: 0

            val nuevoSum =
                ratingSum + nuevaCalificacion

            val nuevoCount =
                ratingCount + 1

            val promedio =
                nuevoSum / nuevoCount

            userRef.updateChildren(
                mapOf(
                    "rating" to promedio,
                    "ratingSum" to nuevoSum,
                    "ratingCount" to nuevoCount
                )
            )
        }
    }

    private fun verificarFinalizacion() {

        val ref = FirebaseDatabase.getInstance()
            .getReference("solicitudes")
            .child(solicitudId)

        ref.get().addOnSuccessListener { snapshot ->

            val clienteYaEvaluo =
                snapshot.child("clienteYaEvaluo")
                    .getValue(Boolean::class.java) ?: false

            val tecnicoYaEvaluo =
                snapshot.child("tecnicoYaEvaluo")
                    .getValue(Boolean::class.java) ?: false

            if (clienteYaEvaluo && tecnicoYaEvaluo) {

                ref.child("estado")
                    .setValue("completada")

                Toast.makeText(
                    requireContext(),
                    "Evaluación registrada",
                    Toast.LENGTH_SHORT
                ).show()

                findNavController()
                    .navigate(R.id.nav_home)
            } else {

                Toast.makeText(
                    requireContext(),
                    "Esperando evaluación de la otra parte",
                    Toast.LENGTH_SHORT
                ).show()

                if (SessionManager(requireContext()).getRole() == "tecnico") {
                    findNavController().navigate(R.id.nav_tec_servicio_activo)
                } else {
                    findNavController().navigate(R.id.nav_viaje)
                }
            }
        }
    }
}