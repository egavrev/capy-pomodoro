package com.capypomodoro.timer

import android.os.SystemClock
import com.capypomodoro.data.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The single source of truth for the timer, shared by the UI (through TimerViewModel) and by
 * TimerService (notification, music, chime). It lives for the whole process, so the countdown keeps
 * going when the activity is closed; the foreground service keeps the process alive meanwhile.
 *
 * All calls must be made on the main thread.
 */
class PomodoroTimer(
    private val scope: CoroutineScope,
    private val settings: StateFlow<AppSettings>,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) {
    private val _state = MutableStateFlow(TimerLogic.initial(settings.value))
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private val _muted = MutableStateFlow(false)
    /** Quick music override from the timer screen. The chime still plays when muted. */
    val muted: StateFlow<Boolean> = _muted.asStateFlow()

    private val _events = MutableSharedFlow<PhaseFinished>(extraBufferCapacity = 8)
    val events: SharedFlow<PhaseFinished> = _events.asSharedFlow()

    private var ticker: Job? = null

    init {
        scope.launch {
            settings.collect { s -> _state.update { TimerLogic.applySettings(it, s) } }
        }
    }

    fun toggle() = if (_state.value.isRunning) pause() else start()

    fun start() {
        _state.update { TimerLogic.start(it, clock()) }
        ensureTicking()
    }

    fun pause() {
        _state.update { TimerLogic.pause(it, clock()) }
    }

    fun reset() {
        _state.update { TimerLogic.reset(it, settings.value) }
    }

    fun skip() {
        _state.update { TimerLogic.skip(it, settings.value, clock()) }
        ensureTicking()
    }

    fun selectPhase(phase: Phase) {
        _state.update { TimerLogic.select(it, phase, settings.value) }
    }

    fun toggleMute() = _muted.update { !it }

    private fun ensureTicking() {
        if (!_state.value.isRunning || ticker?.isActive == true) return
        ticker = scope.launch {
            while (isActive && _state.value.isRunning) {
                val (next, finished) = TimerLogic.tick(_state.value, settings.value, clock())
                _state.value = next
                if (finished != null) _events.tryEmit(finished)
                // Wake up just after the next whole second flips, capped so the ring animates smoothly.
                val untilNextSecond = next.remainingMs % 1000L
                delay(if (untilNextSecond in 1..250) untilNextSecond + 5 else 250L)
            }
        }
    }
}
