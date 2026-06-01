package com.example.tuserviciohogar.ui.auth

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
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot

class LoginFragment : Fragment() {

    private lateinit var sessionManager: SessionManager
    private var userRole: String = "cliente"
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        auth = FirebaseAuth.getInstance()
        userRole = arguments?.getString("role") ?: "cliente"



        view.findViewById<TextView>(R.id.tvRoleLabel).text =
            "Entrar como ${userRole.replaceFirstChar { it.uppercase() }}"

        view.findViewById<Button>(R.id.btnLogin).setOnClickListener {
            val email    = view.findViewById<TextInputEditText>(R.id.etEmail).text.toString().trim()
            val password = view.findViewById<TextInputEditText>(R.id.etPassword).text.toString().trim()

            if (validateInputs(email, password)) {
                loginConFirebase(email, password)
            }
        }

        view.findViewById<TextView>(R.id.tvGoRegister).setOnClickListener {
            val bundle = Bundle().apply { putString("role", userRole) }
            findNavController().navigate(R.id.action_login_to_register, bundle)
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            Toast.makeText(requireContext(), "Ingresa tu email", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "Email inválido", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.length < 6) {
            Toast.makeText(requireContext(), "Mínimo 6 caracteres", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun loginConFirebase(email: String, password: String) {
        Toast.makeText(requireContext(), "Iniciando sesión...", Toast.LENGTH_SHORT).show()

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener

                // Obtener datos del usuario desde Realtime Database
                val database = FirebaseDatabase.getInstance()
                database.getReference("usuarios").child(uid)
                    .get().addOnSuccessListener { snapshot ->
                        val name     = snapshot.child("name").getValue(String::class.java) ?: ""
                        val role     = snapshot.child("role").getValue(String::class.java) ?: userRole
                        val phone    = snapshot.child("phone").getValue(String::class.java) ?: ""
                        val category = snapshot.child("category").getValue(String::class.java) ?: ""
                        val cvFileName = snapshot.child("cvFileName").getValue(String::class.java) ?: ""

                        sessionManager.saveSession(email, role, name)
                        sessionManager.saveRegistration(name, email, phone, "", role, category)
                        sessionManager.saveCvInfo(cvFileName, "")
                        sessionManager.saveUid(uid)

                        if (role == "tecnico") {
                            findNavController().navigate(R.id.nav_tec_home)
                        } else {
                            // Verificar si tiene una solicitud activa
                            database.getReference("solicitudes")
                                .orderByChild("clienteUid")
                                .equalTo(uid)
                                .get()
                                .addOnSuccessListener { solicitudes ->
                                    var solicitudActiva: DataSnapshot? = null

                                    for (solicitud in solicitudes.children) {
                                        val estado = solicitud.child("estado")
                                            .getValue(String::class.java) ?: ""
                                        if (estado == "pendiente" || estado == "aceptada") {
                                            solicitudActiva = solicitud
                                            break
                                        }
                                    }

                                    if (solicitudActiva != null) {
                                        val solicitudId  = solicitudActiva.child("id")
                                            .getValue(String::class.java) ?: ""
                                        val metodoPago   = solicitudActiva.child("metodoPago")
                                            .getValue(String::class.java) ?: ""
                                        val descripcion  = solicitudActiva.child("descripcion")
                                            .getValue(String::class.java) ?: ""
                                        val lat = solicitudActiva.child("lat")
                                            .getValue(Float::class.java) ?: 0f
                                        val lon = solicitudActiva.child("lon")
                                            .getValue(Float::class.java) ?: 0f

                                        val bundle = Bundle().apply {
                                            putString("solicitudId", solicitudId)
                                            putString("metodoPago", metodoPago)
                                            putString("descripcion", descripcion)
                                            putFloat("domicilioLat", lat)
                                            putFloat("domicilioLon", lon)
                                        }
                                        findNavController().navigate(R.id.nav_viaje, bundle)
                                    } else {
                                        findNavController().navigate(R.id.nav_home)
                                    }
                                }.addOnFailureListener {
                                    findNavController().navigate(R.id.nav_home)
                                }
                        }
            }}
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(),
                    "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
            }
    }
}