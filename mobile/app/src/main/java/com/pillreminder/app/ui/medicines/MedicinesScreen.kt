package com.pillreminder.app.ui.medicines

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pillreminder.app.R
import com.pillreminder.app.data.Medication
import com.pillreminder.app.ui.TimesEditor
import com.pillreminder.app.ui.components.AppIcons
import com.pillreminder.app.ui.components.MedicineIcon
import com.pillreminder.app.ui.components.PrimaryButton
import com.pillreminder.app.ui.components.ScreenHeader
import com.pillreminder.app.ui.components.SecondaryButton
import com.pillreminder.app.ui.components.TimeChip
import com.pillreminder.app.ui.home.HomeViewModel
import com.pillreminder.app.ui.rememberLongDate
import com.pillreminder.app.ui.rememberTimeFormatter
import com.pillreminder.app.ui.theme.AppColors
import java.time.LocalDate
import java.time.LocalDateTime

/** Every medicine, with search, its times, next dose, end date, and edit/delete. */
@Composable
fun MedicinesScreen(
    onBack: () -> Unit,
    onAddPrescription: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = AppColors.current
    var query by rememberSaveable { mutableStateOf("") }
    var toEdit by remember { mutableStateOf<Medication?>(null) }
    var toDelete by remember { mutableStateOf<Medication?>(null) }
    val shown = state.medications.filter { HomeViewModel.matchesSearch(it, query) }

    Scaffold(containerColor = colors.background) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { ScreenHeader(stringResource(R.string.medicines_title), onBack) }
            if (state.medications.isEmpty()) {
                item {
                    Text(stringResource(R.string.home_empty), style = MaterialTheme.typography.bodyLarge, color = colors.muted)
                }
                item {
                    PrimaryButton(stringResource(R.string.home_add_prescription), onAddPrescription, icon = AppIcons.Plus)
                }
            } else {
                item { SearchField(query, onQueryChange = { query = it }) }
            }
            if (state.medications.isNotEmpty() && shown.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.search_no_results, query.trim()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.ink,
                    )
                }
            }
            items(shown, key = { "med-${it.id}" }) { med ->
                MedicineCard(
                    med = med,
                    nextDose = state.nextDoses[med.id],
                    today = state.now.toLocalDate(),
                    onEditTimes = { toEdit = med },
                    onDelete = { toDelete = med },
                )
            }
        }
    }

    toEdit?.let { med ->
        EditTimesDialog(
            medication = med,
            onSave = { times ->
                viewModel.updateTimes(med, times)
                toEdit = null
            },
            onDismiss = { toEdit = null },
        )
    }
    toDelete?.let { med ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            containerColor = colors.surface,
            title = { Text(stringResource(R.string.delete_title, med.name), style = MaterialTheme.typography.headlineSmall) },
            text = { Text(stringResource(R.string.delete_body), style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(med); toDelete = null }) {
                    Text(stringResource(R.string.delete), color = colors.danger, style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) {
                    Text(stringResource(R.string.cancel), style = MaterialTheme.typography.labelLarge)
                }
            },
        )
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val colors = AppColors.current
    // Only hide the keyboard: clearing focus would let the same Enter key press land on
    // (and click) the next focusable button.
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.search_medicines), color = colors.muted) },
        leadingIcon = { Icon(AppIcons.Search, contentDescription = null, tint = colors.muted, modifier = Modifier.size(26.dp)) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(AppIcons.Close, contentDescription = stringResource(R.string.clear_search), tint = colors.ink)
                }
            }
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 21.sp),
        shape = RoundedCornerShape(20.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = colors.border,
            focusedBorderColor = colors.accentText,
            unfocusedContainerColor = colors.surface,
            focusedContainerColor = colors.surface,
            focusedTextColor = colors.ink,
            unfocusedTextColor = colors.ink,
        ),
        modifier = Modifier.fillMaxWidth().height(64.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MedicineCard(
    med: Medication,
    nextDose: LocalDateTime?,
    today: LocalDate,
    onEditTimes: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = AppColors.current
    val timeFormat = rememberTimeFormatter()
    Surface(shape = RoundedCornerShape(26.dp), color = colors.surface, contentColor = colors.ink, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                MedicineIcon(med)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(med.name, style = MaterialTheme.typography.titleLarge)
                    Text(med.dose, style = MaterialTheme.typography.titleMedium.copy(fontSize = 21.sp))
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                med.times.forEach { TimeChip(timeFormat.format(it)) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DetailLine(AppIcons.Clock, colors.accentText, nextDoseText(nextDose, today))
                med.durationDays?.let { days ->
                    val last = med.startDate.plusDays(days.toLong() - 1)
                    DetailLine(AppIcons.Calendar, colors.muted, stringResource(R.string.med_until, rememberLongDate(last, capitalize = false)))
                }
                if (med.instructions.isNotBlank()) {
                    Text(med.instructions, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 19.sp), color = colors.muted)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
                SecondaryButton(
                    text = stringResource(R.string.edit_times),
                    onClick = onEditTimes,
                    icon = AppIcons.Clock,
                    accent = true,
                    height = 56.dp,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    onClick = onDelete,
                    shape = RoundedCornerShape(18.dp),
                    color = colors.surface,
                    contentColor = colors.danger,
                    border = BorderStroke(2.dp, colors.dangerBorder),
                    modifier = Modifier.size(56.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            AppIcons.Trash,
                            contentDescription = stringResource(R.string.delete_named, med.name),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailLine(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: androidx.compose.ui.graphics.Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 19.sp), color = AppColors.current.ink)
    }
}

/** "Next dose: today, 8:00 PM"; spelling out the day makes a wrong time or AM/PM obvious. */
@Composable
private fun nextDoseText(next: LocalDateTime?, today: LocalDate): String {
    if (next == null) return stringResource(R.string.next_dose_none)
    val time = rememberTimeFormatter().format(next.toLocalTime())
    return when (next.toLocalDate()) {
        today -> stringResource(R.string.next_dose_today, time)
        today.plusDays(1) -> stringResource(R.string.next_dose_tomorrow, time)
        else -> stringResource(R.string.next_dose_on, rememberLongDate(next.toLocalDate(), capitalize = false), time)
    }
}

@Composable
private fun EditTimesDialog(medication: Medication, onSave: (List<String>) -> Unit, onDismiss: () -> Unit) {
    val colors = AppColors.current
    var times by remember(medication.id) { mutableStateOf(medication.times) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text(stringResource(R.string.edit_times_title, medication.name), style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(medication.dose, style = MaterialTheme.typography.titleSmall, color = colors.ink)
                Text(stringResource(R.string.edit_times_hint), style = MaterialTheme.typography.bodyMedium, color = colors.muted)
                TimesEditor(times = times, onChange = { times = it })
                if (times.isEmpty()) {
                    Text(stringResource(R.string.times_need_one), style = MaterialTheme.typography.bodyMedium, color = colors.danger)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(times) }, enabled = times.isNotEmpty() && times != medication.times) {
                Text(stringResource(R.string.save), style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), style = MaterialTheme.typography.labelLarge) }
        },
    )
}
