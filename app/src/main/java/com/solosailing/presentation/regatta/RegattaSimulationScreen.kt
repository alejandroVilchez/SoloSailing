// com/solosailing/presentation/regatta/IntroSimulationScreen.kt
package com.solosailing.presentation.regatta

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.solosailing.R
import com.solosailing.viewModel.RegattaSimulationViewModel
import com.solosailing.viewModel.IntroPoint
import kotlinx.coroutines.delay

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegattaSimulationScreen(
    vm: RegattaSimulationViewModel = viewModel()
) {
    val buoys by vm.buoys.collectAsState()
    val allRoutes by vm.positions.collectAsState()

    // 1) Dropdown de barcos para seguir cámara
    var expanded by remember { mutableStateOf(false) }
    var selectedId by remember { mutableStateOf(allRoutes.keys.firstOrNull() ?: "") }

    // 2) Índice global de simulación
    var simIdx by remember { mutableStateOf(0) }
    var running by remember { mutableStateOf(false) }

    // 3) Cámara “follow”
    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            allRoutes[selectedId]?.getOrNull(0)?.latLng ?: LatLng(0.0,0.0),
            14f
        )
    }

//    LaunchedEffect(allRoutes.keys, selectedId) {
//        simIdx = 0
//        if (allRoutes.containsKey(selectedId).not()) {
//            selectedId = allRoutes.keys.firstOrNull() ?: ""
//        }
//    }

    LaunchedEffect(running) {
        while (running) {
            delay(500L)
            simIdx++
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Demo Introductoria") })
        },
        bottomBar = {
            Row(Modifier.fillMaxWidth().padding(8.dp), Arrangement.SpaceBetween) {
                // dropdown
                Box {
                    TextButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.List, null)
                        Text(selectedId)
                    }
                    DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                        allRoutes.keys.forEach { boat ->
                            DropdownMenuItem(
                                text = { Text(boat) },
                                onClick = {
                                    selectedId = boat
                                    //simIdx = 0
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                // controles
                Row {
                    IconButton(onClick = { simIdx = (simIdx - 50).coerceAtLeast(0) }) {
                        Icon(Icons.Default.KeyboardArrowLeft, null)
                    }
                    IconButton(onClick = { simIdx = 0 }) { Icon(Icons.Default.Refresh, null) }
                    IconButton(onClick = { running = true }) { Icon(Icons.Default.PlayArrow, null) }
                    IconButton(onClick = { running = false }) { Icon(Icons.Default.Lock, null) }
                    IconButton(onClick = { simIdx = (simIdx + 50).coerceAtMost(allRoutes[selectedId]?.lastIndex ?: 0) }) {
                        Icon(Icons.Default.KeyboardArrowRight, null)
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            // para evitar out of bounds
            val positionsByBoat = remember(allRoutes, simIdx) {
                allRoutes.mapValues { (_, route) ->
                    route.getOrNull(simIdx.coerceIn(0, route.lastIndex))!!
                }
            }
            // cámara al barco seleccionado
            LaunchedEffect(simIdx, selectedId) {
                positionsByBoat[selectedId]?.latLng
                    ?.let { CameraUpdateFactory.newLatLng(it) }
                    ?.let { cameraState.animate(update = it) }
            }

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraState
            ) {
                buoys.forEach { buoy ->
                    Marker(state = MarkerState(buoy.latLng), title = "Boya ${buoy.name}")
                }
                positionsByBoat.entries.forEachIndexed { index, (boatId, pt) ->                    val iconResId = when (index % 9) {
                        0 -> R.drawable.direction_icon1
                        1 -> R.drawable.direction_icon2
                        2 -> R.drawable.direction_icon3
                        3 -> R.drawable.direction_icon4
                        4 -> R.drawable.direction_icon5
                        5 -> R.drawable.direction_icon6
                        6 -> R.drawable.direction_icon7
                        7 -> R.drawable.direction_icon8
                        else -> R.drawable.direction_icon9
                    }
                    Marker(
                        state = MarkerState(pt.latLng),
                        title = boatId,
                        rotation = pt.yaw,
                        anchor = Offset(0.5f, 0.5f),
                        icon = BitmapDescriptorFactory.fromResource(iconResId)
                    )
                }
            }
        }
    }
}
