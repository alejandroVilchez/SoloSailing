package com.solosailing.sensors

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext // Importa la anotación de Hilt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject // Importa Inject
import javax.inject.Singleton // Importa Singleton

@Singleton // Hilt creará una sola instancia de esta clase
class SensorsManager @Inject constructor( // Hilt llamará a este constructor
    @ApplicationContext context: Context // Hilt inyectará el Context de la aplicación
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    // Scope para operaciones internas si fueran necesarias fuera de onSensorChanged
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _yaw = MutableStateFlow(0f)
    val yaw: StateFlow<Float> = _yaw.asStateFlow() // Exponer como StateFlow inmutable

    private val _pitch = MutableStateFlow(0f)
    val pitch: StateFlow<Float> = _pitch.asStateFlow()

    private val _roll = MutableStateFlow(0f)
    val roll: StateFlow<Float> = _roll.asStateFlow()

    // Estado para saber si el sensor está disponible
    private val _isSensorAvailable = MutableStateFlow(rotationSensor != null)
    val isSensorAvailable: StateFlow<Boolean> = _isSensorAvailable.asStateFlow()

    private val SMOOTHING = 0.2f
    private var smoothedYaw = 0f
    private var smoothedPitch = 0f
    private var smoothedRoll = 0f
    private var calibrationOffset: Float? = null // Considera persistir esto (DataStore/Prefs)

    init {
        if (rotationSensor != null) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME) // Usar GAME o UI para mejor respuesta
            Log.d("SensorsManager", "Rotation Vector sensor listener registered.")
        } else {
            Log.w("SensorsManager", "Rotation Vector sensor not available.")

        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, it.values)
                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientation)

                val rawYaw = Math.toDegrees(orientation[0].toDouble()).toFloat()
                val rawPitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
                val rawRoll = Math.toDegrees(orientation[2].toDouble()).toFloat()

                smoothedYaw = smoothedYaw * (1 - SMOOTHING) + rawYaw * SMOOTHING
                smoothedPitch = smoothedPitch * (1 - SMOOTHING) + rawPitch * SMOOTHING
                smoothedRoll = smoothedRoll * (1 - SMOOTHING) + rawRoll * SMOOTHING

                val adjustedYaw = calibrationOffset?.let { offset ->
                    ((smoothedYaw - offset) + 360) % 360
                } ?: ((smoothedYaw + 360) % 360)

                // Asignar directamente al .value (asumiendo que onSensorChanged corre en Main o es seguro)
                _yaw.value = adjustedYaw
                _pitch.value = smoothedPitch
                _roll.value = smoothedRoll
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* No-op por ahora */ }

    fun calibrateNorth() {
        calibrationOffset = smoothedYaw
        // Aquí podrías guardar el offset en DataStore/Prefs si quieres persistencia
        Log.d("SensorsManager", "North calibrated. Offset: $calibrationOffset")
    }


    fun stopSensorUpdates() {
        sensorManager.unregisterListener(this)
        Log.d("SensorsManager", "Sensor listener unregistered.")
        scope.cancel() // Cancela coroutines internas si las hubiera
    }

    fun startSensorUpdates() {
        if (_isSensorAvailable.value) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME)
            Log.d("SensorsManager", "Sensor listener re-registered.")
        }
    }
}
@Composable
fun SensorPanel(roll: Float, yaw: Float, pitch: Float) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Inclinación: ${"%.1f".format(roll)}°")
        Text(text = "Orientación (Yaw): ${"%.1f".format(yaw)}°")
        Text(text = "Pitch: ${"%.1f".format(pitch)}°")
    }
}

