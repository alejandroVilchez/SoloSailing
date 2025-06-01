package com.solosailing.ui.components.audio

import android.content.Context
import androidx.annotation.RawRes
import com.solosailing.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.roundToInt

@Singleton
class SpatialAudioManager @Inject constructor(
    private val engine: SpatialAudioEngine,
    @ApplicationContext private val ctx: Context
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // tus PCM:
    private val tiltRes  = R.raw.tilt_pcm
    private val northRes = (1..12).associateWith {
        ctx.resources.getIdentifier("north_hour_${it}_pcm", "raw", ctx.packageName)
    }
    private val beachRes = (1..12).associateWith {
        ctx.resources.getIdentifier("beach_hour_${it}_pcm", "raw", ctx.packageName)
    }
    private val distRes  = listOf(5f,10f,25f,50f,100f,150f,200f)
        .associateWith { d ->
            ctx.resources.getIdentifier("distance_${d.toInt()}_pcm", "raw", ctx.packageName)
        }
    private val boatRes  = (1..9).associateWith {
        ctx.resources.getIdentifier("boat_${it}_pcm","raw",ctx.packageName)
    }
    private val buoyRes  =
        ctx.resources.getIdentifier("buoy_pcm","raw",ctx.packageName)

    private val buoyNumRes = (1..4).associateWith {
        ctx.resources.getIdentifier("boya_${it}_pcm", "raw", ctx.packageName)
    }
    private val dirHour = (1..12).associateWith {
        ctx.resources.getIdentifier("direccion_${it}_pcm", "raw", ctx.packageName)
    }

    companion object {
        private const val MIN_SIGNAL_INTERVAL = 5_000L
        private const val DISTANCE_DELAY      = 2_000L
    }

    private var rollJob: Job?  = null
    private var northJob: Job? = null
    private var beachJob: Job? = null
    private var buoyJob: Job? = null
    private var directionJob: Job? = null
    private var lastRollCount = 0
    private var lastHour = -1
    private var lastBeachParams: Pair<Int,Float>? = null
    private var lastBuoyParams: Pair<Int,Float>? = null

    private fun azimuthToHour(az: Float): Int {
        // lo llevamos a 0..360
        val deg = (az % 360 + 360) % 360
        // cada sector de 30° es una “hora”, floor+1 → [1..12]
        val h = (deg / 30f).toInt() + 1
        return h.coerceIn(1, 12)
    }

    fun initialize() {
        listOf(tiltRes, buoyRes).forEach(engine::loadRawPcm)
        northRes.values.forEach(engine::loadRawPcm)
//        beachRes.values.forEach(engine::loadRawPcm)
//        distRes.values.forEach(engine::loadRawPcm)
        beachRes.values.filter { it != 0 }.forEach(engine::loadRawPcm) //para que no pete cuando no hay marcador
        distRes .values.filter { it != 0 }.forEach(engine::loadRawPcm)
        boatRes.values.forEach(engine::loadRawPcm)
        buoyNumRes.values.forEach(engine::loadRawPcm)
        dirHour.values.forEach(engine::loadRawPcm)
    }

    /** Roll alert */
    fun scheduleRollAlert(roll: Float) {
        // umbral “10°”
        val absRoll = abs(roll)
        val count = when {
            absRoll < 15f -> 0
            absRoll < 25f -> 1
            absRoll < 35f -> 2
            else -> 3
        }
        if (count == 0) {
            rollJob?.cancel()
            return
        }
        rollJob?.let {
            if (it.isActive && lastRollCount == count) return
            it.cancel()
        }
        lastRollCount = count
        rollJob = scope.launch {
            while (isActive) {
                repeat(count) {
                    val pan = if (roll < 0f) -1f else +1f   // -1=izq, +1=der
                    engine.playSpatial(tiltRes, pan * 90f, 0f)
                    delay(100L)
                }
                delay(1_000L)
            }
        }
    }
    fun stopRollAlert() = rollJob?.cancel()

    /** North signal */
    fun scheduleNorthSignal(heading: Float, thresh: Float = 30f) {
        val az = ((-heading+360)%360).let{ if (it>180) it-360 else it }
        if (abs(az)>thresh) { northJob?.cancel(); lastHour=-1; return }
        //val hour = ((az+360)/30).roundToInt().coerceIn(1,12)
        val hour = azimuthToHour(az)
        if (northJob?.isActive==true && lastHour==hour) return
        lastHour=hour; northJob?.cancel()
        val res = northRes[hour] ?: return
        northJob = scope.launch {
            while (isActive) {
                engine.playSpatial(res, az, 0f)
                delay(MIN_SIGNAL_INTERVAL)
            }
        }
    }
    fun stopNorthSignal() = northJob?.cancel()

    /** Beach signal */
    fun scheduleBeachSignal(az: Float, dist: Float) {
        //val hour   = ((az+360)/30).roundToInt().coerceIn(1,12)
        val hour = azimuthToHour(az)
        val bucket = distRes.keys.firstOrNull { dist <= it }?: distRes.keys.maxOrNull()!!
        val params = hour to bucket
        if (beachJob?.isActive==true && lastBeachParams==params) return
        lastBeachParams=params; beachJob?.cancel()
        val resH = beachRes[hour] ?: return
        val resD = distRes[bucket] ?: return
        beachJob = scope.launch {
            while (isActive) {
                engine.playSpatial(resH, az, dist)
                delay(DISTANCE_DELAY)
                engine.playSpatial(resD, az, dist)
                delay(MIN_SIGNAL_INTERVAL - DISTANCE_DELAY)
            }
        }
    }
    fun stopBeachSignal() = beachJob?.cancel()

    fun scheduleBuoySignal(az: Float, dist: Float, num: Int) {
        val hour = azimuthToHour(az)
        val bucket = distRes.keys.firstOrNull { dist <= it }?: distRes.keys.maxOrNull()!!
        val params = hour to bucket
        if (buoyJob?.isActive==true && lastBuoyParams==params) return
        lastBuoyParams=params; buoyJob?.cancel()
        val resNum = buoyNumRes[num] ?: return
        val resH = dirHour[hour] ?: return
        val resD = distRes[bucket] ?: return
        buoyJob = scope.launch {
            while (isActive) {
                engine.playSpatial(resNum, az, dist)
                delay (2_000L)
                engine.playSpatial(resH, az, dist)
                delay(2_000L)
                engine.playSpatial(resD, az, dist)
                delay(MIN_SIGNAL_INTERVAL - DISTANCE_DELAY)
            }
        }
    }
    fun stopBuoySignal() = buoyJob?.cancel()

    fun playBoat(index: Int, azimuth: Float, distance: Float) {
        boatRes[index]?.let { engine.playSpatial(it, azimuth, distance) }
    }
    fun playBuoy(azimuth: Float, distance: Float) {
        engine.playSpatial(buoyRes, azimuth, distance)
    }


    fun release() {
        scope.cancel()
        engine.release()
    }
}
