package com.pillreminder.app.ui

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pillreminder.app.PillReminderApp
import com.pillreminder.app.R
import com.pillreminder.app.alarm.AlarmService
import com.pillreminder.app.alarm.DoseMessage
import com.pillreminder.app.alarm.RingingDose
import com.pillreminder.app.data.AppLanguage
import com.pillreminder.app.ui.theme.HugeButtonText
import com.pillreminder.app.ui.theme.PillReminderTheme
import kotlinx.coroutines.delay

/** Full-screen alarm shown over the lock screen: what to take, and one big button. */
class AlarmActivity : LocalizedActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            PillReminderTheme {
                val dose by AlarmService.current.collectAsStateWithLifecycle()
                var confirmed by remember { mutableStateOf(false) }

                // Back must not dismiss the alarm silently; only the button does.
                BackHandler { }

                LaunchedEffect(dose, confirmed) {
                    if (confirmed) {
                        delay(2_500)
                        finish()
                    } else if (dose == null) {
                        finish()
                    }
                }

                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    when {
                        confirmed -> Confirmed()
                        dose != null -> Ringing(dose!!, (application as PillReminderApp).language) {
                            confirmed = true
                            startService(AlarmService.takenIntent(this))
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (AlarmService.current.value != null) startService(AlarmService.screenShownIntent(this))
    }
}

@Composable
private fun Ringing(dose: RingingDose, language: AppLanguage, onTaken: () -> Unit) {
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(20.dp)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (dose.time.isNotEmpty()) Text(dose.time, style = MaterialTheme.typography.displayLarge)
            Text(
                stringResource(if (dose.medications.size > 1) R.string.alarm_title_plural else R.string.alarm_title),
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
            )
            if (dose.isTest) {
                Text(stringResource(R.string.alarm_test_text), style = MaterialTheme.typography.headlineMedium)
            }
            dose.medications.forEach { med ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(med.name, style = MaterialTheme.typography.headlineLarge)
                        Text(med.dose, style = MaterialTheme.typography.headlineMedium)
                        if (med.instructions.isNotBlank()) {
                            Text(med.instructions, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onTaken,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier.fillMaxWidth().height(160.dp),
        ) {
            Text(
                DoseMessage.confirmLabel(dose.medications.size.coerceAtLeast(1), language),
                style = HugeButtonText,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun Confirmed() {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(160.dp),
        )
        Text(stringResource(R.string.alarm_confirmed), style = MaterialTheme.typography.displayLarge)
    }
}
