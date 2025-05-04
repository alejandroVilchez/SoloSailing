// com/solosailing/presentation/regatta/PastRouteSimulationScreen.kt
package com.solosailing.presentation.regatta

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.solosailing.viewModel.PastRouteViewModel
import com.solosailing.viewModel.PastPoint
import kotlinx.coroutines.delay
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import com.google.android.gms.maps.CameraUpdateFactory


@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastRouteSimulationScreen(
    regattaId: String,
    vm: PastRouteViewModel = hiltViewModel()
) {
    val points  by vm.points.collectAsState()
    val loading by vm.loading.collectAsState()
    val error   by vm.error.collectAsState()

    // Estado de simulación
    var currentIndex by remember { mutableStateOf(0) }
    var isRunning    by remember { mutableStateOf(false) }

    // 1) Cámara: la creamos una sola vez con remember
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            points.firstOrNull()?.latLng ?: LatLng(0.0, 0.0),
            14f
        )
    }

    // 2) Cuando cambie la lista de puntos, asegúrate de no pasarte de bounds
    LaunchedEffect(points.size) {
        currentIndex = when {
            points.isEmpty() -> 0
            currentIndex > points.lastIndex -> points.lastIndex
            else -> currentIndex
        }
    }

    // 3) Loop de simulación
    LaunchedEffect(isRunning, points) {
        if (isRunning && points.isNotEmpty()) {
            while (isRunning && currentIndex < points.lastIndex) {
                delay(1_000L)
                currentIndex++
            }
            if (currentIndex >= points.lastIndex) {
                isRunning = false
            }
        }
    }

    // 4) Dispara la carga de la ruta
    LaunchedEffect(regattaId) {
        vm.loadRoute(regattaId)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Simulación Regata") }) },
        bottomBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton({ currentIndex = (currentIndex - 5).coerceAtLeast(0) }) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Back 5")
                }
                IconButton({ isRunning = false; currentIndex = 0 }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Restart")
                }
                IconButton({ isRunning = true }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                }
                IconButton({ isRunning = false }) {
                    Icon(Icons.Default.Lock, contentDescription = "Pause")
                }
                IconButton({ currentIndex = (currentIndex + 5).coerceAtMost(points.lastIndex) }) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Forward 5")
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                error   != null ->{
                    if (error is String){
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Center))
                    }
                }
                points.isEmpty() -> Text("No hay puntos para esta regata",
                    Modifier.align(Alignment.Center))
                else -> {
                    // 5) Índice “seguro”
                    val safeIndex = currentIndex.coerceIn(0, points.lastIndex)

                    // 6) Cada vez que cambie safeIndex, animamos la cámara
                    LaunchedEffect(safeIndex) {
                        cameraPositionState.animate(
                            update = CameraUpdateFactory.newLatLng(points[safeIndex].latLng)
                        )
                    }

                    GoogleMap(
                        modifier             = Modifier.fillMaxSize(),
                        cameraPositionState  = cameraPositionState
                    ) {
                        // Dibuja toda la ruta
                        Polyline(
                            points = points.map(PastPoint::latLng),
                            color  = MaterialTheme.colorScheme.primary,
                            width  = 6f
                        )

                        // Marcador del barco en la posición “segura”
                        val current = points[safeIndex]
                        Marker(
                            state    = MarkerState(position = current.latLng),
                            title    = "Barco",
                            rotation = current.yaw.toFloat(),
                            anchor   = Offset(0.5f, 0.5f)
                        )
                    }

                    // 7) Info textual
                    Column(
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Paso ${safeIndex + 1} / ${points.size}")
                        Text("Orientación: ${"%.1f".format(points[safeIndex].yaw)}°")
                    }
                }
            }
        }
    }
}
