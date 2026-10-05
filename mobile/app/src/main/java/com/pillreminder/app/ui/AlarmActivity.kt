package com.pillreminder.app.ui

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.pillreminder.app.PillReminderApp
import com.pillreminder.app.R
import com.pillreminder.app.alarm.AlarmService
import com.pillreminder.app.alarm.DoseMessage
import com.pillreminder.app.alarm.RingingDose
import com.pillreminder.app.data.AppLanguage
import com.pillreminder.app.data.Medication
import com.pillreminder.app.ui.components.AppIcons
import com.pillreminder.app.ui.components.MedicineIcon
import com.pillreminder.app.ui.home.HomeViewModel
import com.pillreminder.app.ui.theme.AppColors
import com.pillreminder.app.ui.theme.LightPalette
import com.pillreminder.app.ui.theme.LocalPalette
import com.pillreminder.app.ui.theme.PillReminderTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/** Full-screen alarm shown over the lock screen: what to take, and one big button. */
class AlarmActivity : LocalizedActivity() {
    private val app get() = application as PillReminderApp

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
                var confirmed by remember { mutableStateOf<Confirmed?>(null) }

                // Back must not dismiss the alarm silently; only the button does.
                BackHandler { }

                LaunchedEffect(dose, confirmed) {
                    if (confirmed != null) {
                        // Don't leave the screen on forever if nobody taps "Done".
                        delay(60_000)
                        finish()
                    } else if (dose == null) {
                        finish()
                    }
                }

                val done = confirmed
                when {
                    done != null -> DoneScreen(done.medications, done.at, done.nextDose, onDone = ::finish)
                    dose != null -> Ringing(
                        dose = dose!!,
                        language = app.language,
                        onSayAgain = { startService(AlarmService.sayAgainIntent(this)) },
                        onTaken = {
                            val ringing = dose!!
                            startService(AlarmService.takenIntent(this))
                            lifecycleScope.launch {
                                val now = LocalDateTime.now()
                                val next = app.repository.all().mapNotNull { HomeViewModel.nextDose(it, now) }.minOrNull()
                                confirmed = Confirmed(ringing.medications, now, next)
                            }
                        },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (AlarmService.current.value != null) startService(AlarmService.screenShownIntent(this))
    }

    private data class Confirmed(val medications: List<Medication>, val at: LocalDateTime, val nextDose: LocalDateTime?)
}

@Composable
private fun Ringing(dose: RingingDose, language: AppLanguage, onSayAgain: () -> Unit, onTaken: () -> Unit) {
    val colors = AppColors.current
    val timeFormat = rememberTimeFormatter()
    Column(
        verticalArrangement = Arrangement.spacedBy(22.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(colors.alarmBackground)
            .safeDrawingPadding()
            .padding(start = 20.dp, end = 20.dp, top = 32.dp, bottom = 28.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
        ) {
            BellWithHalo(colors.alarmBackground)
            if (dose.time.isNotEmpty()) {
                val (clock, suffix) = timeFormat.parts(dose.time)
                Text(
                    buildAnnotatedString {
                        append(clock)
                        if (suffix != null) withStyle(SpanStyle(fontSize = 36.sp)) { append(" $suffix") }
                    },
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White,
                )
            }
            Text(
                stringResource(if (dose.medications.size > 1) R.string.alarm_title_plural else R.string.alarm_title),
                style = MaterialTheme.typography.headlineMedium,
                color = colors.alarmSubtle,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(8.dp))
            // The medicine cards stay white in dark mode too, like the design.
            androidx.compose.runtime.CompositionLocalProvider(LocalPalette provides LightPalette) {
                if (dose.isTest) {
                    AlarmCard { Text(stringResource(R.string.alarm_test_text), style = MaterialTheme.typography.headlineSmall, color = LightPalette.ink) }
                }
                dose.medications.forEach { med ->
                    AlarmCard {
                        MedicineIcon(med)
                        Spacer(Modifier.width(14.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(med.name, style = MaterialTheme.typography.headlineSmall, color = LightPalette.ink)
                            Text(med.dose, style = MaterialTheme.typography.titleMedium.copy(fontSize = 24.sp), color = LightPalette.ink)
                            if (med.instructions.isNotBlank()) {
                                Text(med.instructions, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 19.sp), color = LightPalette.muted)
                            }
                        }
                    }
                }
            }
        }
        OutlinedButton(
            onClick = onSayAgain,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, colors.alarmOutline),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(60.dp),
        ) {
            Icon(AppIcons.Speaker, contentDescription = null, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.say_again), style = MaterialTheme.typography.titleSmall.copy(fontSize = 21.sp))
        }
        val bigShape = RoundedCornerShape(30.dp)
        Button(
            onClick = onTaken,
            shape = bigShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = colors.alarmBackground),
            modifier = Modifier.fillMaxWidth().height(120.dp).shadow(14.dp, bigShape),
        ) {
            Icon(AppIcons.Check, contentDescription = null, modifier = Modifier.size(48.dp))
            Spacer(Modifier.width(16.dp))
            Text(
                DoseMessage.confirmLabel(dose.medications.size.coerceAtLeast(1), language),
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 36.sp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun BellWithHalo(background: Color) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(156.dp)) {
        Box(Modifier.size(156.dp).background(Color.White.copy(alpha = 0.06f), CircleShape))
        Box(Modifier.size(124.dp).background(Color.White.copy(alpha = 0.14f), CircleShape))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(96.dp).background(Color.White, CircleShape)) {
            Icon(AppIcons.Bell, contentDescription = null, tint = background, modifier = Modifier.size(52.dp))
        }
    }
}

@Composable
private fun AlarmCard(content: @Composable () -> Unit) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(24.dp))
            .padding(18.dp),
    ) { content() }
}
