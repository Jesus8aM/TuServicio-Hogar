package com.example.tuserviciohogar

import android.os.Bundle
import android.view.Menu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.tuserviciohogar.databinding.ActivityMainBinding
import com.example.tuserviciohogar.utils.SessionManager
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.app.AppCompatDelegate

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        setSupportActionBar(binding.appBarMain.toolbar)
        binding.appBarMain.fab.hide()

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,
                R.id.nav_home_guest,
                R.id.nav_gallery,
                R.id.nav_slideshow,
                R.id.nav_map,
                R.id.nav_viaje,
                R.id.nav_tec_home,
                R.id.nav_tec_solicitudes,
                R.id.nav_tec_servicio_activo,
                R.id.nav_tec_mapa,
                R.id.nav_tec_servicios,
                R.id.nav_tec_perfil,
                R.id.nav_servicio_en_curso,
                R.id.nav_splash,
            ), drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Recargar menú y encabezado cada vez que cambia el destino
        navController.addOnDestinationChangedListener { _, _, _ ->
            actualizarMenu(navView)
        }

        // Bloquear navegación según rol
        navView.setNavigationItemSelectedListener { item ->
            val rutasCliente = setOf(
                R.id.nav_home, R.id.nav_gallery,
                R.id.nav_map, R.id.nav_slideshow
            )
            val rutasTecnico = setOf(
                R.id.nav_tec_home, R.id.nav_tec_solicitudes,
                R.id.nav_tec_mapa, R.id.nav_tec_servicios, R.id.nav_tec_perfil,
                R.id.nav_tec_servicio_activo,R.id.nav_tec_servicio_activo,
            )

            when {
                item.itemId == R.id.nav_select_role -> {
                    drawerLayout.closeDrawers()
                    navController.navigate(R.id.nav_select_role)
                }
                (item.itemId in rutasCliente || item.itemId in rutasTecnico)
                        && !sessionManager.isLoggedIn() -> {
                    drawerLayout.closeDrawers()
                    navController.navigate(R.id.nav_select_role)
                }
                else -> {
                    drawerLayout.closeDrawers()
                    navController.navigate(item.itemId)
                }
            }
            true
        }
    }

    private fun actualizarMenu(navView: NavigationView) {
        navView.menu.clear()
        when {
            !sessionManager.isLoggedIn() ->
                navView.inflateMenu(R.menu.guest_drawer)
            sessionManager.getRole() == "tecnico" ->
                navView.inflateMenu(R.menu.tecnico_drawer)
            else ->
                navView.inflateMenu(R.menu.activity_main_drawer)
        }

        val headerView = navView.getHeaderView(0)
        val tvNombre = headerView.findViewById<TextView>(R.id.tvNombreUsuario)
        val tvEmail = headerView.findViewById<TextView>(R.id.tvEmailUsuario)

        if (sessionManager.isLoggedIn()) {
            tvNombre.text = sessionManager.getName()
            tvEmail.text = sessionManager.getEmail()
        } else {
            tvNombre.text = "TuServicio Hogar"
            tvEmail.text = "Inicia sesión para continuar"
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}