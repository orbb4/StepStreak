package com.example.stepstreak.routes

import android.content.pm.PackageManager
import android.graphics.Canvas
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stepstreak.data.auth
import com.example.stepstreak.locationscreen.guardarLugarEnFirebase
import com.example.stepstreak.locationscreen.obtenerPOI
import com.example.stepstreak.locationscreen.obtenerUbicacion
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay




@Composable
fun RouteCreatorScreen(
    routeName: String,
    routeViewModel: RouteViewModel = viewModel(),
    onSaveRoute: (String, List<GeoPoint>) -> Unit = { _, _ -> } // callback to parent
) {
    val context = LocalContext.current
    var userLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var mapLoading by remember { mutableStateOf(true) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            obtenerUbicacion(context) { lat, lon ->
                userLocation = GeoPoint(lat, lon)
            }
        } else {
            userLocation = GeoPoint(-33.45, -70.66) // fallback
        }
    }

    LaunchedEffect(Unit) {
        routeViewModel.startRoute(routeName)
        permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapScreen(
            center = userLocation,
            mapLoading = mapLoading,
            onMapLoaded = { mapLoading = false },
            onPointSelected = { geoPoint ->
                routeViewModel.addMarker(geoPoint)
            }
        )

        Button(
            onClick = {
                onSaveRoute(routeViewModel.currentRouteName, routeViewModel.markers)
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Text("Guardar ruta")
        }
    }
}
@Composable
fun MapScreen(
    center: GeoPoint?,
    mapLoading: Boolean,
    onMapLoaded: () -> Unit,
    onPointSelected: (GeoPoint) -> Unit
) {
    val context = LocalContext.current

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(16.0)

            overlays.add(object : Overlay() {
                override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
                    val geoPoint = mapView.projection
                        .fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint

                    val marker = Marker(mapView).apply {
                        position = geoPoint
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }

                    mapView.overlays.add(marker)
                    mapView.invalidate()
                    onPointSelected(geoPoint)

                    return true
                }
            })

            overlays.add(object : Overlay() {
                var firstDraw = true
                override fun draw(canvas: Canvas, mapView: MapView?, shadow: Boolean) {
                    super.draw(canvas, mapView, shadow)
                    if (firstDraw) {
                        firstDraw = false
                        onMapLoaded()
                    }
                }
            })
        }
    }

    LaunchedEffect(center) {
        center?.let { mapView.controller.setCenter(it) }
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose { mapView.onPause() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView }
        )

        if (mapLoading) {
            Text("Cargando mapa…", modifier = Modifier.align(Alignment.Center))
        }
    }
}