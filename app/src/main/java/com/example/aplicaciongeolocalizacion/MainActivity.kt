package com.example.aplicaciongeolocalizacion

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.ActionBarDrawerToggle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.aplicaciongeolocalizacion.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import java.util.concurrent.TimeUnit

class MainActivity :  AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener{

    lateinit var fragmentManager: FragmentManager
    lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        var toggle =
            ActionBarDrawerToggle(this,binding.drawerLayout,binding.toolbar,R.string.open,R.string.close)
                binding.drawerLayout.addDrawerListener(toggle)
                toggle.syncState()
                binding.navView.setNavigationItemSelectedListener(this)
                fragmentManager = supportFragmentManager
                openFragment(fragment_ubicacion())

        val locationWorkRequest = PeriodicWorkRequestBuilder<LocationWorker>(120, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "LocationWorker",
            ExistingPeriodicWorkPolicy.REPLACE,
            locationWorkRequest
        )
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawerLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


    }
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.nav_miubicacion -> openFragment(fragment_ubicacion())
            R.id.nav_mihistorialubicacion -> openFragment(fragment_historico())
            R.id.nav_ubicaciontiemporeal-> openFragment(fragment_tiemporeal())
            R.id.nav_info-> openFragment(credits_fragment())
            R.id.nav_CerrarSesion -> logout()

        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
    private fun logout(){
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        SecurePreferences.borrarCredenciales(this)
        val intent = Intent(this, SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
    private fun openFragment(fragment: Fragment){
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragment_container,fragment)
        fragmentTransaction.commit()
    }
    override fun onBackPressed() {
        if(binding.drawerLayout.isDrawerOpen(GravityCompat.START)){
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }else{
            super.onBackPressed()
        }

    }
}