package com.solosailing.ui.components.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.annotation.RawRes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioSequencer @Inject constructor(@ApplicationContext private val ctx: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val pool: SoundPool by lazy {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        SoundPool.Builder()
            .setAudioAttributes(attrs)
            .setMaxStreams(16)
            .build()
    }

    private val jobs = mutableListOf<Job>()

    fun loadSample(@RawRes resId: Int): Int = pool.load(ctx, resId, 1)

    fun playSequence(events: List<AudioEvent>) {
        jobs.forEach { it.cancel() }
        jobs.clear()

        events.forEach { ev ->
            val job = scope.launch {
                while (isActive) {
                    playWithSpatialization(ev)
                    val minMs = 200L
                    val maxMs = 2000L
                    val t = ((ev.distance / PROX_METERS)
                        .coerceIn(0f, 1f) * (maxMs - minMs) + minMs).toLong()
                    delay(t)
                }
            }
            jobs += job
        }
    }

    private fun playWithSpatialization(ev: AudioEvent) {
        val vol = (1f / (1f + ev.distance * ev.distance / (PROX_METERS*PROX_METERS)))
            .coerceIn(0f,1f)

        val pan = (-ev.azimuth / 90f).coerceIn(-1f,1f)

        val volL = if (pan < 0) vol * (1f + pan) else vol
        val volR = if (pan > 0) vol * (1f - pan) else vol

        pool.play(ev.soundId, volL, volR, 1, 0, 1f)
    }


    fun release() {
        jobs.forEach { it.cancel() }
        scope.cancel()
        pool.release()
    }

    companion object {
        private const val PROX_METERS = 100f
    }
}
