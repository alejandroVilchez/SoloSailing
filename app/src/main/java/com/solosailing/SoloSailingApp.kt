package com.solosailing // Ajusta tu paquete

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SoloSailingApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}