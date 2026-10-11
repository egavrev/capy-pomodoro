package com.capypomodoro.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import java.io.IOException

/** Plays the short end-of-phase bell through SoundPool. */
class ChimePlayer(context: Context) {

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(1)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private var soundId = 0
    private var loaded = false
    private var pendingVolume: Float? = null

    init {
        pool.setOnLoadCompleteListener { _, id, status ->
            if (id == soundId && status == 0) {
                loaded = true
                pendingVolume?.let { play(it) }
                pendingVolume = null
            }
        }
        soundId = try {
            context.assets.openFd(Tracks.CHIME_FILE).use { pool.load(it, 1) }
        } catch (e: IOException) {
            Log.w("ChimePlayer", "No ${Tracks.CHIME_FILE} in assets/audio; the chime is silent", e)
            0
        }
    }

    fun play(volume: Float) {
        if (soundId == 0) return
        val v = volume.coerceIn(0f, 1f)
        if (!loaded) {
            pendingVolume = v
            return
        }
        pool.play(soundId, v, v, 1, 0, 1f)
    }

    fun release() = pool.release()
}
