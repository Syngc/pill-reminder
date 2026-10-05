package com.pillreminder.app

import com.pillreminder.app.ui.intervalMinutes
import com.pillreminder.app.ui.replaceTime
import com.pillreminder.app.ui.shiftAll
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimesEditorTest {
    @Test fun replacingKeepsTheListSorted() {
        assertEquals(listOf("07:30", "20:00"), listOf("08:00", "20:00").replaceTime("08:00", "07:30"))
        assertEquals(listOf("08:00", "22:00"), listOf("08:00", "20:00").replaceTime("20:00", "22:00"))
    }

    @Test fun replacingWithAnExistingTimeMergesThem() {
        assertEquals(listOf("20:00"), listOf("08:00", "20:00").replaceTime("08:00", "20:00"))
    }

    @Test fun detectsEvenIntervals() {
        assertEquals(8 * 60, listOf("06:00", "14:00", "22:00").intervalMinutes())
        assertEquals(12 * 60, listOf("08:00", "20:00").intervalMinutes())
        assertEquals(6 * 60, listOf("00:00", "06:00", "12:00", "18:00").intervalMinutes())
        // Wraps past midnight.
        assertEquals(8 * 60, listOf("07:00", "15:00", "23:00").intervalMinutes())
    }

    @Test fun unevenOrSingleTimesHaveNoInterval() {
        assertNull(listOf("08:00", "21:00").intervalMinutes())
        assertNull(listOf("08:00", "13:00", "21:00").intervalMinutes())
        assertNull(listOf("08:00").intervalMinutes())
        assertNull(emptyList<String>().intervalMinutes())
    }

    @Test fun shiftingTheFirstTimeMovesTheRest() {
        assertEquals(listOf("07:00", "15:00", "23:00"), listOf("06:00", "14:00", "22:00").shiftAll("06:00", "07:00"))
    }

    @Test fun shiftingAnyTimeMovesAllByTheSameAmount() {
        assertEquals(listOf("07:00", "15:00", "23:00"), listOf("06:00", "14:00", "22:00").shiftAll("22:00", "23:00"))
    }

    @Test fun shiftingWrapsAroundMidnight() {
        assertEquals(listOf("00:30", "08:30", "16:30"), listOf("06:00", "14:00", "22:00").shiftAll("22:00", "00:30"))
        assertEquals(listOf("05:00", "17:00"), listOf("08:00", "20:00").shiftAll("08:00", "05:00"))
    }

    @Test fun shiftingKeepsTheInterval() {
        val shifted = listOf("08:00", "16:00", "00:00").shiftAll("08:00", "06:15")
        assertEquals(listOf("06:15", "14:15", "22:15"), shifted)
        assertEquals(8 * 60, shifted.intervalMinutes())
    }
}
