package com.solosailing.ui.components.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.solosailing.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlin.math.*
import javax.inject.Singleton
import javax.inject.Inject
import kotlin.collections.set

@Singleton
class AudioManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var pool: SoundPool

    // Roll alert
    private var snapSoundId: Int = 0
    private var snapJob: Job? = null
    private var lastGroupCount: Int = 0


    // Señal Norte
    private val northHourIds = mutableMapOf<Int, Int>()
    private var northJob: Job? = null
    private var lastNorthHour: Int? = null

    // Señal Playa
    private val beachHourIds = mutableMapOf<Int, Int>()
    private val distanceSoundIds = mutableMapOf<Float, Int>()
    private var beachJob: Job? = null
    private var lastBeachParams: Pair<Int,Float>? = null

    companion object {
        private const val MIN_SIGNAL_INTERVAL = 5_000L  // 5 s
        private const val DISTANCE_DELAY       = 2_000L // 2 s tras la hora
        private val DISTANCE_BUCKETS = listOf(5f,10f,25f,50f,100f,150f,200f)
    }

    fun initialize() {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        pool = SoundPool.Builder()
            .setAudioAttributes(attrs)
            .setMaxStreams(8)
            .build()

        // Carga tilt (roll)
        snapSoundId = pool.load(context, R.raw.tilt_pcm, 1)

        // Carga horas norte / playa
        for(i in 1..12) {
            northHourIds[i] = context.resources
                .getIdentifier("north_hour_$i","raw",context.packageName)
                .let { pool.load(context, it, 1) }
            beachHourIds[i] = context.resources
                .getIdentifier("beach_hour_$i","raw",context.packageName)
                .let { pool.load(context, it, 1) }
        }
        // Carga audios de distancia
        DISTANCE_BUCKETS.forEach { d ->
            val res = context.resources
                .getIdentifier("distance_${d.toInt()}","raw",context.packageName)
            distanceSoundIds[d] = pool.load(context, res, 1)
        }
    }

    fun scheduleRollAlert(roll: Float, threshold: Float = 10f) {
        val a = abs(roll)
        if (a < threshold) {
            snapJob?.cancel()
            lastGroupCount = 0
            return
        }

        val groupCount = ((a - threshold) / threshold).toInt().coerceIn(0, 2) + 1
        if (snapJob?.isActive == true && groupCount == lastGroupCount) return
        lastGroupCount = groupCount

        val pan = if (roll < 0f) +1f else -1f
        val vol = 1f
        val volL = if (pan < 0f) vol * (1f + pan) else vol
        val volR = if (pan > 0f) vol * (1f - pan) else vol

        snapJob?.cancel()
        snapJob = scope.launch {
            while (isActive) {
                repeat(groupCount) {
                    pool.play(snapSoundId, volL, volR, 1, 0, 1f)
                    delay(100L)
                }
                delay(1000L)
            }
        }
    }

    fun stopRollAlert() { snapJob?.cancel() }

    /** Señal Norte: sólo (re)lanza si cambia la hora de reloj */
    fun scheduleNorthSignal(heading: Float, thresholdAngle: Float = 30f) {
        // Azimut relativo norte
        val az  = ((-heading + 360) % 360).let { if (it > 180) it - 360 else it }
        if (abs(az) > thresholdAngle) {
            lastNorthHour = null
            northJob?.cancel()
            return
        }
        val hour = azimuthToHour(az)
        if (northJob?.isActive == true && lastNorthHour == hour) return

        lastNorthHour = hour
        northJob?.cancel()
        northJob = scope.launch {
            while (isActive) {
                pool.play(northHourIds[hour] ?: return@launch,1f,1f,1,0,1f)
                delay(MIN_SIGNAL_INTERVAL)
            }
        }
    }
    fun stopNorthSignal() {
        lastNorthHour = null
        northJob?.cancel()
    }

    /** Señal Playa: ping de hora + distancia, sólo si cambia hora/distancia */
    fun scheduleBeachSignal(
        beachAzimuth: Float,
        distance: Float,
        minDistance: Float,
        thresholdMeters: Float = minDistance
    ) {
        if (distance > thresholdMeters) {
            lastBeachParams = null
            beachJob?.cancel()
            return
        }
        val hour   = azimuthToHour(beachAzimuth)
        val bucket = DISTANCE_BUCKETS.firstOrNull { distance <= it } ?: DISTANCE_BUCKETS.last()
        val params = hour to bucket
        if (beachJob?.isActive == true && lastBeachParams == params) return

        lastBeachParams = params
        beachJob?.cancel()
        beachJob = scope.launch {
            val vol = ((thresholdMeters - distance)/thresholdMeters).coerceIn(0f,1f)
            while (isActive) {
                pool.play(beachHourIds[hour] ?: return@launch, vol, vol,1,0,1f)
                delay(DISTANCE_DELAY)
                pool.play(distanceSoundIds[bucket] ?: return@launch, vol, vol,1,0,1f)
                delay(MIN_SIGNAL_INTERVAL - DISTANCE_DELAY)
            }
        }
    }
    fun stopBeachSignal() {
        lastBeachParams = null
        beachJob?.cancel()
    }

    private fun azimuthToHour(az: Float): Int {
        val deg = (az + 360) % 360
        return ((deg / 30f).roundToInt().coerceIn(0,11) + 1)
    }

    fun release() {
        scope.cancel()
        snapJob?.cancel()
        northJob?.cancel()
        beachJob?.cancel()
        pool.release()
    }
}