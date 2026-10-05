package com.pillreminder.app.ui

import android.content.Context
import androidx.activity.ComponentActivity
import com.pillreminder.app.PillReminderApp
import com.pillreminder.app.data.withLanguage

/** Shows its UI in the language chosen in the app, whatever the phone's language is. */
abstract class LocalizedActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val app = newBase.applicationContext as PillReminderApp
        super.attachBaseContext(newBase.withLanguage(app.language))
    }
}
