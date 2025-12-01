package com.example.stepstreak
import android.R.attr.onClick
import android.app.Activity
import android.content.Intent
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

import androidx.compose.material.icons.Icons.

import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.stepstreak.data.repository.addFriend
import com.example.stepstreak.data.repository.addVisitedCell
import com.example.stepstreak.data.repository.createWalkSession
import com.example.stepstreak.data.repository.sendFriendRequest
import com.example.stepstreak.ui.theme.StepStreakTheme
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.temporal.ChronoUnit
import com.google.firebase.Firebase

import com.google.firebase.FirebaseApp
// paquetes propios!
import com.example.stepstreak.health.HealthRepository
import com.example.stepstreak.locationscreen.LocationScreen
import com.example.stepstreak.roadmap.DisplayRoadmap
import com.example.stepstreak.roadmap.roadmap1
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.database


enum class RoadmapScreen(){
    Roadmap,
    YourSteps,
    Shop,
    Dog,
    Location

}



val requestPermissionActivityContract = PermissionController.createRequestPermissionResultContract()
class MainActivity : ComponentActivity() {

    private var permissions_granted by mutableStateOf(false)
    val requestPermissions =
        registerForActivityResult(requestPermissionActivityContract) { granted ->
            permissions_granted = granted.containsAll((PERMISSIONS))
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        FirebaseApp.initializeApp(this)
        val healthConnectClient = HealthConnectClient.getOrCreate(this)
        val healthRepository = HealthRepository(healthConnectClient)
        val status = HealthConnectClient.getSdkStatus(this)
        Log.d("HealthConnectTest", "SDK status: $status")
        super.onCreate(savedInstanceState)


        setContent {
            val navController = rememberNavController()
            var coins by remember { mutableIntStateOf(300) }
            var steps by remember { mutableStateOf<Long?>(null) }
            var username by remember { mutableStateOf<String?>(null) }
            var friends by remember { mutableStateOf<List<Pair<String, Long>>>(emptyList()) }


            LaunchedEffect(Unit) {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    val db = Firebase.database
                    val usersRef = db.getReference("users").child(user.uid)
                    val friendsRef = db.getReference("friends").child(user.uid)
                    usersRef.child("username").get()
                        .addOnSuccessListener { snapshot ->
                            username = snapshot.getValue(String::class.java)
                        }
                    friendsRef.get().addOnSuccessListener { snapshot ->
                        val friendIds = snapshot.children.map { it.key!! }

                        val tmpList = mutableListOf<Pair<String, Long>>()

                        friendIds.forEach { friendUid ->
                            db.getReference("users").child(friendUid).get()
                                .addOnSuccessListener { friendSnap ->
                                    val friendName = friendSnap.child("username").getValue(String::class.java) ?: "?"
                                    val friendSteps = friendSnap.child("stepsToday").getValue(Long::class.java) ?: 0L

                                    tmpList.add(friendName to friendSteps)
                                    friends = tmpList.toList()
                                }
                        }
                    }
                }
            }

            //TestScreen()

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
                                        Icons.Filled.Star,
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
                                    onClick = { navController.navigate("Location") },
                                    modifier = Modifier.weight(1f)
                                ){
                                    Icon(
                                        Icons.Filled.LocationOn,
                                        contentDescription = "Location"
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
                        var showAddFriendDialog by remember { mutableStateOf(false) }
                        var friendUsernameInput by remember { mutableStateOf("") }
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
                                var showAddFriendDialog by remember { mutableStateOf(false) }
                                var friendUsernameInput by remember { mutableStateOf("") }

                                Column(modifier = Modifier.fillMaxWidth()) {

                                    // "hola, usuario" texto
                                    if (username != null) {
                                        Text(
                                            text = "¡Hola, $username!",
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }

                                    // display de pasos
                                    DisplaySteps(steps)

                                    Spacer(modifier = Modifier.height(20.dp))
                                    // "tus amigos" texto
                                    Text(
                                        "Tus amigos",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(start = 16.dp)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // lista de amigos
                                    FriendsList(friends)

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // boton agregar amigo
                                    Button(
                                        onClick = { showAddFriendDialog = true },
                                        modifier = Modifier
                                            .padding(16.dp)
                                            .fillMaxWidth()
                                    ) {
                                        Text("Agregar amigo")
                                    }
                                    if (showAddFriendDialog) {
                                        Dialog(onDismissRequest = { showAddFriendDialog = false }) {
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                tonalElevation = 4.dp
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(20.dp),
                                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {

                                                    Text(
                                                        "Agregar amigo",
                                                        fontSize = 20.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )

                                                    TextField(
                                                        value = friendUsernameInput,
                                                        onValueChange = { friendUsernameInput = it },
                                                        placeholder = { Text("Nombre de usuario…") },
                                                        singleLine = true
                                                    )

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.End
                                                    ) {
                                                        TextButton(onClick = { showAddFriendDialog = false }) {
                                                            Text("Cancelar")
                                                        }
                                                        Button(
                                                            onClick = {
                                                                showAddFriendDialog = false
                                                                sendFriendRequest(friendUsernameInput)
                                                                friendUsernameInput = ""
                                                            }
                                                        ) {
                                                            Text("Enviar")
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                }


                            }
                        }
                    }
                    composable(route=RoadmapScreen.Location.name){
                        LocationScreen()
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


@Composable
fun TestScreen() {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "testUID"

    Column {
        Button(onClick = {
            createWalkSession(uid)
        }) {
            Text("Crear sesión de caminata")
        }

        Button(onClick = {
            addFriend(uid, "otroUsuarioUID")
        }) {
            Text("Enviar solicitud de amistad")
        }

        Button(onClick = {
            addVisitedCell("SESSION_ID_AQUI", 12345, 54321)
        }) {
            Text("Agregar celda visitada")
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


@Composable
fun FriendsList(friends: List<Pair<String, Long>>) {
    Column(modifier = Modifier.fillMaxWidth()) {

        if (friends.isEmpty()) {
            Text(
                text = "Todavía no tienes amigos...",
                modifier = Modifier.padding(16.dp),
                color = Color.Gray
            )
            return
        }

        friends.forEach { (name, steps) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = name, fontSize = 18.sp)
                Text(text = "$steps pasos", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}


@Composable
fun AddFriendDialog(
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    var username by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Agregar amigo", fontSize = 20.sp, fontWeight = FontWeight.Bold)

                TextField(
                    value = username,
                    onValueChange = { username = it },
                    placeholder = { Text("Nombre de usuario…") },
                    singleLine = true
                )

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Button(onClick = { onSend(username) }) {
                        Text("Enviar")
                    }
                }
            }
        }
    }
}




val PERMISSIONS =
    setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class)
    )