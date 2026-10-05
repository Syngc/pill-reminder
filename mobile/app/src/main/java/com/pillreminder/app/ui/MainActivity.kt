package com.pillreminder.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pillreminder.app.PillReminderApp
import com.pillreminder.app.alarm.AlarmService
import com.pillreminder.app.ui.add.AddPrescriptionScreen
import com.pillreminder.app.ui.home.HomeScreen
import com.pillreminder.app.ui.theme.PillReminderTheme

class MainActivity : LocalizedActivity() {
    private val app get() = application as PillReminderApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PillReminderTheme {
                // While the app is open, show the big alarm screen directly instead of a heads-up notification.
                val ringing by AlarmService.current.collectAsStateWithLifecycle()
                LaunchedEffect(ringing != null) {
                    if (ringing != null) {
                        startActivity(Intent(this@MainActivity, AlarmActivity::class.java))
                    }
                }
                val nav = rememberNavController()
                NavHost(navController = nav, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            language = app.language,
                            onAddPrescription = { nav.navigate("add") },
                            onChangeLanguage = { language ->
                                app.setLanguage(language)
                                recreate()
                            },
                        )
                    }
                    composable("add") {
                        AddPrescriptionScreen(onDone = { nav.popBackStack() })
                    }
                }
            }
        }
    }
}
