package com.example.aplicaciongeolocalizacion

import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import java.util.Date

class LocationWorker(
    context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {


    override suspend fun doWork(): Result {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)

        val location = try {
            fusedLocationClient.lastLocation.await()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        if (location == null) {
            return Result.failure()
        }

        // Extrae latitud y longitud
        val lat = location.latitude
        val lon = location.longitude

        return try {
            var(email,password)= SecurePreferences.obtenerCredenciales(applicationContext)
            val db = FirebaseFirestore.getInstance()
            if(email.toString().isEmpty() || password.toString().isEmpty()){
                return Result.failure()
            }
            val userId = FirebaseAuth.getInstance().signInWithEmailAndPassword(email.toString(),password.toString())
            val data = hashMapOf(
                "lat" to lat,
                "lon" to lon,
                "timestamp" to Date()
            )

            db.collection("usuarios").document(userId.toString())
                .collection("ubicaciones")
                .add(data)
                .await()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}