package com.pillreminder.app.ui

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pillreminder.app.R
import java.time.LocalTime
import java.util.Locale

/**
 * The times of day for one medicine: tap a time to change it, ✕ to remove it, or add a new one.
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
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        times.forEach { time ->
            InputChip(
                selected = false,
                onClick = {
                    beforePick()
                    pickTime(context, initial = time) { picked -> onChange(times.replaceTime(time, picked)) }
                },
                label = { Text(time, style = MaterialTheme.typography.titleMedium) },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.change_time, time)) },
                trailingIcon = {
                    IconButton(onClick = { onChange(times - time) }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_time, time))
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
}

/** Swaps [old] for [new], keeping the list sorted and without duplicates. */
fun List<String>.replaceTime(old: String, new: String): List<String> = (this - old + new).distinct().sorted()

private fun pickTime(context: Context, initial: String?, onPicked: (String) -> Unit) {
    val start = initial?.let(LocalTime::parse) ?: LocalTime.of(8, 0)
    TimePickerDialog(
        context,
        { _, hour, minute -> onPicked(String.format(Locale.ROOT, "%02d:%02d", hour, minute)) },
        start.hour,
        start.minute,
        true,
    ).show()
}
