package com.capypomodoro.audio

import android.content.Context
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * Loops one phase track at a time with Media3 ExoPlayer. Missing or broken files are logged and
 * skipped, so the timer keeps working with whatever audio is present.
 */
class MusicPlayer(context: Context) {

    private val player: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            /* handleAudioFocus = */ true,
        )
        setHandleAudioBecomingNoisy(true)
        setWakeMode(C.WAKE_MODE_LOCAL)
        repeatMode = Player.REPEAT_MODE_ONE
        addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Log.w(TAG, "Can't play $currentFile; add it to assets/audio or change Tracks.kt", error)
                brokenFile = currentFile
            }
        })
    }

    private var currentFile: String? = null
    private var brokenFile: String? = null

    /**
     * @param file asset file name from [Tracks], or null for silence.
     * @param play whether it should be audible right now (timer running and not muted).
     * @param volume 0f..1f
     */
    fun update(file: String?, play: Boolean, volume: Float) {
        player.volume = volume.coerceIn(0f, 1f)
        if (file != currentFile) {
            currentFile = file
            brokenFile = null
            if (file == null) {
                player.stop()
                player.clearMediaItems()
            } else {
                player.setMediaItem(MediaItem.fromUri("asset:///$file"))
                player.prepare()
            }
        }
        player.playWhenReady = play && file != null && file != brokenFile
    }

    fun release() = player.release()

    private companion object {
        const val TAG = "MusicPlayer"
    }
}
