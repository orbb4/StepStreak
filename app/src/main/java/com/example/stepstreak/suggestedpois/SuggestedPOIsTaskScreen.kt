package com.example.stepstreak.suggestedpois

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.stepstreak.locationscreen.obtenerPOIsEnUnRadio
import com.example.stepstreak.locationscreen.obtenerUbicacion
import org.osmdroid.util.GeoPoint

import android.util.Log
import com.example.stepstreak.data.repository.DailyTask
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.database
import java.time.LocalDate


@Composable
fun SuggestedPOIsTaskScreen() {
    val context = LocalContext.current
    var userLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var suggestions by remember { mutableStateOf<List<DailyTask>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }

    val TAG = "SuggestedPOIScreen"

    // Lanzador de permisos
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            Log.d(TAG, "Permiso concedido, obteniendo ubicación...")
            obtenerUbicacion(context) { lat, lon ->
                Log.d(TAG, "Ubicación obtenida: lat=$lat, lon=$lon")
                userLocation = GeoPoint(lat, lon)
            }
        } else {
            Log.d(TAG, "Permiso denegado, usando fallback en Santiago")
            userLocation = GeoPoint(-33.45, -70.66)
        }
    }

    // Pedir permiso al entrar en pantalla
    LaunchedEffect(Unit) {
        Log.d(TAG, "Solicitando permiso de ubicación...")
        permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // Buscar o generar tareas
    LaunchedEffect(userLocation) {
        userLocation?.let { loc ->
            loading = true
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Log.d(TAG, "Usuario no autenticado")
                suggestions = listOf(DailyTask("Debes iniciar sesión", false))
                loading = false
                return@let
            }

            val today = LocalDate.now().toString()
            val db = Firebase.database
            val tasksRef = db.getReference("dailyTasks/${currentUser.uid}/$today")

            tasksRef.get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        // Ya hay tareas guardadas
                        val tasks = snapshot.children.mapNotNull { it.getValue(DailyTask::class.java) }
                        suggestions = tasks
                        Log.d(TAG, "Tareas recuperadas: $tasks")
                        loading = false
                    } else {
                        // Generar nuevas
                        obtenerPOIsEnUnRadio(loc.latitude, loc.longitude, radius = 1000) { pois ->
                            if (pois.isNotEmpty() && pois.first() != "No se pudo obtener POIs") {
                                val newTasks = pois.shuffled().take(5).map { poi ->
                                    DailyTask(poiName = poi, completed = false)
                                }
                                tasksRef.setValue(newTasks)
                                suggestions = newTasks
                                Log.d(TAG, "Nuevas tareas generadas y guardadas: $newTasks")
                                loading = false
                            } else {
                                // Intentar con radio mayor
                                obtenerPOIsEnUnRadio(loc.latitude, loc.longitude, radius = 5000) { pois2 ->
                                    if (pois2.isNotEmpty() && pois2.first() != "No se pudo obtener POIs") {
                                        val newTasks = pois2.shuffled().take(5).map { poi ->
                                            DailyTask(poiName = poi, completed = false)
                                        }
                                        tasksRef.setValue(newTasks)
                                        suggestions = newTasks
                                        Log.d(TAG, "Nuevas tareas generadas y guardadas: $newTasks")
                                    } else {
                                        suggestions = listOf(DailyTask("No se encontraron POIs cercanos", false))
                                        Log.d(TAG, "No se encontraron POIs cercanos")
                                    }
                                    loading = false
                                }
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    Log.e(TAG, "Error al leer tareas", it)
                    suggestions = listOf(DailyTask("Error al cargar tareas", false))
                    loading = false
                }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("5 lugares para visitar hoy:", style = MaterialTheme.typography.titleMedium)

        if (loading) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn {
                items(suggestions) { task ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Text(
                            text = if (task.completed) "✔ ${task.poiName}" else "Tarea: visitar ${task.poiName}",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}