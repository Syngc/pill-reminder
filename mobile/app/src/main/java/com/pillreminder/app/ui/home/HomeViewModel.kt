package com.pillreminder.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.pillreminder.app.PillReminderApp
import com.pillreminder.app.data.DoseLog
import com.pillreminder.app.data.DoseStatus
import com.pillreminder.app.data.Medication
import com.pillreminder.app.data.MedicationRepository
import com.pillreminder.app.alarm.AlarmScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

enum class SlotStatus { TAKEN, MISSED, PENDING, UPCOMING }

data class TodaySlot(val time: String, val medications: List<Medication>, val status: SlotStatus)

data class HomeState(
    val today: List<TodaySlot> = emptyList(),
    val medications: List<Medication> = emptyList(),
)

class HomeViewModel(
    private val repository: MedicationRepository,
    private val scheduler: AlarmScheduler,
) : ViewModel() {
    private val date = LocalDate.now()

    val state: StateFlow<HomeState> =
        combine(repository.observeMedications(), repository.observeDoseLogs(date)) { meds, logs ->
            HomeState(today = todaySlots(meds, logs, date, LocalTime.now()), medications = meds)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    fun delete(medication: Medication) {
        viewModelScope.launch {
            repository.delete(medication.id)
            scheduler.rescheduleAll()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as PillReminderApp
                HomeViewModel(app.repository, app.scheduler)
            }
        }

        fun todaySlots(
            medications: List<Medication>,
            logs: List<DoseLog>,
            date: LocalDate,
            now: LocalTime,
        ): List<TodaySlot> {
            val active = medications.filter { it.isActiveOn(date) }
            return active.flatMap { it.times }.distinct().sorted().map { time ->
                val slotLogs = logs.filter { it.time == time }
                val status = when {
                    slotLogs.isNotEmpty() && slotLogs.all { it.status == DoseStatus.TAKEN } -> SlotStatus.TAKEN
                    slotLogs.any { it.status == DoseStatus.MISSED } -> SlotStatus.MISSED
                    slotLogs.isNotEmpty() || !LocalTime.parse(time).isAfter(now) -> SlotStatus.PENDING
                    else -> SlotStatus.UPCOMING
                }
                TodaySlot(time, active.filter { time in it.times }, status)
            }
        }
    }
}
