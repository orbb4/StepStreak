package com.example.stepstreak.routes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import org.osmdroid.util.GeoPoint

class RouteViewModel : ViewModel() {
    var currentRouteName by mutableStateOf("")
        private set

    var markers = mutableStateListOf<GeoPoint>()
        private set

    fun startRoute(name: String) {
        currentRouteName = name
        markers.clear()
    }

    fun addMarker(point: GeoPoint) {
        markers.add(point)
    }
}