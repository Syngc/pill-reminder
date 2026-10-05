package com.pillreminder.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
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
import com.pillreminder.app.ui.medicines.MedicinesScreen
import com.pillreminder.app.ui.theme.PillReminderTheme

class MainActivity : LocalizedActivity() {
    private val app get() = application as PillReminderApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Transparent bars in both modes, with icons that follow the system light/dark setting.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
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
                            onOpenMedicines = { nav.navigate("medicines") },
                            onChangeLanguage = { language ->
                                app.setLanguage(language)
                                recreate()
                            },
                        )
                    }
                    composable("medicines") {
                        MedicinesScreen(
                            onBack = { nav.popBackStack() },
                            onAddPrescription = { nav.navigate("add") },
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
