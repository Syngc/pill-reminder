package com.pillreminder.app

import com.pillreminder.app.alarm.AlarmScheduler
import com.pillreminder.app.data.DoseLog
import com.pillreminder.app.data.DoseStatus
import com.pillreminder.app.data.Medication
import com.pillreminder.app.ui.home.HomeViewModel
import com.pillreminder.app.ui.home.SlotStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class SchedulingTest {
    private val today = LocalDate.of(2026, 10, 5)

    private fun med(
        id: Long = 1,
        times: List<String> = listOf("08:00", "20:00"),
        start: LocalDate = today,
        days: Int? = null,
    ) = Medication(id, "Losartán", "50 mg", times, "", start, days)

    @Test fun activeWindowIsInclusiveOfStartAndExclusiveOfEnd() {
        val m = med(days = 3)
        assertFalse(m.isActiveOn(today.minusDays(1)))
        assertTrue(m.isActiveOn(today))
        assertTrue(m.isActiveOn(today.plusDays(2)))
        assertFalse(m.isActiveOn(today.plusDays(3)))
    }

    @Test fun nextOccurrenceIsLaterToday() {
        val next = AlarmScheduler.nextOccurrence(listOf(med()), "20:00", today.atTime(9, 0))
        assertEquals(today.atTime(20, 0), next)
    }

    @Test fun nextOccurrenceRollsToTomorrowOncePassed() {
        val next = AlarmScheduler.nextOccurrence(listOf(med()), "08:00", today.atTime(8, 0))
        assertEquals(today.plusDays(1).atTime(8, 0), next)
    }

    @Test fun nextOccurrenceIsNullAfterTreatmentEnds() {
        val m = med(days = 1)
        assertNull(AlarmScheduler.nextOccurrence(listOf(m), "08:00", LocalDateTime.of(today, LocalTime.NOON)))
    }

    @Test fun nextOccurrenceWaitsForFutureStart() {
        val m = med(start = today.plusDays(3))
        assertEquals(today.plusDays(3).atTime(8, 0), AlarmScheduler.nextOccurrence(listOf(m), "08:00", today.atTime(7, 0)))
    }

    @Test fun nextOccurrenceIgnoresMedicinesAtOtherTimes() {
        assertNull(AlarmScheduler.nextOccurrence(listOf(med(times = listOf("08:00"))), "13:00", today.atTime(7, 0)))
    }

    @Test fun todaySlotsGroupMedicinesAndReflectLogs() {
        val a = med(id = 1, times = listOf("08:00", "20:00"))
        val b = med(id = 2, times = listOf("08:00"))
        val logs = listOf(
            DoseLog(1, 1, today, "08:00", DoseStatus.TAKEN),
            DoseLog(2, 2, today, "08:00", DoseStatus.TAKEN),
        )
        val slots = HomeViewModel.todaySlots(listOf(a, b), logs, today, LocalTime.of(12, 0))
        assertEquals(listOf("08:00", "20:00"), slots.map { it.time })
        assertEquals(2, slots[0].medications.size)
        assertEquals(SlotStatus.TAKEN, slots[0].status)
        assertEquals(SlotStatus.UPCOMING, slots[1].status)
    }

    @Test fun slotWithAnyMissedDoseShowsMissed() {
        val logs = listOf(DoseLog(1, 1, today, "08:00", DoseStatus.MISSED))
        val slots = HomeViewModel.todaySlots(listOf(med()), logs, today, LocalTime.of(12, 0))
        assertEquals(SlotStatus.MISSED, slots[0].status)
    }

    @Test fun pastSlotWithoutLogIsPending() {
        val slots = HomeViewModel.todaySlots(listOf(med()), emptyList(), today, LocalTime.of(12, 0))
        assertEquals(SlotStatus.PENDING, slots[0].status)
    }
}
