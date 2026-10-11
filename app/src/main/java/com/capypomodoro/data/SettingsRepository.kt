package com.capypomodoro.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.capypomodoro.audio.Tracks
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

enum class BackgroundStyle { DOTS, GINGHAM, CREAM, ROOM }

/** Everything the Settings screen controls. Defaults match HANDOFF.md. */
data class AppSettings(
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val rounds: Int = 4,
    val autoStartNext: Boolean = true,
    val chime: Boolean = true,
    val keepScreenOn: Boolean = false,
    val background: BackgroundStyle = BackgroundStyle.DOTS,
    val focusTrackId: String = Tracks.DEFAULT_FOCUS,
    val breakTrackId: String = Tracks.DEFAULT_BREAK,
    val volume: Int = 60,
) {
    companion object {
        val FOCUS_RANGE = 5..90
        const val FOCUS_STEP = 5
        val SHORT_RANGE = 1..30
        const val SHORT_STEP = 1
        val LONG_RANGE = 5..60
        const val LONG_STEP = 5
        val ROUNDS_RANGE = 1..10
        const val ROUNDS_STEP = 1
    }

    /** Keeps values inside their ranges, whatever was stored. */
    fun sanitized(): AppSettings = copy(
        focusMinutes = focusMinutes.coerceIn(FOCUS_RANGE),
        shortBreakMinutes = shortBreakMinutes.coerceIn(SHORT_RANGE),
        longBreakMinutes = longBreakMinutes.coerceIn(LONG_RANGE),
        rounds = rounds.coerceIn(ROUNDS_RANGE),
        volume = volume.coerceIn(0, 100),
        focusTrackId = if (Tracks.byId(focusTrackId) != null) focusTrackId else Tracks.DEFAULT_FOCUS,
        breakTrackId = if (Tracks.byId(breakTrackId) != null) breakTrackId else Tracks.DEFAULT_BREAK,
    )
}

/** Persists [AppSettings] in Preferences DataStore. Every change is written straight away. */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val focus = intPreferencesKey("focus_minutes")
        val short = intPreferencesKey("short_break_minutes")
        val long = intPreferencesKey("long_break_minutes")
        val rounds = intPreferencesKey("rounds")
        val autoStart = booleanPreferencesKey("auto_start_next")
        val chime = booleanPreferencesKey("chime")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val background = stringPreferencesKey("background")
        val focusTrack = stringPreferencesKey("focus_track")
        val breakTrack = stringPreferencesKey("break_track")
        val volume = intPreferencesKey("volume")
    }

    val settings: Flow<AppSettings> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.toSettings() }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { p ->
            val current = p.toSettings()
            val next = transform(current).sanitized()
            p[Keys.focus] = next.focusMinutes
            p[Keys.short] = next.shortBreakMinutes
            p[Keys.long] = next.longBreakMinutes
            p[Keys.rounds] = next.rounds
            p[Keys.autoStart] = next.autoStartNext
            p[Keys.chime] = next.chime
            p[Keys.keepScreenOn] = next.keepScreenOn
            p[Keys.background] = next.background.name
            p[Keys.focusTrack] = next.focusTrackId
            p[Keys.breakTrack] = next.breakTrackId
            p[Keys.volume] = next.volume
        }
    }

    private fun Preferences.toSettings(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            focusMinutes = this[Keys.focus] ?: d.focusMinutes,
            shortBreakMinutes = this[Keys.short] ?: d.shortBreakMinutes,
            longBreakMinutes = this[Keys.long] ?: d.longBreakMinutes,
            rounds = this[Keys.rounds] ?: d.rounds,
            autoStartNext = this[Keys.autoStart] ?: d.autoStartNext,
            chime = this[Keys.chime] ?: d.chime,
            keepScreenOn = this[Keys.keepScreenOn] ?: d.keepScreenOn,
            background = this[Keys.background]
                ?.let { name -> BackgroundStyle.entries.firstOrNull { it.name == name } }
                ?: d.background,
            focusTrackId = this[Keys.focusTrack] ?: d.focusTrackId,
            breakTrackId = this[Keys.breakTrack] ?: d.breakTrackId,
            volume = this[Keys.volume] ?: d.volume,
        ).sanitized()
    }
}
