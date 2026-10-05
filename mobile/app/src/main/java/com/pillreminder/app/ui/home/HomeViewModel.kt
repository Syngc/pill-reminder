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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

enum class SlotStatus { TAKEN, MISSED, PENDING, UPCOMING }

data class TodaySlot(val time: String, val medications: List<Medication>, val status: SlotStatus)

data class HomeState(
    val now: LocalDateTime = LocalDateTime.now(),
    val today: List<TodaySlot> = emptyList(),
    val medications: List<Medication> = emptyList(),
    /** Next scheduled dose per medicine id; missing when the treatment has ended. */
    val nextDoses: Map<Long, LocalDateTime> = emptyMap(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: MedicationRepository,
    private val scheduler: AlarmScheduler,
) : ViewModel() {
    // Ticks so statuses move from "later" to "pending" and the day rolls over at midnight
    // while the screen stays open.
    private val clock = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(CLOCK_TICK_MILLIS)
        }
    }

    private val todaysLogs = clock.map { it.toLocalDate() }.distinctUntilChanged()
        .flatMapLatest { repository.observeDoseLogs(it) }

    val state: StateFlow<HomeState> =
        combine(clock, repository.observeMedications(), todaysLogs) { now, meds, logs ->
            HomeState(
                now = now,
                today = todaySlots(meds, logs, now.toLocalDate(), now.toLocalTime()),
                medications = meds,
                nextDoses = meds.mapNotNull { med -> nextDose(med, now)?.let { med.id to it } }.toMap(),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    fun updateTimes(medication: Medication, times: List<String>) {
        viewModelScope.launch {
            repository.updateTimes(medication.id, times)
            scheduler.rescheduleAll()
        }
    }

    fun delete(medication: Medication) {
        viewModelScope.launch {
            repository.delete(medication.id)
            scheduler.rescheduleAll()
        }
    }

    companion object {
        private const val CLOCK_TICK_MILLIS = 30_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as PillReminderApp
                HomeViewModel(app.repository, app.scheduler)
            }
        }

        /**
         * Whether [medication] matches a search, ignoring case and accents so "acetaminofen"
         * finds "Acetaminofén". Searches the name and the instructions.
         */
        fun matchesSearch(medication: Medication, query: String): Boolean {
            val needle = query.normalizedForSearch()
            if (needle.isEmpty()) return true
            return medication.name.normalizedForSearch().contains(needle) ||
                medication.instructions.normalizedForSearch().contains(needle)
        }

        private fun String.normalizedForSearch(): String =
            Normalizer.normalize(trim(), Normalizer.Form.NFD).replace(COMBINING_MARKS, "").lowercase(Locale.ROOT)

        private val COMBINING_MARKS = Regex("\\p{Mn}+")

        /** When this medicine's alarm will next ring, or null when its treatment has ended. */
        fun nextDose(medication: Medication, now: LocalDateTime): LocalDateTime? =
            medication.times.mapNotNull { AlarmScheduler.nextOccurrence(listOf(medication), it, now) }.minOrNull()

        fun todaySlots(
            medications: List<Medication>,
            logs: List<DoseLog>,
            date: LocalDate,
            now: LocalTime,
        ): List<TodaySlot> {
            // Logged doses stay in today's history even if their time was edited afterwards.
            val times = (medications.flatMap { it.times } + logs.map { it.time }).distinct().sorted()
            return times.mapNotNull { time ->
                val due = medications.filter { med ->
                    med.isDueAt(date, time) || logs.any { it.medicationId == med.id && it.time == time }
                }
                if (due.isEmpty()) return@mapNotNull null
                val slotLogs = logs.filter { it.time == time }
                val status = when {
                    slotLogs.isNotEmpty() && slotLogs.all { it.status == DoseStatus.TAKEN } -> SlotStatus.TAKEN
                    slotLogs.any { it.status == DoseStatus.MISSED } -> SlotStatus.MISSED
                    slotLogs.isNotEmpty() || !LocalTime.parse(time).isAfter(now) -> SlotStatus.PENDING
                    else -> SlotStatus.UPCOMING
                }
                TodaySlot(time, due, status)
            }
        }
    }
}
