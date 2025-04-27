package com.solosailing

import android.os.Bundle
import android.util.Log
import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.solosailing.navigation.AppNavGraph // Importa tu NavGraph
import com.solosailing.ui.theme.SoloSailingTheme
import dagger.hilt.android.AndroidEntryPoint // Importa la anotación

@AndroidEntryPoint // Hilt necesita esto para inyectar en Activities/Fragments
class App : ComponentActivity() {

    private val requestLocation = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        Log.i("App", "Location permission granted: $granted")
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestLocation.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        setContent {
            SoloSailingTheme {
                AppNavGraph()
            }
        }
    }
}