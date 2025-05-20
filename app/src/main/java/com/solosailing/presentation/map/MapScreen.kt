package com.solosailing.presentation.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.solosailing.R
import com.solosailing.data.remote.dto.ObstacleDto
import com.solosailing.navigation.Routes
import com.solosailing.sensors.SensorPanel
import com.solosailing.viewModel.TrackingViewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.solosailing.data.remote.dto.CreateObstacleRequest
import kotlinx.coroutines.channels.ActorScope
import java.util.UUID

@SuppressLint("MissingPermission", "UnrememberedGetBackStackEntry")
@Composable
fun MapScreen(navController: NavController) {

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val locationViewModel: LocationViewModel = hiltViewModel()

    val parentEntry = remember {
        navController.getBackStackEntry(Routes.HOME)
    }
    val trackingViewModel: TrackingViewModel = hiltViewModel(parentEntry)

    val obstacles by locationViewModel.obstacles.collectAsState()
    val currentLocation by locationViewModel.currentLocation.collectAsState()

    val isLoading by locationViewModel.isLoading.collectAsState()
    val errorMessage by locationViewModel.errorMessage.collectAsState()

    val trackingActive by trackingViewModel.trackingActive.collectAsState()
    val trackingTimeLeft by trackingViewModel.trackingTimeLeft.collectAsState()

    val yaw by trackingViewModel.yaw.collectAsState()
    val pitch by trackingViewModel.pitch.collectAsState()
    val roll by trackingViewModel.roll.collectAsState()

    val isSensorAvailable by trackingViewModel.isSensorAvailable.collectAsState()
    val beachSignalActive by locationViewModel.beachSignalActive.collectAsState()
    val northSignalActive by locationViewModel.northSignalActive.collectAsState()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(41.3851, 2.1734), 0f)
    }

    var hasLocationPermission by remember { mutableStateOf(
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    ) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasLocationPermission = granted }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                val json = stream.bufferedReader().readText()
                locationViewModel.importObstacles(json) { success, err ->
                    if (!success) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(err ?: "Import error")
                        }
                    }
                }
            }
        }
    }

    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var newMarkerLatLng by remember { mutableStateOf<LatLng?>(null) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var obstacleIdToDelete by rememberSaveable { mutableStateOf<String?>(null) }
    var obstacleTypeToDelete by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedMarkerId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            locationViewModel.startLocationUpdates()
        } else {
            Log.e("MapScreen", "Location permission not granted.")
        }
    }
    var isFirstLocationUpdate by remember { mutableStateOf(true) }

    LaunchedEffect(currentLocation) {
        currentLocation?.let { loc ->
            val latLng = LatLng(loc.latitude, loc.longitude)
            if (isFirstLocationUpdate) {
                cameraPositionState.animate(update = CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                isFirstLocationUpdate = false
            } else {
                cameraPositionState.animate(update = CameraUpdateFactory.newLatLng(latLng))
            }
        }
    }
    LaunchedEffect(trackingActive, trackingTimeLeft) {
        if (!trackingActive) {
            navController.navigate(Routes.HOME)
        }
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage?.isNotEmpty() == true) {
            snackbarHostState.showSnackbar(
                message = errorMessage.toString(),
                actionLabel = "Cerrar"
            )
            locationViewModel.clearErrorMessage()
        }
    }

    // Mapa
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = hasLocationPermission, mapType = MapType.NORMAL),
                uiSettings = MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = hasLocationPermission),
                onMapClick = { latLng ->
                    newMarkerLatLng = latLng
                    showAddDialog = true
                    selectedMarkerId = null
                }
            ) {
                // Usuario
                currentLocation?.let { loc ->
                    val userLatLng = LatLng(loc.latitude, loc.longitude)
                    UserLocationMarker(
                        latLang = userLatLng,
                        yaw = yaw.toFloat(),
                        arrowRes = R.drawable.direction_icon1
                    )
                }

                // Marcadores
                obstacles.forEach { obstacle ->
                    val pos = LatLng(obstacle.latitude, obstacle.longitude)
                    val hue = when (obstacle.type) {
                        "Boya" -> BitmapDescriptorFactory.HUE_BLUE
                        //"Bote" -> BitmapDescriptorFactory.fromResource(R.drawable.direction_icon8)
                        "Playa" -> BitmapDescriptorFactory.HUE_YELLOW
                        else -> BitmapDescriptorFactory.HUE_RED
                    }
                    val markerState = rememberUpdatedMarkerState(position = pos)
                    if(obstacle.type == "Bote") { 
                        Marker(
                            state = markerState,
                            title = obstacle.name,
                            icon = BitmapDescriptorFactory.fromResource(R.drawable.direction_icon8),
                            flat = true,
                            onClick = {
                                obstacleIdToDelete = obstacle.id
                                obstacleTypeToDelete = obstacle.type
                                showDeleteDialog = true
                                true

                            }
                        )
                    }else {
                        Marker(
                            state = markerState,
                            title = obstacle.name,
                            icon = BitmapDescriptorFactory.defaultMarker(hue),
                            flat = true,
                            onClick = {
                                obstacleIdToDelete = obstacle.id
                                obstacleTypeToDelete = obstacle.type
                                showDeleteDialog = true
                                true

                            }
                        )
                    }

                }
            }

            // Cargando
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            // Interfaz de Usuario

            Column(
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Botón Señal Playa
                FloatingActionButton(
                    onClick = { locationViewModel.toggleBeachSignal() },
                    //modifier = Modifier.size(48.dp),
                    containerColor = if (beachSignalActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer // Acceso a beachSignalActive
                ) {
                    //Icon(Icons.Default.Star, contentDescription = "Activar/Desactivar Señal Playa") // Icono WavingHand o similar
                    Text("Dirección playa", modifier = Modifier.padding(start = 8.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Botón Señal Norte
                FloatingActionButton(
                    onClick = { locationViewModel.toggleNorthSignal() },
                    //modifier = Modifier.size(48.dp),
                    containerColor = if (northSignalActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer // Acceso a northSignalActive
                ) {
                    //Icon(Icons.Default.Star, contentDescription = "Activar/Desactivar Señal Norte")
                    Text("Dirección norte", modifier = Modifier.padding(start = 8.dp))
                }

            }
            Column(
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
                horizontalAlignment = Alignment.Start
            ){
                // Exportar/Importar
                FloatingActionButton(onClick = {
                    locationViewModel.exportObstacles { uri ->
                        uri?.let {
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_STREAM, it)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(
                                Intent.createChooser(share, "Compartir obstáculos")
                            )
                        } ?: run {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Error al exportar obstáculos",
                                    actionLabel = "Cerrar"
                                )
                            }
                        }
                    }
                }) {
                    Icon(Icons.Default.Share, contentDescription="Exportar")
                }
                Spacer(modifier = Modifier.height(8.dp))
                FloatingActionButton(onClick = {
                    importLauncher.launch(arrayOf("application/json"))
                }) {
                    Icon(Icons.Default.Add, contentDescription="Importar")
                }

            }

            // Sensores
            if (trackingActive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            shape = MaterialTheme.shapes.medium
                        )
                ) {
                    if (isSensorAvailable) {
                        SensorPanel(roll = roll, yaw = yaw, pitch = pitch)
                    } else {
                        Text("Sensor no disponible", modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }


        // Diálogos
        if (showAddDialog && newMarkerLatLng != null) {

            AddObstacleDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { type, name ->
                    locationViewModel.addObstacle(CreateObstacleRequest(
                        latitude = newMarkerLatLng!!.latitude,
                        longitude = newMarkerLatLng!!.longitude,
                        type = type,
                        name = name
                    ))
                    showAddDialog = false
                    newMarkerLatLng = null
                }
            )
        }


        if (showDeleteDialog && obstacleIdToDelete != null) {

            DeleteObstacleDialog(
                obstacleType = obstacleTypeToDelete ?: "Marcador",
                onDismiss = {
                    showDeleteDialog = false
                    obstacleIdToDelete = null
                    obstacleTypeToDelete = null
                    selectedMarkerId = null
                },
                onConfirm = {
                    locationViewModel.removeObstacle(obstacleIdToDelete!!)
                    showDeleteDialog = false
                    obstacleIdToDelete = null
                    obstacleTypeToDelete = null
                    selectedMarkerId = null
                }
            )
        }
    }
}


@Composable
private fun AddObstacleDialog(
    onDismiss: () -> Unit,
    onConfirm: (type: String, name: String) -> Unit
) {
    var selectedType by rememberSaveable { mutableStateOf("Boya") }
    var obstacleName by rememberSaveable { mutableStateOf("") }
    val types = listOf("Boya", "Bote", "Playa")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir Obstáculo") },
        text = {
            Column {
                Text("Tipo:")
                Spacer(modifier = Modifier.height(8.dp))
                types.forEach { type ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedType = type }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = (type == selectedType),
                            onClick = { selectedType = type }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = type.replaceFirstChar { it.uppercase() })
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = obstacleName,
                    onValueChange = { obstacleName = it },
                    label = { Text("Nombre del marcador") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedType, obstacleName.trim()) }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun UserLocationMarker(latLang: LatLng, yaw: Float, @DrawableRes arrowRes: Int) {
    val descriptor = remember(arrowRes) {
        BitmapDescriptorFactory.fromResource(arrowRes)
    }
    val markerState = rememberUpdatedMarkerState(position = latLang)

    Marker(
        state = markerState,
        title = "Mi Ubicación",
        snippet = "Yaw: ${"%.1f".format(yaw)}°",
        icon = descriptor,
        rotation = yaw,
        anchor = Offset(0.5f, 0.5f),
        flat = true,
    )
}

@Composable
private fun DeleteObstacleDialog(
    obstacleType: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eliminar ${obstacleType.replaceFirstChar { it.uppercase() }}") },
        text = { Text("¿Estás seguro?") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Eliminar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}