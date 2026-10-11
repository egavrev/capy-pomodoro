package com.capypomodoro.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.capypomodoro.MainActivity
import com.capypomodoro.R
import java.util.Locale

/** Builds the ongoing timer notification and the "phase finished" alert. */
object TimerNotifications {
    const val CHANNEL_TIMER = "timer"
    const val CHANNEL_ALERTS = "phase_finished"
    const val ID_ONGOING = 1
    const val ID_FINISHED = 2

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_TIMER, context.getString(R.string.channel_timer), NotificationManager.IMPORTANCE_LOW).apply {
                description = context.getString(R.string.channel_timer_desc)
                setShowBadge(false)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERTS, context.getString(R.string.channel_alerts), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = context.getString(R.string.channel_alerts_desc)
                // The app plays its own chime, so the alert itself stays silent.
                setSound(null, null)
            },
        )
    }

    fun ongoing(context: Context, state: TimerState): Notification {
        val time = formatTime(state.secondsLeft)
        val builder = NotificationCompat.Builder(context, CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_stat_bow)
            .setColor(ContextCompat.getColor(context, R.color.rose))
            .setContentTitle(titleFor(context, state))
            .setContentIntent(openApp(context))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setProgress(1000, (state.progress * 1000).toInt(), false)

        if (state.isRunning) {
            // The system counts down by itself, so we don't have to re-post every second.
            builder.setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis() + state.remainingMs)
                .setContentText(context.getString(lineFor(state.phase)))
                .addAction(0, context.getString(R.string.action_pause), serviceAction(context, TimerService.ACTION_TOGGLE))
        } else {
            builder.setShowWhen(false)
                .setContentText(context.getString(R.string.notif_paused, time))
                .addAction(0, context.getString(R.string.action_resume), serviceAction(context, TimerService.ACTION_TOGGLE))
        }
        builder.addAction(0, context.getString(R.string.action_skip), serviceAction(context, TimerService.ACTION_SKIP))
        return builder.build()
    }

    fun finished(context: Context, event: PhaseFinished): Notification =
        NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_bow)
            .setColor(ContextCompat.getColor(context, R.color.rose))
            .setContentTitle(context.getString(R.string.notif_done_title, context.getString(labelFor(event.finished))))
            .setContentText(context.getString(R.string.notif_done_text, context.getString(labelFor(event.next)).lowercase()))
            .setContentIntent(openApp(context))
            .setAutoCancel(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

    private fun titleFor(context: Context, state: TimerState): String {
        val phase = context.getString(labelFor(state.phase))
        return if (state.phase == Phase.FOCUS) {
            "$phase · " + context.getString(R.string.round_of, state.round, state.rounds)
        } else phase
    }

    fun labelFor(phase: Phase): Int = when (phase) {
        Phase.FOCUS -> R.string.phase_focus
        Phase.SHORT_BREAK -> R.string.phase_short
        Phase.LONG_BREAK -> R.string.phase_long
    }

    fun lineFor(phase: Phase): Int = when (phase) {
        Phase.FOCUS -> R.string.line_focus
        Phase.SHORT_BREAK -> R.string.line_short
        Phase.LONG_BREAK -> R.string.line_long
    }

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun serviceAction(context: Context, action: String): PendingIntent = PendingIntent.getService(
        context, action.hashCode(),
        Intent(context, TimerService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

fun formatTime(totalSeconds: Int): String {
    val s = totalSeconds.coerceAtLeast(0)
    return String.format(Locale.ROOT, "%02d:%02d", s / 60, s % 60)
}
