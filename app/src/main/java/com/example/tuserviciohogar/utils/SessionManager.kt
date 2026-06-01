package com.example.tuserviciohogar.utils

import android.content.Context

class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)

    fun saveRegistration(
        name: String, email: String, phone: String,
        password: String, role: String, category: String
    ) {
        prefs.edit().apply {
            putString("name", name)
            putString("email", email)
            putString("phone", phone)
            putString("password", password)
            putString("role", role)
            putString("category", category)
            apply()
        }
    }

    fun saveSession(email: String, role: String, name: String) {
        prefs.edit().apply {
            putBoolean("is_logged_in", true)
            putString("email", email)
            putString("role", role)
            putString("name", name)
            apply()
        }
    }
    fun saveCvInfo(nombre: String, uri: String) {
        prefs.edit().apply {
            putString("cv_file_name", nombre)
            putString("cv_file_uri", uri)
            apply()
        }
    }

    fun getCvUri() = prefs.getString("cv_file_uri", "") ?: ""

    fun isLoggedIn() = prefs.getBoolean("is_logged_in", false)
    fun getRole()    = prefs.getString("role", "") ?: ""
    fun getName()    = prefs.getString("name", "") ?: ""
    fun getEmail()   = prefs.getString("email", "") ?: ""
    fun getPassword()= prefs.getString("password", "") ?: ""

    fun getPhone() = prefs.getString("phone", "") ?: ""
    fun getCategory() = prefs.getString("category", "") ?: ""
    fun logout() = prefs.edit().clear().apply()
    fun isDisponible() = prefs.getBoolean("is_disponible", true)
    fun setDisponible(value: Boolean) = prefs.edit().putBoolean("is_disponible", value).apply()
    fun getCvFileName() = prefs.getString("cv_file_name", "") ?: ""
    fun saveUid(uid: String) = prefs.edit().putString("uid", uid).apply()
    fun getUid() = prefs.getString("uid", "") ?: ""
}