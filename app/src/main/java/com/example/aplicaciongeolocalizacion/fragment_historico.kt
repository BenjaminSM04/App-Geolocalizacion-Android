package com.example.aplicaciongeolocalizacion

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import java.text.SimpleDateFormat
import java.util.Locale

class fragment_historico : Fragment() {
    private lateinit var mapView: MapView
    private lateinit var btnLineas: Button
    private val puntos: MutableList<Point> = mutableListOf()
    private var userID: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_historico, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userID = FirebaseAuth.getInstance().currentUser?.uid
        mapView = view.findViewById(R.id.mapViewHistorico)
        btnLineas = view.findViewById(R.id.btnTrazarlinea)

        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) { style ->
            // Cargar el recurso y verificar que no sea null
            val bitmap = BitmapFactory.decodeResource(requireContext().resources, R.drawable.iconsearch)
            if (bitmap != null) {
                style.addImage("mio_icon", bitmap)
            } else {
                Toast.makeText(requireContext(), "Error: No se pudo cargar el recurso ic_realtime", Toast.LENGTH_SHORT).show()
            }

            mapView.getMapboxMap().setCamera(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(-98.0, 39.5))
                    .zoom(3.0)
                    .build()
            )
            ConseguirUbicaciones()
        }

        btnLineas.setOnClickListener {
            try {
                TrazarLinea()
            } catch (ex: Exception) {
                Toast.makeText(requireContext(), "Error: " + ex.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun ConseguirUbicaciones() {
        val db = FirebaseFirestore.getInstance()
        db.collection("usuarios")
            .document(userID.toString())
            .collection("ubicaciones")
            .get()
            .addOnSuccessListener { querySnapshot ->
                puntos.clear()
                for (document in querySnapshot.documents) {
                    val lat = document.getDouble("lat")
                    val long = document.getDouble("lon")
                    val timestamp = document.getDate("timestamp")
                    if (lat != null && long != null) {
                        val punto = Point.fromLngLat(long, lat)
                        puntos.add(punto)
                        agregarMarca(punto, timestamp)
                    }
                }
                if (puntos.isEmpty()) {
                    Toast.makeText(requireContext(), "No hay ubicaciones almacenadas", Toast.LENGTH_SHORT).show()
                } else {
                    centrarEnUbicacionMasAntigua()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: " + e.message, Toast.LENGTH_SHORT).show()
            }
    }

    private fun agregarMarca(punto: Point, timestamp: java.util.Date?) {
        val annotationApi = mapView.annotations
        val pointMan: PointAnnotationManager = annotationApi.createPointAnnotationManager()
        val fecha = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val fechaString = fecha.format(timestamp)
        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(punto)
            .withIconImage("mio_icon")
            .withTextField(fechaString)
            .withTextOffset(listOf(0.0, -2.0))
            .withTextHaloColor("#FFFFFF")
            .withTextHaloWidth(2.0)
        pointMan.create(pointAnnotationOptions)
    }

    private fun TrazarLinea() {
        if (puntos.size < 2) {
            Toast.makeText(requireContext(), "No hay suficientes ubicaciones para trazar un recorrido", Toast.LENGTH_SHORT).show()
            return
        }
        val annotationApi = mapView.annotations
        val polylineAnnotationManager: PolylineAnnotationManager = annotationApi.createPolylineAnnotationManager()
        val polylineAnnotationOptions = PolylineAnnotationOptions()
            .withPoints(puntos)
            .withLineColor("#FF0000")
            .withLineWidth(4.0)
        polylineAnnotationManager.create(polylineAnnotationOptions)
    }

    private fun centrarEnUbicacionMasAntigua() {
        if (puntos.isNotEmpty()) {
            val oldestPoint = puntos.first()
            mapView.getMapboxMap().setCamera(
                CameraOptions.Builder()
                    .center(oldestPoint)
                    .zoom(12.0)
                    .build()
            )
        }
    }
}
