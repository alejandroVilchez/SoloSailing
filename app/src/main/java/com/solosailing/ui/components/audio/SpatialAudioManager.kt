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

    companion object {
        private const val MIN_SIGNAL_INTERVAL = 5_000L
        private const val DISTANCE_DELAY      = 2_000L
    }

    private var rollJob: Job?  = null
    private var northJob: Job? = null
    private var beachJob: Job? = null
    private var lastHour = -1
    private var lastBeachParams: Pair<Int,Float>? = null

    fun initialize() {
        listOf(tiltRes, buoyRes).forEach(engine::loadRawPcm)
        northRes.values.forEach(engine::loadRawPcm)
        beachRes.values.forEach(engine::loadRawPcm)
        distRes.values.forEach(engine::loadRawPcm)
        boatRes.values.forEach(engine::loadRawPcm)
    }

    /** Roll alert */
    fun scheduleRollAlert(roll: Float, threshold: Float = 10f) {
        val mag = abs(roll)
        if (mag < threshold) { rollJob?.cancel(); return }
        val count = ((mag - threshold)/threshold).toInt().coerceIn(1,3)
        rollJob?.cancel()
        rollJob = scope.launch {
            while (isActive) {
                repeat(count) {
                    val pan = if (roll<0) -30f else +30f
                    engine.playSpatial(tiltRes, pan, 0f)
                    delay(100L)
                }
                delay(1000L)
            }
        }
    }
    fun stopRollAlert() = rollJob?.cancel()

    /** North signal */
    fun scheduleNorthSignal(heading: Float, thresh: Float = 30f) {
        val az = ((-heading+360)%360).let{ if (it>180) it-360 else it }
        if (abs(az)>thresh) { northJob?.cancel(); lastHour=-1; return }
        val hour = ((az+360)/30).roundToInt().coerceIn(1,12)
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
        val hour   = ((az+360)/30).roundToInt().coerceIn(1,12)
        val bucket = distRes.keys.first { dist<=it }
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
