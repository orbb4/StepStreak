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
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await


@Composable
fun LocationScreen() {
    val context = LocalContext.current
    var locationText by remember { mutableStateOf("Obteniendo ubicación...") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            obtenerUbicacion(context) { locationText = it }
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
            // 👇 ESTO HACE QUE APAREZCA EL POPUP
            permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            obtenerUbicacion(context) { locationText = it }
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
fun obtenerUbicacion(context: Context, onResult: (String) -> Unit) {
    val fused = LocationServices.getFusedLocationProviderClient(context)

    fused.lastLocation.addOnSuccessListener { location ->
        if (location != null) {
            onResult("Lat: ${location.latitude}\nLon: ${location.longitude}")
        } else {
            onResult("No se pudo obtener ubicación")
        }
    }
}
