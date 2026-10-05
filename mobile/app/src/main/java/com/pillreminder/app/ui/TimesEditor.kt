package com.pillreminder.app.ui

import android.app.TimePickerDialog
import android.content.Context
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pillreminder.app.R
import java.time.LocalTime
import java.util.Locale

/**
 * The times of day for one medicine: tap a time to change it, ✕ to remove it, or add a new one.
 * When the times divide the day evenly ("every 8 hours"), a switch lets one change move them all.
 * [beforePick] runs before the clock opens (the review screen uses it to drop keyboard focus).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimesEditor(
    times: List<String>,
    onChange: (List<String>) -> Unit,
    beforePick: () -> Unit = {},
) {
    val context = LocalContext.current
    val timeFormat = rememberTimeFormatter()
    val interval = times.intervalMinutes()
    var keepInterval by rememberSaveable { mutableStateOf(true) }
    val moveTogether = interval != null && keepInterval

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            times.forEach { time ->
                InputChip(
                    selected = false,
                    onClick = {
                        beforePick()
                        pickTime(context, initial = time) { picked ->
                            onChange(if (moveTogether) times.shiftAll(time, picked) else times.replaceTime(time, picked))
                        }
                    },
                    label = { Text(timeFormat.format(time), style = MaterialTheme.typography.titleMedium) },
                    leadingIcon = {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.change_time, timeFormat.format(time)))
                    },
                    trailingIcon = {
                        IconButton(onClick = { onChange(times - time) }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_time, timeFormat.format(time)))
                        }
                    },
                )
            }
            AssistChip(
                onClick = {
                    beforePick()
                    pickTime(context, initial = null) { picked -> onChange((times + picked).distinct().sorted()) }
                },
                label = { Text(stringResource(R.string.review_add_time)) },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
            )
        }
        if (interval != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(value = keepInterval, role = Role.Switch, onValueChange = { keepInterval = it })
                    .padding(vertical = 4.dp),
            ) {
                Text(
                    stringResource(R.string.keep_interval, intervalText(interval)),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                Switch(checked = keepInterval, onCheckedChange = null)
            }
        }
    }
}

@Composable
private fun intervalText(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return if (rest == 0) {
        pluralStringResource(R.plurals.interval_hours, hours, hours)
    } else {
        stringResource(R.string.interval_hours_minutes, hours, rest)
    }
}

/** Swaps [old] for [new], keeping the list sorted and without duplicates. */
fun List<String>.replaceTime(old: String, new: String): List<String> = (this - old + new).distinct().sorted()

private const val MINUTES_PER_DAY = 24 * 60

private fun String.toMinutes(): Int = LocalTime.parse(this).let { it.hour * 60 + it.minute }

private fun Int.toTime(): String = Math.floorMod(this, MINUTES_PER_DAY).let {
    String.format(Locale.ROOT, "%02d:%02d", it / 60, it % 60)
}

/**
 * The spacing in minutes when the times divide the day evenly ("every 8 hours" → 480),
 * or null when they don't (for example breakfast and dinner) or there is only one time.
 */
fun List<String>.intervalMinutes(): Int? {
    if (size < 2) return null
    val minutes = map { it.toMinutes() }.sorted()
    val gaps = minutes.zipWithNext { a, b -> b - a } + (minutes.first() + MINUTES_PER_DAY - minutes.last())
    return gaps.first().takeIf { first -> gaps.all { it == first } }
}

/** Moves every time by the same amount that [old] moved to become [new], keeping the spacing. */
fun List<String>.shiftAll(old: String, new: String): List<String> {
    val delta = new.toMinutes() - old.toMinutes()
    return map { (it.toMinutes() + delta).toTime() }.distinct().sorted()
}

private fun pickTime(context: Context, initial: String?, onPicked: (String) -> Unit) {
    val start = initial?.let(LocalTime::parse) ?: LocalTime.of(8, 0)
    TimePickerDialog(
        context,
        { _, hour, minute -> onPicked(String.format(Locale.ROOT, "%02d:%02d", hour, minute)) },
        start.hour,
        start.minute,
        // Match the phone's clock: a 12-hour phone gets AM/PM, so 5:39 can't silently mean 05:39.
        DateFormat.is24HourFormat(context),
    ).show()
}
