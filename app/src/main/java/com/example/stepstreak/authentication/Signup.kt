package com.example.stepstreak.authentication
import com.google.firebase.database.*
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.viewbinding.ViewBinding
import com.example.stepstreak.MainActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError

import com.google.firebase.database.database
import com.google.firebase.database.getValue
import java.time.format.TextStyle


class SignupActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private val database = Firebase.database

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        setContent {
            SignupScreen { email, username, password, repeatedPassword ->

                if (password != repeatedPassword) {
                    Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                    return@SignupScreen
                }

                if (username.isBlank()) {
                    Toast.makeText(this, "Debes elegir un nombre de usuario", Toast.LENGTH_SHORT).show()
                    return@SignupScreen
                }

                createAccount(email, password, username)
            }

        }
    }

    private fun createAccount(email: String, password: String, username: String) {

        // 1) Validar username único
        val usernamesRef = database.getReference("usernames").child(username)

        usernamesRef.get().addOnSuccessListener { snapshot ->

            if (snapshot.exists()) {
                Toast.makeText(this, "Ese nombre de usuario ya está tomado", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            // 2) Crear usuario en Firebase Auth
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (!task.isSuccessful) {
                        Toast.makeText(this, "Error al crear cuenta", Toast.LENGTH_SHORT).show()
                        return@addOnCompleteListener
                    }

                    val uid = auth.currentUser!!.uid

                    // 3) Guardar usuario en la base de datos
                    val userData = mapOf(
                        "uid" to uid,
                        "email" to email,
                        "username" to username
                    )

                    database.getReference("users").child(uid).setValue(userData)
                    database.getReference("usernames").child(username).setValue(uid)

                    Toast.makeText(this, "Cuenta creada con éxito", Toast.LENGTH_SHORT).show()

                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser

        if (currentUser != null) {
            currentUser.reload()

            val db = Firebase.database
            val usersRef = db.getReference("users").child(currentUser.uid)

            usersRef.get().addOnSuccessListener { snapshot ->
                if (!snapshot.hasChild("username")) {
                    // Crear username por defecto
                    val defaultUsername = "user_${currentUser.uid.take(6)}"

                    usersRef.child("username").setValue(defaultUsername)
                        .addOnSuccessListener {
                            Log.d("MainActivity", "Se asignó username por defecto: $defaultUsername")
                        }
                        .addOnFailureListener {
                            Log.e("MainActivity", "Error asignando username", it)
                        }
                }
            }
        }
    }

}

@Composable
fun SignupScreen(
    onSignup: (email: String, username: String, password: String, repeatedPassword: String) -> Unit
    ){

    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var repeatedPassword by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.padding(16.dp).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Crea tu cuenta",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.size(10.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Nombre de usuario") }
        )

        Spacer(modifier = Modifier.size(10.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo electrónico") }
        )

        Spacer(modifier = Modifier.size(10.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            visualTransformation = PasswordVisualTransformation(),
            label = { Text("Contraseña") }
        )

        Spacer(modifier = Modifier.size(10.dp))

        OutlinedTextField(
            value = repeatedPassword,
            onValueChange = { repeatedPassword = it },
            visualTransformation = PasswordVisualTransformation(),
            label = { Text("Repetir contraseña") }
        )

        Spacer(modifier = Modifier.size(10.dp))

        Button(onClick = { onSignup(email, username, password, repeatedPassword) }) {
            Text("Registrarse")
        }

        Spacer(modifier = Modifier.size(20.dp))

        val context = LocalContext.current

        Text(
            text = "¿Ya tienes cuenta?",
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Iniciar sesión",
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable {
                context.startActivity(Intent(context, LoginActivity::class.java))
            }
        )
    }
}
