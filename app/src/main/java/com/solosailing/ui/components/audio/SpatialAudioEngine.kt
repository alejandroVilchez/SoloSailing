package com.solosailing.ui.components.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import androidx.annotation.RawRes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

data class PcmSample(val data: ShortArray, val sampleRate: Int)

private data class Voice(
    val data: ShortArray,
    var pos: Int,
    val gainL: Float,
    val gainR: Float,
    val padL: Int,
    val padR: Int
)

@Singleton
class SpatialAudioEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val sampleCache = mutableMapOf<Int, PcmSample>() // PCM mono samples
    private val voices = mutableListOf<Voice>() // voces activas
    private val voiceLock = Mutex() // para sincronizar acceso a `voices`

    private val sampleRate = 48_000
    private val frameSize = 256 // frames por iteración de mezcla
    private val mixBuf = ShortArray(frameSize * 2) // stereo interleaved

    // AudioTrack en modo STREAM
    private val minBufBytes = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_STEREO,
        AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(frameSize * 2 * 2)

    private val track = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build()
        )
        .setBufferSizeInBytes(minBufBytes)
        .setTransferMode(AudioTrack.MODE_STREAM)
        .build().apply { play() }

    companion object {
        //private const val MAX_DISTANCE_METERS = 300f
        private const val MAX_ITD_MS = 2f   // un poquito más si lo deseas
    }

    init {
        scope.launch {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            while (isActive) {
                mixLockAndWrite()
            }
        }
    }

    /** Carga un raw PCM monofónico (sin header) en memoria */
    fun loadRawPcm(@RawRes resId: Int) {
        if (sampleCache.containsKey(resId)) return
        val bytes = context.resources.openRawResource(resId).use(InputStream::readBytes)
        val shorts = ShortArray(bytes.size / 2)
        ByteBuffer.wrap(bytes)
            .order(ByteOrder.LITTLE_ENDIAN)
            .asShortBuffer()
            .get(shorts)
        sampleCache[resId] = PcmSample(shorts, sampleRate)
    }

    /**
     * Dispara un voice ONE-SHOT aplicando ITD e ILD.
     * Se añade a la lista de voces activas y el mixer lo mezclará en el siguiente buffer.
     */
    fun playSpatial(@RawRes resId: Int, azimuth: Float, distance: Float) {
        //if (distance > MAX_DISTANCE_METERS) return
        val pcm = sampleCache[resId] ?: return
        val mono = pcm.data
        val fs   = pcm.sampleRate

        // ITD clamp
        val itdSec = (distance * sin(Math.toRadians(azimuth.toDouble())) / 343f).toFloat()
        val maxItdSamp = (MAX_ITD_MS / 1000f * fs).roundToInt()
        val ds = (itdSec * fs).roundToInt().coerceIn(-maxItdSamp, maxItdSamp)
        val padL = max(0, ds)
        val padR = max(0, -ds)

        // ILD
        val baseGain = (1f / (1f + distance / 10f)).coerceIn(0f,1f)
        val pan = (azimuth / 180f).coerceIn(-1f,1f)
        val gainL = baseGain * (1f - pan.coerceAtLeast(0f))
        val gainR = baseGain * (1f + pan.coerceAtMost(0f))

        // Creamos la voice
        val voice = Voice(mono, -padL, gainL, gainR, padL, padR)

        scope.launch {
            voiceLock.withLock {
                voices += voice
            }
        }
    }

    /** mixer: mezcla `frameSize` muestras en `mixBuf` y escribe en AudioTrack */
    private suspend fun mixLockAndWrite() {
        voiceLock.withLock {
            // limpia buffer
            mixBuf.fill(0)
            // mezcla cada voice
            val it = voices.iterator()
            while (it.hasNext()) {
                val v = it.next()
                for (f in 0 until frameSize) {
                    val srcIdx = v.pos + f
                    if (srcIdx < 0 || srcIdx >= v.data.size) continue
                    val sample = v.data[srcIdx]
                    val iL = 2 * f
                    val iR = iL + 1
                    mixBuf[iL] = (mixBuf[iL] + (sample * v.gainL).toInt()).coerceIn(Short.MIN_VALUE.toInt(),Short.MAX_VALUE.toInt()).toShort()
                    mixBuf[iR] = (mixBuf[iR] + (sample * v.gainR).toInt()).coerceIn(Short.MIN_VALUE.toInt(),Short.MAX_VALUE.toInt()).toShort()
                }
                // avanzamos posición
                v.pos += frameSize
                // si terminó la voice, la quitamos
                if (v.pos >= v.data.size + v.padR) {
                    it.remove()
                }
            }
        }
        // luego de liberar el lock, escribimos
        track.write(mixBuf, 0, mixBuf.size)
    }

    fun release() {
        scope.cancel()
        track.stop()
        track.release()
    }

    private val mixLock = Mutex() // para serializar la mezcla+write
}
