package com.example.stepstreak
import android.R.attr.onClick
import android.app.Activity
import android.widget.TextView
import androidx.core.view.setPadding
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.stepstreak.ui.theme.StepStreakTheme
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.temporal.ChronoUnit

// paquetes propios!
import com.example.stepstreak.health.HealthRepository
import com.example.stepstreak.roadmap.DisplayRoadmap
import com.example.stepstreak.roadmap.roadmap1


enum class RoadmapScreen(){
    Roadmap,
    YourSteps,
    Shop,
    Dog
}



val requestPermissionActivityContract = PermissionController.createRequestPermissionResultContract()
class MainActivity : ComponentActivity() {

    private var permissions_granted by mutableStateOf(false)
    val requestPermissions =
        registerForActivityResult(requestPermissionActivityContract) { granted ->
            permissions_granted = granted.containsAll((PERMISSIONS))
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        val healthConnectClient = HealthConnectClient.getOrCreate(this)
        val healthRepository = HealthRepository(healthConnectClient)
        val status = HealthConnectClient.getSdkStatus(this)
        Log.d("HealthConnectTest", "SDK status: $status")
        super.onCreate(savedInstanceState)


        setContent {
            val navController = rememberNavController()
            var coins by remember { mutableIntStateOf(300) }
            var steps by remember { mutableStateOf<Long?>(null) }
            Scaffold(

                bottomBar = {
                    BottomAppBar(
                        {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                IconButton(
                                    onClick = { navController.navigate("Roadmap") },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Filled.LocationOn,
                                        contentDescription = "Roadmap"
                                    )
                                }

                                IconButton(
                                    onClick = { navController.navigate("YourSteps") },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Filled.Home,
                                        contentDescription = "Step Counter"
                                    )
                                }
                                IconButton(
                                    onClick = { navController.navigate("Shop") },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Filled.ShoppingCart,
                                        contentDescription = "Shop"
                                    )
                                }
                                IconButton(
                                    onClick = { navController.navigate("Dog") },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.dog),
                                        contentDescription = "Dog",
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    )
                }
            ){
                innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = RoadmapScreen.Roadmap.name,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    // pantalla roadmap

                    composable(route = RoadmapScreen.Roadmap.name){
                        val scrollState = rememberScrollState()
                        var totalSteps by remember { mutableStateOf<Long?>(null) }
                        LaunchedEffect(Unit) {

                            scrollState.scrollTo(0)

                            totalSteps = healthRepository.readTotalSteps(Instant.now().minus(7, ChronoUnit.DAYS))
                        }
                        Box(){

                            Column(){
                                if(totalSteps!=null){
                                    DisplayTotalSteps(totalSteps)
                                    DisplayRoadmap(roadmap1, totalSteps, onClick = {coins+=20})
                                }
                                else{
                                    Text("Cargando...")
                                }
                            }


                            Text(
                                text = "Coins: $coins",
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp),

                            )
                        }
                    }
                    // pantalla pasos
                    composable(route= RoadmapScreen.YourSteps.name){
                        StepStreakTheme {

                            LaunchedEffect(Unit) {
                                checkPermissionsAndRun(healthRepository)
                                var stepsToday: Long

                                do {
                                    stepsToday = healthRepository.readTodaySteps()
                                    if (stepsToday == 0L) {
                                        delay(2000)
                                    }
                                } while (stepsToday == 0L)
                                steps = stepsToday
                            }
                            if (!permissions_granted) {
                                DisplayText("Pidiendo permisos…")
                            } else {
                                DisplaySteps(steps)

                            }
                        }
                    }
                    composable(route= RoadmapScreen.Shop.name){
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ){
                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth().padding(20.dp)
                            ){
                                Text("Tienda", style = MaterialTheme.typography.bodyLarge, color = Color(0xFFFFA500))
                                DisplayText("Cosméticos")
                                DisplayText("Pelajes")
                                Row(
                                    horizontalArrangement = Arrangement.Center,

                                    modifier = Modifier.horizontalScroll(rememberScrollState())

                                ){
                                    DogImage(Color.DarkGray, 0.7F, {coins-=100})
                                    Spacer(modifier = Modifier.width(8.dp))
                                    DogImage(Color.Blue, 0.5F, {coins-=100})
                                    Spacer(modifier = Modifier.width(8.dp))
                                    DogImage(Color.Red, 0.3F, {coins-=100})
                                    Spacer(modifier = Modifier.width(8.dp))
                                    DogImage(Color.Red, 0.9F, {coins-=100})
                                }
                            }
                            Text(
                                text = "Coins: $coins",

                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp),

                                )
                        }


                    }
                    composable(route=RoadmapScreen.Dog.name){
                        Box(
                            modifier = Modifier.fillMaxHeight().fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ){
                            Image(
                                painter = painterResource(id = R.drawable.dog),
                                contentDescription = "Dog",
                                colorFilter = ColorFilter.tint(Color.Red.copy(alpha = 0.7f)),
                                modifier = Modifier.size(200.dp)
                            )
                        }

                    }
                }
            }


        }

    }

    private suspend fun checkPermissionsAndRun(healthRepository: HealthRepository) {
        if (healthRepository.hasPermissions()) {
            permissions_granted=true
            Log.d("HealthConnectTest", "PERMISOS OK!")
            // Permissions already granted; proceed with inserting or reading data
        } else {
            requestPermissions.launch(PERMISSIONS)
            Log.d("HealthConnectTest", "se ha intentado hacer requestpermissions")
        }
    }
}


class HealthConnectRationaleActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tv = TextView(this).apply {
            text = "StepStreak necesita acceso a leer tu cantidad de pasos diaria para funcionar."
            setPadding(32)
        }
        setContentView(tv)
    }
}


@Composable
fun DogImage(color: Color, alpha: Float, onClick: () -> Unit){
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, Color.LightGray, RoundedCornerShape(16.dp))
            .padding(5.dp)
            .clickable{
                onClick()
            },
        contentAlignment = Alignment.Center
    ){
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.dog),
                contentDescription = "Dog",
                colorFilter = ColorFilter.tint(color.copy(alpha = alpha)),
                modifier = Modifier
                    .size(100.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("100")
        }
    }
}


// pasos de hoy.
@Composable
fun DisplaySteps(steps: Long?){
    Box(
        modifier = Modifier.fillMaxSize().padding(5.dp),
        contentAlignment = Alignment.Center
    ){
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Tus pasos de hoy", style = MaterialTheme.typography.bodyMedium)
            Text(text = "${steps ?: "cargando…"}", style = MaterialTheme.typography.bodyLarge, color = Color(0xFFFFA500))
        }
    }
}

@Composable
fun DisplayTotalSteps(steps: Long?){
    Box(
        modifier = Modifier.padding(5.dp),
        contentAlignment = Alignment.Center
    ){
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Tus pasos totales", style = MaterialTheme.typography.bodyMedium)
            Text(text = "${steps ?: "cargando…"}", style = MaterialTheme.typography.bodyLarge, color = Color(0xFFFFA500))
        }
    }
}
@Composable
fun DisplayText(text: String){
    Text(text = text, Modifier.padding(5.dp).statusBarsPadding())
}



val PERMISSIONS =
    setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class)
    )