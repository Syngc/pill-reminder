package com.pillreminder.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import java.time.LocalDate

/** Receives the exact alarm and hands it to [AlarmService], which rings and speaks. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val time = intent.getStringExtra(EXTRA_TIME) ?: return
        val (date, attempt) = when (intent.action) {
            ACTION_RING -> LocalDate.now() to 1
            ACTION_RETRY -> LocalDate.ofEpochDay(intent.getLongExtra(EXTRA_DATE, LocalDate.now().toEpochDay())) to
                intent.getIntExtra(EXTRA_ATTEMPT, 2)
            else -> return
        }
        ContextCompat.startForegroundService(context, AlarmService.ringIntent(context, date, time, attempt))
    }

    companion object {
        const val ACTION_RING = "com.pillreminder.app.RING"
        const val ACTION_RETRY = "com.pillreminder.app.RETRY"
        const val EXTRA_TIME = "time"
        const val EXTRA_DATE = "date"
        const val EXTRA_ATTEMPT = "attempt"
    }
}
