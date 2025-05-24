package com.solosailing.presentation.regatta

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.solosailing.App
import com.solosailing.R
import com.solosailing.viewModel.RegattaSimulationViewModel
import kotlinx.coroutines.delay
import androidx.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaSessionCompat
import android.view.KeyEvent


@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegattaSimulationScreen(
    vm: RegattaSimulationViewModel = hiltViewModel(),
) {
    val buoys by vm.buoys.collectAsState()
    val routes by vm.positions.collectAsState()
    val simIdx by vm.simIdx.collectAsState()

    val northSignalActive by vm.northSignalActive.collectAsState()

    var expanded by remember { mutableStateOf(false) }
    var selectedId by remember { mutableStateOf(routes.keys.firstOrNull() ?: "") }

    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            routes[selectedId]?.getOrNull(0)?.latLng ?: LatLng(0.0,0.0),
            14f
        )
    }
    val context = LocalContext.current
    val mediaSession = remember { (context as App).getMediaSession() }
    val mediaButtonIntent = remember {
        Intent(Intent.ACTION_MEDIA_BUTTON)
            .setClass(context, MediaButtonReceiver::class.java)
    }
    val mediaButtonPendingIntent = remember {
        PendingIntent.getBroadcast(
            context, 0,
            mediaButtonIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    var running by remember { mutableStateOf(false) }
    DisposableEffect(mediaSession, mediaButtonPendingIntent) {
        mediaSession.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )
        mediaSession.setMediaButtonReceiver(mediaButtonPendingIntent)
        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            override fun onMediaButtonEvent(intent: Intent): Boolean {
                val ev = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                if (ev?.action == KeyEvent.ACTION_DOWN) {
                    when (ev.keyCode) {
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            running = !running
                            vm.toggleNorthSignal()
                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_NEXT -> {
                            vm.advance()
                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                            vm.rewind()
                            return true
                        }
                    }
                }
                return super.onMediaButtonEvent(intent)
            }
        })
        mediaSession.isActive = true
        onDispose {
            mediaSession.isActive = false
            mediaSession.setCallback(null)
            mediaSession.setMediaButtonReceiver(null)
        }
    }

    LaunchedEffect(running) {
        while (running) {
            delay(500L)
            vm.nextStep()
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
                        routes.keys.forEach { boat ->
                            DropdownMenuItem(
                                text = { Text(boat) },
                                onClick = {
                                    selectedId = boat
                                    vm.selectBoat(boat)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                // controles
                Row {
                    IconButton(onClick = { vm.rewind() }) {
                        Icon(Icons.Default.KeyboardArrowLeft, null)
                    }
                    IconButton(onClick = { vm.reset() }) { Icon(Icons.Default.Refresh, null) }
                    IconButton(onClick = { running = true }) { Icon(Icons.Default.PlayArrow, null) }
                    IconButton(onClick = { running = false }) { Icon(Icons.Default.Lock, null) }
                    IconButton(onClick = { vm.advance() }) {
                        Icon(Icons.Default.KeyboardArrowRight, null)
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            // para evitar out of bounds
            val positionsByBoat = remember(routes, simIdx) {
                routes.mapValues { (_, route) ->
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
                positionsByBoat.entries.forEachIndexed { index, (boatId, pt) ->
                    val iconResId = when (index % 9) {
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
            Column(
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Botón Señal Norte
                FloatingActionButton(
                    onClick = { vm.toggleNorthSignal() },
                    //modifier = Modifier.size(48.dp),
                    containerColor = if (northSignalActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                ) {
                    //Icon(Icons.Default.Star, contentDescription = "Activar/Desactivar Señal Norte")
                    //align
                    Text("Norte")
                }
            }
        }
    }
}
