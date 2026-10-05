package com.pillreminder.app.ui.home

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pillreminder.app.R
import com.pillreminder.app.alarm.AlarmService
import com.pillreminder.app.data.AppLanguage
import com.pillreminder.app.data.Medication
import com.pillreminder.app.ui.TimesEditor
import com.pillreminder.app.ui.rememberTimeFormatter
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    language: AppLanguage,
    onAddPrescription: () -> Unit,
    onChangeLanguage: (AppLanguage) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var toDelete by remember { mutableStateOf<Medication?>(null) }
    var toEdit by remember { mutableStateOf<Medication?>(null) }
    var choosingLanguage by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    TextButton(onClick = { choosingLanguage = true }) {
                        Icon(Icons.Default.Language, contentDescription = stringResource(R.string.language_title))
                        Spacer(Modifier.width(6.dp))
                        Text(language.nativeName)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { PermissionBanners() }

            item { SectionTitle(stringResource(R.string.home_today)) }
            if (state.today.isEmpty()) {
                item { Text(stringResource(R.string.home_no_doses_today), style = MaterialTheme.typography.bodyLarge) }
            }
            items(state.today, key = { "slot-${it.time}" }) { TodaySlotCard(it) }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onAddPrescription, modifier = Modifier.fillMaxWidth().height(64.dp)) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.home_add_prescription))
                    }
                    OutlinedButton(
                        onClick = { ContextCompat.startForegroundService(context, AlarmService.testIntent(context)) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.home_test_alarm))
                    }
                }
            }

            item { SectionTitle(stringResource(R.string.home_medicines)) }
            if (state.medications.isEmpty()) {
                item { Text(stringResource(R.string.home_empty), style = MaterialTheme.typography.bodyLarge) }
            }
            items(state.medications, key = { "med-${it.id}" }) { med ->
                MedicationCard(med, onEditTimes = { toEdit = med }, onDelete = { toDelete = med })
            }

            item {
                Text(
                    stringResource(R.string.home_disclaimer),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }

    if (choosingLanguage) {
        LanguageDialog(
            current = language,
            onChoose = { choice ->
                choosingLanguage = false
                if (choice != language) onChangeLanguage(choice)
            },
            onDismiss = { choosingLanguage = false },
        )
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
            title = { Text(stringResource(R.string.delete_title, med.name)) },
            text = { Text(stringResource(R.string.delete_body)) },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(med); toDelete = null }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun LanguageDialog(current: AppLanguage, onChoose: (AppLanguage) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language_title)) },
        text = {
            Column {
                // Each option is written in its own language so anyone can find theirs.
                AppLanguage.entries.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = option == current, role = Role.RadioButton) { onChoose(option) }
                            .padding(vertical = 12.dp),
                    ) {
                        RadioButton(selected = option == current, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(option.nativeName, style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun TodaySlotCard(slot: TodaySlot) {
    val (label, icon, container) = when (slot.status) {
        SlotStatus.TAKEN -> Triple(R.string.status_taken, Icons.Default.CheckCircle, MaterialTheme.colorScheme.primaryContainer)
        SlotStatus.MISSED -> Triple(R.string.status_missed, Icons.Default.Warning, MaterialTheme.colorScheme.errorContainer)
        SlotStatus.PENDING -> Triple(R.string.status_pending, Icons.Default.Schedule, MaterialTheme.colorScheme.surfaceVariant)
        SlotStatus.UPCOMING -> Triple(R.string.status_upcoming, Icons.Default.Schedule, MaterialTheme.colorScheme.surfaceVariant)
    }
    Card(colors = CardDefaults.cardColors(containerColor = container), modifier = Modifier.fillMaxWidth()) {
        // Time and status on top, medicines below at full width, so long names and "6:00 p. m." both fit.
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    rememberTimeFormatter().format(slot.time),
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.weight(1f),
                )
                StatusChip(stringResource(label), icon)
            }
            slot.medications.forEach { Text("${it.name} · ${it.dose}", style = MaterialTheme.typography.bodyLarge) }
        }
    }
}

@Composable
private fun StatusChip(text: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun MedicationCard(med: Medication, onEditTimes: () -> Unit, onDelete: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val dateFormat = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(med.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                }
            }
            Text(med.dose, style = MaterialTheme.typography.bodyLarge)
            val timeFormat = rememberTimeFormatter()
            Text(
                stringResource(R.string.med_times, med.times.joinToString("  ·  ") { timeFormat.format(it) }),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (med.instructions.isNotBlank()) Text(med.instructions, style = MaterialTheme.typography.bodyMedium)
            med.durationDays?.let { days ->
                val last = med.startDate.plusDays(days.toLong() - 1)
                Text(stringResource(R.string.med_until, last.format(dateFormat)), style = MaterialTheme.typography.bodyMedium)
            }
            OutlinedButton(onClick = onEditTimes, modifier = Modifier.padding(top = 8.dp).height(52.dp)) {
                Icon(Icons.Default.Schedule, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.edit_times))
            }
        }
    }
}

@Composable
private fun EditTimesDialog(medication: Medication, onSave: (List<String>) -> Unit, onDismiss: () -> Unit) {
    var times by remember(medication.id) { mutableStateOf(medication.times) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_times_title, medication.name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(medication.dose, style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.edit_times_hint), style = MaterialTheme.typography.bodyMedium)
                TimesEditor(times = times, onChange = { times = it })
                if (times.isEmpty()) {
                    Text(stringResource(R.string.times_need_one), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(times) }, enabled = times.isNotEmpty() && times != medication.times) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Shows a banner for each permission the alarms depend on that is currently missing. */
@Composable
private fun PermissionBanners() {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        refresh++
        onPauseOrDispose { }
    }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }
    val needsNotifications = remember(refresh) {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    }
    LaunchedEffect(Unit) {
        if (needsNotifications) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    val needsFullScreen = remember(refresh) {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            !context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
    }
    val needsExact = remember(refresh) {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (needsNotifications) {
            Banner(stringResource(R.string.perm_notifications)) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (needsFullScreen) {
            Banner(stringResource(R.string.perm_full_screen)) {
                context.openSettings(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
            }
        }
        if (needsExact) {
            Banner(stringResource(R.string.perm_exact_alarm)) {
                context.openSettings(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            }
        }
    }
}

@Composable
private fun Banner(text: String, onFix: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = onFix) { Text(stringResource(R.string.perm_fix)) }
        }
    }
}

private fun Context.openSettings(action: String) {
    startActivity(Intent(action, Uri.parse("package:$packageName")))
}
