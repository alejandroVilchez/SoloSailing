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
import com.solosailing.viewModel.PastRegattaViewModel
import com.solosailing.viewModel.PastPoint
import kotlinx.coroutines.delay
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory


@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastRegattaScreen(
    regattaId: String,
    vm: PastRegattaViewModel = hiltViewModel()
) {
    val points  by vm.points.collectAsState()
    val loading by vm.loading.collectAsState()
    val error   by vm.error.collectAsState()

    var idx by remember { mutableStateOf(0) }
    var running by remember { mutableStateOf(false) }

    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(points.firstOrNull()?.latLng ?: LatLng(0.0,0.0), 14f)
    }

    LaunchedEffect(regattaId) { vm.loadRoute(regattaId) }
    LaunchedEffect(points.size) {
        idx = idx.coerceIn(0, points.lastIndex.coerceAtLeast(0))
    }
    LaunchedEffect(running) {
        while (running && idx < points.lastIndex) {
            delay(1_000L)
            idx++
        }
        if (idx>=points.lastIndex) running = false
    }

    Scaffold(
        topBar = { TopAppBar(title={Text("Regata pasada")}) },
        bottomBar = {
            Row(Modifier.fillMaxWidth().padding(8.dp), Arrangement.SpaceEvenly) {
                IconButton({ idx = (idx-5).coerceAtLeast(0) }) { Icon(Icons.Default.KeyboardArrowLeft, null) }
                IconButton({ running=false; idx=0 }) { Icon(Icons.Default.Refresh, null) }
                IconButton({ running=true }) { Icon(Icons.Default.PlayArrow, null) }
                IconButton({ running=false }) { Icon(Icons.Default.Lock, null) }
                IconButton({ idx = (idx+5).coerceAtMost(points.lastIndex) }) {
                    Icon(Icons.Default.KeyboardArrowRight, null)
                }
            }
        }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when {
                loading-> CircularProgressIndicator(Modifier.align(Alignment.Center))
                error != null-> Text(
                    text = error ?: "Error desconocido",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center))
                points.isEmpty()-> Text("Sin puntos", Modifier.align(Alignment.Center))
                else -> {
                    val safe = idx.coerceIn(0, points.lastIndex)
                    LaunchedEffect(safe) {
                        cameraState.animate(
                            update = CameraUpdateFactory.newLatLng(points[safe].latLng)
                        )
                    }
                    GoogleMap(cameraPositionState=cameraState, modifier=Modifier.fillMaxSize()) {
                        Polyline(points=points.map{it.latLng}, color=MaterialTheme.colorScheme.primary, width=6f)
                        val cur = points[safe]
                        Marker(
                            state=MarkerState(cur.latLng),
                            rotation=cur.yaw,
                            anchor=Offset(0.5f,0.5f)
                        )
                    }
                    Column(Modifier.align(Alignment.TopCenter).padding(8.dp), horizontalAlignment=Alignment.CenterHorizontally) {
                        Text("Paso ${safe+1} / ${points.size}")
                        Text("Yaw: ${"%.1f".format(points[safe].yaw)}°")
                    }
                }
            }
        }
    }
}
