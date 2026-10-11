package com.capypomodoro.audio

import androidx.annotation.StringRes
import com.capypomodoro.R
import com.capypomodoro.data.AppSettings
import com.capypomodoro.timer.Phase

enum class TrackGroup { FOCUS, BREAK }

/**
 * One music option in Settings.
 *
 * @param file name of a looping audio file in the repo's `assets/audio/` folder, or null for "No music".
 *   Swap a sound by replacing the file (or pointing this at a new one) and rebuilding.
 */
data class Track(
    val id: String,
    val group: TrackGroup,
    @StringRes val title: Int,
    val file: String?,
)

/** Every sound the app plays. This is the only file to touch when changing music. */
object Tracks {
    const val NONE = "none"
    const val DEFAULT_FOCUS = "rain"
    const val DEFAULT_BREAK = "birds"

    /** Short bell played when a phase ends (SoundPool, so keep it small and uncompressed-friendly). */
    const val CHIME_FILE = "chime.ogg"

    val focus: List<Track> = listOf(
        Track("rain", TrackGroup.FOCUS, R.string.track_rain, "focus_lofi_rain.ogg"),
        Track("piano", TrackGroup.FOCUS, R.string.track_piano, "focus_soft_piano.ogg"),
        Track("cafe", TrackGroup.FOCUS, R.string.track_cafe, "focus_cafe_murmur.ogg"),
        Track(NONE, TrackGroup.FOCUS, R.string.track_none, null),
    )

    val breaks: List<Track> = listOf(
        Track("birds", TrackGroup.BREAK, R.string.track_birds, "break_birdsong_piano.ogg"),
        Track("ocean", TrackGroup.BREAK, R.string.track_ocean, "break_ocean_waves.ogg"),
        Track("musicbox", TrackGroup.BREAK, R.string.track_musicbox, "break_music_box.ogg"),
        Track(NONE, TrackGroup.BREAK, R.string.track_none, null),
    )

    fun byId(id: String): Track? = focus.firstOrNull { it.id == id } ?: breaks.firstOrNull { it.id == id }

    fun forPhase(phase: Phase, s: AppSettings): Track =
        if (phase == Phase.FOCUS) focus.firstOrNull { it.id == s.focusTrackId } ?: focus.first()
        else breaks.firstOrNull { it.id == s.breakTrackId } ?: breaks.first()
}
