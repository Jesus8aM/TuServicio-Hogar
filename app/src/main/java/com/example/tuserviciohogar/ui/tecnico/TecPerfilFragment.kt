package com.example.tuserviciohogar.ui.tecnico

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.tuserviciohogar.R
import com.example.tuserviciohogar.utils.SessionManager
import java.io.File

class TecPerfilFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tec_perfil, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionManager = SessionManager(requireContext())

        view.findViewById<TextView>(R.id.tvTecNombre).text = sessionManager.getName()
        view.findViewById<TextView>(R.id.tvTecEspecialidad).text = sessionManager.getCategory()
        view.findViewById<TextView>(R.id.tvTecEmail).text = sessionManager.getEmail()
        view.findViewById<TextView>(R.id.tvTecTelefono).text = sessionManager.getPhone()

        val tvCV = view.findViewById<TextView>(R.id.tvTecCV)
        val cvNombre = sessionManager.getCvFileName()

        if (cvNombre.isNotEmpty()) {
            tvCV.text = "📄 $cvNombre"
            tvCV.setOnClickListener {
                abrirCV(cvNombre)
            }
        } else {
            tvCV.text = "📄 Sin CV subido"
        }

        view.findViewById<Button>(R.id.btnTecCerrarSesion).setOnClickListener {
            sessionManager.logout()
            Toast.makeText(requireContext(), "Sesión cerrada", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.nav_home)
        }
    }

    private fun abrirCV(cvNombre: String) {
        try {
            // Buscar el archivo en almacenamiento interno de la app
            val cvFile = File(requireContext().filesDir, cvNombre)

            if (!cvFile.exists()) {
                Toast.makeText(requireContext(),
                    "Archivo no encontrado", Toast.LENGTH_SHORT).show()
                return
            }

            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                cvFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_ACTIVITY_NO_HISTORY
            }

            startActivity(Intent.createChooser(intent, "Abrir CV con..."))

        } catch (e: Exception) {
            Toast.makeText(requireContext(),
                "No se pudo abrir el CV", Toast.LENGTH_SHORT).show()
        }
    }
}