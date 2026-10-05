package com.pillreminder.app.ui

import android.app.TimePickerDialog
import android.content.Context
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pillreminder.app.R
import com.pillreminder.app.ui.components.CheckRow
import com.pillreminder.app.ui.components.DashedButton
import com.pillreminder.app.ui.components.TimeChip
import java.time.LocalTime
import java.util.Locale

/**
 * The times of day for one medicine: tap a time to change it, ✕ to remove it, or add a new one.
 * When the times divide the day evenly ("every 8 hours"), a checkbox lets one change move them all.
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            times.forEach { time ->
                TimeChip(
                    text = timeFormat.format(time),
                    onClick = {
                        beforePick()
                        pickTime(context, initial = time) { picked ->
                            onChange(if (moveTogether) times.shiftAll(time, picked) else times.replaceTime(time, picked))
                        }
                    },
                    onRemove = { onChange(times - time) },
                    removeLabel = stringResource(R.string.remove_time, timeFormat.format(time)),
                )
            }
            DashedButton(
                text = stringResource(R.string.review_add_time),
                onClick = {
                    beforePick()
                    pickTime(context, initial = null) { picked -> onChange((times + picked).distinct().sorted()) }
                },
                modifier = Modifier.height(48.dp),
            )
        }
        if (interval != null) {
            CheckRow(
                checked = keepInterval,
                onCheckedChange = { keepInterval = it },
                text = stringResource(R.string.keep_interval, intervalText(interval)),
            )
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
