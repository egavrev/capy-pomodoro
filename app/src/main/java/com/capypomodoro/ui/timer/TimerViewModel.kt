package com.capypomodoro.ui.timer

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.capypomodoro.AppContainer
import com.capypomodoro.appContainer
import com.capypomodoro.audio.Track
import com.capypomodoro.audio.Tracks
import com.capypomodoro.data.AppSettings
import com.capypomodoro.timer.Phase
import com.capypomodoro.timer.TimerService
import com.capypomodoro.timer.TimerState
import com.capypomodoro.ui.components.CapyArt
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class TimerUiState(
    val timer: TimerState,
    val settings: AppSettings,
    val muted: Boolean,
) {
    val track: Track get() = Tracks.forPhase(timer.phase, settings)
    val art: CapyArt get() = CapyArt.forPhase(timer.phase)
}

/** Screen-facing wrapper around the app-wide [com.capypomodoro.timer.PomodoroTimer]. */
class TimerViewModel(
    private val app: Application,
    private val container: AppContainer,
) : ViewModel() {

    private val timer = container.timer

    val uiState: StateFlow<TimerUiState> =
        combine(timer.state, container.settings, timer.muted, ::TimerUiState)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                TimerUiState(timer.state.value, container.settings.value, timer.muted.value),
            )

    fun onToggle() {
        val wasRunning = timer.state.value.isRunning
        timer.toggle()
        // Starting from the UI is what brings up the foreground service (allowed: we're on screen).
        if (!wasRunning && timer.state.value.isRunning) TimerService.start(app)
    }

    fun onReset() = timer.reset()
    fun onSkip() = timer.skip()
    fun onSelectPhase(phase: Phase) = timer.selectPhase(phase)
    fun onToggleMute() = timer.toggleMute()

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                TimerViewModel(app, app.appContainer)
            }
        }
    }
}
