package com.solosailing.presentation.regatta

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.solosailing.viewModel.LiveRegattaViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

@Composable
fun LiveRegattaScreen(regattaId: String) {
    val vm: LiveRegattaViewModel = hiltViewModel()
    val route by vm.route.collectAsState()

    LaunchedEffect(regattaId) {
        vm.startListening(regattaId)
    }
    DisposableEffect(Unit) {
        onDispose { vm.stop() }
    }

    val camera = rememberCameraPositionState {
        position = route.firstOrNull()
            ?.let { CameraPosition.fromLatLngZoom(it, 15f) }
            ?: CameraPosition.fromLatLngZoom(LatLng(0.0,0.0), 1f)
    }

    GoogleMap(cameraPositionState = camera, modifier = Modifier.fillMaxSize()) {
        if (route.isNotEmpty()) {
            Polyline(points = route)
            Marker(state = remember{MarkerState(position = route.last())}, title = "Vivo")
        }
    }
}