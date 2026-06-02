package com.example.tuserviciohogar.ui.auth

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.tuserviciohogar.R
import com.example.tuserviciohogar.utils.SessionManager
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterFragment : Fragment() {

    private lateinit var sessionManager: SessionManager
    private var userRole: String = "cliente"
    private var selectedCategory: String = ""
    private var cvUri: Uri? = null
    private lateinit var auth: FirebaseAuth

    private val pickPdf = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            cvUri = result.data?.data
            cvUri?.let { uri ->
                val nombre = obtenerNombreArchivo(uri)
                view?.findViewById<TextView>(R.id.tvCvSeleccionado)?.text = "📄 $nombre"
                view?.findViewById<TextView>(R.id.tvCvSeleccionado)?.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        auth = FirebaseAuth.getInstance()
        userRole = arguments?.getString("role") ?: "cliente"

        val layoutCategory = view.findViewById<View>(R.id.layoutCategory)
        val layoutCV = view.findViewById<View>(R.id.layoutCV)

        layoutCategory.visibility = if (userRole == "tecnico") View.VISIBLE else View.GONE
        layoutCV.visibility = if (userRole == "tecnico") View.VISIBLE else View.GONE

        configurarCategorias(view)

        view.findViewById<Button>(R.id.btnSeleccionarCV).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "application/pdf"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            pickPdf.launch(intent)
        }

        view.findViewById<Button>(R.id.btnRegister).setOnClickListener {
            val name = view.findViewById<TextInputEditText>(R.id.etName).text.toString().trim()
            val email = view.findViewById<TextInputEditText>(R.id.etEmail).text.toString().trim()
            val phone = view.findViewById<TextInputEditText>(R.id.etPhone).text.toString().trim()
            val password = view.findViewById<TextInputEditText>(R.id.etPassword).text.toString().trim()
            val category = if (userRole == "tecnico") selectedCategory else ""

            if (userRole == "tecnico" && selectedCategory.isEmpty()) {
                Toast.makeText(requireContext(), "Selecciona una especialidad", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (userRole == "tecnico" && cvUri == null) {
                Toast.makeText(requireContext(), "Por favor sube tu CV en PDF", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (validateInputs(name, email, phone, password)) {
                registrarEnFirebase(name, email, phone, password, category)
            }
        }
    }

    private fun configurarCategorias(view: View) {
        val categorias = mapOf(
            R.id.cardPlomeria to "Plomería",
            R.id.cardElectricidad to "Electricidad",
            R.id.cardCarpinteria to "Carpintería",
            R.id.cardPintura to "Pintura",
            R.id.cardCerrajeria to "Cerrajería",
            R.id.cardAire to "Aire Acondicionado"
        )

        categorias.forEach { (cardId, categoria) ->
            view.findViewById<MaterialCardView>(cardId).setOnClickListener {
                selectedCategory = categoria
                limpiarSeleccionCategorias(view)
                marcarCategoriaSeleccionada(view.findViewById(cardId))
            }
        }
    }

    private fun limpiarSeleccionCategorias(view: View) {
        val ids = listOf(
            R.id.cardPlomeria,
            R.id.cardElectricidad,
            R.id.cardCarpinteria,
            R.id.cardPintura,
            R.id.cardCerrajeria,
            R.id.cardAire
        )

        ids.forEach { id ->
            view.findViewById<MaterialCardView>(id).apply {
                strokeColor = resources.getColor(R.color.border, null)
                strokeWidth = 1
                setCardBackgroundColor(resources.getColor(R.color.surface, null))
            }
        }
    }

    private fun marcarCategoriaSeleccionada(card: MaterialCardView) {
        card.strokeColor = resources.getColor(R.color.primary, null)
        card.strokeWidth = 4
        card.setCardBackgroundColor(resources.getColor(R.color.primary_light, null))
    }

    private fun registrarEnFirebase(
        name: String, email: String, phone: String,
        password: String, category: String
    ) {
        Toast.makeText(requireContext(), "Registrando...", Toast.LENGTH_SHORT).show()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener

                val cvNombre = if (cvUri != null) obtenerNombreArchivo(cvUri!!) else ""

                if (cvUri != null && cvNombre.isNotEmpty()) {
                    copiarPdfInterno(cvUri!!, cvNombre)
                }

                val database = FirebaseDatabase.getInstance()
                val userRef = database.getReference("usuarios").child(uid)

                val userData = mapOf(
                    "uid" to uid,
                    "name" to name,
                    "email" to email,
                    "phone" to phone,
                    "role" to userRole,
                    "category" to category,
                    "cvFileName" to cvNombre,
                    "isAvailable" to (userRole == "tecnico"),
                    "rating" to 5.0,
                    "ratingSum" to 0.0,
                    "ratingCount" to 0
                )

                userRef.setValue(userData).addOnSuccessListener {
                    sessionManager.saveRegistration(name, email, phone, password, userRole, category)
                    sessionManager.saveSession(email, userRole, name)
                    sessionManager.saveCvInfo(cvNombre, cvUri?.toString() ?: "")
                    sessionManager.saveUid(uid)

                    Toast.makeText(requireContext(), "¡Registro exitoso!", Toast.LENGTH_SHORT).show()

                    if (userRole == "tecnico") {
                        findNavController().navigate(R.id.nav_tec_home)
                    } else {
                        findNavController().navigate(R.id.nav_home)
                    }
                }.addOnFailureListener {
                    Toast.makeText(requireContext(), "Error guardando datos", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun copiarPdfInterno(uri: Uri, nombre: String) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val destFile = java.io.File(requireContext().filesDir, nombre)
            val outputStream = java.io.FileOutputStream(destFile)

            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun obtenerNombreArchivo(uri: Uri): String {
        return try {
            val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                it.moveToFirst()
                it.getString(index)
            } ?: "documento.pdf"
        } catch (e: Exception) {
            "documento.pdf"
        }
    }

    private fun validateInputs(
        name: String, email: String, phone: String, password: String
    ): Boolean {
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Ingresa tu nombre", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "Email inválido", Toast.LENGTH_SHORT).show()
            return false
        }

        if (phone.length < 10) {
            Toast.makeText(requireContext(), "Teléfono inválido", Toast.LENGTH_SHORT).show()
            return false
        }

        if (password.length < 6) {
            Toast.makeText(requireContext(), "Mínimo 6 caracteres", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }
}