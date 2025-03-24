package com.example.aplicaciongeolocalizacion

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager

class fragment_tiemporeal : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var textTiempo: TextView
    private lateinit var pointAnnotationManager: PointAnnotationManager
    private var locationListener: ListenerRegistration? = null
    // Variable para almacenar la última ubicación
    private var currentPoint: Point? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tiemporeal, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView = view.findViewById(R.id.mapViewTiempoReal)
        val btnCenterCamera: FloatingActionButton = view.findViewById(R.id.btnCenterCamera)
        textTiempo = view.findViewById(R.id.TextoFecha)

        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) { style ->
            val bitmap = BitmapFactory.decodeResource(requireContext().resources, R.drawable.iconsearch)
            style.addImage("mio_icon", bitmap)
            pointAnnotationManager = mapView.annotations.createPointAnnotationManager()
            iniciarEscuchaUbicacionEnTiempoReal()
        }

        btnCenterCamera.setOnClickListener {
            currentPoint?.let { point ->
                // Centra la cámara en la última ubicación almacenada
                mapView.getMapboxMap().setCamera(
                    CameraOptions.Builder()
                        .center(point)
                        .zoom(15.0)
                        .build()
                )
            } ?: run {
                Toast.makeText(requireContext(), "Ubicación no disponible", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun iniciarEscuchaUbicacionEnTiempoReal() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "No hay usuario autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val docRef = FirebaseFirestore.getInstance()
            .collection("usuarios")
            .document(userId)

        locationListener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Toast.makeText(requireContext(), "Error al escuchar cambios: ${error.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val lat = snapshot.getDouble("lat")
                val lon = snapshot.getDouble("lon")
                val timestamp = snapshot.getTimestamp("timestamp")
                val date = timestamp?.toDate()
                if (date==null){
                    textTiempo.text ="No hay datos disponibles"
                }else{
                    val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
                    val dateString = dateFormat.format(date)
                    textTiempo.text ="Última actualización:\n$dateString"
                }



                if (lat != null && lon != null) {
                    val point = Point.fromLngLat(lon, lat)
                    // Almacena la ubicación para usarla al centrar la cámara
                    currentPoint = point
                    actualizarMapa(point)
                }
            }
        }
    }

    private fun actualizarMapa(point: Point) {
        mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .center(point)
                .zoom(15.0)
                .build()
        )

        pointAnnotationManager.deleteAll()

        val marcador = PointAnnotationOptions()
            .withPoint(point)
            .withTextField("Ubicación actual\n")
            .withIconImage("mio_icon")
            .withTextOffset(listOf(0.0, -1.5))

        pointAnnotationManager.create(marcador)
    }

    override fun onDestroy() {
        super.onDestroy()
        locationListener?.remove()
        locationListener = null
        pointAnnotationManager.deleteAll()
    }
}
