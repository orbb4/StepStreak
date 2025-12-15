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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star

import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.stepstreak.authentication.LoginActivity
import com.example.stepstreak.data.db
import com.example.stepstreak.data.repository.FriendshipDisplay
import com.example.stepstreak.data.repository.acceptFriendRequest
import com.example.stepstreak.data.repository.acceptRequest
import com.example.stepstreak.data.repository.addFriend
import com.example.stepstreak.data.repository.addVisitedCell
import com.example.stepstreak.data.repository.createWalkSession
import com.example.stepstreak.data.repository.denyRequest
import com.example.stepstreak.data.repository.getUsername
import com.example.stepstreak.data.repository.loadPendingRequests
import com.example.stepstreak.data.repository.sendFriendRequest
import com.example.stepstreak.ui.theme.StepStreakTheme
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.temporal.ChronoUnit
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.database

import com.google.firebase.FirebaseApp
// paquetes propios!
import com.example.stepstreak.health.HealthRepository
import com.example.stepstreak.locationscreen.LocationScreen
import com.example.stepstreak.roadmap.DisplayRoadmap
import com.example.stepstreak.roadmap.roadmap1
import com.example.stepstreak.data.repository.loadFriends
import com.example.stepstreak.routes.RouteCreatorScreen
import com.google.android.libraries.places.api.model.LocalDate
import com.google.firebase.database.FirebaseDatabase
import org.osmdroid.config.Configuration
import java.time.format.DateTimeFormatter
import kotlin.math.max

enum class RoadmapScreen(){
    Roadmap,
    YourSteps,
    Shop,
    Dog,
    Location,
    RouteCreator

}



val requestPermissionActivityContract = PermissionController.createRequestPermissionResultContract()
class MainActivity : ComponentActivity() {

    private var permissions_granted by mutableStateOf(false)
    val requestPermissions =
        registerForActivityResult(requestPermissionActivityContract) { granted ->
            permissions_granted = granted.containsAll((PERMISSIONS))
        }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        FirebaseApp.initializeApp(this)
        val healthConnectClient = HealthConnectClient.getOrCreate(this)
        val healthRepository = HealthRepository(healthConnectClient)
        val status = HealthConnectClient.getSdkStatus(this)
        Log.d("HealthConnectTest", "SDK status: $status")
        super.onCreate(savedInstanceState)

        Configuration.getInstance().userAgentValue = packageName


        setContent {
            val navController = rememberNavController()
            var coins by remember { mutableIntStateOf(300) }
            var steps by remember { mutableStateOf<Long?>(null) }
            var username by remember { mutableStateOf<String?>(null) }
            var friends by remember { mutableStateOf(listOf<Pair<String, Long>>()) }

            LaunchedEffect(Unit) {
                loadFriends { list ->
                    friends = list
                }
            }



            LaunchedEffect(Unit) {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    val db = Firebase.database
                    val usersRef = db.getReference("users").child(user.uid)
                    val friendsRef = db.getReference("friendships").child(user.uid)
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
                topBar = {
                    val context = LocalContext.current
                    TopAppBar(

                        title = { Text("StepStreak") },
                        actions = {
                            Text(
                                "Cerrar sesión",
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .clickable {
                                        FirebaseAuth.getInstance().signOut()
                                        // abrir LoginActivity

                                        val intent = Intent(context, LoginActivity::class.java)
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        context.startActivity(intent)
                                    },
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline
                            )
                        }
                    )
                },
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
                                    onClick = { navController.navigate("RouteCreator") },
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
                        if (permissions_granted) {
                            LaunchedEffect(Unit) {
                                scrollState.scrollTo(0)
                                totalSteps = healthRepository.readTotalSteps(
                                    Instant.now().minus(7, ChronoUnit.DAYS)
                                )
                            }
                        } else {

                            Text("Activa permisos para ver tu progreso")
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
                                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                                    var dailySteps by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
                                    LaunchedEffect(uid) {
                                        val db = FirebaseDatabase.getInstance().reference
                                        if (uid != null) {
                                            db.child("userDailySteps").child(uid)
                                                .get()
                                                .addOnSuccessListener { snapshot ->
                                                    val map = snapshot.children.associate {
                                                        it.key!! to (it.getValue(Long::class.java) ?: 0L)
                                                    }
                                                    dailySteps = map
                                                }
                                        }
                                    }
                                    DisplaySteps(steps)
                                    Spacer(modifier = Modifier.height(20.dp))
                                    StepsScreen(dailySteps, 5000L)
                                    Spacer(modifier = Modifier.height(20.dp))
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
                                    FriendRequestsScreen()
                                    var resultMessage by remember { mutableStateOf<String?>(null) }
                                    if (showAddFriendDialog) {
                                        Dialog(onDismissRequest = { showAddFriendDialog = false }) {
                                            Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 4.dp) {
                                                Column(
                                                    modifier = Modifier.padding(20.dp),
                                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {

                                                    Text("Agregar amigo", fontSize = 20.sp, fontWeight = FontWeight.Bold)

                                                    TextField(
                                                        value = friendUsernameInput,
                                                        onValueChange = {
                                                            friendUsernameInput = it
                                                            resultMessage = null   // limpiar mensajes al escribir
                                                        },
                                                        placeholder = { Text("Nombre de usuario…") },
                                                        singleLine = true
                                                    )

                                                    // Mensaje de error o éxito
                                                    resultMessage?.let { msg ->
                                                        Text(msg, color = Color.Red)
                                                    }

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.End
                                                    ) {
                                                        TextButton(onClick = { showAddFriendDialog = false }) {
                                                            Text("Cancelar")
                                                        }
                                                        Button(
                                                            onClick = {
                                                                sendFriendRequest(friendUsernameInput) { success, message ->
                                                                    resultMessage = message
                                                                    if (success) {
                                                                        // cerrar solo si realmente se envió
                                                                        showAddFriendDialog = false
                                                                        friendUsernameInput = ""
                                                                    }
                                                                }
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
                    composable(route=RoadmapScreen.RouteCreator.name){
                        RouteCreatorScreen()
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
                            val uid = FirebaseAuth.getInstance().currentUser?.uid
                            var dailySteps by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }

                            LaunchedEffect(uid) {
                                if (uid != null) {
                                    FirebaseDatabase.getInstance().reference
                                        .child("userDailySteps")
                                        .child(uid)
                                        .get()
                                        .addOnSuccessListener { snapshot ->
                                            val map = snapshot.children.associate {
                                                it.key!! to (it.getValue(Long::class.java) ?: 0L)
                                            }
                                            dailySteps = map
                                        }
                                }
                            }

                            StatsScreen(
                                dailySteps = dailySteps,
                                stepGoal = 5000L
                            )
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
fun DisplaySteps(steps: Long?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Tus pasos de hoy", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "${steps ?: "cargando…"}",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFFFA500)
            )
        }
    }
}


@Composable
fun WeeklyBarChart(stepsPerDay: Map<String, Long>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.Bottom
    ) {
        stepsPerDay.forEach { (day, steps) ->
            val heightFactor = (steps / 10000f).coerceIn(0f, 1f)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .fillMaxHeight(heightFactor)
                        .background(Color(0xFF4CAF50), shape = RoundedCornerShape(4.dp))
                )
                Text(text = day.takeLast(2)) // muestra solo el día, ej "24"
            }
        }
    }
}

@Composable
fun StatsScreen(
    dailySteps: Map<String, Long>,
    stepGoal: Long
) {
    var currentStreak by remember { mutableStateOf(0) }
    var bestStreak by remember { mutableStateOf(0) }

    LaunchedEffect(dailySteps) {
        if (dailySteps.isNotEmpty()) {
            val (current, best) = calculateStreak(dailySteps, stepGoal)
            currentStreak = current
            bestStreak = best
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {

        Text("Estadísticas", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        // rachas
        Text("Racha actual: $currentStreak días", fontSize = 18.sp)
        Text("Mejor racha: $bestStreak días", fontSize = 18.sp)

        Spacer(Modifier.height(24.dp))

        // últimos 7 días
        Text("Últimos 7 días", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        val last7 = dailySteps
            .toSortedMap()
            .entries
            .reversed()
            .take(7)
            .associate { it.key to it.value }
            .toSortedMap()

        WeeklyBarChart(last7)

        Spacer(Modifier.height(24.dp))

        val avg = if (last7.isNotEmpty()) last7.values.average().toInt() else 0
        val total = last7.values.sum()

        Text("Promedio diario: $avg pasos", fontSize = 16.sp)
        Text("Total semanal: $total pasos", fontSize = 16.sp)
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

@Composable
fun FriendRequestsScreen() {
    var pendingRequests by remember { mutableStateOf<List<FriendshipDisplay>>(emptyList()) }

    LaunchedEffect(Unit) {
        loadPendingRequests { list ->
            pendingRequests = list
        }
    }

    PendingRequestsSection(pendingRequests)
}



@Composable
fun PendingRequestsSection(pendingRequests: List<FriendshipDisplay>) {
    Column(modifier = Modifier.padding(16.dp)) {

        if (pendingRequests.isNotEmpty()) {
            Text(
                "Solicitudes pendientes",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(12.dp))

            pendingRequests.forEach { request ->
                PendingRequestItem(request)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}


@Composable
fun PendingRequestItem(request: FriendshipDisplay) {

    var username: String? by remember { mutableStateOf("...") }

    // Obtener username del remitente
    LaunchedEffect(request.senderUid) {
        getUsername(request.senderUid) {
            username = it
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        username?.let { Text(it, fontSize = 18.sp) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

            Button(
                onClick = { acceptFriendRequest(request.id) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("Aceptar")
            }

            Button(
                onClick = { denyRequest(request.id) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
            ) {
                Text("Denegar")
            }
        }
    }
}
@Composable
fun StepsScreen(
    dailySteps: Map<String, Long>,
    stepGoal: Long
) {
    var currentStreak by remember { mutableStateOf(0) }
    var bestStreak by remember { mutableStateOf(0) }

    LaunchedEffect(dailySteps) {
        if (dailySteps.isNotEmpty()) {
            val (current, best) = calculateStreak(dailySteps, stepGoal)
            currentStreak = current
            bestStreak = best
        }
    }

    Column {
        Text("Racha actual: $currentStreak días")
        Text("Mejor racha: $bestStreak días")
    }
}


fun calculateStreak(dailySteps: Map<String, Long>, goal: Long): Pair<Int, Int> {
    if (dailySteps.isEmpty()) return 0 to 0

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // Convertir a lista ordenada por fecha
    val days = dailySteps.entries
        .map { java.time.LocalDate.parse(it.key, formatter) to it.value }
        .sortedBy { it.first }

    var currentStreak = 0
    var bestStreak = 0

    var previousDay: java.time.LocalDate? = null

    for ((date, steps) in days) {
        val metGoal = steps >= goal

        if (previousDay == null) {
            // primer día
            currentStreak = if (metGoal) 1 else 0
        } else {
            // revisar si es consecutivo
            if (date == previousDay!!.plusDays(1)) {
                if (metGoal) currentStreak++ else currentStreak = 0
            } else {
                // se rompió la continuidad de días
                currentStreak = if (metGoal) 1 else 0
            }
        }

        bestStreak = max(bestStreak, currentStreak)
        previousDay = date
    }

    return currentStreak to bestStreak
}


val PERMISSIONS =
    setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class)
    )