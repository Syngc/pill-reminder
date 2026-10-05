package com.pillreminder.app

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import com.pillreminder.app.alarm.AlarmScheduler
import com.pillreminder.app.data.AppDatabase
import com.pillreminder.app.data.AppLanguage
import com.pillreminder.app.data.LanguageSettings
import com.pillreminder.app.data.MedicationRepository
import com.pillreminder.app.data.withLanguage
import com.pillreminder.app.network.ExtractionApi

class PillReminderApp : Application() {
    lateinit var repository: MedicationRepository
        private set
    lateinit var scheduler: AlarmScheduler
        private set
    lateinit var languageSettings: LanguageSettings
        private set
    val extractionApi by lazy { ExtractionApi(BuildConfig.BACKEND_URL, BuildConfig.BACKEND_API_KEY) }

    val language: AppLanguage get() = languageSettings.language.value

    /** Resources in the language chosen in the app, for code that runs outside an activity. */
    fun localized(): Context = withLanguage(language)

    override fun onCreate() {
        super.onCreate()
        languageSettings = LanguageSettings(this)
        repository = MedicationRepository(AppDatabase.create(this))
        scheduler = AlarmScheduler(this, repository)
        createAlarmChannel()
    }

    fun setLanguage(language: AppLanguage) {
        languageSettings.set(language)
        // Re-registering an existing channel updates its name in system settings.
        createAlarmChannel()
    }

    private fun createAlarmChannel() {
        val res = localized()
        // The service plays its own sound and speech, so the channel itself stays silent.
        val channel = NotificationChannel(
            ALARM_CHANNEL_ID,
            res.getString(R.string.channel_alarms),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = res.getString(R.string.channel_alarms_description)
            setSound(null, null as AudioAttributes?)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val ALARM_CHANNEL_ID = "dose_alarms"
    }
}
