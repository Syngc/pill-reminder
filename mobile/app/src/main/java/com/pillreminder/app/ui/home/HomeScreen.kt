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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pillreminder.app.R
import com.pillreminder.app.alarm.AlarmService
import com.pillreminder.app.alarm.DoseMessage
import com.pillreminder.app.data.AppLanguage
import com.pillreminder.app.ui.DoneScreen
import com.pillreminder.app.ui.components.AppIcons
import com.pillreminder.app.ui.components.Banner
import com.pillreminder.app.ui.components.BannerKind
import com.pillreminder.app.ui.components.MedicineIcon
import com.pillreminder.app.ui.components.PrimaryButton
import com.pillreminder.app.ui.components.SecondaryButton
import com.pillreminder.app.ui.components.StatusBadge
import com.pillreminder.app.ui.dayAndTime
import com.pillreminder.app.ui.rememberLongDate
import com.pillreminder.app.ui.rememberTimeFormatter
import com.pillreminder.app.ui.theme.AppColors

@Composable
fun HomeScreen(
    language: AppLanguage,
    onAddPrescription: () -> Unit,
    onOpenMedicines: () -> Unit,
    onChangeLanguage: (AppLanguage) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val confirmation by viewModel.confirmation.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val colors = AppColors.current
    var choosingLanguage by remember { mutableStateOf(false) }

    confirmation?.let { done ->
        DoneScreen(
            medications = done.medications,
            takenAt = done.at,
            nextDose = state.nextDose,
            onDone = viewModel::dismissConfirmation,
        )
        return
    }

    Scaffold(containerColor = colors.background) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item { TopBar(language, onLanguage = { choosingLanguage = true }) }
            item { PermissionBanners() }
            item { Greeting(state) }

            state.nowSlot?.let { slot ->
                item {
                    NowCard(
                        slot = slot,
                        language = language,
                        onTaken = { viewModel.confirm(slot) },
                        onReadAloud = { viewModel.readAloud(slot) },
                    )
                }
            }
            if (state.nowSlot == null) {
                state.nextDose?.let { next -> item { NextDoseCard(state, next) } }
            }

            item {
                Text(
                    stringResource(R.string.home_today),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.ink,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp).semantics { heading() },
                )
            }
            if (state.today.isEmpty()) {
                item {
                    Text(
                        stringResource(if (state.medications.isEmpty()) R.string.home_empty else R.string.home_no_doses_today),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.muted,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
            items(state.today, key = { "slot-${it.time}" }) { TodayRow(it) }

            item { AllMedicinesButton(onOpenMedicines) }
            item {
                ForFamily(
                    onAddPrescription = onAddPrescription,
                    onTestAlarm = { ContextCompat.startForegroundService(context, AlarmService.testIntent(context)) },
                )
            }
            item {
                Text(
                    stringResource(R.string.home_disclaimer),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                    modifier = Modifier.padding(horizontal = 4.dp),
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
}

@Composable
private fun TopBar(language: AppLanguage, onLanguage: () -> Unit) {
    val colors = AppColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(40.dp).background(colors.accent, RoundedCornerShape(12.dp)),
        ) {
            Icon(AppIcons.Capsule, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            color = colors.ink,
            modifier = Modifier.weight(1f),
        )
        Surface(
            onClick = onLanguage,
            shape = RoundedCornerShape(24.dp),
            color = colors.surface,
            contentColor = colors.ink,
            border = BorderStroke(1.5.dp, colors.border),
            modifier = Modifier.height(48.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                Icon(AppIcons.Globe, contentDescription = stringResource(R.string.language_title), modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(language.nativeName, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun Greeting(state: HomeState) {
    val colors = AppColors.current
    val greeting = when (state.now.hour) {
        in 0..11 -> R.string.greeting_morning
        in 12..18 -> R.string.greeting_afternoon
        else -> R.string.greeting_evening
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(horizontal = 4.dp)) {
        Text(
            rememberLongDate(state.now.toLocalDate()),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 22.sp),
            color = colors.muted,
        )
        Text(
            stringResource(greeting),
            style = MaterialTheme.typography.headlineLarge,
            color = colors.ink,
            modifier = Modifier.semantics { heading() },
        )
    }
}

/** The dose to take now: what to take and one big button. */
@Composable
private fun NowCard(slot: TodaySlot, language: AppLanguage, onTaken: () -> Unit, onReadAloud: () -> Unit) {
    val colors = AppColors.current
    val timeFormat = rememberTimeFormatter()
    val shape = RoundedCornerShape(28.dp)
    Surface(
        shape = shape,
        color = colors.surface,
        contentColor = colors.ink,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, shape, ambientColor = colors.accent, spotColor = colors.accent),
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().background(colors.accent).padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                Icon(AppIcons.Bell, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.now_at, timeFormat.format(slot.time)),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                    color = Color.White,
                )
            }
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                slot.medications.forEach { med ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        MedicineIcon(med, size = 64.dp)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(med.name, style = MaterialTheme.typography.headlineSmall.copy(fontSize = 30.sp, lineHeight = 34.sp))
                            Text(med.dose, style = MaterialTheme.typography.titleMedium.copy(fontSize = 24.sp))
                            if (med.instructions.isNotBlank()) {
                                Text(med.instructions, style = MaterialTheme.typography.bodyLarge, color = colors.muted)
                            }
                        }
                    }
                }
                PrimaryButton(
                    text = DoseMessage.confirmLabel(slot.medications.size, language),
                    onClick = onTaken,
                    icon = AppIcons.Check,
                    height = 88.dp,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(fontSize = 30.sp),
                )
                SecondaryButton(
                    text = stringResource(R.string.read_aloud),
                    onClick = onReadAloud,
                    icon = AppIcons.Speaker,
                    height = 56.dp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun NextDoseCard(state: HomeState, next: java.time.LocalDateTime) {
    val colors = AppColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(22.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Icon(AppIcons.Clock, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(30.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text(stringResource(R.string.done_next), style = MaterialTheme.typography.bodyMedium, color = colors.muted)
            Text(
                dayAndTime(next, state.now.toLocalDate(), rememberTimeFormatter()),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                color = colors.ink,
            )
        }
    }
}

@Composable
private fun TodayRow(slot: TodaySlot) {
    val colors = AppColors.current
    val (clock, suffix) = rememberTimeFormatter().parts(slot.time)
    val isNow = slot.status == SlotStatus.NOW
    val shape = RoundedCornerShape(22.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isNow) colors.tint else colors.surface, shape)
            .then(if (isNow) Modifier.border(2.dp, colors.accentText, shape) else Modifier)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Column(Modifier.width(88.dp)) {
            Text(clock, style = MaterialTheme.typography.titleLarge, color = colors.ink)
            if (suffix != null) Text(suffix, style = MaterialTheme.typography.bodyMedium, color = colors.muted)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            slot.medications.forEach {
                Text("${it.name} · ${it.dose}", style = MaterialTheme.typography.titleSmall, color = colors.ink)
            }
            StatusBadge(slot.status)
        }
    }
}

@Composable
private fun AllMedicinesButton(onClick: () -> Unit) {
    val colors = AppColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = colors.strong,
        contentColor = colors.onStrong,
        modifier = Modifier.fillMaxWidth().height(72.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 22.dp)) {
            Icon(AppIcons.List, contentDescription = null, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.all_medicines),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                modifier = Modifier.weight(1f),
            )
            Icon(AppIcons.Forward, contentDescription = null, modifier = Modifier.size(26.dp))
        }
    }
}

/** Setup tasks, kept visually quiet so they don't compete with taking medicines. */
@Composable
private fun ForFamily(onAddPrescription: () -> Unit, onTestAlarm: () -> Unit) {
    val colors = AppColors.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HorizontalDivider(thickness = 2.dp, color = colors.border)
        Text(
            stringResource(R.string.for_family),
            style = MaterialTheme.typography.labelMedium,
            color = colors.muted,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton(
                text = stringResource(R.string.family_prescription),
                onClick = onAddPrescription,
                icon = AppIcons.Plus,
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(
                text = stringResource(R.string.home_test_alarm),
                onClick = onTestAlarm,
                icon = AppIcons.Bell,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LanguageDialog(current: AppLanguage, onChoose: (AppLanguage) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.current.surface,
        title = { Text(stringResource(R.string.language_title), style = MaterialTheme.typography.headlineSmall) },
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
            PermissionBanner(stringResource(R.string.perm_notifications)) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (needsFullScreen) {
            PermissionBanner(stringResource(R.string.perm_full_screen)) {
                context.openSettings(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
            }
        }
        if (needsExact) {
            PermissionBanner(stringResource(R.string.perm_exact_alarm)) {
                context.openSettings(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            }
        }
    }
}

@Composable
private fun PermissionBanner(text: String, onFix: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Banner(text, BannerKind.WARNING)
        SecondaryButton(text = stringResource(R.string.perm_fix), onClick = onFix, accent = true, modifier = Modifier.fillMaxWidth())
    }
}

private fun Context.openSettings(action: String) {
    startActivity(Intent(action, Uri.parse("package:$packageName")))
}
