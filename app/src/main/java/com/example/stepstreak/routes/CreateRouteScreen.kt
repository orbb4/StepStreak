package com.example.stepstreak.routes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.stepstreak.data.repository.aggregatePlaces
import com.example.stepstreak.data.repository.loadUserRoutes
import com.example.stepstreak.locationscreen.obtenerPOI
import com.example.stepstreak.locationscreen.obtenerPOIsEnUnRadio
import org.osmdroid.util.GeoPoint
import java.util.concurrent.CountDownLatch

@Composable
fun CreateRouteScreen(
    onCreateRoute: (String) -> Unit
) {
    var routeName by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var routes by remember { mutableStateOf<List<Route>>(emptyList()) }
    var places by remember { mutableStateOf<List<PlaceSummary>>(emptyList()) }

    LaunchedEffect(Unit) {
        loadUserRoutes { loaded ->
            routes = loaded
            places = aggregatePlaces(loaded)
        }
    }


    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = { showDialog = true }) {
            Text("Crear ruta")
        }



        Spacer(modifier = Modifier.height(16.dp))


        Text("Tus lugares más visitados:", style = MaterialTheme.typography.titleMedium)

        LazyColumn {
            items(places) { place ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(place.name, style = MaterialTheme.typography.bodyLarge)
                        Text("Visitas: ${place.num_of_points}")
                    }
                }
            }
        }

    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Nueva ruta") },
            text = {
                OutlinedTextField(
                    value = routeName,
                    onValueChange = { routeName = it },
                    label = { Text("Nombre de la ruta") }
                )
            },
            confirmButton = {
                Button(
                    enabled = routeName.isNotBlank(),
                    onClick = {
                        showDialog = false
                        onCreateRoute(routeName)
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

fun enrichRouteWithPOIs(
    routeName: String,
    markers: List<GeoPoint>,
    onResult: (Map<String, PlaceSummary>) -> Unit
) {
    val placeCounts = mutableMapOf<String, PlaceSummary>()
    val latch = CountDownLatch(markers.size)

    markers.forEach { point ->
        obtenerPOIsEnUnRadio(point.latitude, point.longitude) { displayNames ->
            displayNames.forEach { displayName ->
                val key = displayName
                    .lowercase()
                    .replace("[^a-z0-9]+".toRegex(), "_")

                val current = placeCounts[key]
                if (current == null) {
                    placeCounts[key] = PlaceSummary(name = displayName, num_of_points = 1)
                } else {
                    placeCounts[key] = current.copy(
                        num_of_points = current.num_of_points + 1
                    )
                }
            }

            latch.countDown()
            if (latch.count == 0L) {
                onResult(placeCounts)
            }
        }
    }
}