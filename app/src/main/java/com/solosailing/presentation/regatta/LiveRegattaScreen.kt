// LiveRegattaScreen.kt
package com.solosailing.presentation.regatta

import android.annotation.SuppressLint
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.solosailing.viewModel.LiveRegattaViewModel
import com.solosailing.viewModel.AuthViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.*
import com.solosailing.R
import androidx.compose.ui.geometry.Offset
import com.google.android.gms.maps.model.CameraPosition
import com.solosailing.viewModel.LiveRegattasListViewModel
import kotlinx.coroutines.launch

@SuppressLint("UnrememberedMutableState")
@Composable
fun LiveRegattaScreen(
    regattaId: String,
    vm: LiveRegattaViewModel = hiltViewModel(),
    authVm: AuthViewModel     = hiltViewModel(),
    listVm: LiveRegattasListViewModel = hiltViewModel()
) {
    val positions by vm.positions.collectAsState()
    val token     by authVm.userToken.collectAsState()

    LaunchedEffect(regattaId, token) {
        token?.let { vm.start(regattaId) }
    }
    DisposableEffect(regattaId) {
        onDispose {
            vm.stop()
            listVm.viewModelScope.launch { listVm.stopSimulation(regattaId) }
        }
    }

    val first = positions.values.firstOrNull()?.firstOrNull()?.latLng
    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(first ?: LatLng(0.0,0.0), if (first!=null)15f else 5f)
    }

    GoogleMap(cameraPositionState = cameraState, modifier = Modifier.fillMaxSize()) {
        positions.entries.forEachIndexed { idx, (boatId, route) ->
            route.lastOrNull()?.let { pt ->
                val icon = when(idx%9) {
                    0->R.drawable.direction_icon1;1->R.drawable.direction_icon2;2->R.drawable.direction_icon3
                    3->R.drawable.direction_icon4;4->R.drawable.direction_icon5;5->R.drawable.direction_icon6
                    6->R.drawable.direction_icon7;7->R.drawable.direction_icon8;else->R.drawable.direction_icon9
                }
                Marker(
                    state = MarkerState(pt.latLng),
                    title = boatId,
                    rotation = pt.yaw,
                    anchor = Offset(0.5f, 0.5f),
                    icon = BitmapDescriptorFactory.fromResource(icon)
                )
            }
        }
    }
}