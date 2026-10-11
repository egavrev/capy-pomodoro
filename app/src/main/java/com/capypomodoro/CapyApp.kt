package com.capypomodoro

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.capypomodoro.data.AppSettings
import com.capypomodoro.data.SettingsRepository
import com.capypomodoro.timer.PomodoroTimer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/** Process-wide objects, created once in [CapyApp]. */
class AppContainer(context: Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val settingsRepository = SettingsRepository(context.settingsDataStore)

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(scope, SharingStarted.Eagerly, AppSettings())

    val timer = PomodoroTimer(scope, settings)

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        scope.launch { settingsRepository.update(transform) }
    }
}

class CapyApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

val Context.appContainer: AppContainer get() = (applicationContext as CapyApp).container
