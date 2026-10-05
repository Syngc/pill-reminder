package com.pillreminder.app.alarm

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.pillreminder.app.PillReminderApp
import com.pillreminder.app.R
import com.pillreminder.app.data.AppLanguage
import com.pillreminder.app.data.Medication
import com.pillreminder.app.ui.AlarmActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate

/** The dose currently ringing, observed by [AlarmActivity]. */
data class RingingDose(
    val date: LocalDate,
    val time: String,
    val medications: List<Medication>,
    val isTest: Boolean,
)

/**
 * Rings like an alarm clock, alternating the alarm tone with the spoken instructions,
 * until the person confirms. If nobody confirms, it tries again later and finally logs the dose as missed.
 */
class AlarmService : LifecycleService() {
    private val app get() = application as PillReminderApp
    private lateinit var speaker: Speaker
    private lateinit var language: AppLanguage
    /** Strings in the language chosen in the app (the service has no activity to inherit it from). */
    private lateinit var res: Context
    private var player: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var ringing: Job? = null

    override fun onCreate() {
        super.onCreate()
        language = app.language
        res = app.localized()
        speaker = Speaker(this, language)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_SCREEN_SHOWN) {
            // The alarm screen is up; re-post without the full-screen intent so the heads-up stops covering it.
            _current.value?.let { NotificationManagerCompat.from(this).notifyIfAllowed(alarmNotification(it, fullScreen = false)) }
            return START_NOT_STICKY
        }
        // Must be in the foreground within seconds of being started, before any database work.
        startInForeground(placeholderNotification())

        when (intent?.action) {
            ACTION_RING -> {
                val date = LocalDate.ofEpochDay(intent.getLongExtra(EXTRA_DATE, LocalDate.now().toEpochDay()))
                val time = intent.getStringExtra(EXTRA_TIME)
                if (time == null) stop() else ring(date, time, intent.getIntExtra(EXTRA_ATTEMPT, 1))
            }
            ACTION_TEST -> ringTest()
            ACTION_TAKEN -> confirmTaken()
            else -> stop()
        }
        return START_NOT_STICKY
    }

    private fun ring(date: LocalDate, time: String, attempt: Int) {
        ringing?.cancel()
        ringing = lifecycleScope.launch {
            if (attempt == 1) app.scheduler.rescheduleAll()
            val due = app.repository.dueAt(date, time)
            if (due.isEmpty()) return@launch stop()

            app.repository.markRinging(date, time, due)
            val dose = RingingDose(date, time, due, isTest = false)
            val answered = ringUntilAnswered(dose, DoseMessage.spoken(due, language))
            if (!answered) {
                if (attempt < MAX_ATTEMPTS) {
                    app.scheduler.scheduleRetry(date, time, attempt + 1, RETRY_DELAY_MINUTES)
                } else {
                    app.repository.markMissed(date, time)
                }
                stop()
            }
        }
    }

    private fun ringTest() {
        ringing?.cancel()
        ringing = lifecycleScope.launch {
            val dose = RingingDose(LocalDate.now(), "", emptyList(), isTest = true)
            if (!ringUntilAnswered(dose, DoseMessage.test(language))) stop()
        }
    }

    /** Returns true if the person confirmed, false if the ringing timed out. */
    private suspend fun ringUntilAnswered(dose: RingingDose, message: String): Boolean {
        _current.value = dose
        NotificationManagerCompat.from(this).notifyIfAllowed(alarmNotification(dose))
        acquireWakeLock()
        startVibrating()
        val result = withTimeoutOrNull(RING_DURATION_MILLIS) {
            while (true) {
                playTone(TONE_MILLIS)
                speaker.speak(message)
                delay(PAUSE_MILLIS)
            }
        }
        silence()
        return result != null
    }

    private fun confirmTaken() {
        val dose = _current.value ?: return stop()
        ringing?.cancel()
        silence()
        _current.value = null
        lifecycleScope.launch {
            withContext(NonCancellable) {
                if (!dose.isTest) {
                    app.repository.markTaken(dose.date, dose.time)
                    app.scheduler.cancelRetry(dose.time)
                }
                speaker.speak(DoseMessage.confirmed(language))
            }
            stop()
        }
    }

    private suspend fun playTone(millis: Long) {
        val mp = player ?: createPlayer()?.also { player = it } ?: return delay(millis)
        mp.start()
        try {
            delay(millis)
        } finally {
            if (mp.isPlaying) mp.pause()
        }
    }

    private fun createPlayer(): MediaPlayer? {
        val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: return null
        return runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmService, uri)
                isLooping = true
                prepare()
            }
        }.getOrNull()
    }

    private fun vibrator(): Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        }

    private fun startVibrating() {
        val pattern = longArrayOf(0, 800, 600)
        vibrator().vibrate(VibrationEffect.createWaveform(pattern, 0))
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PillReminder:alarm")
            .apply { acquire(RING_DURATION_MILLIS + 60_000) }
    }

    private fun silence() {
        player?.run { if (isPlaying) pause() }
        vibrator().cancel()
    }

    private fun stop() {
        _current.value = null
        silence()
        wakeLock?.takeIf { it.isHeld }?.release()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        _current.value = null
        player?.release()
        player = null
        vibrator().cancel()
        wakeLock?.takeIf { it.isHeld }?.release()
        speaker.shutdown()
        super.onDestroy()
    }

    private fun startInForeground(notification: Notification) {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
            } else {
                0
            },
        )
    }

    private fun placeholderNotification(): Notification =
        NotificationCompat.Builder(this, PillReminderApp.ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(res.getString(R.string.alarm_title))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()

    private fun alarmNotification(dose: RingingDose, fullScreen: Boolean = true): Notification {
        val openAlarm = PendingIntent.getActivity(
            this, 0, Intent(this, AlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val taken = PendingIntent.getService(
            this, 1, takenIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val text = if (dose.isTest) {
            res.getString(R.string.alarm_test_text)
        } else {
            dose.medications.joinToString(", ") { "${it.name} ${it.dose}" }
        }
        val title = if (dose.medications.size > 1) R.string.alarm_title_plural else R.string.alarm_title
        return NotificationCompat.Builder(this, PillReminderApp.ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(res.getString(title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(openAlarm)
            .setOnlyAlertOnce(true)
            .apply { if (fullScreen) setFullScreenIntent(openAlarm, true) }
            .addAction(0, DoseMessage.confirmLabel(dose.medications.size.coerceAtLeast(1), language), taken)
            .build()
    }

    private fun NotificationManagerCompat.notifyIfAllowed(notification: Notification) {
        if (areNotificationsEnabled()) {
            @Suppress("MissingPermission")
            notify(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val ACTION_RING = "com.pillreminder.app.service.RING"
        private const val ACTION_TEST = "com.pillreminder.app.service.TEST"
        private const val ACTION_TAKEN = "com.pillreminder.app.service.TAKEN"
        private const val ACTION_SCREEN_SHOWN = "com.pillreminder.app.service.SCREEN_SHOWN"
        private const val EXTRA_DATE = "date"
        private const val EXTRA_TIME = "time"
        private const val EXTRA_ATTEMPT = "attempt"
        private const val NOTIFICATION_ID = 42

        private const val RING_DURATION_MILLIS = 3 * 60_000L
        private const val TONE_MILLIS = 6_000L
        private const val PAUSE_MILLIS = 1_500L
        private const val MAX_ATTEMPTS = 3
        private const val RETRY_DELAY_MINUTES = 10L

        private val _current = MutableStateFlow<RingingDose?>(null)
        val current: StateFlow<RingingDose?> = _current.asStateFlow()

        fun ringIntent(context: Context, date: LocalDate, time: String, attempt: Int): Intent =
            Intent(context, AlarmService::class.java)
                .setAction(ACTION_RING)
                .putExtra(EXTRA_DATE, date.toEpochDay())
                .putExtra(EXTRA_TIME, time)
                .putExtra(EXTRA_ATTEMPT, attempt)

        fun testIntent(context: Context): Intent =
            Intent(context, AlarmService::class.java).setAction(ACTION_TEST)

        fun screenShownIntent(context: Context): Intent =
            Intent(context, AlarmService::class.java).setAction(ACTION_SCREEN_SHOWN)

        fun takenIntent(context: Context): Intent =
            Intent(context, AlarmService::class.java).setAction(ACTION_TAKEN)
    }
}
