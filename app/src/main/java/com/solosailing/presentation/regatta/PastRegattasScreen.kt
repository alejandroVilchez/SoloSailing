package com.solosailing.presentation.regatta

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.solosailing.viewModel.PastRegattasViewModel
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController

@Composable
fun PastRegattasScreen() {
    val vm: PastRegattasViewModel = hiltViewModel()
    val loading by vm.loading.collectAsState()
    val points  by vm.points.collectAsState()

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        val camera = rememberCameraPositionState {
            position = if (points.isNotEmpty())
                CameraPosition.fromLatLngZoom(points.first(), 12f)
            else
                CameraPosition.fromLatLngZoom(LatLng(0.0,0.0), 1f)
        }
        GoogleMap(cameraPositionState = camera, modifier = Modifier.fillMaxSize()) {
            if (points.isNotEmpty()) {
                Polyline(points = points, color = MaterialTheme.colorScheme.primary)
                points.forEachIndexed { i, latLng ->
                    Marker(state = MarkerState(position = latLng),
                        title = "T${i}", snippet = null)
                }
            }
        }
    }
}