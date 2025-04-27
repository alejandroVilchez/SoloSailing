package com.solosailing.ui.components.audio


import android.content.Context
import android.media.MediaPlayer
import androidx.navigation.NavHostController
import com.solosailing.R

object SoundConfig {

    private var currentPlayer: MediaPlayer? = null

    enum class Category(val label: String, val sounds: List<Sound>) {
        OBSTACLES("Obstacles", listOf(
            Sound("Boya", R.raw.buoy),
            Sound("Bote", R.raw.boat)
        )),
        BEACH_HOURS("Beach Hours", (1..12).map {
            Sound("Hour $it", getRawIdByName("beach_hour_$it"))
        }),
        DISTANCES("Distances", listOf(
            Sound("5m", R.raw.distance_5),
            Sound("10m", R.raw.distance_10),
            Sound("25m", R.raw.distance_25),
            Sound("50m", R.raw.distance_50),
            Sound("100m", R.raw.distance_100),
            Sound("150m", R.raw.distance_150),
            Sound("200m", R.raw.distance_200),
        )),
        NORTH_HOURS("North Hours", (1..12).map {
            Sound("Hour $it", getRawIdByName("north_hour_$it"))
        })
    }

    data class Sound(val name: String, val resId: Int)

    fun play(context: Context, sound: Sound, onComplete: (() -> Unit)? = null) {
        stop()
        currentPlayer = MediaPlayer.create(context, sound.resId).apply {
            setOnCompletionListener {
                onComplete?.invoke()
                release()
            }
            start()
        }
    }

    fun stop() {
        currentPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        currentPlayer = null
    }

    private fun getRawIdByName(name: String): Int {
        return R.raw::class.java.getField(name).getInt(null)
    }
}
