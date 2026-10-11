package com.capypomodoro.timer

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.capypomodoro.appContainer
import com.capypomodoro.audio.ChimePlayer
import com.capypomodoro.audio.MusicPlayer
import com.capypomodoro.audio.Tracks
import com.capypomodoro.data.AppSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Foreground service that runs while a Pomodoro session is active. It keeps the process alive
 * with the timer notification (Pause/Resume + Skip), loops the phase music and plays the chime.
 * The countdown itself lives in [PomodoroTimer].
 */
class TimerService : Service() {

    private val scope = MainScope()
    private lateinit var music: MusicPlayer
    private lateinit var chime: ChimePlayer
    private var wakeLock: PowerManager.WakeLock? = null
    private var inForeground = false
    private var stopJob: Job? = null

    private val timer get() = appContainer.timer
    private val settings get() = appContainer.settings

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        TimerNotifications.ensureChannels(this)
        music = MusicPlayer(this)
        chime = ChimePlayer(this)

        // Notification: re-post only when something visible changes (the countdown runs by itself).
        scope.launch {
            timer.state
                .map { s -> NotificationKey(s.phase, s.round, s.rounds, s.isRunning, s.sessionActive, if (s.isRunning) (s.progress * 50).toInt() else s.secondsLeft) }
                .distinctUntilChanged()
                .collect { onSessionChanged(timer.state.value) }
        }

        // Music follows phase, running state, mute and volume.
        scope.launch {
            combine(timer.state, settings, timer.muted) { s, st, muted ->
                MusicKey(
                    file = if (s.sessionActive) Tracks.forPhase(s.phase, st).file else null,
                    play = s.isRunning && !muted,
                    volume = st.volume / 100f,
                )
            }.distinctUntilChanged().collect { music.update(it.file, it.play, it.volume) }
        }

        scope.launch {
            timer.events.collect { event -> onPhaseFinished(event, settings.value) }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> timer.toggle()
            ACTION_SKIP -> timer.skip()
        }
        // startForegroundService() requires startForeground() promptly, even if there is nothing to show.
        val state = timer.state.value
        if (!inForeground) enterForeground(TimerNotifications.ongoing(this, state))
        onSessionChanged(state)
        return START_NOT_STICKY
    }

    private fun onSessionChanged(state: TimerState) {
        if (!state.sessionActive) {
            leaveForeground()
            return
        }
        stopJob?.cancel()
        val nm = getSystemService(NotificationManager::class.java)
        nm.cancel(TimerNotifications.ID_FINISHED)
        val notification = TimerNotifications.ongoing(this, state)
        if (!inForeground) enterForeground(notification)
        else nm.notify(TimerNotifications.ID_ONGOING, notification)
        updateWakeLock(state)
    }

    private fun enterForeground(notification: android.app.Notification) {
        try {
            ServiceCompat.startForeground(
                this, TimerNotifications.ID_ONGOING, notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0,
            )
            inForeground = true
        } catch (e: Exception) {
            // e.g. ForegroundServiceStartNotAllowedException; the timer still works while the app is open.
            Log.w(TAG, "Couldn't enter the foreground", e)
        }
    }

    private fun leaveForeground() {
        updateWakeLock(null)
        if (inForeground) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            inForeground = false
        }
        // Linger briefly so a chime that just started isn't cut off.
        stopJob?.cancel()
        stopJob = scope.launch {
            delay(4_000)
            if (!timer.state.value.sessionActive) stopSelf()
        }
    }

    private fun onPhaseFinished(event: PhaseFinished, s: AppSettings) {
        if (s.chime) chime.play((s.volume / 100f).coerceAtLeast(0.35f))
        if (!event.autoStarted) {
            getSystemService(NotificationManager::class.java)
                .notify(TimerNotifications.ID_FINISHED, TimerNotifications.finished(this, event))
        }
    }

    /** A partial wake lock keeps the countdown (and the chime) on time with the screen off. */
    @SuppressLint("WakelockTimeout")
    private fun updateWakeLock(state: TimerState?) {
        if (state?.isRunning == true) {
            val lock = wakeLock ?: getSystemService(PowerManager::class.java)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CapyPomodoro:timer")
                .also { it.setReferenceCounted(false); wakeLock = it }
            // Re-armed on every change; the timeout is only a safety net.
            lock.acquire(state.remainingMs + 60_000L)
        } else {
            wakeLock?.takeIf { it.isHeld }?.release()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        updateWakeLock(null)
        music.release()
        chime.release()
        super.onDestroy()
    }

    private data class NotificationKey(
        val phase: Phase, val round: Int, val rounds: Int,
        val running: Boolean, val active: Boolean, val tick: Int,
    )

    private data class MusicKey(val file: String?, val play: Boolean, val volume: Float)

    companion object {
        private const val TAG = "TimerService"
        const val ACTION_START = "com.capypomodoro.action.START"
        const val ACTION_TOGGLE = "com.capypomodoro.action.TOGGLE"
        const val ACTION_SKIP = "com.capypomodoro.action.SKIP"

        /** Call from the UI right after the user starts the timer. */
        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(
                    context, Intent(context, TimerService::class.java).setAction(ACTION_START),
                )
            } catch (e: Exception) {
                Log.w(TAG, "Couldn't start the timer service", e)
            }
        }
    }
}
