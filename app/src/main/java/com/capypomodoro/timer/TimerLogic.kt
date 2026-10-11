package com.capypomodoro.timer

import com.capypomodoro.data.AppSettings
import kotlin.math.ceil

enum class Phase {
    FOCUS, SHORT_BREAK, LONG_BREAK;

    val isBreak: Boolean get() = this != FOCUS
}

/**
 * Snapshot of the Pomodoro timer.
 *
 * @param round the current focus round (1..[rounds]). During a break it is the round just finished.
 * @param endAtMs monotonic clock time (elapsedRealtime) at which the running phase ends; only valid while running.
 * @param sessionActive true from the moment the user starts the timer until they reset it, pick a phase
 *   tab, or a phase ends without auto-start. The foreground service lives exactly as long as this is true.
 */
data class TimerState(
    val phase: Phase,
    val round: Int,
    val rounds: Int,
    val durationMs: Long,
    val remainingMs: Long,
    val isRunning: Boolean = false,
    val endAtMs: Long = 0L,
    val sessionActive: Boolean = false,
) {
    /** 0 at the start of a phase, 1 when it ends. */
    val progress: Float
        get() = if (durationMs <= 0L) 0f else (1f - remainingMs.toFloat() / durationMs).coerceIn(0f, 1f)

    /** Whole seconds shown on screen (rounded up, so 00:00 only appears at the very end). */
    val secondsLeft: Int get() = ceil(remainingMs / 1000.0).toInt()

    /** Focus sessions finished in the current cycle: the filled round dots. */
    val completedRounds: Int get() = if (phase == Phase.FOCUS) round - 1 else round

    val isPristine: Boolean get() = !isRunning && remainingMs == durationMs

    val next: Pair<Phase, Int> get() = TimerLogic.next(phase, round, rounds)
}

/** Emitted when a phase runs down to 00:00 on its own (not when skipped). */
data class PhaseFinished(val finished: Phase, val next: Phase, val autoStarted: Boolean)

/** Pure state transitions, kept free of Android and coroutines so they can be unit-tested. */
object TimerLogic {

    fun durationMs(phase: Phase, s: AppSettings): Long = 60_000L * when (phase) {
        Phase.FOCUS -> s.focusMinutes
        Phase.SHORT_BREAK -> s.shortBreakMinutes
        Phase.LONG_BREAK -> s.longBreakMinutes
    }

    /** Focus → Short break → Focus … and after the last round a Long break, then round 1 again. */
    fun next(phase: Phase, round: Int, rounds: Int): Pair<Phase, Int> = when (phase) {
        Phase.FOCUS -> if (round >= rounds) Phase.LONG_BREAK to round else Phase.SHORT_BREAK to round
        Phase.SHORT_BREAK -> Phase.FOCUS to (round + 1).coerceAtMost(rounds)
        Phase.LONG_BREAK -> Phase.FOCUS to 1
    }

    fun initial(s: AppSettings): TimerState {
        val d = durationMs(Phase.FOCUS, s)
        return TimerState(phase = Phase.FOCUS, round = 1, rounds = s.rounds, durationMs = d, remainingMs = d)
    }

    fun start(state: TimerState, now: Long): TimerState =
        if (state.isRunning || state.remainingMs <= 0) state
        else state.copy(isRunning = true, endAtMs = now + state.remainingMs, sessionActive = true)

    fun pause(state: TimerState, now: Long): TimerState =
        if (!state.isRunning) state
        else state.copy(isRunning = false, remainingMs = (state.endAtMs - now).coerceAtLeast(0L))

    /** Restarts the current phase (picking up any new duration) and stops the timer. */
    fun reset(state: TimerState, s: AppSettings): TimerState {
        val d = durationMs(state.phase, s)
        return state.copy(
            rounds = s.rounds, round = state.round.coerceAtMost(s.rounds),
            durationMs = d, remainingMs = d, isRunning = false, sessionActive = false,
        )
    }

    /** A phase tab was tapped: switch to it, reset the time and stop. */
    fun select(state: TimerState, phase: Phase, s: AppSettings): TimerState {
        val d = durationMs(phase, s)
        return state.copy(
            phase = phase, rounds = s.rounds, round = state.round.coerceAtMost(s.rounds),
            durationMs = d, remainingMs = d, isRunning = false, sessionActive = false,
        )
    }

    /** Jump to the next phase in the cycle. A running timer keeps running. */
    fun skip(state: TimerState, s: AppSettings, now: Long): TimerState =
        advance(state, s, now, keepRunning = state.isRunning).copy(sessionActive = state.sessionActive)

    /**
     * Moves the clock forward. Returns the new state plus a [PhaseFinished] event when the phase
     * reached 00:00.
     */
    fun tick(state: TimerState, s: AppSettings, now: Long): Pair<TimerState, PhaseFinished?> {
        if (!state.isRunning) return state to null
        val remaining = state.endAtMs - now
        if (remaining > 0) return state.copy(remainingMs = remaining) to null

        val auto = s.autoStartNext
        // Start the next phase from the moment the last one ended, so a late tick loses no time.
        val nextState = advance(state, s, now = state.endAtMs, keepRunning = auto)
        val caughtUp = if (auto) nextState.copy(remainingMs = (nextState.endAtMs - now).coerceAtLeast(0L)) else nextState
        return caughtUp.copy(sessionActive = auto) to PhaseFinished(state.phase, nextState.phase, auto)
    }

    /**
     * Settings changed. Durations apply to the next phase start, not to one already under way,
     * but a phase that hasn't been started yet picks up the new length straight away.
     */
    fun applySettings(state: TimerState, s: AppSettings): TimerState {
        val withRounds = state.copy(rounds = s.rounds, round = state.round.coerceAtMost(s.rounds))
        if (!state.isPristine) return withRounds
        val d = durationMs(state.phase, s)
        return withRounds.copy(durationMs = d, remainingMs = d)
    }

    private fun advance(state: TimerState, s: AppSettings, now: Long, keepRunning: Boolean): TimerState {
        val (phase, round) = next(state.phase, state.round, s.rounds)
        val d = durationMs(phase, s)
        return TimerState(
            phase = phase, round = round, rounds = s.rounds,
            durationMs = d, remainingMs = d,
            isRunning = keepRunning, endAtMs = if (keepRunning) now + d else 0L,
            sessionActive = keepRunning,
        )
    }
}
