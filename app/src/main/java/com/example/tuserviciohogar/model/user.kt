package com.example.tuserviciohogar.model

data class user(val uid: String = "",
                val name: String = "",
                val email: String = "",
                val phone: String = "",
                val role: String = "",       // "cliente" o "tecnico"
                val category: String = "",   // solo para técnicos
                val rating: Float = 0f,
                val isAvailable: Boolean = false
)
