package com.example.stepstreak.authentication
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
import com.example.stepstreak.ui.theme.StepStreakTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import java.time.format.TextStyle

class LoginActivity: ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth

        // SI YA ESTÁ LOGEADO, SALTAR LOGIN
        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContent {
            LoginScreen { email, password ->
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Log.d("Login StepStreak", "signInWithEmail:success")

                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Log.w("Login StepStreak", "signInWithEmail:failure", task.exception)
                            Toast.makeText(
                                baseContext,
                                "Authentication failed.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }
        }
    }
}

@Composable
fun LoginScreen(onSignup: (String, String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.padding(16.dp).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "¡Bienvenido!",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.size(10.dp))
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
            },
            label = { Text("Correo electrónico") }
        )
        Spacer(modifier = Modifier.size(10.dp))
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
            },
            visualTransformation = PasswordVisualTransformation(),
            label = { Text("Contraseña") }
        )

        Spacer(modifier = Modifier.size(10.dp))
        Button(onClick = { onSignup(email, password) }) {
            Text("Iniciar sesión")
        }
        Spacer(modifier = Modifier.size(20.dp))
        Text(
            text = "¿No tienes una cuenta?",
            color = MaterialTheme.colorScheme.primary,

            )
        val context = LocalContext.current
        Text(
            text = "Registrarse",
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable(onClick = {
                context.startActivity(Intent(context, SignupActivity::class.java))
            })

        )

    }
}