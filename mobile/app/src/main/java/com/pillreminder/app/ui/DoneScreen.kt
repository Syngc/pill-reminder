package com.pillreminder.app.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pillreminder.app.R
import com.pillreminder.app.data.Medication
import com.pillreminder.app.ui.components.AppIcons
import com.pillreminder.app.ui.theme.AppColors
import com.pillreminder.app.ui.theme.LightPalette
import java.time.LocalDate
import java.time.LocalDateTime

/** "Well done!" after a dose is confirmed, from the alarm or the home screen. */
@Composable
fun DoneScreen(
    medications: List<Medication>,
    takenAt: LocalDateTime,
    nextDose: LocalDateTime?,
    onDone: () -> Unit,
) {
    val colors = AppColors.current
    val timeFormat = rememberTimeFormatter()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(colors.doneBackground)
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 28.dp),
    ) {
        Spacer(Modifier.height(56.dp))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(248.dp)) {
            Box(Modifier.size(248.dp).background(Color.White.copy(alpha = 0.06f), CircleShape))
            Box(Modifier.size(204.dp).background(Color.White.copy(alpha = 0.14f), CircleShape))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(168.dp).background(Color.White, CircleShape)) {
                Icon(AppIcons.Check, contentDescription = null, tint = colors.doneBackground, modifier = Modifier.size(104.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.done_title), style = MaterialTheme.typography.displayMedium, color = Color.White, textAlign = TextAlign.Center)
        if (medications.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.done_taken_at, medications.joinToString(", ") { it.name }, timeFormat.format(takenAt.toLocalTime())),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 24.sp, lineHeight = 32.sp),
                color = colors.doneSubtle,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.weight(1f))
        if (nextDose != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.18f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            ) {
                Icon(AppIcons.Clock, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(stringResource(R.string.done_next), style = MaterialTheme.typography.bodyMedium, color = colors.doneSubtle)
                    Text(
                        dayAndTime(nextDose, takenAt.toLocalDate(), timeFormat),
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                        color = Color.White,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        Button(
            onClick = onDone,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = LightPalette.ink),
            modifier = Modifier.fillMaxWidth().height(80.dp),
        ) {
            Text(stringResource(R.string.done_button), style = MaterialTheme.typography.titleLarge)
        }
    }
}

/** "Today, 8:00 PM", "Tomorrow, 6:00 AM", or a date. */
@Composable
fun dayAndTime(at: LocalDateTime, today: LocalDate, timeFormat: TimeFormatter): String {
    val time = timeFormat.format(at.toLocalTime())
    return when (at.toLocalDate()) {
        today -> stringResource(R.string.day_today, time)
        today.plusDays(1) -> stringResource(R.string.day_tomorrow, time)
        else -> stringResource(R.string.day_on, rememberLongDate(at.toLocalDate()), time)
    }
}
