package com.example.gpsloggertest

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.gpsloggertest.ui.theme.GPSLoggerTESTTheme
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.database.DatabaseError

class MenuActivity : ComponentActivity() {

    // Cliente de ubicación
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Referencia a la base de datos de Firebase
    private val database = FirebaseDatabase.getInstance().reference

    // Usuario actual
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown_user"

    // Lanzador de permisos
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            getCoordinates()
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            GPSLoggerTESTTheme {
                MenuScreen(
                    onGetCoordinatesClick = {
                        checkLocationPermission()
                    },
                    onLogoutClick = {
                        handleLogout()
                    }
                )
            }
        }
    }

    // Función para verificar y solicitar permisos
    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permiso otorgado, obtener coordenadas
                getCoordinates()
            }
            else -> {
                // Solicitar permiso
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    // Función para obtener coordenadas
    private fun getCoordinates() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude
                val timestamp = System.currentTimeMillis()

                // Guardar en Firebase
                saveCoordinatesToFirebase(latitude, longitude, timestamp)
            } else {
                Toast.makeText(this, "No se pudo obtener la ubicación.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Función para guardar coordenadas en Firebase
    private fun saveCoordinatesToFirebase(latitude: Double, longitude: Double, timestamp: Long) {
        val data = mapOf(
            "latitude" to latitude,
            "longitude" to longitude,
            "timestamp" to timestamp
        )

        database.child("users").child(userId).push().setValue(data)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Coordenadas guardadas.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Error al guardar en Firebase.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Función para manejar el cierre de sesión
    private fun handleLogout() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}

@Composable
fun MenuScreen(
    onGetCoordinatesClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    // Estado para almacenar la lista de coordenadas
    var coordinatesList by remember { mutableStateOf<List<CoordinateData>>(emptyList()) }

    // Cargar datos desde Firebase al iniciar y agregar un listener para ver si hay cambios
    LaunchedEffect(Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown_user"
        val database = FirebaseDatabase.getInstance().reference
        database.child("users").child(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<CoordinateData>()
                for (child in snapshot.children) {
                    val latitude = child.child("latitude").getValue(Double::class.java)
                    val longitude = child.child("longitude").getValue(Double::class.java)
                    val timestamp = child.child("timestamp").getValue(Long::class.java)
                    if (latitude != null && longitude != null && timestamp != null) {
                        list.add(CoordinateData(latitude, longitude, timestamp))
                    }
                }
                coordinatesList = list
            }

            override fun onCancelled(error: DatabaseError) {
                // Manejar el error si es necesario
            }
        })
    }

    // Interfaz de usuario
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Botón para obtener coordenadas
            Button(
                onClick = onGetCoordinatesClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Obtener coordenadas GPS")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón para cerrar sesión
            Button(
                onClick = onLogoutClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Cerrar Sesión")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mostrar las coordenadas en una tabla
            CoordinatesTable(coordinatesList)
        }
    }
}

@Composable
fun CoordinatesTable(coordinatesList: List<CoordinateData>) {
    Column {
        // Encabezados
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Latitud",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = "Longitud",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = "Fecha y Hora",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filas de datos
        for (coordinate in coordinatesList) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = coordinate.latitude.toString(),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = coordinate.longitude.toString(),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = coordinate.getFormattedDate(),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

// Data class para almacenar coordenadas
data class CoordinateData(val latitude: Double, val longitude: Double, val timestamp: Long) {
    fun getFormattedDate(): String {
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
        return format.format(date)
    }
}

@Preview(showBackground = true)
@Composable
fun MenuScreenPreview() {
    GPSLoggerTESTTheme {
        MenuScreen(
            onGetCoordinatesClick = {},
            onLogoutClick = {}
        )
    }
}