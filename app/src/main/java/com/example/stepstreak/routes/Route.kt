package com.example.stepstreak.routes

data class Route(
    val ownerUid: String = "",
    val name: String = "",
    val points: List<LatLngPoint> = emptyList(),
    val places: Map<String, PlaceSummary> = emptyMap()
)

data class LatLngPoint(
    val lat: Double = 0.0,
    val lon: Double = 0.0
)

data class PlaceSummary(
    val name: String = "",
    val num_of_points: Int = 0
)