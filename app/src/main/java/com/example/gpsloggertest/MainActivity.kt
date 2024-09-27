package com.example.gpsloggertest

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gpsloggertest.ui.theme.GPSLoggerTESTTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    // Instancia de FirebaseAuth
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicializar FirebaseAuth
        auth = FirebaseAuth.getInstance()

        setContent {
            GPSLoggerTESTTheme {
                LoginScreen { email, password ->
                    loginUser(email, password)
                }
            }
        }
    }

    // Función para autenticar al usuario con Firebase
    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Si la autenticación es exitosa, navegar a MenuActivity
                    startActivity(Intent(this, MenuActivity::class.java))
                    finish()
                } else {
                    // Si falla, mostrar mensaje de error
                    Toast.makeText(this, "Credenciales incorrectas. Inténtalo de nuevo.", Toast.LENGTH_SHORT).show()
                }
            }
    }
}

@Composable
fun LoginScreen(onLoginClick: (String, String) -> Unit) {
    // Variables de estado para los campos de entrada
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Interfaz de usuario
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            // Campo de Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Campo de Contraseña
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Botón de Login
            Button(
                onClick = { onLoginClick(email, password) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Iniciar Sesión")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    GPSLoggerTESTTheme {
        LoginScreen { _, _ -> }
    }
}
