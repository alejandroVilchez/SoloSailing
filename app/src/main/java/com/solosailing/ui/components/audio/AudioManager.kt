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

@Singleton
class AudioManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var pool: SoundPool

    // Obstáculos
    private val obstacleSoundIds = mutableMapOf<String, Int>()
    private val obstacleStreams  = mutableMapOf<String, Int>()

    // Inclinación (roll)
    private var snapSoundId: Int = 0
    private var snapJob: Job? = null

    // Horas playa / norte
    private val beachHourIds = mutableMapOf<Int, Int>()
    private val northHourIds = mutableMapOf<Int, Int>()
    private val distanceSoundIds = mutableMapOf<Float, Int>()
    private var beachJob: Job? = null
    private var northJob: Job? = null

    companion object {
        private const val MIN_SIGNAL_INTERVAL = 5_000L  // 5 segundos
        private const val DISTANCE_DELAY       = 2_000L // 2 segundos tras la hora
        private val DISTANCE_BUCKETS = listOf(5f,10f,25f,50f,100f,150f,200f)
    }

    fun initialize() {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        pool = SoundPool.Builder()
            .setAudioAttributes(attrs)
            .setMaxStreams(12)
            .build()


        for(i in 1..12) {
            val res = context.resources.getIdentifier("beach_hour_$i","raw",context.packageName)
            beachHourIds[i] = pool.load(context, res, 1)
        }
        for(i in 1..12) {
            val res = context.resources.getIdentifier("north_hour_$i","raw",context.packageName)
            northHourIds[i] = pool.load(context, res, 1)
        }
        DISTANCE_BUCKETS.forEach { d ->
            val name = "distance_${d.toInt()}"
            val res  = context.resources.getIdentifier(name,"raw",context.packageName)
            distanceSoundIds[d] = pool.load(context, res, 1)
        }
        // Carga tilt
        snapSoundId = pool.load(context, R.raw.tilt, 1)
    }

    fun scheduleObstacleAlert(type: String, distance: Float, azimuth: Float, minDistance: Float) {
        val sid = obstacleSoundIds[type] ?: return
        val vol = ((minDistance - distance)/minDistance).coerceIn(0f,1f)
        val pan = (azimuth/90f).coerceIn(-1f,1f)
        val left  = if(pan<0) 1f else 1f-pan
        val right = if(pan>0) 1f else 1f+pan
        obstacleStreams[type]?.let {
            pool.setVolume(it, vol*left, vol*right)
        } ?: run {
            val stream = pool.play(sid, vol*left,vol*right,1,-1,1f)
            obstacleStreams[type]=stream
        }
    }
    fun stopObstacleAlert(type:String){
        obstacleStreams.remove(type)?.let{ pool.stop(it) }
    }

    fun scheduleRollAlert(roll: Float, threshold: Float = 10f) {
        val a = abs(roll)
        if (a < threshold) { snapJob?.cancel(); return }
        val interval = ((1f - ((a - threshold) / (90f - threshold))).coerceIn(0f,1f)*900 + 100).toLong()
        if (snapJob?.isActive == true) return
        snapJob?.cancel()
        snapJob = scope.launch {
            while (isActive) {
                pool.play(snapSoundId,1f,1f,1,0,1f)
                delay(interval)
            }
        }
    }
    fun stopRollAlert(){ snapJob?.cancel() }

    fun scheduleBeachSignal(
        beachAzimuth: Float,    // [-180..180]
        distance: Float,
        minDistance: Float,
        thresholdMeters: Float = minDistance
    ) {
        if (distance > thresholdMeters) {
            beachJob?.cancel()
            return
        }
        val hour = azimuthToHour(beachAzimuth)
        val vol = ((thresholdMeters - distance)/thresholdMeters).coerceIn(0f,1f)

        beachJob?.cancel()
        beachJob = scope.launch {
            while (isActive) {
                pool.play(beachHourIds[hour] ?: return@launch, vol, vol, 1, 0, 1f)

                delay(DISTANCE_DELAY)
                val bucket = DISTANCE_BUCKETS.firstOrNull { distance <= it } ?: DISTANCE_BUCKETS.last()
                distanceSoundIds[bucket]?.let { dsid ->
                    pool.play(dsid, vol, vol, 1, 0, 1f)
                }

                delay(MIN_SIGNAL_INTERVAL - DISTANCE_DELAY)
            }
        }
    }
    fun stopBeachSignal(){ beachJob?.cancel() }

    fun scheduleNorthSignal(
        heading: Float,
        thresholdAngle: Float = 30f
    ) {
        // Azimut relativo norte
        val az = ((-heading + 360) % 360).let { if(it>180) it-360 else it }
        if (abs(az) > thresholdAngle) {
            northJob?.cancel()
            return
        }
        val hour = azimuthToHour(az)
        northJob?.cancel()
        northJob = scope.launch {
            while (isActive) {
                pool.play(northHourIds[hour] ?: return@launch, 1f, 1f, 1, 0, 1f)
                delay(MIN_SIGNAL_INTERVAL)
            }
        }
    }
    fun stopNorthSignal(){ northJob?.cancel() }

    private fun azimuthToHour(az: Float): Int {
        val deg = (az+360)%360
        val h = ((deg/30f).roundToInt() % 12).let{ if(it==0)12 else it }
        return h
    }

    fun release(){
        scope.cancel()
        obstacleStreams.values.forEach{ pool.stop(it) }
        snapJob?.cancel()
        northJob?.cancel()
        beachJob?.cancel()
        pool.release()
    }
}