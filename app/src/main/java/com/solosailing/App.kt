package com.solosailing

import android.os.Bundle
import android.util.Log
import android.Manifest
import android.content.Intent
import android.support.v4.media.session.MediaSessionCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.media.session.MediaButtonReceiver
import com.solosailing.navigation.AppNavGraph // Importa tu NavGraph
import com.solosailing.ui.theme.SoloSailingTheme
import dagger.hilt.android.AndroidEntryPoint // Importa la anotación

@AndroidEntryPoint // Hilt necesita esto para inyectar en Activities/Fragments
class App : ComponentActivity() {

    private val requestLocation = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        Log.i("App", "Location permission granted: $granted")
    }
    private lateinit var mediaSession: MediaSessionCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaSession = MediaSessionCompat(this, "SoloSailingSession").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            // Opcional - poner PendingIntent global
            // val pi = ... PendingIntent para MEDIA_BUTTON ...
            // setMediaButtonReceiver(pi)
            isActive = true
        }
        requestLocation.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        setContent {
            SoloSailingTheme {
                AppNavGraph()
            }
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        MediaButtonReceiver.handleIntent(mediaSession, intent)
    }

    fun getMediaSession(): MediaSessionCompat = mediaSession

    override fun onDestroy() {
        super.onDestroy()
        if (::mediaSession.isInitialized) {
            mediaSession.release()
        }
    }
}