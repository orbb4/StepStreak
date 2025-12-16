package com.example.stepstreak.routes

data class Route(
    val ownerUid: String = "",
    val name: String = "",
    val points: List<LatLngPoint> = emptyList()
)

data class LatLngPoint(
    val lat: Double = 0.0,
    val lon: Double = 0.0
)