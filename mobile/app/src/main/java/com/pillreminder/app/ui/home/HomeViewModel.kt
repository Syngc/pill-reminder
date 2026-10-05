package com.pillreminder.app.ui.home

import android.media.AudioAttributes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pillreminder.app.PillReminderApp
import com.pillreminder.app.alarm.AlarmScheduler
import com.pillreminder.app.alarm.AlarmService
import com.pillreminder.app.alarm.DoseMessage
import com.pillreminder.app.alarm.Speaker
import com.pillreminder.app.data.DoseLog
import com.pillreminder.app.data.DoseStatus
import com.pillreminder.app.data.Medication
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

/** How a dose looks on the home screen: always shown as icon + word + color. */
enum class SlotStatus { TAKEN, NOW, MISSED, UPCOMING }

data class TodaySlot(val time: String, val medications: List<Medication>, val status: SlotStatus)

data class HomeState(
    val now: LocalDateTime = LocalDateTime.now(),
    val today: List<TodaySlot> = emptyList(),
    val medications: List<Medication> = emptyList(),
    /** Next scheduled dose per medicine id; missing when the treatment has ended. */
    val nextDoses: Map<Long, LocalDateTime> = emptyMap(),
) {
    /** The dose to take right now, shown in the big card at the top. */
    val nowSlot: TodaySlot? get() = today.firstOrNull { it.status == SlotStatus.NOW }

    /** The next alarm across all medicines. */
    val nextDose: LocalDateTime? get() = nextDoses.values.minOrNull()
}

/** Shown on the "Well done" screen after a dose is confirmed from the home screen. */
data class Confirmation(val medications: List<Medication>, val at: LocalDateTime)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(private val app: PillReminderApp) : ViewModel() {
    private val repository = app.repository
    private val scheduler = app.scheduler

    // Ticks so statuses move from "later" to "now" and the day rolls over at midnight
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

    private val _confirmation = MutableStateFlow<Confirmation?>(null)
    val confirmation: StateFlow<Confirmation?> = _confirmation.asStateFlow()

    // Normal media volume: the person is looking at the screen, unlike when an alarm rings.
    private val speaker by lazy { Speaker(app, app.language, AudioAttributes.USAGE_MEDIA) }
    private var reading: Job? = null

    /** "I took it" on the home screen's "Now" card. */
    fun confirm(slot: TodaySlot) {
        val date = state.value.now.toLocalDate()
        val ringing = AlarmService.current.value
        if (ringing != null && !ringing.isTest && ringing.date == date && ringing.time == slot.time) {
            // The alarm for this dose is ringing: let it stop, log and say "well done".
            app.startService(AlarmService.takenIntent(app))
        } else {
            viewModelScope.launch {
                repository.confirmTaken(date, slot.time, slot.medications)
                scheduler.cancelRetry(slot.time)
            }
        }
        _confirmation.value = Confirmation(slot.medications, LocalDateTime.now())
    }

    fun dismissConfirmation() {
        _confirmation.value = null
    }

    fun readAloud(slot: TodaySlot) {
        reading?.cancel()
        reading = viewModelScope.launch { speaker.speak(DoseMessage.spoken(slot.medications, app.language)) }
    }

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

    override fun onCleared() {
        reading?.cancel()
        speaker.shutdown()
    }

    companion object {
        private const val CLOCK_TICK_MILLIS = 30_000L

        /** A dose whose time passed this long ago without an answer is shown as not confirmed. */
        private val NOW_WINDOW: Duration = Duration.ofMinutes(60)

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(this[APPLICATION_KEY] as PillReminderApp) }
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
                TodaySlot(time, due, statusOf(LocalTime.parse(time), logs.filter { it.time == time }, now))
            }
        }

        private fun statusOf(time: LocalTime, slotLogs: List<DoseLog>, now: LocalTime): SlotStatus = when {
            slotLogs.isNotEmpty() && slotLogs.all { it.status == DoseStatus.TAKEN } -> SlotStatus.TAKEN
            slotLogs.any { it.status == DoseStatus.MISSED } -> SlotStatus.MISSED
            // Ringing, or waiting for one of the alarm's retries.
            slotLogs.any { it.status == DoseStatus.PENDING } -> SlotStatus.NOW
            time.isAfter(now) -> SlotStatus.UPCOMING
            Duration.between(time, now) < NOW_WINDOW -> SlotStatus.NOW
            else -> SlotStatus.MISSED
        }
    }
}
