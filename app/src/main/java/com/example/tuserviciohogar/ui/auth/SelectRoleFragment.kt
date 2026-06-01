package com.example.tuserviciohogar.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.tuserviciohogar.R

class SelectRoleFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_select_role, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.btnCliente).setOnClickListener {
            val bundle = Bundle().apply { putString("role", "cliente") }
            findNavController().navigate(R.id.action_selectRole_to_login, bundle)
        }

        view.findViewById<Button>(R.id.btnTecnico).setOnClickListener {
            val bundle = Bundle().apply { putString("role", "tecnico") }
            findNavController().navigate(R.id.action_selectRole_to_login, bundle)
        }
    }
}