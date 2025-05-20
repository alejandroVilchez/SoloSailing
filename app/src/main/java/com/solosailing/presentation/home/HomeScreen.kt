package com.solosailing.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock // Icono para login
import androidx.compose.material.icons.filled.LocationOn // Icono para mapa
import androidx.compose.material.icons.filled.Settings // Icono para settings
import androidx.compose.material.icons.filled.PlayArrow // Icono para simulación/tracking
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel // Importar para obtener ViewModels Hilt
import androidx.navigation.NavController
import com.solosailing.navigation.Routes // Asumiendo que creas este objeto/sealed class
import com.solosailing.sensors.SensorPanel
import com.solosailing.viewModel.AuthViewModel
import com.solosailing.viewModel.TrackingViewModel
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit


@OptIn(ExperimentalMaterial3Api::class) // Para Scaffold y TopAppBar
@Composable
fun HomeScreen(navController: NavController) {

    val authViewModel: AuthViewModel = hiltViewModel()
    val trackingViewModel: TrackingViewModel = hiltViewModel()

    val isLoggedIn by authViewModel.userToken.map { it != null }.collectAsState(initial = false)
    val trackingActive by trackingViewModel.trackingActive.collectAsState()
    val trackingTimeLeft by trackingViewModel.trackingTimeLeft.collectAsState()

    val yaw by trackingViewModel.yaw.collectAsState()
    val pitch by trackingViewModel.pitch.collectAsState()
    val roll by trackingViewModel.roll.collectAsState()
    val isSensorAvailable by trackingViewModel.isSensorAvailable.collectAsState()

    var showRestrictedAccessDialog by remember { mutableStateOf(false) }
    var targetRouteForDialog by remember { mutableStateOf<String?>(null) }

    val handleRestrictedAccess: (String) -> Unit = { route ->
        if (!isLoggedIn) {
            targetRouteForDialog = route
            showRestrictedAccessDialog = true
        } else {
            navController.navigate(route)
        }
    }

    var showNameDialog by remember { mutableStateOf(false) }
    var newRegattaName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Solo Sailing") },
                actions = {
                    if (isLoggedIn) {
                        IconButton(onClick = { authViewModel.logout() }) {
                            Icon(Icons.AutoMirrored.Default.ExitToApp, contentDescription = "Cerrar sesión")
                        }
                    } else {
                        IconButton(onClick = { navController.navigate(Routes.LOGIN) }) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Iniciar sesión")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            if (!isLoggedIn) {
                // Vista sin login
                Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Bienvenido a Solo Sailing!",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Botón Iniciar Sesión
                Button(
                    onClick = { navController.navigate(Routes.LOGIN) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Iniciar sesión")
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Botón Comenzar Simulación
                Button(
                    onClick = { navController.navigate(Routes.REGATTA_SIMULATION) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Comenzar simulación")
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Botones de Configuración
                Button(
                    onClick = { handleRestrictedAccess(Routes.SOUND_SETTINGS) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Configuración de sonido")
                }
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { handleRestrictedAccess(Routes.NAV_SETTINGS) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Configuración de navegación")
                }
//                Spacer(modifier = Modifier.height(16.dp))
//
//                Button(
//                    //onClick = { handleRestrictedAccess(Routes.NAV_SETTINGS) },
//                    onClick = { authViewModel.setDummyToken("dummy_token") }, // Simulación de login para pruebas
//                    modifier = Modifier.fillMaxWidth()
//                ) {
//                    Text(text = "Cambiar menú")
//                }

            } else {
                // sesión iniciada
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Listo para navegar", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))


                // Botón tracking
                Button(
                    onClick = {
                        if (!trackingActive) showNameDialog = true
                        else trackingViewModel.stopTracking()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (trackingActive) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = if (trackingActive) "DETENER TRACKING" else "INICIAR TRACKING")
                }
                if (showNameDialog) {
                    AlertDialog(
                        onDismissRequest = { showNameDialog = false },
                        title = { Text("Nombre de la regata") },
                        text = {
                            OutlinedTextField(
                                value = newRegattaName,
                                onValueChange = { newRegattaName = it },
                                label = { Text("Regatta") }
                            )
                        },
                        confirmButton = {
                            Button(onClick = {
                                trackingViewModel.startTracking(newRegattaName.trim())
                                showNameDialog = false
                            }) {
                                Text("Crear")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showNameDialog = false }) {
                                Text("Cancelar")
                            }
                        }
                    )
                }
                if(trackingActive){
                    Spacer(Modifier.height(8.dp))
                    val m = trackingTimeLeft / 60
                    val s = trackingTimeLeft % 60
                    Text("Tiempo restante: %02d:%02d".format(m, s))

                }
                Spacer(modifier = Modifier.height(16.dp))

                // botón mapa
                Button(
                    onClick = { navController.navigate(Routes.MAP) },
                    enabled = trackingActive,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(text = "Ver mapa")
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Botón regatas pasadas
                Button(
                    onClick = { navController.navigate(Routes.PAST_REGATTAS) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(text = "Ver regatas pasadas")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botónregatas activas
                Button(
                    onClick = {  navController.navigate(Routes.LIVE_REGATTAS) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(text = "Ver regatas activas")
                }

                // Botones de Configuración
                Button(
                    onClick = { navController.navigate(Routes.SOUND_SETTINGS) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(text = "Configuración de sonido")
                }
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { navController.navigate(Routes.NAV_SETTINGS) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(text = "Configuración de navegación")

                }
                Spacer(modifier = Modifier.height(16.dp))

                // Mostrar información de tracking y sensores si está activo
                if (trackingActive) {
                    Spacer(modifier = Modifier.height(24.dp))
                    if (isSensorAvailable) {
                        SensorPanel(roll = roll, yaw = yaw, pitch = pitch)
                    } else {
                        Text("Sensor de orientación no disponible en este dispositivo.")
                    }
                }
            }
        }
    }

    // sin login
    if (showRestrictedAccessDialog) {
        AlertDialog(
            onDismissRequest = { showRestrictedAccessDialog = false },
            title = { Text("Acceso Restringido") },
            text = { Text("Debes iniciar sesión para acceder a esta funcionalidad.") },
            confirmButton = {
                Button(onClick = {
                    showRestrictedAccessDialog = false
                    navController.navigate(Routes.LOGIN) // Navega a la pantalla de login
                }) {
                    Text("Iniciar Sesión")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestrictedAccessDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}