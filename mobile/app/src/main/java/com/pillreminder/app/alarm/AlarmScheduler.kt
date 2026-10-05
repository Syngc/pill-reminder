package com.pillreminder.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.pillreminder.app.data.Medication
import com.pillreminder.app.data.MedicationRepository
import com.pillreminder.app.ui.MainActivity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * One alarm per distinct time of day, so medicines due together ring together.
 * The receiver works out which medicines are due when the alarm fires.
 */
class AlarmScheduler(
    private val context: Context,
    private val repository: MedicationRepository,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val prefs = context.getSharedPreferences("alarm_scheduler", Context.MODE_PRIVATE)

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    suspend fun rescheduleAll(now: LocalDateTime = LocalDateTime.now()) {
        val medications = repository.all()
        val times = medications.flatMap { it.times }.toSortedSet()

        val previous = prefs.getStringSet(KEY_SCHEDULED, emptySet()).orEmpty()
        (previous - times).forEach { alarmManager.cancel(ringIntent(it)) }

        val scheduled = mutableSetOf<String>()
        for (time in times) {
            val next = nextOccurrence(medications, time, now) ?: continue
            setExact(next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), ringIntent(time))
            scheduled += time
        }
        prefs.edit().putStringSet(KEY_SCHEDULED, scheduled).apply()
    }

    fun scheduleRetry(date: LocalDate, time: String, attempt: Int, delayMinutes: Long) {
        val trigger = System.currentTimeMillis() + delayMinutes * 60_000
        setExact(trigger, retryIntent(date, time, attempt))
    }

    fun cancelRetry(time: String) {
        alarmManager.cancel(retryIntent(LocalDate.now(), time, 0))
    }

    private fun setExact(triggerAtMillis: Long, operation: PendingIntent) {
        if (canScheduleExact()) {
            // setAlarmClock is exempt from Doze and shows the alarm icon in the status bar.
            val show = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
            )
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAtMillis, show), operation)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
        }
    }

    private fun ringIntent(time: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_RING)
            // The data URI makes each time slot a distinct PendingIntent.
            .setData(Uri.parse("pillreminder://ring/$time"))
            .putExtra(AlarmReceiver.EXTRA_TIME, time),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun retryIntent(date: LocalDate, time: String, attempt: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, AlarmReceiver::class.java)
                .setAction(AlarmReceiver.ACTION_RETRY)
                .setData(Uri.parse("pillreminder://retry/$time"))
                .putExtra(AlarmReceiver.EXTRA_TIME, time)
                .putExtra(AlarmReceiver.EXTRA_DATE, date.toEpochDay())
                .putExtra(AlarmReceiver.EXTRA_ATTEMPT, attempt),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        private const val KEY_SCHEDULED = "scheduled_times"

        /** The next moment after [now] at [time] on which at least one medicine is active. */
        fun nextOccurrence(medications: List<Medication>, time: String, now: LocalDateTime): LocalDateTime? {
            val clock = LocalTime.parse(time)
            val firstDate = if (clock.isAfter(now.toLocalTime())) now.toLocalDate() else now.toLocalDate().plusDays(1)
            return medications
                .filter { time in it.times }
                .mapNotNull { med ->
                    val date = maxOf(firstDate, med.startDate)
                    date.takeIf { med.isActiveOn(it) }
                }
                .minOrNull()
                ?.atTime(clock)
        }
    }
}
