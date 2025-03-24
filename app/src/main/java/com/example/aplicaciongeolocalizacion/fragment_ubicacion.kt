package com.example.aplicaciongeolocalizacion

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.viewport.viewport
import com.google.android.material.floatingactionbutton.FloatingActionButton

class fragment_ubicacion : Fragment() {
    var userID: String? = null
    private lateinit var mapView: MapView
    private var currentLat: Double = 0.0
    private var currentLon: Double = 0.0
    private var lastUpdateTime: Long = 0
    private var lastLat: Double = 0.0
    private var lastLon: Double = 0.0
    private val updateInterval: Long = 30 * 1000
    private val distanceThreshold: Float = 5f

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_ubicacion, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userID = FirebaseAuth.getInstance().currentUser?.uid
        mapView = view.findViewById(R.id.mapViewUbi)

        mapView.mapboxMap.setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(-98.0, 39.5))
                .pitch(0.0)
                .zoom(2.0)
                .bearing(0.0)
                .build()
        )

        with(mapView) {
            location.locationPuck = createDefault2DPuck(withBearing = true)
            location.enabled = true
            location.pulsingEnabled = true
            location.puckBearing = PuckBearing.COURSE
            location.puckBearingEnabled = true
            viewport.transitionTo(
                targetState = viewport.makeFollowPuckViewportState(),
                transition = viewport.makeImmediateViewportTransition()
            )
        }


        mapView.location.addOnIndicatorPositionChangedListener { point ->
            val newLat = point.latitude()
            val newLon = point.longitude()
            currentLat = newLat
            currentLon = newLon

            val currentTime = System.currentTimeMillis()
            val distance = FloatArray(1)
            android.location.Location.distanceBetween(lastLat, lastLon, newLat, newLon, distance)

            if (currentTime - lastUpdateTime >= updateInterval || distance[0] >= distanceThreshold) {
                lastUpdateTime = currentTime
                lastLat = newLat
                lastLon = newLon
                actualizarUbicacionFirebase(newLat, newLon)
            }
        }

        val btnUpload = view.findViewById<FloatingActionButton>(R.id.btn_upload)
        btnUpload.setOnClickListener {
            try {
                actualizarUbicacionFirebase(currentLat, currentLon)
            } catch (ex: Exception) {
                Toast.makeText(requireContext(), "Error: ${ex.message}", Toast.LENGTH_SHORT).show()
            }
        }

        val btnCenter = view.findViewById<FloatingActionButton>(R.id.btn_center)
        btnCenter.setOnClickListener {
            val point = Point.fromLngLat(currentLon, currentLat)
            mapView.mapboxMap.setCamera(
                CameraOptions.Builder()
                    .center(point)
                    .zoom(15.0)
                    .build()
            )
        }
    }

    private fun actualizarUbicacionFirebase(lat: Double, lon: Double) {
        val db = FirebaseFirestore.getInstance()
        val datosUbicacion = hashMapOf(
            "lat" to lat,
            "lon" to lon,
            "timestamp" to java.util.Date()
        )

        db.collection("usuarios").document(userID.toString())
            .set(datosUbicacion)
            .addOnSuccessListener {
                // Opcional: Notificar que se actualizó correctamente
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al enviar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
