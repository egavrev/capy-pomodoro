package com.capypomodoro.timer

import com.capypomodoro.data.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerLogicTest {

    private val s = AppSettings(focusMinutes = 25, shortBreakMinutes = 5, longBreakMinutes = 15, rounds = 4)
    private val min = 60_000L

    @Test
    fun cycleRunsFocusShortFocusUntilLongBreakThenRestarts() {
        val seen = mutableListOf<Pair<Phase, Int>>()
        var phase = Phase.FOCUS
        var round = 1
        repeat(9) {
            seen += phase to round
            val n = TimerLogic.next(phase, round, rounds = 4)
            phase = n.first; round = n.second
        }
        assertEquals(
            listOf(
                Phase.FOCUS to 1, Phase.SHORT_BREAK to 1,
                Phase.FOCUS to 2, Phase.SHORT_BREAK to 2,
                Phase.FOCUS to 3, Phase.SHORT_BREAK to 3,
                Phase.FOCUS to 4, Phase.LONG_BREAK to 4,
                Phase.FOCUS to 1,
            ),
            seen,
        )
    }

    @Test
    fun countdownFinishesAndAutoStartsNextPhase() {
        var st = TimerLogic.start(TimerLogic.initial(s), now = 0L)
        assertTrue(st.isRunning)

        val (mid, e1) = TimerLogic.tick(st, s, now = 10 * min)
        assertNull(e1)
        assertEquals(15 * min, mid.remainingMs)

        val (next, e2) = TimerLogic.tick(mid, s, now = 25 * min + 500)
        assertNotNull(e2)
        assertEquals(PhaseFinished(Phase.FOCUS, Phase.SHORT_BREAK, autoStarted = true), e2)
        assertEquals(Phase.SHORT_BREAK, next.phase)
        assertTrue(next.isRunning)
        assertTrue(next.sessionActive)
        // The 500ms the tick was late are already taken off the break.
        assertEquals(5 * min - 500, next.remainingMs)
        assertEquals(1, next.completedRounds)
    }

    @Test
    fun withoutAutoStartThePhaseEndsPaused() {
        val manual = s.copy(autoStartNext = false)
        val st = TimerLogic.start(TimerLogic.initial(manual), now = 0L)
        val (next, e) = TimerLogic.tick(st, manual, now = 25 * min)
        assertEquals(false, e!!.autoStarted)
        assertFalse(next.isRunning)
        assertFalse(next.sessionActive)
        assertEquals(Phase.SHORT_BREAK, next.phase)
        assertEquals(5 * min, next.remainingMs)
    }

    @Test
    fun pauseKeepsRemainingTimeAndResumeContinues() {
        val st = TimerLogic.start(TimerLogic.initial(s), now = 0L)
        val paused = TimerLogic.pause(st, now = 2 * min)
        assertFalse(paused.isRunning)
        assertEquals(23 * min, paused.remainingMs)
        assertTrue(paused.sessionActive)

        val resumed = TimerLogic.start(paused, now = 100 * min)
        assertEquals(123 * min, resumed.endAtMs)
    }

    @Test
    fun skipKeepsRunningStateAndResetRestartsPhase() {
        val running = TimerLogic.start(TimerLogic.initial(s), now = 0L)
        val skipped = TimerLogic.skip(running, s, now = 1_000L)
        assertEquals(Phase.SHORT_BREAK, skipped.phase)
        assertTrue(skipped.isRunning)

        val reset = TimerLogic.reset(TimerLogic.pause(skipped, now = 60_000L), s)
        assertEquals(5 * min, reset.remainingMs)
        assertFalse(reset.sessionActive)
    }

    @Test
    fun newDurationAppliesOnlyToAPhaseNotYetStarted() {
        val idle = TimerLogic.initial(s)
        assertEquals(30 * min, TimerLogic.applySettings(idle, s.copy(focusMinutes = 30)).remainingMs)

        val running = TimerLogic.tick(TimerLogic.start(idle, 0L), s, 5 * min).first
        val after = TimerLogic.applySettings(running, s.copy(focusMinutes = 30))
        assertEquals(25 * min, after.durationMs)
        assertEquals(20 * min, after.remainingMs)
    }

    @Test
    fun selectingATabStopsAndResets() {
        val running = TimerLogic.start(TimerLogic.initial(s), now = 0L)
        val long = TimerLogic.select(running, Phase.LONG_BREAK, s)
        assertEquals(Phase.LONG_BREAK, long.phase)
        assertEquals(15 * min, long.remainingMs)
        assertFalse(long.isRunning)
        assertFalse(long.sessionActive)
    }

    @Test
    fun secondsLeftRoundsUp() {
        val st = TimerLogic.initial(s).copy(remainingMs = 59_001L)
        assertEquals(60, st.secondsLeft)
        assertEquals("01:00", formatTime(st.secondsLeft))
        assertEquals("00:00", formatTime(0))
    }
}
