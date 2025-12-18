package com.example.stepstreak.locationscreen

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.stepstreak.data.auth
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL


@Composable
fun LocationScreen() {
    val context = LocalContext.current
    var locationText by remember { mutableStateOf("Obteniendo ubicación...") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {

            obtenerUbicacion(context) { lat, lon ->
                obtenerPOI(lat, lon) { poi ->
                    locationText = "Lat: $lat\nLon: $lon\n\nLugar cercano:\n$poi"
                }
            }
        } else {
            locationText = "Permiso rechazado"
        }
    }

    LaunchedEffect(Unit) {
        val permission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (permission != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            obtenerUbicacion(context) { lat, lon ->
                obtenerPOI(lat, lon) { poi ->
                    guardarLugarEnFirebase(auth.currentUser!!.uid, poi, lat, lon)
                    locationText = "Lugar visitado guardado:\n$poi"
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(locationText)
    }
}

@SuppressLint("MissingPermission")
fun obtenerUbicacion(context: Context, onResult: (Double, Double) -> Unit) {
    val fused = LocationServices.getFusedLocationProviderClient(context)

    fused.lastLocation.addOnSuccessListener { location ->
        if (location != null) {
            onResult(location.latitude, location.longitude)
        }
    }
}


fun obtenerPOI(lat: Double, lon: Double, onResult: (String) -> Unit) {
    val url =
        "https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lon&zoom=18&addressdetails=1"

    Thread {
        try {
            val connection = URL(url).openConnection()
            connection.setRequestProperty("User-Agent", "StepStreakApp/1.0")

            val data = connection.getInputStream().bufferedReader().use { it.readText() }
            val json = JSONObject(data)

            val displayName = json.optString("display_name", "Sin información")

            onResult(displayName)
        } catch (e: Exception) {
            onResult("No se pudo obtener punto de interés")
        }
    }.start()
}

fun obtenerPOIsEnUnRadio(
    lat: Double,
    lon: Double,
    radius: Int = 1000,
    onResult: (List<String>) -> Unit
) {
    val url = "https://overpass-api.de/api/interpreter" +
            "?data=[out:json];(node(around:$radius,$lat,$lon)[\"amenity\"];);out;"

    Thread {
        try {
            val connection = URL(url).openConnection()
            connection.setRequestProperty("User-Agent", "StepStreakApp/1.0")
            val data = connection.getInputStream().bufferedReader().use { it.readText() }
            val json = JSONObject(data)
            val elements = json.getJSONArray("elements")

            val names = mutableListOf<String>()
            for (i in 0 until elements.length()) {
                val obj = elements.getJSONObject(i)
                val tags = obj.optJSONObject("tags")
                val name = tags?.optString("name")
                if (!name.isNullOrBlank()) {
                    names.add(name)
                }
            }

            onResult(names)
        } catch (e: Exception) {
            onResult(listOf("No se pudo obtener POIs"))
        }
    }.start()
}
fun generarPlaceId(nombre: String): String {
    return nombre.lowercase()
        .replace("[^a-z0-9]".toRegex(), "_")
        .replace("_+".toRegex(), "_")
        .trim('_')
}

fun guardarLugarEnFirebase(uid: String, nombreLugar: String, lat: Double, lon: Double) {
    val db = FirebaseDatabase.getInstance().reference
    val placeId = generarPlaceId(nombreLugar)

    val ref = db.child("users").child(uid).child("places").child(placeId)

    val data = mapOf(
        "name" to nombreLugar,
        "lat" to lat,
        "lon" to lon,
        "last_visit" to System.currentTimeMillis(),
    )

    ref.updateChildren(data)
}
